package com.atheris.qrcodepass.qrcode

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.datamatrix.DataMatrixWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.net.URLDecoder

var logd = {msg:String-> Log.d("mtag",msg)}
open class QrGetter(val context: Context, val sharedPrefName : String,val width:Int){
    fun setQr(imageView : ImageView,qrNo:Int=0){
        getQr(qrNo)
        //logd("after get qr $qrNo")
        if(isInitialised(qrNo)) {
            imageView.setImageBitmap(mImages[qrNo])
            imageView.imageAlpha=255
        }else{
            imageView.imageAlpha=0
        }
    }

    companion object{
        var mImages = HashMap<Int,Bitmap>()
        val mImage : Bitmap
            get(){
                return mImages[0]!!
            }

        var isInits = HashMap<Int,Boolean>()
        val isInit : Boolean
            get(){
                return isInitialised()
            }
        fun isInitialised(noQr:Int=0):Boolean{
            return isInits.containsKey(noQr) && isInits[noQr]!!
        }
        fun keyOf(qrNo: Int=0):String{
            return """qrCode${if (qrNo>0) qrNo else ""}"""
        }
    }


    fun getQr(qrNo:Int=0):Bitmap{
        if (!isInitialised(qrNo)){
            //logd("not init ${keyOf(qrNo)} $qrNo")
            val qrCodeString = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getString(keyOf(qrNo),"")!!
            //logd("post shared pref")
            //logd(qrCodeString)
            if (!qrCodeString.isNullOrEmpty()) {
                updateQr(qrCodeString,qrNo)
                //logd("update succeed")
                isInits[qrNo] = true

                return mImages[qrNo]!!
            }


            return BitmapFactory.decodeResource(context.resources, R.mipmap.qrcode_default)
        }
        return mImages[qrNo]!!
    }

    open fun widgetUpdate(){

    }

    fun updateQr(ivalue:String,qrNo: Int=0) {

        var value = ivalue

        if (value.contains('#')){
            value = value.slice(value.findAnyOf(listOf("#"))?.first!!+1 until value.length)
            value = URLDecoder.decode(value)
        }

        val sharedPref = context.getSharedPreferences(sharedPrefName,Context.MODE_PRIVATE)?: return
        with (sharedPref.edit()) {
            putString(keyOf(qrNo), value)
            apply()
        }
        mImages[qrNo] = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565)
        //Log.d("mtag",value);
        try {
            if (!value.isNullOrEmpty()) {
                val qrCodeWriter = if (isEU(qrNo)) {
                    QRCodeWriter()
                } else {
                    DataMatrixWriter()
                }
                val bitMatrix = qrCodeWriter.encode(
                    value,
                    if (isEU(qrNo)) {
                        BarcodeFormat.QR_CODE
                    } else {
                        BarcodeFormat.DATA_MATRIX
                    },
                    width, width,
                    mapOf(Pair(EncodeHintType.ERROR_CORRECTION,ErrorCorrectionLevel.Q),Pair(EncodeHintType.MARGIN,1))
                )

                for (i in 0 until width) {
                    for (j in 0 until width) {
                        mImages[qrNo]!!.setPixel(i, j, if (bitMatrix[i, j]) 0 else 0xFFFFFF)

                    }
                }

            } else {
                isInits[qrNo] = false;
            }
        }catch (e:IllegalArgumentException){
            isInits[qrNo]=false
            e.printStackTrace()
            Toast.makeText(context,"I'm sorry, exception raised, bad qrcode \n $e",Toast.LENGTH_LONG).show()
        }
        //request a widget update if we update the first qr code
        if (qrNo==0)
            widgetUpdate()

    }

    fun isEU(qrNo:Int) : Boolean{
        var start = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getString(keyOf(qrNo), "")!!
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