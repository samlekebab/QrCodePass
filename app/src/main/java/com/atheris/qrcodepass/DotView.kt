package com.atheris.qrcodepass

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.viewpager2.widget.ViewPager2

class DotView(mContext : Context, attrs:AttributeSet) : LinearLayout(mContext,attrs) {
    private lateinit var dots : MutableList<ImageView>



    private var dotResource = R.drawable.ic_dot
    private var focusDotResource = R.drawable.ic_dot_focus
    var pageNumber = 1
        set(v){
            field=v
            onPageNumberChange()
        }
    var currentPage = 0
        set(v){
            if (v>pageNumber-1){
                currentPage=pageNumber-1
                return
            }
            field.let{
                field=v
                onCurrentPageChange(it,v)
            }
        }

    init{
        pageNumber=if (isInEditMode)(4)else(1)
        currentPage=if (isInEditMode)(2)else(0)
    }
    private fun inflateDot():View=inflate(context,R.layout.dot_view,this)

    private fun onPageNumberChange(){
        this.removeAllViews()
        dots = mutableListOf()
        for (i in 1..pageNumber) {
            inflateDot()
            dots.add(
                (this.getChildAt(this.childCount-1) as ImageView))
            //dots.add(((inflateDot() as FrameLayout).getChildAt(0)) as ImageView)
            dots.last().setImageResource(dotResource)
        }

        currentPage=currentPage
    }
    private fun onCurrentPageChange(lastPage : Int, mCurrentPage: Int = currentPage){
        if(lastPage<pageNumber) {
            dots[lastPage].setImageResource(dotResource)
        }
        dots[mCurrentPage].setImageResource(focusDotResource)
    }


    fun registerViewPager2(viewPager2 : ViewPager2){
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                currentPage = position
            }
        })

    }


}