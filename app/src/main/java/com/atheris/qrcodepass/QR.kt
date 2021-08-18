package com.atheris.qrcodepass

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import co.nstant.`in`.cbor.CborDecoder
import co.nstant.`in`.cbor.model.Array
import co.nstant.`in`.cbor.model.ByteString
import com.atheris.qrcodepass.qrcode.QrGetter
import java.io.ByteArrayInputStream
import java.util.*
import java.util.zip.Inflater


class QR(context: Context) : QrGetter(context, sharedPrefName, width) {


    override fun widgetUpdate(){
        val intent = Intent(context.applicationContext, QrCodeWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val widgetManager = AppWidgetManager.getInstance(context)
        val ids = widgetManager.getAppWidgetIds(
            ComponentName(
                context,
                QrCodeWidget::class.java
            )
        )

        widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list)

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }
    fun getData_fr(qrNo:Int=0):String{
        val qrCodeString = context.getSharedPreferences(sharedPrefName,Context.MODE_PRIVATE).getString(keyOf(qrNo),"")
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

    fun isFR():Boolean{
        TODO("faire en sorte que le qrcode soit par defaut")
    }
    fun getData(qrNo: Int = 0):String {
        if (isEU(qrNo)) {
            //logd("getData")
            val qrCodeString = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
                .getString(keyOf(qrNo), "")
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

        return getData_fr(qrNo)
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