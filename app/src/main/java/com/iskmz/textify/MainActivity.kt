package com.iskmz.textify

import androidx.appcompat.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_main.*
import com.cloudinary.android.MediaManager
import androidx.core.app.ActivityCompat
import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.content.ContextCompat
import android.widget.Toast
import android.provider.MediaStore
import android.content.Intent
import android.net.Uri
import android.os.*
import android.os.Environment.*
import android.util.Log
import java.io.File.separator
import java.io.File
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    companion object{
        const val PERMISSION_REQ_CODE = 11
        const val CAMERA_RESULT = 22
    }

    // if we have a permission to the camera & read-write permissions
    var havePermission = Build.VERSION.SDK_INT<Build.VERSION_CODES.M // nice trick for older android APIs //

    // pointers to fileName & its parent directory
    var fileName=""
    val DIR = getExternalStoragePublicDirectory(DIRECTORY_PICTURES).toString() + separator + "textify"


    var textResult = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initCloudinary()
        initViews()

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) checkForPermission()

        setOnClicks()

    }

    private fun initViews() {
        imgCurrent.visibility = GONE
        imgDefault.visibility = VISIBLE
        txtText.text = getString(R.string.txt_default)
    }

    private fun checkForPermission() {
        // we create a list of permission to ask, so we can add more later on.
        val lstPermissions = ArrayList<String>()

        val cam = ContextCompat.checkSelfPermission(this, CAMERA)
        val wrt = ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
        val rd = ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE)

        //check if we have permission
        if (cam != PERMISSION_GRANTED) lstPermissions.add(CAMERA)
        if (wrt != PERMISSION_GRANTED) lstPermissions.add(WRITE_EXTERNAL_STORAGE)
        if (rd != PERMISSION_GRANTED) lstPermissions.add(READ_EXTERNAL_STORAGE)


        if (lstPermissions.isNotEmpty())  // in case missing a permission or two .... //
        { //send request for permission to the user
            ActivityCompat.requestPermissions(this,
                lstPermissions.toTypedArray(), PERMISSION_REQ_CODE)
        }
        else
        { // all permissions are OK & GRANTED
            havePermission = true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //checking if we got a permission result
        if (grantResults.isNotEmpty()) {
            var i = 0
            while (i < grantResults.size && grantResults[i] == PERMISSION_GRANTED) i++
            // if all are GRANTED
            if (i == grantResults.size) {
                havePermission = true
                return
            }
        }
        // if we reached here, then either no permissions were given, or some were denied! //
        Toast.makeText(this, "Cannot take photo without permissions!",
            Toast.LENGTH_SHORT).show()
    }

    private fun initCloudinary() {
        val config = HashMap<String,String>()
        config["cloud_name"] = "dsbqen2df"
        MediaManager.init(this, config)
    }

    private fun setOnClicks() {

        btnExit.setOnClickListener { eraseImgCache(); finish() }
        btnAbout.setOnClickListener { showAboutDialog() }
        btnShoot.setOnClickListener { shootAndTextify() }

        txtText.setOnClickListener { showCopyShareFor(2000) }
        txtText.movementMethod = ScrollingMovementMethod()

        imgCurrent.setOnClickListener { resizeImgCurrent() }
        // imgDefault.setOnClickListener { resizeImgCurrent() } // for check !

        btnCopyClipboard.setOnClickListener { copyTxtToClipboard() }
        btnShare.setOnClickListener { shareTxt() }
    }

    private fun shareTxt() {
        // TODO
    }

    private fun copyTxtToClipboard() {
        // TODO
    }

    private fun eraseImgCache() {
        eraseAllLocal()
        eraseAllRemote()
    }

    private fun eraseAllRemote() {
        /*
        no need!
        done from backend!
         */
    }

    private fun eraseAllLocal() {
        val directory = File(DIR)
        directory.deleteRecursively()
    }

    private fun resizeImgCurrent() {
        txtText.visibility = if (txtText.visibility == VISIBLE) GONE else VISIBLE
    }

    private fun showCopyShareFor(msec:Long) {

        Thread(Runnable {

            runOnUiThread {
                btnShare.visibility = VISIBLE
                btnCopyClipboard.visibility = VISIBLE
            }

            SystemClock.sleep(msec)

            runOnUiThread {
                btnShare.visibility = GONE
                btnCopyClipboard.visibility = GONE
            }

        }).start()
    }

    private fun showAboutDialog() {
        val alert:AlertDialog = AlertDialog.Builder(this)
            .setTitle("About ...")
            .setMessage("\n\tTextify!\n\n\tby Iskandar Mazzawi\n\tiskandar1123@hotmail.com\n\n\tpowered by:\n\t\tcloudinary.com\n\t\tocr.space/ocrapi\n")
            .setIcon(R.drawable.ic_info_orange)
            .setPositiveButton("OK"){d,_->d.dismiss()}
            .create()
        alert.setCanceledOnTouchOutside(false)
        alert.show()
    }


    private fun shootAndTextify() {
       shoot()
    }

    private fun shoot() {
        // check permissions, if we have them .... before shooting ! //
        if (!havePermission) {
            Toast.makeText(this, "missing permissions!",
                Toast.LENGTH_SHORT).show()
            checkForPermission(); return
        }

        // by this point, we have all needed permissions ....

        //to avoid api26+ policy restrictions
        val builder = StrictMode.VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        // check external storage STATE FIRST ! //
        if (!extStorageOK()) return  // exit, no need to continue, as photo cannot be stored ! //
        val file = assignFile() // assign file & create directory if NOT exist

        // android image capture + add filepath to store it (if taken by user eventually!) //
        val intent = Intent("android.media.action.IMAGE_CAPTURE")
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file))
        //since it can create an exception, we surround with try/catch to avoid app-crash
        try {
            //call the intent and wait for result after intent is closed
            startActivityForResult(intent, CAMERA_RESULT)
        } catch (e: Exception) {
            Log.e("Err_PHOTO_SHOOT", "shootPhoto():Intent_Exception " + e.message)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode== Activity.RESULT_OK && requestCode== CAMERA_RESULT)
        {
            // get saved file into a bitmap object
            val file = File(fileName)

            //read the image from the file and convert it to a bitmap
            val opts = BitmapFactory.Options()
            opts.inSampleSize = 5
            val myBitmap = BitmapFactory.decodeFile(file.absolutePath,opts)

            // compress bitmap  // already compress by 1/5 (sample size) //
            val out = FileOutputStream(file)
            myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)

            // update views !
            imgCurrent.setImageBitmap(myBitmap)
            imgCurrent.visibility = VISIBLE
            imgDefault.visibility = GONE
            setTextifyResult(fileName)

        }
    }

    private fun setTextifyResult(path: String)
    {
        MediaManager.get().upload(path)
            .unsigned("f9lwsqzh")
            .option("folder","textify")
            .callback(object : UploadCallback {
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onStart(requestId: String?) {
                    textResult = "analyzing ..."
                    txtText.text = textResult
                }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) { errMsg() }
                override fun onError(requestId: String?, error: ErrorInfo?) { errMsg() }

                private fun errMsg() {
                    Toast.makeText(this@MainActivity,
                        "error while uploading img!", Toast.LENGTH_LONG).show()
                    textResult = "error!"
                    txtText.text = textResult
                }

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val url = resultData!!["url"].toString()
                    textResult = analyzeImage(url)
                    txtText.text = textResult
                }
            }
            )
            .dispatch()
    }

    private fun analyzeImage(url: String): String {

        var res = ""


        return  res
    }


    private fun extStorageOK(): Boolean {
        val extStorageState = getExternalStorageState()
        if (MEDIA_MOUNTED != extStorageState) {
            var msg = "Storage ERROR!"
            if (MEDIA_MOUNTED_READ_ONLY == extStorageState) msg = "Storage Media is READ-ONLY!"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun assignFile(): File {
        fileName = DIR + separator + "img_" + getStamp() + ".jpg"
        createDir(DIR) // if NOT exist! //
        return File(fileName)
    }

    private fun getStamp(): String { // time & device stamp !!

        // for now , only time stamp
        return System.currentTimeMillis().toString()

        // TODO for later
                // add device stamp:
                // either IMEI ,
                // or combination of Router's IP + MAC address of device!
                // or GPS location (not preferred!)
                // i.e. anything that uniquely identifies the device
    }

    private fun createDir(dirName: String) {
        val directory = File(dirName)
        // create directory, if NOT exist! // If the parent dir doesn't exist, create it //
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                Log.e("DIR", "Successfully created the parent dir:" + directory.name)
            } else {
                Log.e("DIR", "Failed to create the parent dir:" + directory.name)
            }
        }
    }

}
