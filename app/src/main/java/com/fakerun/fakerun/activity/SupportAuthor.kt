package com.fakerun.fakerun.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.fakerun.fakerun.R

class SupportAuthor : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support_author)


        findViewById<Button>(R.id.support_author_back).setOnClickListener { finish() }
        findViewById<Button>(R.id.support_author_github).setOnClickListener {
            val uri = Uri.parse("https://github.com/FlowerBlackG/FakeRun")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        findViewById<Button>(R.id.support_author_qrcode_wxpay).setOnClickListener {
            showBigQrCode(R.drawable.wxpay)
        }
        findViewById<Button>(R.id.support_author_qrcode_alipay).setOnClickListener {
            showBigQrCode(R.drawable.alipay)
        }
        findViewById<Button>(R.id.support_author_qrcode_wxreward).setOnClickListener {
            showBigQrCode(R.drawable.wxreward)
        }
    }

    fun showBigQrCode(@DrawableRes id: Int) {
        val imgView = ImageView(this)

        val drawable = resources.getDrawable(id)
        imgView.setImageDrawable(drawable)

        AlertDialog.Builder(this)
            .setView(imgView)
            .setPositiveButton("关闭") { dialog, which -> dialog.dismiss() }
            .show()
    }

}