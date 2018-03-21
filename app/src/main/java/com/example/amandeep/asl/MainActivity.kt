package com.example.amandeep.asl

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.annotation.RequiresApi
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import android.widget.*
import com.example.mohammed_2284.simpleapp.retrofit.APIKindaStuff
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.google.gson.JsonObject
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import okhttp3.ResponseBody

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*

import java.util.*

class MainActivity : AppCompatActivity() {
    private var imgPath: String? = null
    private val TAKE_PHOTO_REQUEST = 101
    var imgvPhoto: SimpleDraweeView? = null
    private var encodedString: String? = null

    private var mCurrentPhotoPath: String = ""
    var mainContainer: ConstraintLayout? = null

    private val imageName: String
        get() = "/DCIM/" + "image" + Date().time + ".png"

    override fun onCreate(savedInstanceState: Bundle?) {
        Fresco.initialize(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ib = findViewById(R.id.btnRetry) as Button
        ib.setOnClickListener {

            validatePermissions();
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
     //   finish()
        //`super.onActivityResult(requestCode, resultCode, data)
        Toast.makeText(applicationContext, resultCode.toString() + "hi", Toast.LENGTH_SHORT).show()
      //  processCapturedPhoto()

        if (resultCode == Activity.RESULT_OK) {
          processCapturedPhoto()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        //    finish()
        }

    }
    private fun processCapturedPhoto() {
        val cursor = contentResolver.query(Uri.parse(mCurrentPhotoPath),
                Array(1) { android.provider.MediaStore.Images.ImageColumns.DATA },
                null, null, null)
        cursor.moveToFirst()
        val photoPath = cursor.getString(0)
        cursor.close()
        val file = File(photoPath)
        val uri = Uri.fromFile(file)

        val height = resources.getDimensionPixelSize(R.dimen.photo_height)
        val width = resources.getDimensionPixelSize(R.dimen.photo_width)

        val request = ImageRequestBuilder.newBuilderWithSource(uri)
                .setResizeOptions(ResizeOptions(width, height))
                .build()
        val controller = Fresco.newDraweeControllerBuilder()
                .setOldController(imgvPhoto?.controller)
                .setImageRequest(request)
                .build()
        val inputStream = FileInputStream(photoPath)//You can get an inputStream using any IO API
        val img: Bitmap? = BitmapFactory.decodeStream(inputStream);
        val baos = ByteArrayOutputStream()
        img?.compress(Bitmap.CompressFormat.JPEG, 50, baos) //bm is the bitmap object
        val b = baos.toByteArray()
        val imgView = findViewById(R.id.imageView1) as ImageView
        imgView.setImageBitmap(img)
         encodedString = Base64.getEncoder().encodeToString(b)
         AsyncTaskExample().execute(encodedString)

        imgvPhoto?.controller = controller
    }

    private fun validatePermissions() {
        Dexter.withActivity(this)
                .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)

                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        val values = ContentValues(1)
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
                        val fileUri = contentResolver
                                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        values)
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        if (intent.resolveActivity(packageManager) != null) {
                            mCurrentPhotoPath = fileUri.toString()
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            startActivityForResult(intent, TAKE_PHOTO_REQUEST)
                        }                    }
                })
                .check()
    }

    inner class AsyncTaskExample : AsyncTask<String, String, String>() {

        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun doInBackground(vararg p0: String?): String {
            val jsonObj = JsonObject()
            jsonObj.addProperty("title", "rhythm")
            jsonObj.addProperty("singer", "meee")
            jsonObj.addProperty("text", encodedString)
            //  POST demo
            APIKindaStuff
                    .service
                    .getVectors(jsonObj)
                    .enqueue(object : Callback<ResponseBody> {
                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            println("---TTTT :: POST Throwable EXCEPTION:: " + t.message)
                        }

                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            if (response.isSuccessful) {
                                val msg = response.body()?.string()
                                println("---TTTT :: POST msg from server :: " + msg)
                                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                                var msgSVM= msg?.subSequence(1,2);
                                var msgMLP= msg?.subSequence(2,3);
                                val textViewSVM = findViewById(R.id.textSVM) as TextView
                                val textViewMLP = findViewById(R.id.textViewMLP) as TextView
                                textViewSVM.text = msgSVM
                                textViewMLP.text = msgMLP

                            }
                        }
                    })
            return ""

        }


        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

        }
    }


}
