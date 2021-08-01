package com.atheris.qrcodepass

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class QrFragment() : Fragment(), deleteInterface {
    lateinit var qr: QR;
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        var inf = inflater.inflate(R.layout.qrcode_fragment, container, false)
        qr = QR(this.requireContext());
        return inf
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateQr()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateQr()
    }

    override fun onResume() {
        super.onResume()
        updateQr()
    }
    fun updateQr(){
        var textName = view?.findViewById<TextView>(R.id.dataText)
        var imageView = view?.findViewById<ImageView>(R.id.img1)

        if (imageView != null) {
            qr.setQr(imageView)
            textName?.text = qr.getData()
        }
    }

    override fun deleteContent() {
        qr.updateQr("")
        updateQr();
    }
}