package com.jongsip.cafe.fragment

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequest
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.jongsip.cafe.BuildConfig
import com.jongsip.cafe.R
import com.jongsip.cafe.model.wifiIdPw
import com.jongsip.cafe.util.PackageManagerUtils
import com.jongsip.cafe.util.PermissionUtils
import com.jongsip.cafe.util.WifiLoginUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.timer

class WifiFragment : Fragment() {
    private lateinit var imgWifi: ImageView
    lateinit var textWifiStatus: TextView
    lateinit var btnCamera: ImageButton

    private var lastSuggestedNetwork: WifiNetworkSuggestion? = null
    var wifiManager: WifiManager? = null
    lateinit var checkWifiThread: Timer
    var timeStamp: String? = null

    lateinit var fireStore: FirebaseFirestore
    lateinit var dialogBuilder: AlertDialog.Builder
    lateinit var currentCafeName: String
    lateinit var currentCafeUrl: String

    var tryId: String? = null
    var tryPw: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fireStore = FirebaseFirestore.getInstance()
        wifiManager =
            requireContext().getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager
        //MainActivity ?????? ????????????, url ?????? ?????????
        currentCafeName = arguments?.getString("currentCafeName").toString()
        currentCafeUrl = arguments?.getString("currentCafeUrl").toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val rootView = inflater.inflate(R.layout.fragment_wifi, container, false)

        imgWifi = rootView.findViewById(R.id.img_wifi)
        textWifiStatus = rootView.findViewById(R.id.text_wifi_status)
        btnCamera = rootView.findViewById(R.id.btn_camera)

        btnCamera.setOnClickListener {
            startCamera()
        }

        dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle(currentCafeName)
        dialogBuilder.setIcon(R.drawable.cafe_icon)
        val listener = DialogInterface.OnClickListener { _, p1 ->
            when (p1) {
                //????????? ????????????????????? ??????
                DialogInterface.BUTTON_POSITIVE -> {
                    val temp: String = currentCafeUrl.substring(27)
                    Log.d("???????????? : ", temp)
                    fireStore.collection("wifiInfo").document(temp).set(wifiIdPw(tryId!!, tryPw!!))
                }
            }
        }
        dialogBuilder.setPositiveButton("?????????!", listener)
        dialogBuilder.setNegativeButton("????????????..", listener)

        return rootView
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(tag, "onReceive: $tryId, $tryPw")
            if (!intent.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                return
            }
            showToast("?????????...")

