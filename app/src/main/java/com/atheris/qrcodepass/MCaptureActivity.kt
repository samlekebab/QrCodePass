package com.atheris.qrcodepass

import android.app.ActionBar
import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import com.journeyapps.barcodescanner.CaptureActivity
import com.atheris.qrcodepass.qrcode.logd

class MCaptureActivity : CaptureActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addContentView(layoutInflater.inflate(R.layout.top_of_scanner,null), ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT))
        with(findViewById<ImageButton>(R.id.torch)) {
            setImageResource(R.drawable.ic_baseline_flash_on_24)
            setOnClickListener(){
                setResult(MainActivity.ResultCodes.QR_TORCH_TOGGLE.ordinal)
                finish()
            }
        }
        with(findViewById<ImageButton>(R.id.qr_file)){
            setImageResource(R.drawable.ic_baseline_folder_24)
            setOnClickListener(){
                setResult(MainActivity.ResultCodes.QR_FILE_PICK.ordinal)
                finish()
            }
        }

    }
}