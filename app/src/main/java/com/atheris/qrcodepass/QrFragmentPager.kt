package com.atheris.qrcodepass

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.viewpager2.widget.ViewPager2
import com.atheris.qrcodepass.qrcode.QrGetter
import kotlin.math.min

class QrFragmentPager(val dotView : DotView?) : Fragment(), DeleteInterface {
    constructor():this(null)

    lateinit var viewPager2 : ViewPager2
    private val sharedPreferences:SharedPreferences
        get(){
            return requireContext().getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        }
    override fun deleteContent() {
        if (viewPager2.currentItem == 0)
            ((viewPager2.adapter as QrPageAdapter).map[0] as QrFragment).deleteContent()
        else
            (viewPager2.adapter as QrPageAdapter).also{adapt ->
                sharedPreferences.also{sp->
                    sp.edit().apply{
                        var key = QrGetter.keyOf(viewPager2.currentItem)

                        //remove current item from disk
                        for (i in viewPager2.currentItem until (adapt.count-1)){
                            var nextKey = QrGetter.keyOf(i+1)
                            putString(key,sp.getString(nextKey,""))
                            key=nextKey

                            QrGetter.isInits[i]=false//invalidate the qr code saved in memory
                        }
                        putString(key,"")

                        //remove item on the view
                        adapt.putAtTheEnd(viewPager2.currentItem)
                        adapt.count--

                        putInt("qrCount",adapt.count)
                        apply()
                    }
                }
                adapt.notifyItemRemoved(viewPager2.currentItem)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.qr_code_fragment,container,true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager2 = view.findViewById<ViewPager2>(R.id.qr_view_pager_2).also{vp->
            dotView?.registerViewPager2(vp)
            vp.adapter = QrPageAdapter().also {adapt->
                adapt.count = sharedPreferences.getInt("qrCount", 1)
                adapt.notifyDataSetChanged()

                setFragmentResultListener("addQrcode"){_,_->
                    adapt.addElement()
                    adapt.notifyDataSetChanged()
                    vp.currentItem=adapt.count-1
                    sharedPreferences.edit().apply {
                        putInt("qrCount",adapt.count)
                        apply()
                    }
                }
                setFragmentResultListener("addQrcodeFailed"){_,_->
                    adapt.count--
                    adapt.notifyDataSetChanged()
                    vp.currentItem=adapt.count-1
                    sharedPreferences.edit().apply {
                        putInt("qrCount",adapt.count)
                        apply()
                    }
                }
            }
            vp.offscreenPageLimit=2
            vp.setPageTransformer { page, position ->
                if(position<=-1 || position>=1){
                    page.alpha=1f
                    page.translationY=0f
                    page.scaleX=1f
                    page.scaleY=1f
                }
                else if (position<0){
                    page.alpha=1+position*1.5f
                    page.translationY=-page.height*position*0.8f
                    page.scaleX=1+position*0.5f
                    page.scaleY=1+position*0.5f
                }else{
                    page.alpha=1f-position
                    page.translationY=-page.height*position*0.5f
                    page.scaleX=1f+min(position*0.2f,0.05f)
                    page.scaleY=1f+min(position*0.2f,0.05f)
                }
            }
            setFragmentResultListener("getCurrentPage"){_,_->
                setFragmentResult("getCurrentPageResult", bundleOf(Pair("currentPage",vp.currentItem)) )
            }
        }

    }


    private inner class QrPageAdapter : PagerAdapter(childFragmentManager,lifecycle,dotView){

        override fun createFragment(position: Int): Fragment = QrFragment(position).also{frag->
            map[position]=frag
        }

        override fun putAtTheEnd(position: Int) {
            super.putAtTheEnd(position)
            for (i in (count-1) downTo position){
                    (map[i] as QrFragment).updatePosition(i)
            }
        }

    }
}