package com.atheris.qrcodepass.wear

import android.app.Activity
import android.os.Bundle
import com.atheris.qrcodepass.qrcode.QrGetter

//import com.atheris.qrcodepass.QrGetter
//import com.atheris.qrcodepass.dev.QrGetter


class MainActivity() : Activity() {
    private lateinit var qr: QrGetter;

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.main_activity)
        qr = QrGetter(this,"mySharedPref",200)
        qr.setQr(findViewById(R.id.qrcode))
        super.onCreate(savedInstanceState)
    }
}