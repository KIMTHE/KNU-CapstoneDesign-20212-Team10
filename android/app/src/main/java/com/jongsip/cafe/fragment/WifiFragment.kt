package com.jongsip.cafe.fragment

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.jongsip.cafe.activity.MainActivity
import com.jongsip.cafe.R
import com.jongsip.cafe.util.PackageManagerUtils
import com.jongsip.cafe.util.PermissionUtils
import com.jongsip.cafe.util.WifiLoginUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.ArrayList
import kotlin.concurrent.timer

class WifiFragment : Fragment() {
    private lateinit var imgWifi: ImageView
    lateinit var textWifiStatus: TextView
    lateinit var btnCamera: ImageButton

    private var lastSuggestedNetwork: WifiNetworkSuggestion? = null
    var wifiManager: WifiManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wifiManager = requireContext().getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager
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

        getWifiSSID()
        //1초마다 현재 wifi 상태 갱신
        timer(period = 1000) {
            requireActivity().runOnUiThread {
                when (PermissionUtils.requestLocationPermission(requireActivity())) {
                    PermissionUtils.PERMISSION_CODE_ACCEPTED -> getWifiSSID()
                }
            }
        }


        return rootView
    }

    //카메라 시작
    private fun startCamera() {
        if (PermissionUtils.requestPermission(
                requireActivity(),
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.CAMERA
            )
        ) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val photoUri = FileProvider.getUriForFile(
                requireContext(),
                requireActivity().applicationContext.packageName + ".provider",
                cameraFile
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST)
        }
    }

    //카메라,갤러리 파일반환
    private val cameraFile: File
        get() {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            return File(dir, FILE_NAME)
        }

    //카메라에서 찍은 후 콜백
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            uploadImage(data!!.data)
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK) {
            val photoUri = FileProvider.getUriForFile(
                requireActivity(),
                requireContext().packageName + ".provider",
                cameraFile
            )
            uploadImage(photoUri)
        }
    }

    //권한요청결과 콜백
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

    //google cloud vision 에 이미지 uri 업로드
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
                Log.d(TAG, "Image picking failed because " + e.message)
                Toast.makeText(activity, R.string.image_picker_error, Toast.LENGTH_LONG).show()
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.")
            Toast.makeText(activity, R.string.image_picker_error, Toast.LENGTH_LONG).show()
        }
    }

    //convertResponseToString 메소드를 비동기로 수행함
    private fun callCloudVision(bitmap: Bitmap) {
        // Do the real work in an async task, because we need to use the network anyway
        try {
            val labelDetectionTask: LabelDetectionTask =
                LabelDetectionTask(this, prepareAnnotationRequest(bitmap))
            labelDetectionTask.execute()


        } catch (e: IOException) {
            Log.d(
                TAG, "failed to make API request because of other IOException " +
                        e.message
            )
        }
    }

    //비동기처리를 수행함
    class LabelDetectionTask internal constructor(
        fragment: WifiFragment,
        annotate: Vision.Images.Annotate
    ) : AsyncTask<Any?, Void?, String>() {
        private val mActivityWeakReference: WeakReference<WifiFragment> = WeakReference(fragment)
        private val mRequest: Vision.Images.Annotate = annotate

        override fun doInBackground(vararg params: Any?): String? {
            try {
                //Log.d(MainActivity.TAG, "created Cloud Vision request object, sending request")
                val response = mRequest.execute()
                return convertResponseToString(response)
            } catch (e: GoogleJsonResponseException) {
                //Log.d(MainActivity.TAG, "failed to make API request because " + e.content)
            } catch (e: IOException) {
                Log.d(
                    TAG, "failed to make API request because of other IOException " +
                            e.message
                )
            }
            return "Cloud Vision API 요청이 실패했습니다. 자세한 내용은 로그를 확인하세요."
        }

        //이 Task 에서(즉 이 스레드에서) 수행되던 작업이 종료되었을 때 호출됨
        override fun onPostExecute(result: String) {
            val fragment = mActivityWeakReference.get()
            if (fragment != null) {
                connectWithOCR(fragment)
            }
        }

    }

    //google cloud vision 에 요청준비
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
        Log.d(TAG, "created Cloud Vision request object, sending request")
        return annotateRequest
    }

    //대역폭을 절약하기 위해 이미지 크기 조정
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




    //정적 property, method
    companion object {
        private const val CLOUD_VISION_API_KEY = "AIzaSyADg5z34EPSdRj4lbgYxS9FEU3ExQQfSfc"
        const val FILE_NAME = "temp.jpg"
        private const val ANDROID_CERT_HEADER = "X-Android-Cert"
        private const val ANDROID_PACKAGE_HEADER = "X-Android-Package"
        private const val MAX_LABEL_RESULTS = 10
        private const val MAX_DIMENSION = 1200
        val TAG = WifiFragment::class.java.simpleName
        private const val GALLERY_IMAGE_REQUEST = 1
        const val CAMERA_PERMISSIONS_REQUEST = 2
        const val CAMERA_IMAGE_REQUEST = 3

        //ocr 프로세스에서 최종변환
        fun convertResponseToString(response: BatchAnnotateImagesResponse): String {
            var message = "I found these things:\n\n"
            val labels = response.responses[0].textAnnotations
            message = if (labels != null) {
                labels[0].description
            } else {
                "nothing"
            }

            WifiLoginUtils.extract(message)

            return message
        }

        //모든 경우로 로그인 시도
        fun connectWithOCR(wifiFragment: WifiFragment) {

            if (WifiLoginUtils.idCase.size == 0 || WifiLoginUtils.pwCase.size == 0)
                wifiFragment.showToast(wifiFragment.getString(R.string.image_picker_error))
            else {
                for (id in WifiLoginUtils.idCase) {
                    for (pw in WifiLoginUtils.pwCase) {
                        wifiFragment.wifiManager!!.disconnect()
                        wifiFragment.connectUsingNetworkSuggestion(ssid = id, password = pw)
                        wifiFragment.wifiManager!!.reconnect()

                        val ssid = wifiFragment.getWifiSSID()
                        if (ssid != null && ssid == id) break
                    }
                }
            }
        }
    }


    /*와이파이 이름을 얻기 위한 부분*/
    private fun getWifiSSID(): String? {
        val mWifiManager: WifiManager =
            (requireActivity().applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
        val info: WifiInfo = mWifiManager.connectionInfo

        if (info.supplicantState === SupplicantState.COMPLETED) {
            val ssid: String = info.ssid
            if (ssid == "<unknown ssid>") {
                textWifiStatus.text = getString(R.string.text_not_connect)
                imgWifi.setImageResource(R.drawable.ic_baseline_wifi_off_24)
            } else {
                textWifiStatus.text = getString(R.string.text_wifi_connect, ssid)
                imgWifi.setImageResource(R.drawable.ic_baseline_wifi_24)
                Log.d("wifi name", ssid)

                return ssid
            }
        } else {
            Log.d("wifi name", "could not obtain the wifi name")
            textWifiStatus.text = getString(R.string.text_not_connect)
            imgWifi.setImageResource(R.drawable.ic_baseline_wifi_off_24)
        }
        return null
    }

    /*와이파이 연결을 위한 부분*/
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
                showToast("연결중...")
            }
        }

        requireActivity().registerReceiver(broadcastReceiver, intentFilter)

        lastSuggestedNetwork?.let {
            val status = wifiManager!!.removeNetworkSuggestions(listOf(it))
            Log.i("WifiNetworkSuggestion", "Removing Network suggestions status is $status")
        }
        val suggestionsList = listOf(wifiNetworkSuggestion)

        var status = wifiManager!!.addNetworkSuggestions(suggestionsList)
        Log.i("WifiNetworkSuggestion", "Adding Network suggestions status is $status")
        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE) {
            showToast("이미 등록되있습니다")
            status = wifiManager!!.removeNetworkSuggestions(suggestionsList)
            Log.i("WifiNetworkSuggestion", "Removing Network suggestions status is $status")
            status = wifiManager!!.addNetworkSuggestions(suggestionsList)
        }

        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            lastSuggestedNetwork = wifiNetworkSuggestion
            showToast("연결성공")
        }
    }

    fun showToast(s: String) {
        Toast.makeText(requireActivity().applicationContext, s, Toast.LENGTH_LONG).show()
    }

}