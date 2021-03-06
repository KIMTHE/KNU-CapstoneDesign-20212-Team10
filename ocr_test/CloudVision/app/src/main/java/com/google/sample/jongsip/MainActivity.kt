/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jongsip

import android.Manifest
import android.content.*
import android.widget.TextView
import android.os.Bundle
import android.provider.MediaStore
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.widget.Toast
//import kotlin.Throws
import com.google.api.services.vision.v1.Vision.Images.Annotate
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.VisionRequest
import com.google.api.services.vision.v1.Vision
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.JsonFactory
import com.google.api.services.vision.v1.model.*
import com.google.sample.jongsip.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.ArrayList
import kotlin.concurrent.timer

class MainActivity : AppCompatActivity() {
    //private var mImageDetails: TextView? = null
    //private var mMainImage: ImageView? = null

    lateinit var wifi_image: ImageView
    lateinit var wifi_status: TextView
    private var lastSuggestedNetwork: WifiNetworkSuggestion? = null
    var wifiManager: WifiManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_init)
        val camera_button = findViewById<ImageButton>(R.id.camera_button)

        camera_button.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(this@MainActivity)
            builder
                .setMessage(R.string.dialog_select_prompt)
                .setPositiveButton(R.string.dialog_select_gallery) { dialog: DialogInterface?, which: Int -> startGalleryChooser() }
                .setNegativeButton(R.string.dialog_select_camera) { dialog: DialogInterface?, which: Int -> startCamera() }
            builder.create().show()
        }

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        wifi_image = findViewById(R.id.wifi_image)
        wifi_status = findViewById(R.id.wifi_st)
        getWifiSSID()

        wifiManager!!.disconnect()
        //connectUsingNetworkSuggestion(ssid = "???????????? ?????????", password = "???????????? ????????????")
        wifiManager!!.reconnect()

        //mImageDetails = findViewById(R.id.wifi_unconnected)
        //mMainImage = findViewById(R.id.wifi_image)

        timer(period = 1000) {
            runOnUiThread {
                when (requestLocationPermission()) {
                    PERMISSION_CODE_ACCEPTED -> getWifiSSID()
                }
            }
        }
    }

    //??????????????? ??????
    private fun startGalleryChooser() {
        if (PermissionUtils.requestPermission(
                this,
                GALLERY_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Select a photo"),
                GALLERY_IMAGE_REQUEST
            )
        }
    }

    //????????? ??????
    private fun startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
        ) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoUri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".provider",
                cameraFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST)
        }
    }

    private val cameraFile: File
        get() {
            val dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            return File(dir, FILE_NAME)
        }

    //???????????? ??????????????? ?????? ??? ??????
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK) {
            uploadImage(data!!.data)
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            val photoUri = FileProvider.getUriForFile(
                this,
                applicationContext.packageName + ".provider",
                cameraFile
            )
            uploadImage(photoUri)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSIONS_REQUEST -> if (PermissionUtils.permissionGranted(
                    requestCode,
                    CAMERA_PERMISSIONS_REQUEST,
                    grantResults
                )
            ) {
                startCamera()
            }
            GALLERY_PERMISSIONS_REQUEST -> if (PermissionUtils.permissionGranted(
                    requestCode,
                    GALLERY_PERMISSIONS_REQUEST,
                    grantResults
                )
            ) {
                startGalleryChooser()
            }
        }
    }

    private fun uploadImage(uri: Uri?) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                val bitmap = scaleBitmapDown(
                    MediaStore.Images.Media.getBitmap(contentResolver, uri),
                    MAX_DIMENSION
                )
                callCloudVision(bitmap)
                //mMainImage!!.setImageBitmap(bitmap)
            } catch (e: IOException) {
                Log.d(TAG, "Image picking failed because " + e.message)
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show()
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.")
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show()
        }
    }

    @Throws(IOException::class)
    private fun prepareAnnotationRequest(bitmap: Bitmap): Annotate {
        val httpTransport = AndroidHttp.newCompatibleTransport()
        val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()
        val requestInitializer: VisionRequestInitializer = object : VisionRequestInitializer(
            CLOUD_VISION_API_KEY
        ) {
            /**
             * We override this so we can inject important identifying fields into the HTTP
             * headers. This enables use of a restricted cloud platform API key.
             */
            @Throws(IOException::class)
            override fun initializeVisionRequest(visionRequest: VisionRequest<*>) {
                super.initializeVisionRequest(visionRequest)
                val packageName = packageName
                visionRequest.requestHeaders[ANDROID_PACKAGE_HEADER] = packageName
                val sig = PackageManagerUtils.getSignature(packageManager, packageName)
                visionRequest.requestHeaders[ANDROID_CERT_HEADER] = sig
            }
        }
        val builder = Vision.Builder(httpTransport, jsonFactory, null)
        builder.setVisionRequestInitializer(requestInitializer)
        val vision = builder.build()
        val batchAnnotateImagesRequest = BatchAnnotateImagesRequest()
        batchAnnotateImagesRequest.requests = object : ArrayList<AnnotateImageRequest?>() {
            init {
                val annotateImageRequest = AnnotateImageRequest()

                // Add the image
                val base64EncodedImage = Image()
                // Convert the bitmap to a JPEG
                // Just in case it's a format that Android understands but Cloud Vision
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
                val imageBytes = byteArrayOutputStream.toByteArray()

                // Base64 encode the JPEG
                base64EncodedImage.encodeContent(imageBytes)
                annotateImageRequest.image = base64EncodedImage

                // add the features we want
                annotateImageRequest.features = object : ArrayList<Feature?>() {
                    init {
                        val textDetection = Feature()
                        textDetection.type = "TEXT_DETECTION"
                        textDetection.maxResults = 10
                        add(textDetection)
                    }
                }

                // Add the list of one thing to the request
                add(annotateImageRequest)
            }
        }
        val annotateRequest = vision.images().annotate(batchAnnotateImagesRequest)
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.disableGZipContent = true
        Log.d(TAG, "created Cloud Vision request object, sending request")
        return annotateRequest
    }

    private class LableDetectionTask internal constructor(
        activity: MainActivity,
        annotate: Annotate
    ) : AsyncTask<Any?, Void?, String>() {
        private val mActivityWeakReference: WeakReference<MainActivity> = WeakReference(activity)
        private val mRequest: Annotate = annotate
        protected override fun doInBackground(vararg params: Any?): String? {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request")
                val response = mRequest.execute()
                return convertResponseToString(response)
            } catch (e: GoogleJsonResponseException) {
                Log.d(TAG, "failed to make API request because " + e.content)
            } catch (e: IOException) {
                Log.d(
                    TAG, "failed to make API request because of other IOException " +
                            e.message
                )
            }
            return "Cloud Vision API request failed. Check logs for details."
        }

        override fun onPostExecute(result: String) {
            val activity = mActivityWeakReference.get()
            if (activity != null && !activity.isFinishing) {
                val imageDetail = activity.findViewById<TextView>(R.id.wifi_unconnected)
                imageDetail.text = result
            }
        }

    }

    private fun callCloudVision(bitmap: Bitmap) {
        // Switch text to loading
        //mImageDetails!!.setText(R.string.loading_message)

        // Do the real work in an async task, because we need to use the network anyway
        try {
            val labelDetectionTask: LableDetectionTask =
                LableDetectionTask(this, prepareAnnotationRequest(bitmap))
            labelDetectionTask.execute()
        } catch (e: IOException) {
            Log.d(
                TAG, "failed to make API request because of other IOException " +
                        e.message
            )
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        var resizedWidth = maxDimension
        var resizedHeight = maxDimension
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension
            resizedWidth =
                (resizedHeight * originalWidth.toFloat() / originalHeight.toFloat()).toInt()
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension
            resizedHeight =
                (resizedWidth * originalHeight.toFloat() / originalWidth.toFloat()).toInt()
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension
            resizedWidth = maxDimension
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false)
    }

    companion object {
        const val PERMISSION_CODE_ACCEPTED = 1
        const val PERMISSION_CODE_NOT_AVAILABLE = 0


        private const val CLOUD_VISION_API_KEY = "AIzaSyADg5z34EPSdRj4lbgYxS9FEU3ExQQfSfc"
        const val FILE_NAME = "temp.jpg"
        private const val ANDROID_CERT_HEADER = "X-Android-Cert"
        private const val ANDROID_PACKAGE_HEADER = "X-Android-Package"
        private const val MAX_LABEL_RESULTS = 10
        private const val MAX_DIMENSION = 1200
        private val TAG = MainActivity::class.java.simpleName
        private const val GALLERY_PERMISSIONS_REQUEST = 0
        private const val GALLERY_IMAGE_REQUEST = 1
        const val CAMERA_PERMISSIONS_REQUEST = 2
        const val CAMERA_IMAGE_REQUEST = 3
        private fun convertResponseToString(response: BatchAnnotateImagesResponse): String {
            var message = "I found these things:\n\n"

//        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
//        if (labels != null) {
//            for (EntityAnnotation label : labels) {
//                message  = String.format(Locale.US, "%.3f: %s", label.getScore(), label.getDescription());
//                message  = "\n";
//            }
//            //message  = labels.get(0).getDescription();
//        } else {
//            message  = "nothing";
//        }
//
//        return message.toString();
            val labels = response.responses[0].textAnnotations
            message = if (labels != null) {
                labels[0].description
            } else {
                "nothing"
            }
            return message
        }
    }

    /*?????? ??????*/
    fun requestLocationPermission(): Int {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                // request permission
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_CODE_ACCEPTED
                )

            }
        } else {
            // already granted
            return PERMISSION_CODE_ACCEPTED
        }

        // not available
        return PERMISSION_CODE_NOT_AVAILABLE
    }

    /*???????????? ????????? ?????? ?????? ??????*/
    fun getWifiSSID() {
        val mWifiManager: WifiManager = (this.getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager)!!
        val info: WifiInfo = mWifiManager.getConnectionInfo()

        if (info.getSupplicantState() === SupplicantState.COMPLETED) {
            val ssid: String = info.getSSID()
            if(ssid == "<unknown ssid>"){
                wifi_status.setText("????????????")
                wifi_image.setImageResource(R.drawable.ic_baseline_wifi_off_24)
            }
            else {
                wifi_status.setText("?????? ???????????? WIFI : " + ssid)
                wifi_image.setImageResource(R.drawable.ic_baseline_wifi_24)
                Log.d("wifi name", ssid)
            }
        } else {
            Log.d("wifi name", "could not obtain the wifi name")
            wifi_status.setText("????????????")
            wifi_image.setImageResource(R.drawable.ic_baseline_wifi_off_24)
        }
    }

    /*???????????? ????????? ?????? ??????*/
    private fun connectUsingNetworkSuggestion(ssid: String, password: String) {
        val wifiNetworkSuggestion = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()
        val intentFilter =
            IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!intent.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    return
                }
                showToast("Connection Suggestion Succeeded")
            }
        }

        registerReceiver(broadcastReceiver, intentFilter)

        lastSuggestedNetwork?.let {
            val status = wifiManager!!.removeNetworkSuggestions(listOf(it))
            Log.i("WifiNetworkSuggestion", "Removing Network suggestions status is $status")
        }
        val suggestionsList = listOf(wifiNetworkSuggestion)

        var status = wifiManager!!.addNetworkSuggestions(suggestionsList)
        Log.i("WifiNetworkSuggestion", "Adding Network suggestions status is $status")
        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE) {
            showToast("Suggestion Update Needed")
            status = wifiManager!!.removeNetworkSuggestions(suggestionsList)
            Log.i("WifiNetworkSuggestion", "Removing Network suggestions status is $status")
            status = wifiManager!!.addNetworkSuggestions(suggestionsList)
        }
        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            lastSuggestedNetwork = wifiNetworkSuggestion
            showToast("Suggestion Added")
        }
    }

    private fun showToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show()
    }
}