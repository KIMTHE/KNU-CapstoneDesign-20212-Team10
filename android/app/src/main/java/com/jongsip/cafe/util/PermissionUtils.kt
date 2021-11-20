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
package com.jongsip.cafe.util

import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.jongsip.cafe.MainActivity
import java.util.ArrayList

// 권환 요청관련 util method
object PermissionUtils {
    const val PERMISSION_CODE_ACCEPTED = 1
    const val PERMISSION_CODE_NOT_AVAILABLE = 0


    fun requestPermission(
        activity: androidx.appcompat.app.AppCompatActivity?, requestCode: Int, vararg permissions: String
    ): Boolean {
        var granted = true
        val permissionsNeeded = ArrayList<String>()
        for (s in permissions) {
            val permissionCheck = ContextCompat.checkSelfPermission(activity!!, s)
            val hasPermission = permissionCheck == PackageManager.PERMISSION_GRANTED
            granted = granted and hasPermission
            if (!hasPermission) {
                permissionsNeeded.add(s)
            }
        }
        return if (granted) {
            true
        } else {
            ActivityCompat.requestPermissions(
                activity!!,
                permissionsNeeded.toTypedArray(),
                requestCode
            )
            false
        }
    }

    fun permissionGranted(
        requestCode: Int, permissionCode: Int, grantResults: IntArray
    ): Boolean {
        return requestCode == permissionCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    /*wifi 관련 권한 요청*/
    fun requestLocationPermission(activity: androidx.appcompat.app.AppCompatActivity?): Int {
        if (ContextCompat.checkSelfPermission(
                activity!!,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
            } else {
                // request permission
                ActivityCompat.requestPermissions(
                    activity,
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

}