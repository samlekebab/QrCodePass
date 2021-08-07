package com.atheris.qrcodepass.qrcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.datamatrix.DataMatrixWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.lang.IllegalArgumentException
import java.net.URLDecoder

var logd = {msg:String-> Log.d("mtag",msg)}
open class QrGetter(val context: Context, val sharedPrefName : String,val width:Int){
    fun setQr(imageView : ImageView){
        getQr()
        if(isInit) {
            imageView.setImageBitmap(mImage)
            imageView.imageAlpha=255
        }else{
            imageView.imageAlpha=0
        }
    }
    companion object{
        lateinit var mImage : Bitmap
        var isInit=false
    }
    fun isInitialised():Boolean{
        return isInit
    }
    fun getQr():Bitmap{
        if (!isInit){
            //logd("not init")
            val qrCodeString = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getString("qrCode","")!!
            //logd("post shared pref")
            //logd(qrCodeString)
            if (!qrCodeString.isNullOrEmpty()) {
                updateQr(qrCodeString.toString())
                //logd("update succeed")
                isInit = true
                return mImage
            }


            return BitmapFactory.decodeResource(context.resources, R.mipmap.qrcode_default)
        }
        return mImage
    }

    open fun widgetUpdate(){

    }

    fun updateQr(ivalue:String) {

        var value = ivalue

        if (value.contains('#')){
            value = value.slice(value.findAnyOf(listOf("#"))?.first!!+1 until value.length)
            value = URLDecoder.decode(value)
        }

        val sharedPref = context.getSharedPreferences(sharedPrefName,Context.MODE_PRIVATE)?: return
        with (sharedPref.edit()) {
            putString("qrCode", value)
            apply()
        }
        mImage = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565)
        //Log.d("mtag",value);
        try {
            if (!value.isNullOrEmpty()) {
                val qrCodeWriter = if (isEU()) {
                    QRCodeWriter()
                } else {
                    DataMatrixWriter()
                }
                val bitMatrix = qrCodeWriter.encode(
                    value,
                    if (isEU()) {
                        BarcodeFormat.QR_CODE
                    } else {
                        BarcodeFormat.DATA_MATRIX
                    },
                    width, width
                )

                for (i in 0 until width) {
                    for (j in 0 until width) {
                        mImage.setPixel(i, j, if (bitMatrix[i, j]) 0 else 0xFFFFFF)

                    }
                }

            } else {
                isInit = false;
            }
        }catch (e:IllegalArgumentException){
            isInit=false
            e.printStackTrace()
            Toast.makeText(context,"I'm sorry, exception raised, bad qrcode \n $e",Toast.LENGTH_LONG).show()
        }
        //request a widget update
        widgetUpdate()

    }
    fun isEU() : Boolean{
        var start = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getString("qrCode", "")!!
        if (start.length>=4) {
            start = start.slice(0..3)
            //logd("$start")
            if (start == "HC1:" || start == "HC2:" || start == "HC3:" || start == "HC4:" ) {
                return true
            }
        }
        return false
    }
}