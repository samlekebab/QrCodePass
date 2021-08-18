package com.atheris.qrcodepass

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.atheris.qrcodepass.qrcode.logd

//page adapter for the viewpager2 that show the qr code and the documents
open abstract class PagerAdapter(fm: FragmentManager, lc: Lifecycle, val dotView: DotView?=null): FragmentStateAdapter(fm,lc){
    var map = HashMap<Int, Fragment>()
    var idList = HashMap<Int, Long>()//TODO convert to one same map without losing compatibility (map is used outside)
    var count = 2
        set(v){
            logd("count update $v")
            logd("is dot view null ${dotView==null}")
            field = v
            dotView?.pageNumber=v
        }
    var lastId:Long=0

    override fun getItemCount(): Int = count

    override fun getItemId(position: Int): Long {
        //logd(idList.toString())
        if (idList.containsKey(position))
            return idList[position]!!
        idList[position]=lastId++
        //logd(idList.toString())
        return idList[position]!!
    }

    override fun containsItem(itemId: Long): Boolean {

        //logd("containsItem $itemId on ${idList.toString()}")
        return idList.containsValue(itemId)
    }
    fun addElement(){
        idList[count++]=lastId++
        //logd(idList.toString())
    }
    open fun putAtTheEnd(position:Int){
        Pair(idList[position]!!,map[position]!!).let {
            //logd("$count")
            for (i in position until count - 1) {
                idList[i] = idList[i + 1]!!
                map[i]=map[i+1]!!
            }
            idList[count] = it.first
            map[count]= it.second
        }
        //logd(idList.toString())
    }
}
public class MainPagerAdapter(fm: FragmentManager, lc: Lifecycle, var inZoom: InZoom, dotView: DotView?=null, val familyDotView: DotView) :
        PagerAdapter(fm,lc,dotView){
    override fun createFragment(position: Int): Fragment {
        //logd(idList.toString())
        map[position] = when (position){
            0-> QrFragmentPager(familyDotView)
            else-> ImgFragment(position - 1, inZoom)
        }
        return map[position]!!
    }
}