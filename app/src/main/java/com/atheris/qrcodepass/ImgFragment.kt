package com.atheris.qrcodepass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atheris.qrcodepass.qrcode.logd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import kotlin.math.max

interface ImageApplier{
    fun applyImg(bitmap:Bitmap?)
    fun errorFetchingImage(exception: Exception)
}

class ImgFragment(private var position: Int,inZoom : InZoom?) : ZoomFragment(inZoom), DeleteInterface,ImageApplier{
    constructor():this(0,null)

    private lateinit var imageUri : String
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (savedInstanceState!=null)
            logd("recycle")
            //position = savedInstanceState["position"]!! as Int

        logd("ImgFragment instantiate at $position")
        imageUri = context?.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)?.getString("img$position","")
            .toString()
        logd(position.toString())
        logd(imageUri)

        return inflater.inflate(R.layout.page_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.clipToOutline = true
        }
        ImageLoader(this,imageUri).updateImage()
    }

    override fun deleteContent() {
        File(imageUri).delete()
        imageUri=""
    }
    override fun applyImg(bitmap:Bitmap?){
        if (bitmap!=null)
            view?.findViewById<ImageView>(R.id.img2)?.setImageBitmap(bitmap)
    }

    override fun errorFetchingImage(exception: Exception) {
        Toast.makeText(context,"exception raised while loading images : \n $exception",Toast.LENGTH_LONG).show()
    }

    class ImageLoader(private val imgApplier:ImageApplier, private val imgPath:String) : ViewModel() {
        fun updateImage() {
            viewModelScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {

                        BitmapFactory.Options().run {
                            inJustDecodeBounds = true
                            BitmapFactory.decodeFile(imgPath, this)
                            inSampleSize = max(outWidth, outHeight) / 1024
                            logd("image sample $inSampleSize")
                            inJustDecodeBounds = false
                            BitmapFactory.decodeFile(imgPath, this)

                        }.run {
                            if (width > height) {
                                logd("rotation")
                                return@run rotateBitmap(this).also {
                                    val out = FileOutputStream(imgPath)
                                    it!!.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                }
                            }
                            return@run this
                        }
                    }

                    imgApplier.applyImg(bitmap)
                }catch(e:Exception){
                    e.printStackTrace()
                    imgApplier.errorFetchingImage(e)
                }
            }
        }
        private fun rotateBitmap(source: Bitmap): Bitmap? {
            val matrix = Matrix()
            matrix.postRotate(90f)
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }
    }


    override fun onDestroy() {
        logd("destroy $position")
        super.onDestroy()
    }





}

interface DeleteInterface{
    fun deleteContent()
}