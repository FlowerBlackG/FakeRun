package com.fakerun.fakerun.activity

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.fakerun.fakerun.R
import com.gardilily.common.view.card.InfoCard
import org.json.JSONObject

class MapSelector : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_selector)

        loadMapData(
            findViewById(R.id.mapSelector_scrollView), findViewById(R.id.mapSelector_verShow)
        )
    }

    private fun loadMapData(tarList: LinearLayout, versionView: TextView) {
        val mapStoreObj = JSONObject(getSharedPreferences("FakeRun", MODE_PRIVATE).getString(
            "map data",
            "{\"mapLibVer\":0,\"data\":[]}"
        )!!)
        versionView.text = "地图库版本：${mapStoreObj.getInt("mapLibVer")}"

        val mapList = mapStoreObj.getJSONArray("data")
        for (i in 0 until mapList.length()) {
            val mapObj = mapList.getJSONObject(i)
            val card = InfoCard.Builder(this)
                .setSpMultiply(resources.displayMetrics.scaledDensity)
                .setCardBackground(getDrawable(R.drawable.auth_btn_background))
                .setHasIcon(true)
                .setIcon(mapObj.getString("icon"))
                .setIconTextSizeSp(24f)
                .setHasEndMark(false)
                .setInnerMarginStartSp(16f)
                .setInnerMarginBetweenSp(16f)
                .setOuterMarginTopSp(16f)
                .setOuterMarginBottomSp(0f)
                .setTitle(mapObj.getString("name"))
                .addInfo(InfoCard.Info("介绍", mapObj.getString("desc")))
                .build()
            card.setOnClickListener {
                setResult(RESULT_CODE_SUCCESS)
                getSharedPreferences("FakeRun", MODE_PRIVATE)
                    .edit()
                    .putInt("curr map id", mapObj.getInt("mapId"))
                    .commit()
                finish()
            }

            tarList.addView(card)
        }
    }
    companion object {
        const val RESULT_CODE_SUCCESS = 1
        const val RESULT_CODE_EXIT = RESULT_CANCELED // = 0
    }
}
