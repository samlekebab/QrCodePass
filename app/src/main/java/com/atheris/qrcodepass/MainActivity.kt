package com.atheris.qrcodepass

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.atheris.qrcodepass.picker.ItemModel
import com.atheris.qrcodepass.picker.ItemType
import com.atheris.qrcodepass.picker.pickerDialog
import com.atheris.qrcodepass.qrcode.QrGetter
import com.atheris.qrcodepass.qrcode.logd
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


const val width=350//pixels of the width of a qr_code
const val sharedPrefName = "mySharedPref"
interface InZoom{
    var inZoom:Boolean
}
class MainActivity() : AppCompatActivity(), InZoom{

    private lateinit var scanButton :ImageView
    private lateinit var picButton : ImageView
    private lateinit var delButton :ImageView
    private lateinit var lockButton :ImageView
    private lateinit var unlockButton :ImageView
    private lateinit var leftArrow : FrameLayout
    private lateinit var rightArrow : FrameLayout
    private lateinit var mPager : ViewPager2
    private lateinit var dotView : DotView
    private lateinit var qrFamilyDotView : DotView
    private lateinit var menuButton : FrameLayout
    private lateinit var menu : LinearLayout
    private lateinit var mainContainer : MainView


    //private lateinit var permissionRequest : ActivityResultLauncher<String>;
    private lateinit var unlockToast: Toast

    //From interface InZoom
    override var inZoom = false//used to disable the swipe of viewpager2 when we zoom on pictures -
        set(v){
            mPager.isUserInputEnabled=!v
            field=v
        }

    enum class ResultCodes {
        ABORD,QR_TORCH_TOGGLE,QR_FILE_PICK,QR_PIC,QR_FILE_RESULT
    }


    private val mySharedPref : SharedPreferences
        get () {
            return applicationContext.getSharedPreferences(
                sharedPrefName,
                Context.MODE_PRIVATE)
        }

    private var torch=false
    private var qr= QR(this)

