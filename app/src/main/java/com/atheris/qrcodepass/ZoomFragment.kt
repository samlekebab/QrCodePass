package com.atheris.qrcodepass

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.fragment.app.Fragment
import kotlin.math.exp
import kotlin.math.max
import com.atheris.qrcodepass.qrcode.logd

open class ZoomFragment(var inZoom: InZoom?): Fragment() {
    private var trStartX=0f
    private var trStartY=0f
    private var isInZoomAtBeginning=false
    private val easeOut = TimeInterpolator { f ->
        (1f-exp(-5*f))/(1f-exp(-5f))
    }

    var scaleAnimator  = ValueAnimator().apply {
        duration = 230
        startDelay= 1800
        interpolator = easeOut
        addUpdateListener { v ->
            val scale = v.animatedValue as Float
            (view as ConstraintLayout).getChildAt(0).let{
                it.scaleX = scale
                it.scaleY = scale
                it.translationX = (1-animatedFraction)*trStartX
                it.translationY = (1-animatedFraction)*trStartY
            }
        }
        this.doOnStart {
            startDelay=1800
            (view as ConstraintLayout).getChildAt(0).let {
                trStartX = it.translationX
                trStartY = it.translationY
            }
            isInZoomAtBeginning = inZoom!!.inZoom
        }
        this.doOnEnd {
            if (isInZoomAtBeginning)
                inZoom?.inZoom = false
        }
    }
    var zoomDoubleTapAnimation=  ValueAnimator().apply {
        duration = 230
        interpolator = easeOut
        addUpdateListener { v ->
            val scale = v.animatedValue as Float
            (view as ConstraintLayout).getChildAt(0).let{
                it.scaleX = scale
                it.scaleY = scale
            }
        }
        doOnEnd {
            scaleGestureListener.scale=2f
            scaleGestureListener.onScaleEnd(null)
        }
    }
    private val scaleGestureListener = object :
        ScaleGestureDetector.SimpleOnScaleGestureListener() {


        var scale=1f
            set(v){
                field=max(v,1f)
            }
        var startSpan=1f
        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            if (detector!=null) {
                scale = detector.currentSpan/startSpan
                //logd("current span = ${detector.currentSpan}")
                //logd("scale = $scale")
                (view as ConstraintLayout).getChildAt(0).let{
                    var p= floatArrayOf(it.pivotX,it.pivotY)
                    it.matrix.mapPoints(p)
                    it.translationX +=  detector!!.focusX - p[0]
                    it.translationY += detector!!.focusY - p[1]
                    it.scaleX = scale
                    it.scaleY = scale
                }

                return true
            }
            return false
        }

        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean{

            (view as ConstraintLayout).getChildAt(0).let {

                val p = floatArrayOf(detector!!.focusX,detector!!.focusY)
                //logd("point before matrix : ${p[0]} ${p[1]}")
                var ma=android.graphics.Matrix(it.matrix)
                it.matrix.invert(ma)
                ma.mapPoints(p)
                //logd("point after matrix : ${p[0]} ${p[1]}")
                it.pivotX=p[0]
                it.pivotY=p[1]

                if(inZoom!=null && inZoom!!.inZoom){

                    scale = if(scaleAnimator.isRunning) scaleAnimator.animatedValue as Float else scale
                    scaleAnimator.cancel()
                    it.matrix.mapPoints(p)



                }else{
                    scale=1f
                }


            }
            //logd("scale at beginning : $scale")
            startSpan=detector!!.currentSpan/scale
            //logd("start span = $startSpan")
            inZoom?.inZoom=true
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            //logd("scale end")
            scaleAnimator.setFloatValues(scale,1f)
            scaleAnimator.start()
            if (scale<1.1f)
                inZoom?.inZoom=false
        }

    }
    val dragGestureListener = object : GestureDetector.SimpleOnGestureListener(){
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (!scaleAnimator.isRunning && inZoom!=null && inZoom!!.inZoom) {
                (view as ConstraintLayout).getChildAt(0).let {
                    it.translationX -= distanceX
                    it.translationY -= distanceY
                }
                scaleAnimator.cancel()
                inZoom!!.inZoom=true
            }
            return true
        }

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            if (inZoom!=null && inZoom!!.inZoom) {
                val tmp = scaleAnimator.startDelay
                scaleAnimator.startDelay=0
                scaleAnimator.start()
                //scaleAnimator.startDelay=tmp
                //scaleAnimator.currentPlayTime=0
            }else{
                zoomDoubleTapAnimation.setFloatValues((view as ConstraintLayout).getChildAt(0).scaleX,2f)
                zoomDoubleTapAnimation.start()
                inZoom!!.inZoom=true
            }
            return true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val mScaleGestureDetector = ScaleGestureDetector(context,scaleGestureListener).apply {
            isQuickScaleEnabled = false
        }
        val mDragDetector = GestureDetector(context,dragGestureListener)

        view.setOnTouchListener { _, event ->
            mScaleGestureDetector.onTouchEvent(event);
            mDragDetector.onTouchEvent(event);

            if (event.action == MotionEvent.ACTION_UP && inZoom!=null && inZoom!!.inZoom && !scaleAnimator.isStarted){
                //logd("motion up")
                scaleAnimator.start()
            }
            true
        }
    }

    override fun onDestroy() {
        /*if (scaleAnimator != null && scaleAnimator.isStarted) {
            scaleAnimator.cancel()
        }*/
        scaleAnimator.removeAllListeners()
        scaleAnimator.removeAllUpdateListeners()
        zoomDoubleTapAnimation.removeAllListeners()
        zoomDoubleTapAnimation.removeAllUpdateListeners()

        logd("annimation listner removed on OnDestroy")

        super.onDestroy()
    }


}