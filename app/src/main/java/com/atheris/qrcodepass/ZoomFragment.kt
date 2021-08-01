package com.atheris.qrcodepass

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import kotlin.math.abs
import kotlin.math.max

open class ZoomFragment(var inZoom: InZoom?): Fragment() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with( ScaleGestureDetector(context,object :
            ScaleGestureDetector.SimpleOnScaleGestureListener() {
            var scale=1f
            set(v){
                field=max(v,1f)
            }
            var startSpan=1f
            var startFocusX=0f
            var startFocusY=0f
            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                if (detector!=null) {
                    scale = detector.currentSpan/startSpan
                    //logd("current span = ${detector.currentSpan}")
                    //logd("scale = $scale")
                    (view as ConstraintLayout).getChildAt(0).let{

                        it.pivotX=startFocusX + (startFocusX- detector.focusX)/scale*2
                        it.pivotY=startFocusY + (startFocusY - detector.focusY)/scale*2
                        it.scaleX = scale
                        it.scaleY = scale
                    }

                    return true
                }
                return false
            }

            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean{
                startSpan=detector!!.currentSpan
                startFocusX=detector.focusX
                startFocusY=detector.focusY
                //logd("start span = $startSpan")
                inZoom?.inZoom=true
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector?) {
                inZoom?.inZoom=false
                //logd("scale end")
                scale=1f
                view.scaleX=scale
                (view as ConstraintLayout).getChildAt(0).let{
                    it.scaleX = scale
                    it.scaleY = scale
                }
            }

        })
        ) {
            isQuickScaleEnabled = true
            view.setOnTouchListener { _, event -> onTouchEvent(event); true }
        }
    }

}