    val tvAnnimation = ValueAnimator().apply {
        startDelay=200
        duration=300
        setFloatValues(0f,1f)
        interpolator = ZoomFragment.easeOut
        addUpdateListener {
            mPager.scaleY = animatedValue as Float
        }

    }
    var isJustCreated=false;
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanButton = findViewById(R.id.scan)
        picButton = findViewById(R.id.pic)
        delButton = findViewById(R.id.delete)
        lockButton = findViewById(R.id.lockClose)
        unlockButton = findViewById(R.id.lockOpen)
        mPager = findViewById(R.id.pager)
        dotView = findViewById(R.id.dot_view)
        leftArrow = findViewById(R.id.arrow_prev)
        rightArrow = findViewById(R.id.arrow_next)
        qrFamilyDotView = findViewById(R.id.qr_family_dotview)
        mainContainer = findViewById(R.id.main_constraintLayout)
        menuButton = findViewById(R.id.menu_button)
        menu = findViewById(R.id.menu)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mainContainer.apply {
                    clipToOutline=true
            }
        }

        activButtons()
        if (mySharedPref.getBoolean("lockState",false))
            deacButtons()

        dotView.registerViewPager2(mPager)
        mPager.adapter = MainPagerAdapter(supportFragmentManager, lifecycle, this,dotView = dotView,familyDotView = qrFamilyDotView).also {
            it.count = mySharedPref.getInt("count", 0) + 1
            mPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    if (position>0){
                        leftArrow.visibility=View.VISIBLE
                        (qrFamilyDotView.parent as FrameLayout).visibility=View.INVISIBLE
                    }else{
                        leftArrow.visibility=View.INVISIBLE
                        (qrFamilyDotView.parent as FrameLayout).visibility=View.VISIBLE
                    }
                    if (position<it.count-1){
                        rightArrow.visibility=View.VISIBLE
                    }else{
                        rightArrow.visibility=View.INVISIBLE
                    }
                }


            })
            leftArrow.setOnClickListener {_->
                if (mPager.currentItem>0) {
                    (it.map[mPager.currentItem] as ZoomFragment).endZoom()
                }
                mPager.currentItem--
            }
            rightArrow.setOnClickListener {_->
                if (mPager.currentItem>0) {
                    (it.map[mPager.currentItem] as ZoomFragment).endZoom()
                }
                mPager.currentItem++
            }

        }
        mPager.offscreenPageLimit = 2
        mPager.setPageTransformer (MarginPageTransformer(resources.getDimensionPixelOffset(R.dimen.page_margin_side)))

        unlockToast = Toast.makeText(applicationContext,getString(R.string.long_press_toast),Toast.LENGTH_SHORT)

        mPager.scaleY = 0f
        isJustCreated=true

        menuButton.setOnClickListener{
            mainContainer.toogleMenu()
        }
        menu.getChildAt(0).setOnClickListener {
            supportFragmentManager.setFragmentResult("addQrcode", Bundle())
            isAddingMember=true
            mPager.currentItem=0
            mainContainer.toogleMenu()
            startScan()
        }
        menu.getChildAt(1).setOnClickListener {
            supportFragmentManager.setFragmentResult("getCurrentPage",Bundle())
        }
        supportFragmentManager.setFragmentResultListener("getCurrentPageResult",this){_,bundle->
            val noQr = bundle.getInt("currentPage",0)
            Intent(Intent.ACTION_SEND).also{intent->
                if (QrGetter.mImages.containsKey(noQr)) {
                    intent.type = "image/jpeg"
                    var file = File.createTempFile("qr_code", ".png", externalCacheDir)
                    var outputStream = file.outputStream()
                    QrGetter.mImages[noQr]!!.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                    var uri = Uri.parse(
                        MediaStore.Images.Media.insertImage(
                            contentResolver,
                            QrGetter.mImages[noQr],
                            "qr code",
                            "qr code"
                        )
                    )
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    startActivity(Intent.createChooser(intent, "send qr code"))
                }
            }

        }
        menu.getChildAt(2).setOnClickListener {
            startActivity(PlayIntent.cheeseWheelIntent)
        }
        menu.getChildAt(3).setOnClickListener {
            startActivity(PlayIntent.rateThisAppIntent)
        }




    }



    override fun onResume() {
        super.onResume()
        (mPager.adapter as PagerAdapter).notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()
        if (isJustCreated) {
            tvAnnimation.start()
            isJustCreated = false
        }
    }

    private var isAddingMember=false
    private fun updateQr(qrCode : String){
        qr.updateQr(qrCode, qrFamilyDotView.currentPage)
        isAddingMember=false
    }
    private fun addQrcodeFailed(){
        logd("add qr code failed $isAddingMember")
        if (isAddingMember) {
            supportFragmentManager.setFragmentResult("addQrcodeFailed",Bundle())
            isAddingMember = false
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        logd("activity result $requestCode $resultCode")
        when (requestCode) {

            //result from the qrcode scanner
            ResultCodes.QR_PIC.ordinal -> {
                when (resultCode) {
                    ResultCodes.ABORD.ordinal->{
                        addQrcodeFailed()
                    }
                        //a qr code has been scanned
                    (-1) -> {
                        logd("code QR_PIC")
                        val scanResult =
                            IntentIntegrator.parseActivityResult(49374, resultCode, data)
                        if (scanResult != null) {
                            val qrCode = scanResult.contents
                            if (!qrCode.isNullOrEmpty()) {
                                Toast.makeText(this, qrCode, Toast.LENGTH_LONG).show()
                                updateQr(qrCode)
                            }else{
                                addQrcodeFailed()
                            }
                        } else {
                            addQrcodeFailed()
                            Toast.makeText(this, "erreur qr code", Toast.LENGTH_LONG).show()
                        }
                    }

                    //request to turn on the torch
                    ResultCodes.QR_TORCH_TOGGLE.ordinal -> {
                        torch = !torch
                        startScan()
                    }

                    //request to open a photo to scan the qrcode in it
                    ResultCodes.QR_FILE_PICK.ordinal -> {
                        val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
                            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                        startActivityForResult(intent, ResultCodes.QR_FILE_RESULT.ordinal)


                    }

                }
            }

            //a photo with a potential qrcode in it might have been given as a result
            ResultCodes.QR_FILE_RESULT.ordinal -> {
                logd("${data != null}")
                if (data != null) {//if there is a photo
                    val uri: Uri = data.data!!
                    try {

                        //scan it with zxing
                        val read =
                            BitmapFactory.decodeStream(contentResolver.openInputStream(uri)!!)//get the photo
                                .run {
                                    IntArray(width * height).let { array ->
                                        getPixels(array, 0, width, 0, 0, width, height)
                                        RGBLuminanceSource(width, height, array).let {
                                            var result = ""
                                            try {

                                                //decode it
                                                result = MultiFormatReader().decode(
                                                    BinaryBitmap(HybridBinarizer(it))
                                                ).text
                                            } catch (e: Exception) {
                                                addQrcodeFailed()
                                                //if the decoding failed
                                                e.printStackTrace()
                                                Toast.makeText(
                                                    applicationContext,
                                                    getString(R.string.error_code) + " $e",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            logd(result)
                                            result
                                        }
                                    }
                                }

                        //finally, save the qr code
                        if (!read.isNullOrEmpty())
                            updateQr(read)
                        else
                            addQrcodeFailed()

                    } catch (e: FileNotFoundException) {//if opening the file failed
                        addQrcodeFailed()
                        e.printStackTrace()
                        Toast.makeText(
                            this,
                            "i'm sorry, file not found exception : ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()

                    } catch (e: java.lang.Exception) {//other error in this process
                        addQrcodeFailed()
                        Toast.makeText(
                            this,
                            "i'm sorry, a exception has been raised.. \n $e",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }else{
                    addQrcodeFailed()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    //start the activity that scan the qr code with the camera
    private fun startScan(){
        val integrator = IntentIntegrator(this)
        integrator.setOrientationLocked(false)
        integrator.setPrompt(getString(R.string.qr_code_hint))
        integrator.setCameraId(0)
        integrator.captureActivity = MCaptureActivity::class.java
        integrator.setRequestCode(ResultCodes.QR_PIC.ordinal)
        integrator.setBeepEnabled(false)
        integrator.setTorchEnabled(torch)
        integrator.initiateScan()
    }

    //onclicklistners of the buttons of the main activity
    private fun activButtons(){
        //to scane a qrcode
        scanButton.setOnClickListener {
            startScan()
        }

        //to add a document
        picButton.setOnClickListener {

            val cameraItem = ItemModel(ItemType.ITEM_CAMERA,
                itemBackgroundColor = ContextCompat.getColor(this,R.color.front))
            val picItem = ItemModel(ItemType.ITEM_GALLERY,
                itemBackgroundColor = ContextCompat.getColor(this,R.color.front))

            pickerDialog {
                setTitle(applicationContext.getString(R.string.add_pic))
                setItems(setOf(cameraItem, picItem))
            }.setPickerCloseListener{ _:ItemType, uris:List<Uri>->
                if (uris.isNotEmpty()) {
                    try {
                        val fileInput = contentResolver.openInputStream(uris[0])
                        createImageFile()
                        fileInput!!.copyTo(FileOutputStream(currentPhotoPath))

                        with(
                            mySharedPref.edit()
                        ) {
                            putString(
                                "img${(mPager.adapter as PagerAdapter).count - 1}",
                                currentPhotoPath
                            )
                            apply()
                        }

                        (mPager.adapter as PagerAdapter).also { adapt ->
                            adapt.addElement()
                            mPager.currentItem = adapt.count-1
                        }
                        //cant notify data set change here (busy fragment manager error, wtf) -> pushed on OnResume
                        //(mPager.adapter as PagerAdapter).notifyDataSetChanged()
                    }catch (e:FileNotFoundException){
                        e.printStackTrace()
                        Toast.makeText(this, "i'm sorry, file not found exception : ${e.message}", Toast.LENGTH_LONG).show()

                    }catch (e:java.lang.Exception){
                        Toast.makeText(this, "i'm sorry, a exception has been raised.. \n $e", Toast.LENGTH_LONG).show()
                    }

                }




            }.show()
        }

        //lock and unlock the buttons
        lockButton.setOnClickListener{
            deacButtons()
        }

        delButton.setOnClickListener{
            MaterialAlertDialogBuilder(this).run {
                setMessage(getString(R.string.confirm_delete))
                setNegativeButton(getString(R.string.cancel)){_,_->}
                setPositiveButton(getString(R.string.delete)
                ) { _, _ ->
                    ((mPager.adapter as PagerAdapter).map[mPager.currentItem] as DeleteInterface).deleteContent()
                    if (mPager.currentItem >= 1) {
                        val count = (mPager.adapter as PagerAdapter).count

                        with(mySharedPref.edit()) {
                            for (i in (mPager.currentItem) until count - 1) {
                                putString("img${i - 1}", mySharedPref.getString("img${i}", ""))
                            }
                            apply()
                        }

                        logd("remove ${mPager.currentItem}")
                        (mPager.adapter as PagerAdapter).putAtTheEnd(mPager.currentItem)
                        //(mPager.adapter as PagerAdapter).notifyItemMoved(mPager.currentItem, (mPager.adapter as PagerAdapter).count)
                        //(mPager.adapter as PagerAdapter).notifyItemRemoved(mPager.currentItem-1)
                        (mPager.adapter as PagerAdapter).count--
                        (mPager.adapter as PagerAdapter).notifyDataSetChanged()
                    }
                }
                show()
            }
        }

        try {
            unlockButton.setOnLongClickListener {
                activButtons()
                true
            }
            unlockButton.setOnClickListener {
                unlockToast.show()
            }
        }catch (e:NoSuchMethodError){
            e.printStackTrace()
            unlockButton.setOnClickListener {
                activButtons()
            }
        }

        lockButton.visibility=View.VISIBLE
        unlockButton.visibility=View.GONE

        val color = ColorStateList.valueOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getColor(R.color.front)
        } else {
            ContextCompat.getColor(this,R.color.front)
        })
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanButton.imageTintList = null
            picButton.imageTintList = null
            delButton.imageTintList = null
        }else{
            scanButton.setColorFilter(Color.parseColor("#00000000"))
            picButton.setColorFilter(Color.parseColor("#00000000"))
            delButton.setColorFilter(Color.parseColor("#00000000"))
        }
    }

    //disable the buttons to prevent user to delete by mistake or other...
    private fun deacButtons() {
        lockButton.visibility = View.GONE
        unlockButton.visibility = View.VISIBLE

        val color = ColorStateList.valueOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getColor(R.color.grey)
            } else {
                ContextCompat.getColor(this, R.color.grey)
            }
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scanButton.imageTintList = color
            picButton.imageTintList = color
            delButton.imageTintList = color
        }else{
            scanButton.setColorFilter(color.defaultColor)
            picButton.setColorFilter(color.defaultColor)
            delButton.setColorFilter(color.defaultColor)
        }
        val callback = {_:Any?->Toast.makeText(
            this,
            "locked, \n push the bottom left button",
            Toast.LENGTH_SHORT
        ).show()
        }
        scanButton.setOnClickListener(callback)
        picButton.setOnClickListener (callback)
        delButton.setOnClickListener (callback)
    }

    //we save some shared pref here (counter -> number of pages, lockState -> whether the buttons are enabled or locked)
    override fun onPause() {
        logd("onPause")
        with (mySharedPref.edit()) {
            putInt("count", (mPager.adapter as PagerAdapter).count-1)
            putBoolean("lockState", unlockButton.visibility==View.VISIBLE)
            apply()
        }
        super.onPause()
    }

    //create a file (with timestamp as name) to store locally an image and return it, store the path to this file in currentPhotoPath
    private lateinit var currentPhotoPath: String
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "img_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    override fun onBackPressed() {
        if(mPager.currentItem==0){
            finish()
            return
        }
        mPager.currentItem=0
        inZoom=false
    }

    override fun onDestroy() {
        tvAnnimation.removeAllUpdateListeners()
        tvAnnimation.removeAllListeners()
        super.onDestroy()
    }

}
