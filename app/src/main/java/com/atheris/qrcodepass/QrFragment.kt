package com.atheris.qrcodepass

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.atheris.qrcodepass.qrcode.logd

class QrFragment(private var noQr:Int = 0) : Fragment(), DeleteInterface {

    private lateinit var qr: QR;

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        logd("inflate qr $noQr")
        return inflater.inflate(R.layout.qr_code_fragment_page, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateQr()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //logd("instanciate qr object $noQr")
        qr = QR(this.requireContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.clipToOutline = true
        }
        updateQr()
    }

    override fun onResume() {
        super.onResume()
        updateQr()
    }
    fun updatePosition(position:Int){
        logd("change qr no from $noQr to $position")
        noQr=position
        //updateQr()//not needed
    }
    private fun updateQr(){
        val textName = view?.findViewById<TextView>(R.id.dataText)
        val imageView = view?.findViewById<ImageView>(R.id.img1)
        //logd("update qr $noQr")
        if (imageView != null) {
            //logd("set to image view qr $noQr")
            qr.setQr(imageView,noQr)
            //logd("get data from qr $noQr")
            textName?.text = qr.getData(noQr)
        }
    }

    override fun deleteContent() {
        qr.updateQr("",noQr)
        updateQr();
    }
}