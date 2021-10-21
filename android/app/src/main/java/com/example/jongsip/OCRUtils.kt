package com.example.jongsip

import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.vision.v1.Vision
import java.io.IOException
import java.lang.ref.WeakReference

object OCRUtils {

    //대역폭을 절약하기 위해 이미지 크기 조정
    fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
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

    //비동기처리를 수행함
    class LableDetectionTask internal constructor(
        activity: MainActivity,
        annotate: Vision.Images.Annotate
    ) : AsyncTask<Any?, Void?, String>() {
        private val mActivityWeakReference: WeakReference<MainActivity> = WeakReference(activity)
        private val mRequest: Vision.Images.Annotate = annotate

        override fun doInBackground(vararg params: Any?): String? {
            try {
                Log.d(MainActivity.TAG, "created Cloud Vision request object, sending request")
                val response = mRequest.execute()
                return MainActivity.convertResponseToString(response)
            } catch (e: GoogleJsonResponseException) {
                Log.d(MainActivity.TAG, "failed to make API request because " + e.content)
            } catch (e: IOException) {
                Log.d(
                    MainActivity.TAG, "failed to make API request because of other IOException " +
                            e.message
                )
            }
            return "Cloud Vision API 요청이 실패했습니다. 자세한 내용은 로그를 확인하세요."
        }

        override fun onPostExecute(result: String) {
            val activity = mActivityWeakReference.get()
        }

    }
}