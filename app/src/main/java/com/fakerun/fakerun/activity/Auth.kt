package com.fakerun.fakerun.activity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.fakerun.fakerun.R
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.concurrent.thread

class Auth : Activity() {
    private lateinit var formView: LinearLayout
    private lateinit var loadingView: ProgressBar
    private lateinit var keyView: EditText

    private lateinit var sp: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        sp = getSharedPreferences("FakeRun", Context.MODE_PRIVATE)

        loadingView = findViewById(R.id.auth_loading)
        formView = findViewById(R.id.auth_form)

        keyView = findViewById(R.id.auth_key)
        keyView.text = SpannableStringBuilder(sp.getString("keycode", ""))

        checkUpdate()

        showVersionInfo()
        requestPermissions()

        initButtons()

        initDisclaimerBtn()

    }

    private fun initButtons() {
        findViewById<Button>(R.id.auth_confirm).setOnClickListener {
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }

        findViewById<Button>(R.id.auth_tutor_addMockPermission).setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle("这么做")
                    .setMessage("· 打开设置\n· 找到\"关于\"\n· 找到显示\"版本号\"的地方\n· 不断戳\"版本号\"，直到系统告诉你开发者模式已经启用\n· 回到设置页面，找到开发者设置菜单\n· 在菜单中找到\"虚拟定位应用\"选项\n· 在那里把本app设为默认应用")
                    .setPositiveButton("好的", null)
                    .create()
                    .show()
        }

        findViewById<Button>(R.id.auth_supportAuthor).setOnClickListener {
            val intent = Intent(this, SupportAuthor::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun showVersionInfo() {
        var versionInfoStr = "版本：${packageManager.getPackageInfo(packageName, 0).versionName}"
        versionInfoStr += " (${packageManager.getPackageInfo(packageName, 0).longVersionCode})"
        findViewById<TextView>(R.id.auth_version).text = versionInfoStr
    }

    private val VIEW_LOADING = 0
    private val VIEW_FORM = 1
    private fun switchViews(views: Int) {
        if (views == VIEW_FORM) {
            loadingView.visibility = ProgressBar.GONE
            formView.visibility = LinearLayout.VISIBLE
        } else if (views == VIEW_LOADING) {
            loadingView.visibility = ProgressBar.VISIBLE
            formView.visibility = LinearLayout.INVISIBLE
        }
    }

    private fun requestLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            val perArr : Array<String> = Array(2) { "" }
            perArr[0] = Manifest.permission.ACCESS_COARSE_LOCATION
            perArr[1] = Manifest.permission.ACCESS_FINE_LOCATION
            requestPermissions(perArr, PERMISSION_REQUEST_LOCATION)
        }
    }

    private fun requestPermissions() {
        requestLocationPermission()

        if (!isIgnoringBatteryOptimizations()) {
            requestIgnoreBatteryOptimizations()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            fun requirePostNotificationPermission() = requestPermissions(arrayOf(
                Manifest.permission.POST_NOTIFICATIONS
            ), 0)

            val POST_MSG_FIRED_KEY = "___post msg request fired"

            if ( ! sp.getBoolean(POST_MSG_FIRED_KEY, false)) {

                // 发送通知权限。
                AlertDialog.Builder(this)
                    .setTitle("通知权限")
                    .setMessage("跑吗需要在后台运行。安卓系统后台需要通知权限。请给我权限~~")
                    .setCancelable(false)
                    .setPositiveButton("好的") { dialog, which ->
                        requirePostNotificationPermission()
                    }
                    .show()

                sp.edit().putBoolean(POST_MSG_FIRED_KEY, true).apply()

            } else {

                requirePostNotificationPermission()

            }

        }

    }

    private fun isIgnoringBatteryOptimizations() : Boolean {
        var isIgnoring = false
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager != null) {
            isIgnoring = powerManager.isIgnoringBatteryOptimizations(packageName)
        }
        Log.d("Auth.isIgnoringBatteryOptimizations", "res=" + if (isIgnoring) "true" else "false")
        return isIgnoring
    }

    private fun requestIgnoreBatteryOptimizations() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {

        }
    }

    private val PERMISSION_REQUEST_LOCATION = 1
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

    private fun checkUpdate() {
        val cloudApiRoot = "https://www.gardilily.com/fakeRun/api/"
        thread {
            var tarUrl = "${cloudApiRoot}checkUpdate.php"
            tarUrl += "?version=${packageManager.getPackageInfo(packageName, 0).longVersionCode}"
            val client = OkHttpClient()
            val request: Request = Request.Builder()
                    .url(tarUrl)
                    .build()
            val response: Response = client.newCall(request).execute()
            if (response.code == 200) {
                // success
                val result = response.body?.string()

                if (result.equals("1")) {
                    return@thread
                }

                val resArr = result?.split("_div_")
                runOnUiThread {
                    var msg = "版本：${resArr!![2]}\n"
                    msg += "时间：${resArr[3]}\n"
                    msg += "说明：\n\n${resArr[5]}\n"

                    AlertDialog.Builder(this)
                            .setTitle("有新版本可用")
                            .setMessage(msg)
                            .setPositiveButton("更新") { _, _ ->
                                val uri = Uri.parse(resArr[4])
                                startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                            .setNegativeButton("取消", null)
                            .show()
                }
            }
        }
    }

    private fun initDisclaimerBtn() {
        val msg = "本程序仅可用于地理学习与地图开发测试等必要情境，请勿将其用于任何违规违法活动。\n" +
                "违反此忠告者，产生的一切后果自负。本程序设计者及相关研发人员拒绝承担任何责任。"

        findViewById<Button>(R.id.auth_disclaimerShow).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("免责声明")
                .setMessage(msg)
                .setPositiveButton("好", null)
                .show()
        }
    }
}
