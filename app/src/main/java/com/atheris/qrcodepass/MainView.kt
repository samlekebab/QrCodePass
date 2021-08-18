package com.atheris.qrcodepass

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout

class MainView(context : Context, attrs : AttributeSet) : ConstraintLayout(context, attrs) {
    private var isMenuOpened = false
    set(v){
        field = v
        var margin = dpToPx(5f)
        translationYAnimated = (if(field) 1 else 0) *( 3f *
                (context.resources.getDimensionPixelSize(R.dimen.menu_item_height) + 2*margin)+margin)
    }
    fun dpToPx(dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    private val translationYAnimation = ValueAnimator().apply {
        interpolator = ZoomFragment.easeOut
        addUpdateListener {
            translationY = animatedValue as Float
        }
    }

    private var translationYAnimated : Float = translationY
    set(v) {
        field = v
        translationYAnimation.setFloatValues(translationY,translationYAnimated)
        translationYAnimation.start()
    }

    fun toogleMenu(){
        isMenuOpened=!isMenuOpened
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        translationYAnimation.removeAllUpdateListeners()
        translationYAnimation.removeAllListeners()
    }
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean =
        if (!isMenuOpened) {
            super.onInterceptTouchEvent(ev)
        }else{
            toogleMenu()
            true
        }

}