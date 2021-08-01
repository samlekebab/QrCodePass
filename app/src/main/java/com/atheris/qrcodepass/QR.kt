package com.atheris.qrcodepass

import android.R
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.widget.ImageView
import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.model.Array
import co.nstant.`in`.cbor.model.ByteString
import com.google.zxing.BarcodeFormat
import com.google.zxing.datamatrix.DataMatrixWriter
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayInputStream
import java.lang.Exception
import java.util.*
import java.util.zip.Inflater
import java.net.URLDecoder

var logd = {msg:String->Log.d("mtag",msg)}

class QR(val context: Context) {
    companion object{
        lateinit var mImage : Bitmap
        var isInit=false
    }
    fun setQr(imageView : ImageView){
        getQr()
        if(isInit) {
            imageView.setImageBitmap(mImage)
            imageView.imageAlpha=255
        }else{
            imageView.imageAlpha=0
        }
    }
    fun isInitialised():Boolean{
        return isInit
    }
    fun getQr():Bitmap{
        if (!isInit){

            val qrCodeString = context.getSharedPreferences(sharedPrefName,Context.MODE_PRIVATE).getString("qrCode","")
            if (!qrCodeString.isNullOrEmpty()) {
                updateQr(qrCodeString.toString())
                isInit = true
                return mImage
            }

            return BitmapFactory.decodeResource(context.resources, com.atheris.qrcodepass.R.mipmap.ic_launcher_foreground)
        }
        return mImage
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
        if (!value.isNullOrEmpty()) {
            val qrCodeWriter = if (isEU()){QRCodeWriter()}else{DataMatrixWriter()}
            val bitMatrix = qrCodeWriter.encode(
                value,
                if (isEU()) {BarcodeFormat.QR_CODE}else{BarcodeFormat.DATA_MATRIX},
                width, width
            )

            for (i in 0 until width) {
                for (j in 0 until width) {
                    mImage.setPixel(i, j, if (bitMatrix[i, j]) 0 else 0xFFFFFF)

                }
            }

        }else{
            isInit =false;
        }
            //request a widget update
            val intent = Intent(context.applicationContext, QrCodeWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val widgetManager = AppWidgetManager.getInstance(context)
            val ids = widgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    QrCodeWidget::class.java
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.list)
            }

            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)


    }
    fun getData_fr():String{
        val qrCodeString = context.getSharedPreferences(sharedPrefName,Context.MODE_PRIVATE).getString("qrCode","")
        var res = context.getString(com.atheris.qrcodepass.R.string.default_id)
        if (!qrCodeString.isNullOrEmpty()) {

            val L0 = qrCodeString.findAnyOf(listOf("L0"), 26)
             L0?.let { val L1 = qrCodeString.findAnyOf(listOf("L1"), it.first)
                        L1?.let { it1 -> val L2 = qrCodeString.findAnyOf(listOf("L2"), it1.first)
                            L2?.let {
                                res ="2D-Doc\n"+
                                        qrCodeString.slice(L0!!.first+2 until L1!!.first-1 )+
                                        " "+
                                        qrCodeString.slice(L1!!.first+2 until L2!!.first-1)+
                                        "\n" +
                                        qrCodeString.slice(L2!!.first+2 .. L2!!.first+2+8).let { s: String -> s.slice(0..1)+"/"+s.slice(2..3)+"/"+s.slice(4..7) }+
                                        context.getString(com.atheris.qrcodepass.R.string.pass_francais)
                                //Log.d("mtag",res)

                            }
                        }
             }


        }

        return res

    }
    fun isEU() : Boolean{
        var start = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getString("qrCode", "")!!
        if (start.length>=4) {
            start = start.slice(0..3)
            //logd("$start")
            if (start == "HC1:" || start == "HC2:") {
                return true
            }
        }
        return false
    }
    fun getData():String {
        if (isEU()) {
            //logd("getData")
            val qrCodeString = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                .getString("qrCode", "")
            var res = context.getString(com.atheris.qrcodepass.R.string.default_id)
            if (!qrCodeString.isNullOrEmpty()) {
                try {
                    var inflated =
                        b45toHexAndInflateAndDecode(qrCodeString.slice(4 until qrCodeString.length))
                    //logd(inflated)
                    var i1 = inflated.indexOfAny(listOf(" fn: ")) + 5
                    var e1 = inflated.indexOfAny(listOf(",","}"), i1!!)
                    var i2 = inflated.indexOfAny(listOf(" gn: ")) + 5
                    var e2 = inflated.indexOfAny(listOf(",","}"), i2!!)
                    var i3 = inflated.indexOfAny(listOf(" dob: ")) + 5
                    var e3 = inflated.indexOfAny(listOf(",","}"), i3!!)
                    res =
                        inflated.slice(i1 until e1) + " " + inflated.slice(i2 until e2) + "\n" + inflated.slice(
                            i3 until e3
                        ).replace("-", "/")
                } catch (e: Exception) {

                }
            }
            //logd(res)
            return res+ context.getString(com.atheris.qrcodepass.R.string.pass_europe)
        }
        return getData_fr()
    }
    fun b45toHexAndInflateAndDecode(input : String): String{

        var lut = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9','A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y','Z',' ','$','%','*','+','-','.','/',':')
        var revlut=HashMap<Char,Int>();
        for (i in lut.indices){
            revlut[lut[i]] = i
        }
        var hex=String();
        var length=input.length
        var mbase45 = input+"00"
        for (i in 0 until length step 3){
            var num = (revlut[mbase45[i+2]]!!*45 + revlut[mbase45[i+1]]!!)*45 + revlut[mbase45[i]]!!

            var hexT = ""+lut[num/(16*16*16)]+lut[(num%(16*16*16))/(16*16)]+ lut[(num%(16*16))/16] + lut[num%16]
            //logd("$i $num $hexT")
            if (i+3>length && hexT.slice(0..1)=="00") {
                hexT = hexT.slice(2..3)
            }
            hex+=hexT
        }
        //logd(hex)

        var inflater = Inflater()
        var bytes = ByteArray(hex.length/2)
        for (i in hex.indices step 2){
            bytes[i/2]= (revlut[hex[i]]!!*16 + revlut[hex[i+1]]!!).toByte()
        }
        inflater.setInput(bytes,0,hex.length/2)
        var uncompressed = ByteArray(hex.length*2)
        var lengthUncompressed = inflater.inflate(uncompressed,0,hex.length*2)
        //logd(lengthUncompressed.toString())

        var inputStream = ByteArrayInputStream(uncompressed,0,lengthUncompressed);
        var decoder = CborDecoder(inputStream);
        var decoded = decoder.decode();

        var data2=((decoded[0] as Array).dataItems[2] as ByteString).bytes
        decoder = CborDecoder(ByteArrayInputStream(data2))
        decoded=decoder.decode();
        return decoded[0].toString()
    }

}