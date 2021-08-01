package com.atheris.qrcodepass

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.integration.android.IntentIntegrator
import com.robertlevonyan.components.picker.ItemModel
import com.robertlevonyan.components.picker.ItemType
import com.robertlevonyan.components.picker.pickerDialog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


const val width=350;//pixels of the width of a qr_code
const val sharedPrefName = "mySharedPref"
interface InZoom{
    var inZoom:Boolean
}
class MainActivity() : AppCompatActivity(), InZoom{

    private lateinit var scanButton :FloatingActionButton
    private lateinit var picButton :FloatingActionButton
    private lateinit var delButton :FloatingActionButton
    private lateinit var lockButton :FloatingActionButton
    private lateinit var unlockButton :FloatingActionButton
    private lateinit var mPager : ViewPager2
    private lateinit var dotView : DotView

    private lateinit var permissionRequest : ActivityResultLauncher<String>;
    private lateinit var unlockToast: Toast

    override var inZoom = false//used to disable the swip of viewpager2
        set(v){
            mPager.isUserInputEnabled=!v
            field=v
        }

    enum class ResultCodes() {
        ABORD,QR_TORCH_TOGGLE,QR_FILE_PICK,QR_PIC,QR_FILE_RESULT
    }

    private var permissionRequestCallback = { isGranted: Boolean ->
        if (isGranted) {

        } else {

        }
    }
    private var mySharedPref : SharedPreferences? = null
        get () {
            return applicationContext.getSharedPreferences(
                sharedPrefName,
                Context.MODE_PRIVATE)
        }

    private var torch=false
    private var qr= QR(this)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanButton = findViewById<FloatingActionButton>(R.id.scan)
        picButton = findViewById<FloatingActionButton>(R.id.pic)
        delButton = findViewById<FloatingActionButton>(R.id.delete)
        lockButton = findViewById<FloatingActionButton>(R.id.lockClose)
        unlockButton = findViewById<FloatingActionButton>(R.id.lockOpen)
        mPager = findViewById<ViewPager2>(R.id.pager)
        dotView = findViewById<DotView>(R.id.dot_view)

        activButtons()
        if (mySharedPref!!.getBoolean("lockState",false))
            deacButtons()

        mPager.adapter = PagerAdapter(supportFragmentManager, lifecycle, this).apply {
            count = mySharedPref!!.getInt("count", 0) + 1
        }
        mPager.offscreenPageLimit = 2
        dotView!!.registerViewPager2(mPager)


        /*mPager.getChildAt(0).setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            logd("in zoom $inZoom")

            inZoom
            }*/


        /*permissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            permissionRequestCallback(it)
        }*/

