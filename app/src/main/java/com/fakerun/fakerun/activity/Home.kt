package com.fakerun.fakerun.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.fakerun.fakerun.R
import com.fakerun.fakerun.service.MockRun
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.concurrent.thread

class Home : Activity() {
    private lateinit var logView: TextView

    private val defaultSpeed = 2.5 // meters per second
    private val defaultDeltaT = 200 // ms

    private var speed = defaultSpeed // meters per second
    private var deltaT = defaultDeltaT // ms

    private lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sp = getSharedPreferences("FakeRun", Context.MODE_PRIVATE)

        speed = sp.getString("param speed", defaultSpeed.toString())!!.toDouble()
        deltaT = sp.getInt("param deltaT", defaultDeltaT)

        logView = findViewById(R.id.home_log)

        initFunctionButtons()

        callMockService(MockRun.MOCKRUN_ACTION_LIFE_CREATE)

        initMapSelect()
    }

    override fun onResume() {
        super.onResume()

        findViewById<EditText>(R.id.home_speed_edit).text = SpannableStringBuilder("$speed")
        findViewById<EditText>(R.id.home_deltaT_edit).text = SpannableStringBuilder("$deltaT")
    }

    private fun getCurrentSelectedMapId() : Int {
        val currMapId = getFakeRunSp()
            .getInt("curr map id", -1)
        val mapList = JSONObject(getFakeRunSp()
                .getString("map data", "{\"mapLibVer\":0,\"data\":[]}")!!
            ).getJSONArray("data")
        for (i in 0 until mapList.length()) {
            if (mapList.getJSONObject(i).getInt("mapId") == currMapId) {
                return currMapId
            }
        }
        return -1
    }

    private fun getMapNameById(id: Int) : String {
        val mapList = JSONObject(getSharedPreferences("FakeRun", MODE_PRIVATE)
            .getString("map data", "{\"mapLibVer\":0,\"data\":[]}")!!
        ).getJSONArray("data")
        for (i in 0 until mapList.length()) {
            if (mapList.getJSONObject(i).getInt("mapId") == id) {
                return mapList.getJSONObject(i).getString("name")
            }
        }
        return "无"
    }

    private fun updateSelectMapName() {
        findViewById<TextView>(R.id.home_map_mapName).text =
            getMapNameById(getCurrentSelectedMapId())
    }

    private fun getCurrentMapDataVersion(): Int {
        val mapStoreObj = JSONObject(
            getFakeRunSp().getString("map data", "{\"mapLibVer\":0,\"data\":[]}")!!
        )
        return mapStoreObj.getInt("mapLibVer")
    }

    val REQUEST_SELECT_MAP = 1
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_SELECT_MAP -> {
                if (resultCode == MapSelector.RESULT_CODE_SUCCESS) {
                    Toast.makeText(this,
                        "已选择地图：${getMapNameById(getCurrentSelectedMapId())}",
                        Toast.LENGTH_SHORT
                    ).show()

                    updateSelectMapName()
                }

            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun getFakeRunSp(): SharedPreferences {
        return getSharedPreferences("FakeRun", MODE_PRIVATE)
    }

    private fun initMapSelect() {
        //startActivity(Intent(this, MapSelector::class.java))
        updateSelectMapName()

        findViewById<Button>(R.id.home_map_editBtn).setOnClickListener {
            val intent = Intent(this, MapSelector::class.java)
            startActivityForResult(intent, REQUEST_SELECT_MAP)
        }

        thread {
            val client = OkHttpClient()

            val currMapVer = JSONObject(getSharedPreferences("FakeRun", MODE_PRIVATE)
                    .getString("map data", "{\"mapLibVer\":0,\"data\":[]}")!!)
                .getInt("mapLibVer")
            val uname = getSharedPreferences("FakeRun", MODE_PRIVATE).getString("keycode", "")!!

            val reqUrl = "https://www.gardilily.com/fakeRun/api/updateMapData.php" +
                    "?ac_key=B9D934C1D10F29B1C5201C84291133F4" +
                    "&curr_map_ver=" + currMapVer +
                    "&uname=" + defUrlEnc(uname)

            val req = Request.Builder()
                .url(reqUrl)
                .build()
            val response = client.newCall(req).execute()
            val resStr = response.body?.string()

            if (resStr == null) {
                runOnUiThread {
                    Toast.makeText(this, "无法从云端获取地图库...", Toast.LENGTH_SHORT).show()
                }
                return@thread
            }

            val resObj = JSONObject(resStr)
            when (resObj.getInt("code")) {
                1 -> {
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("地图库更新可用")
                            .setMessage("发现地图库数据更新。\n当前版本：${
                                getCurrentMapDataVersion()
                            }\n更新版本：${resObj.getInt("latestVer")}")
                            .setPositiveButton("好的") {_, _ ->
                                getFakeRunSp()
                                    .edit()
                                    .putString("map data", resObj.getJSONObject("data").toString())
                                    .commit()
                                updateSelectMapName()
                                Toast.makeText(this, "更新好啦～", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("不要", null)
                            .show()
                    }
                }
                2 -> {
                    // 强制更新
                    getFakeRunSp()
                        .edit()
                        .putString("map data", resObj.getJSONObject("data").toString())
                        .apply()
                    runOnUiThread {
                        AlertDialog.Builder(this)
                            .setTitle("地图库强制更新")
                            .setMessage("你的地图库已被强制更新。\n当前地图库版本：${resObj.getInt("latestVer")}")
                            .setPositiveButton("好的") {_, _ ->
                                updateSelectMapName()
                            }
                            .show()
                    }
                }
            }
        }
    }

    private fun getMapStoreObj(): JSONObject {
        val sp = getFakeRunSp()
        return JSONObject(sp.getString("map data", "{\"mapLibVer\":0,\"data\":[]}")!!)
    }

    private fun callMockService(action: Int) {
        val intent = Intent(this, MockRun::class.java)
        if (action == MockRun.MOCKRUN_ACTION_RUN_START) {
            intent.putExtra("speed", speed)
            intent.putExtra("deltaT", deltaT)

            val mapList = getMapStoreObj().getJSONArray("data")
            val currSelect = getCurrentSelectedMapId()
            if (currSelect == -1) {
                Toast.makeText(this, "启动失败。还没选择地图呢...", Toast.LENGTH_SHORT).show()
                return
            }

            for (i in 0 until mapList.length()) {
                val mapObj = mapList.getJSONObject(i)
                if (currSelect == mapObj.getInt("mapId")) {
                    intent.putExtra("mapData", mapObj.getJSONArray("points").toString())
                    break
                }
            }
        }
        intent.putExtra("action", action)
        startService(intent)
    }

    private fun initFunctionButtons() {
        findViewById<Button>(R.id.home_switch_on).setOnClickListener {
            Toast.makeText(this, "正在启动...", Toast.LENGTH_SHORT).show()
            updateEditableParams()
            callMockService(MockRun.MOCKRUN_ACTION_RUN_START)
            thread {
                Thread.sleep(80)
                // reAuth()
            }
        }

        findViewById<Button>(R.id.home_switch_off).setOnClickListener {
            Toast.makeText(this, "正在停止...", Toast.LENGTH_SHORT).show()
            callMockService(MockRun.MOCKRUN_ACTION_RUN_END)
        }

        findViewById<Button>(R.id.home_requestWhiteList).setOnClickListener {
            fun showActivity(packageName: String, activityDir: String) {
                val intent = Intent()
                intent.component = ComponentName(packageName, activityDir)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            fun showActivity(packageName: String) {
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                startActivity(intent)
            }

            if (Build.BRAND == null) {
                Toast.makeText(this, "异常", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val brandStr = Build.BRAND.toLowerCase()
            var helpText = "异常"

            if (brandStr == "huawei" || brandStr == "honor") {

                try {
                    showActivity("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                } catch (e: Exception) {
                    showActivity("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.bootstart.BootStartActivity")
                }

                helpText = "操作步骤：应用启动管理 -> 关闭应用开关 -> 打开允许自启动"

            } else if (brandStr == "xiaomi") {
                showActivity("com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity")

                helpText = "操作步骤：授权管理 -> 自启动管理 -> 允许应用自启动"

            } else if (brandStr == "oppo") {
                try {
                    showActivity("com.coloros.phonemanager")
                } catch (e1: Exception) {
                    try {
                        showActivity("com.oppo.safe")
                    } catch (e2: Exception) {
                        try {
                            showActivity("com.coloros.oppoguardelf")
                        } catch (e3: Exception) {
                            showActivity("com.coloros.safecenter")
                        }
                    }
                }

                helpText = "操作步骤：权限隐私 -> 自启动管理 -> 允许应用自启动"
            } else if (brandStr == "vivo") {
                showActivity("com.iqoo.secure")
                helpText = "操作步骤：权限管理 -> 自启动 -> 允许应用自启动"
            } else if (brandStr == "meizu") {
                showActivity("com.meizu.safe")
                helpText = "权限管理 -> 后台管理 -> 点击应用 -> 允许后台运行"
            } else if (brandStr == "samsung") {
                try {
                    showActivity("com.samsung.android.sm_cn")
                } catch (e: Exception) {
                    showActivity("com.samsung.android.sm")
                }
                helpText = "操作步骤：自动运行应用程序 -> 打开应用开关 -> 电池管理 -> 未监视的应用程序 -> 添加应用"
            }

            Toast.makeText(this, helpText, Toast.LENGTH_LONG).show()
        }

        findViewById<Button>(R.id.home_restoreParams).setOnClickListener {
            speed = defaultSpeed
            deltaT = defaultDeltaT

            val editSpeed = findViewById<EditText>(R.id.home_speed_edit)
            val editDeltaT = findViewById<EditText>(R.id.home_deltaT_edit)
            
            editSpeed.text = SpannableStringBuilder(speed.toString())
            editDeltaT.text = SpannableStringBuilder(deltaT.toString())

            Toast.makeText(this, "完成", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEditableParams() {
        val editSpeed = findViewById<EditText>(R.id.home_speed_edit)
        val editDeltaT = findViewById<EditText>(R.id.home_deltaT_edit)
        val strSpeed = editSpeed.text.toString()
        val strDeltaT = editDeltaT.text.toString()
        if (strSpeed.isNotEmpty()) {
            speed = strSpeed.toDouble()
            sp.edit().putString("param speed", strSpeed).apply()
        }

        if (strDeltaT.isNotEmpty()) {
            deltaT = strDeltaT.toInt()
            sp.edit().putInt("param deltaT", deltaT).apply()
        }
    }

    private fun reAuth() {
        val cloudApiRoot = "https://www.gardilily.com/fakeRun/api/"
        thread {
            var tarUrl = "${cloudApiRoot}auth.php"
            tarUrl += "?ac_key=" + "B9D934C1D10F29B1C5201C84291133F4"
            tarUrl += "&version=${packageManager.getPackageInfo(packageName, 0).longVersionCode}"
            tarUrl += "&keycode=${defUrlEnc(sp.getString("keycode", "_null"))}"
            tarUrl += "&device=${defUrlEnc(Build.BRAND + Build.MODEL)}"
            val client = OkHttpClient()
            val request: Request = Request.Builder()
                    .url(tarUrl)
                    .build()
            val response: Response = client.newCall(request).execute()
            if (response.code == 200) {
                // success
                val result = defUrlDec(response.body?.string())
                val resInt = result.toInt()

                if (resInt < 0) {
                    callMockService(MockRun.MOCKRUN_ACTION_LIFE_DESTROY)
                    runOnUiThread {
                        // 验证失败
                        Toast.makeText(this, "程序异常", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, Auth::class.java))
                        finish()
                    }
                }
            }
            else {
                callMockService(MockRun.MOCKRUN_ACTION_LIFE_DESTROY)
                runOnUiThread {
                    Toast.makeText(this, "网络异常", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, Auth::class.java))
                    finish()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        val intent = Intent(this, MockRun::class.java)
        intent.putExtra("action", MockRun.MOCKRUN_ACTION_LIFE_DESTROY)
        startService(intent)
    }

    /**
     * Encode a String with UTF-8 for better cloud storing
     *
     * @param str String to be encoded
     *
     * @return Encoded String
     */
    private fun defUrlEnc(str: String?): String {
        return URLEncoder.encode(str, "UTF-8")
    }

    /**
     * Decode an encoded String downloaded from the cloud with UTF-8
     *
     * @param str String to be decoded
     *
     * @return Decoded String
     */
    private fun defUrlDec(str: String?): String {
        return URLDecoder.decode(str, "UTF-8")
    }

}