            //?????? ?????????
            dialogBuilder.show()

        }
    }

    override fun onPause() {
        super.onPause()
        checkWifiThread.cancel()
        requireContext().unregisterReceiver(broadcastReceiver)
    }

    override fun onResume() {
        super.onResume()
        //1????????? ?????? wifi ?????? ??????
        checkWifiThread = timer(period = 1000) {
            requireActivity().runOnUiThread {
                when (PermissionUtils.requestLocationPermission(requireActivity())) {
                    PermissionUtils.PERMISSION_CODE_ACCEPTED -> getWifiSSID()
                }
            }
        }

        val intentFilter =
            IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)
        requireContext().registerReceiver(broadcastReceiver, intentFilter)
    }

    //????????? ??????
    private fun startCamera() {
        if (PermissionUtils.requestPermission(
                requireActivity(),
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.CAMERA
            )
        ) {
            timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().applicationContext.packageName + ".provider",
                cameraFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            //intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST)
        }
    }

    //?????????,????????? ????????????
    private val cameraFile: File
        get() {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            return File(dir, "${timeStamp}_temp.jpg")
        }

    //??????????????? ?????? ??? ??????
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            val photoUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().packageName + ".provider",
                cameraFile
            )
            uploadImage(photoUri)
        }
    }

    //?????????????????? ??????
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
        }
    }

    //google cloud vision ??? ????????? uri ?????????
    private fun uploadImage(uri: Uri?) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                val bitmap = scaleBitmapDown(
                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri),
                    MAX_DIMENSION
                )
                callCloudVision(bitmap)
                //mMainImage!!.setImageBitmap(bitmap)
            } catch (e: IOException) {
                Log.d(TAG, "uploadImage : Image picking failed because " + e.message)
                Toast.makeText(activity, R.string.image_picker_error, Toast.LENGTH_LONG).show()
            }
        } else {
            Log.d(TAG, "uploadImage : Image picker gave us a null image.")
            Toast.makeText(activity, R.string.image_picker_error, Toast.LENGTH_LONG).show()
        }
    }

    //convertResponseToString ???????????? ???????????? ?????????
    private fun callCloudVision(bitmap: Bitmap) {
        // Do the real work in an async task, because we need to use the network anyway
        try {
            val labelDetectionTask: LabelDetectionTask =
                LabelDetectionTask(this, prepareAnnotationRequest(bitmap))
            labelDetectionTask.execute()


        } catch (e: IOException) {
            Log.d(
                TAG, "callCloudVision: failed to make API request because of other IOException " +
                        e.message
            )
        }
    }

    //?????????????????? ?????????
    inner class LabelDetectionTask internal constructor(
        fragment: WifiFragment,
        annotate: Vision.Images.Annotate
    ) : AsyncTask<Any?, Void?, String>() {
        private val mActivityWeakReference: WeakReference<WifiFragment> = WeakReference(fragment)
        private val mRequest: Vision.Images.Annotate = annotate

        override fun doInBackground(vararg params: Any?): String? {
            try {
                Log.d(TAG, "doInBackground: created Cloud Vision request object, sending request")
                val response = mRequest.execute()
                return convertResponseToString(response)
            } catch (e: GoogleJsonResponseException) {
                Log.d(TAG, "doInBackground : failed to make API request because " + e.content)
            } catch (e: IOException) {
                Log.d(
                    TAG,
                    "doInBackground : failed to make API request because of other IOException " +
                            e.message
                )
            }
            return "Cloud Vision API ????????? ??????????????????. ????????? ????????? ????????? ???????????????."
        }

        //??? Task ??????(??? ??? ???????????????) ???????????? ????????? ??????????????? ??? ?????????
        override fun onPostExecute(result: String) {
            val fragment = mActivityWeakReference.get()
            if (fragment != null) {
                connectWithOCR(fragment)
            }
        }
    }

    //google cloud vision ??? ????????????
    @Throws(IOException::class)
    private fun prepareAnnotationRequest(bitmap: Bitmap): Vision.Images.Annotate {
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
                val packageName = activity!!.packageName
                visionRequest.requestHeaders[ANDROID_PACKAGE_HEADER] = packageName
                val sig = PackageManagerUtils.getSignature(activity!!.packageManager, packageName)
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
        Log.d(TAG, "prepareAnnotationRequest: created Cloud Vision request object, sending request")
        return annotateRequest
    }

    //???????????? ???????????? ?????? ????????? ?????? ??????
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

    //?????? property, method
    companion object {
        private var CLOUD_VISION_API_KEY = Base64.decode(BuildConfig.google_vision_api_encoded,Base64.DEFAULT).toString()
        private const val ANDROID_CERT_HEADER = "X-Android-Cert"
        private const val ANDROID_PACKAGE_HEADER = "X-Android-Package"
        private const val MAX_DIMENSION = 1200
        val TAG: String = WifiFragment::class.java.simpleName
        const val CAMERA_PERMISSIONS_REQUEST = 2
        const val CAMERA_IMAGE_REQUEST = 3

        //ocr ?????????????????? ????????????
        fun convertResponseToString(response: BatchAnnotateImagesResponse): String {
            var message = "I found these things:\n\n"
            val labels = response.responses[0].textAnnotations
            message = if (labels != null) {
                labels[0].description
            } else {
                "nothing"
            }

            WifiLoginUtils.extract(message)

            Log.d(TAG, "convertResponseToString: $message")
            return message
        }

        //?????? ????????? ????????? ??????
        fun connectWithOCR(
            wifiFragment: WifiFragment
        ) {
            if (WifiLoginUtils.idCase.size == 0 || WifiLoginUtils.pwCase.size == 0)
                wifiFragment.showToast(wifiFragment.getString(R.string.image_picker_error))
            else {
                for (id in WifiLoginUtils.idCase) {
                    for (pw in WifiLoginUtils.pwCase) {
                        if (wifiFragment.getWifiSSID() != null) break
                        wifiFragment.tryId = id
                        wifiFragment.tryPw = pw

                        wifiFragment.wifiManager!!.disconnect()
                        wifiFragment.connectUsingNetworkSuggestion(ssid = id, password = pw)
                        wifiFragment.wifiManager!!.reconnect()

                        wifiFragment.dialogBuilder.setMessage("?????? ?????????????????? ????????? ??????????\nID: $id\nPW: $pw")
                        wifiFragment.dialogBuilder.show()

                    }
                }
            }
        }
    }


    /*???????????? ????????? ?????? ?????? ??????*/
    private fun getWifiSSID(): String? {
        val mWifiManager: WifiManager =
            (requireActivity().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
        val info: WifiInfo = mWifiManager.connectionInfo

        if (info.supplicantState === SupplicantState.COMPLETED) {
            val ssid: String = info.ssid
            if (ssid == "<unknown ssid>") {
                textWifiStatus.text = getString(R.string.text_not_connect)
                imgWifi.setImageResource(R.drawable.no_wifi)
            } else {
                textWifiStatus.text = getString(R.string.text_wifi_connect, ssid)
                imgWifi.setImageResource(R.drawable.wifi)
                Log.d(TAG, "getWifiSSID: $ssid")

                return ssid
            }
        } else {
            Log.d(TAG, "getWifiSSID: could not obtain the wifi name")
            textWifiStatus.text = getString(R.string.text_not_connect)
            imgWifi.setImageResource(R.drawable.no_wifi)
        }
        return null
    }

    /*???????????? ????????? ?????? ??????*/
    private fun connectUsingNetworkSuggestion(ssid: String, password: String) {

        val wifiNetworkSuggestion = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        lastSuggestedNetwork?.let {
            val status = wifiManager!!.removeNetworkSuggestions(listOf(it))
            Log.i("WifiNetworkSuggestion", "Removing Network suggestions status is $status")
        }
        val suggestionsList = listOf(wifiNetworkSuggestion)

        var status = wifiManager!!.addNetworkSuggestions(suggestionsList)
        Log.i("WifiNetworkSuggestion", "Adding Network suggestions status is $status")
        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE) {
            showToast("?????? ?????????????????????")
            status = wifiManager!!.removeNetworkSuggestions(suggestionsList)
            Log.i("WifiNetworkSuggestion", "Removing Network suggestions status is $status")
            status = wifiManager!!.addNetworkSuggestions(suggestionsList)
        }

        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            lastSuggestedNetwork = wifiNetworkSuggestion
            showToast("????????????")
        }
    }

    fun showToast(s: String) {
        Toast.makeText(requireActivity().applicationContext, s, Toast.LENGTH_LONG).show()
    }

}