        unlockToast = Toast.makeText(applicationContext,getString(R.string.long_press_toast),Toast.LENGTH_SHORT)




    }



    override fun onResume() {
        super.onResume()
        (mPager.adapter as PagerAdapter).notifyDataSetChanged()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        logd("activity result $requestCode")
        when (requestCode) {
            ResultCodes.QR_PIC.ordinal -> {
                when (resultCode) {
                    (-1) -> {
                        logd("code QR_PIC")
                        val scanResult =
                            IntentIntegrator.parseActivityResult(49374, resultCode, data)
                        if (scanResult != null) {
                            val qrCode = scanResult.contents
                            if (!qrCode.isNullOrEmpty()) {
                                Toast.makeText(this, qrCode, Toast.LENGTH_LONG).show()
                                qr.updateQr(qrCode)
                            }
                        } else {
                            Toast.makeText(this, "erreur qr code", Toast.LENGTH_LONG).show()
                        }
                    }

                    ResultCodes.QR_TORCH_TOGGLE.ordinal -> {
                        torch = !torch
                        startScan()
                    }

                    ResultCodes.QR_FILE_PICK.ordinal -> {
                        val intent = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
                            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                        startActivityForResult(intent, ResultCodes.QR_FILE_RESULT.ordinal)


                    }

                }
            }
            ResultCodes.QR_FILE_RESULT.ordinal -> {
                logd("${data != null}")
                if (data != null) {
                    val uri: Uri = data.data!!
                    val read =
                        BitmapFactory.decodeStream(contentResolver.openInputStream(uri)!!)
                            .run {
                                IntArray(width * height).let {
                                    getPixels(it, 0, width, 0, 0, width, height)
                                    RGBLuminanceSource(width, height, it).let {
                                        var result = ""
                                        try {
                                            result = MultiFormatReader().decode(
                                                BinaryBitmap(HybridBinarizer(it))
                                            ).text
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                applicationContext,
                                                "erreur qr code",
                                                Toast.LENGTH_LONG
                                            ).show();
                                        }
                                        logd(result)
                                        result
                                    }
                                }
                            }
                    if (!read.isNullOrEmpty())
                        qr.updateQr(read);
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private inner class PagerAdapter(fm: FragmentManager,lc:Lifecycle, var inZoom: InZoom): FragmentStateAdapter(fm,lc){
        var map = HashMap<Int, Fragment>()
        var count = 2
            set(v){
                field = v
                dotView?.pageNumber=field
            }

        var idList = HashMap<Int,Long>()
        var lastId:Long=0;

        override fun getItemCount(): Int = count


        override fun createFragment(position: Int): Fragment {
            //logd(idList.toString())
            map[position] = when (position){
                0-> QrFragment()
                else-> ImgFragment(position-1, inZoom)
            }
            return map[position]!!
        }

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
            idList[count++]=lastId++;
            //logd(idList.toString())
        }
        fun putAtTheEnd(position:Int){
            arrayOf(idList[position]!!,map[position]!!).let {
                //logd("$count")
                for (i in position until count - 1) {
                    idList[i] = idList[i + 1]!!
                    map[i]=map[i+1]!!
                }
                idList[count] = it[0] as Long
                map[count]= it[1] as Fragment
            }
            //logd(idList.toString())
        }


    }
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

    private fun activButtons(){

        scanButton.setOnClickListener() {
            startScan()
        }
        picButton.setOnClickListener() {
            /*val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            val photoFile = createImageFile()
            val photoUri = FileProvider.getUriForFile(this,
                "com.atheris.qrcodepass.fileprovider",
                photoFile)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoUri)
            try {
                startActivityForResult(takePictureIntent, CAMERA_PIC)
            } catch (e: ActivityNotFoundException) {
                logd("error camera")
                // display error state to the user
            }*/
            val cameraItem = ItemModel(ItemType.ITEM_CAMERA,
                itemBackgroundColor = ContextCompat.getColor(this,R.color.front))
            val picItem = ItemModel(ItemType.ITEM_GALLERY,
                itemBackgroundColor = ContextCompat.getColor(this,R.color.front))

            pickerDialog {
                setTitle(applicationContext.getString(R.string.add_pic))
                setItems(setOf(cameraItem, picItem))
            }.setPickerCloseListener{ _:ItemType, uris:List<Uri>->
                if (uris.isNotEmpty()) {
                    val fileInput = contentResolver.openInputStream(uris[0])
                    createImageFile()
                    fileInput!!.copyTo(FileOutputStream(currentPhotoPath))

                    with(
                        mySharedPref?.edit()!!
                    ) {
                        putString(
                            "img${(mPager.adapter as PagerAdapter).count - 1}",
                            currentPhotoPath
                        )
                        apply()
                    }

                    (mPager.adapter as PagerAdapter).addElement()

                    //cant notify data set change here (busy fragment manager error, wtf) -> pushed on OnResume
                    //(mPager.adapter as PagerAdapter).notifyDataSetChanged()

                }




            }.show()
            /*permissionRequestCallback = {
                var conf = ImagePickerConfig {
                    returnMode = ReturnMode.ALL
                    mode = ImagePickerMode.SINGLE
                    isFolderMode = true
                    isIncludeVideo = false
                    theme=R.style.ImagePicker

                    isShowCamera = it
                    logd(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.path)
                    savePath = ImagePickerSavePath(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!.path,false)
                    isSaveImage=true

                }

                picPickerCallback={l:List<Image>->
                    logd("${l.size}")
                    if (l.size>0){
                        logd(l[0].path)
                        File(l[0].path).copyTo(createImageFile(), true)
                        with(
                            applicationContext.getSharedPreferences(
                                sharedPrefName,
                                Context.MODE_PRIVATE
                            ).edit()
                        ) {
                            putString(
                                "img${(mPager.adapter as PagerAdapter).count - 1}",
                                currentPhotoPath
                            )
                            apply()
                        }

                        (mPager.adapter as PagerAdapter).addElement()
                        (mPager.adapter as PagerAdapter).notifyDataSetChanged()
                    }
                    0
                }
                picPicker.launch(conf)
            }*/
            //permissionRequest.launch(android.Manifest.permission.CAMERA)
        }

        lockButton.setOnClickListener{
            deacButtons()
        }

        delButton.setOnClickListener(){
            ((mPager.adapter as PagerAdapter).map[mPager.currentItem] as deleteInterface).deleteContent()
            if (mPager.currentItem>=1){
                var count = (mPager.adapter as PagerAdapter).count

                with(mySharedPref?.edit()!!) {
                    for (i in (mPager.currentItem) until count - 1) {
                        putString("img${i - 1}", mySharedPref?.getString("img${i}", ""))
                    }
                    apply()
                }

                logd("remove ${mPager.currentItem}")
                (mPager.adapter as PagerAdapter).putAtTheEnd(mPager.currentItem)
                //(mPager.adapter as PagerAdapter).notifyItemMoved(mPager.currentItem, (mPager.adapter as PagerAdapter).count)
                //(mPager.adapter as PagerAdapter).notifyItemRemoved(mPager.currentItem-1)
                (mPager.adapter as PagerAdapter).count--
                (mPager.adapter as PagerAdapter).notifyDataSetChanged();
            }
        }

        try {
            unlockButton.setOnLongClickListener() {
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
        scanButton.backgroundTintList = color
        picButton.backgroundTintList = color
        delButton.backgroundTintList = color
    }

    private fun deacButtons(){
        lockButton.visibility=View.GONE
        unlockButton.visibility=View.VISIBLE

        val color = ColorStateList.valueOf( if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getColor(R.color.grey)
        } else {
            ContextCompat.getColor(this,R.color.grey)
        })
        scanButton.backgroundTintList = color
        scanButton.setOnClickListener(){}
        picButton.backgroundTintList = color
        picButton.setOnClickListener(){}
        delButton.backgroundTintList = color
        delButton.setOnClickListener(){}
    }

    override fun onPause() {
        logd("onPause")
        val sharedPref = mySharedPref?: return
        with (sharedPref.edit()) {
            putInt("count", (mPager.adapter as PagerAdapter).count-1)
            putBoolean("lockState", unlockButton.visibility==View.VISIBLE)
            apply()
        }
        super.onPause()
    }

    lateinit var currentPhotoPath: String

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
    }

}
