package com.atheris.qrcodepass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

interface ImageApplier{
    fun applyImg(bitmap:Bitmap?);
}

class ImgFragment(var position: Int,inZoom : InZoom?) : ZoomFragment(inZoom), deleteInterface,ImageApplier{
    constructor():this(0,null)

    lateinit var imageUri : String;
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (savedInstanceState!=null)
            logd("recycle")
            //position = savedInstanceState["position"]!! as Int

        logd("ImgFragment instanciate at $position")
        imageUri = context?.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)?.getString("img$position","")
            .toString()
        logd(position.toString())
        logd(imageUri)
        return inflater.inflate(R.layout.page_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ImageLoader(this,imageUri).updateImage()
    }

    override fun deleteContent() {
        File(imageUri).delete()
        imageUri=""
    }
    override fun applyImg(b:Bitmap?){
        view?.findViewById<ImageView>(R.id.img2)?.setImageBitmap(b)
    }

    class ImageLoader(private val imgApplier:ImageApplier, private val imgPath:String) : ViewModel() {
        fun updateImage() {
            viewModelScope.launch() {
                var bitmap = withContext(Dispatchers.IO) {

                    BitmapFactory.Options().run{
                        inJustDecodeBounds=true
                        BitmapFactory.decodeFile(imgPath,this)
                        inSampleSize = max(outWidth,outHeight) / 1024
                        logd("image sample $inSampleSize")
                        inJustDecodeBounds=false
                        BitmapFactory.decodeFile(imgPath,this)

                    }.run {
                        if (width > height) {
                            logd("rotation")
                            return@run rotateBitmap(this, 90f).also {
                                var out = FileOutputStream(imgPath)
                                it!!.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            }
                        }
                        return@run this
                    }
                }

                imgApplier.applyImg(bitmap)
            }
        }
        fun rotateBitmap(source: Bitmap, angle: Float): Bitmap? {
            val matrix = Matrix()
            matrix.postRotate(angle)
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }
    }


    override fun onDestroy() {
        logd("destroy $position")
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        //outState.putInt("position",position)
        super.onSaveInstanceState(outState)
    }





}

interface deleteInterface{
    fun deleteContent()
}