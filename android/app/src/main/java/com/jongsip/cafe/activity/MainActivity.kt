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
package com.jongsip.cafe.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jongsip.cafe.R
import com.jongsip.cafe.fragment.MapFragment
import com.jongsip.cafe.fragment.SettingFragment
import com.jongsip.cafe.fragment.WifiFragment
import android.content.pm.PackageManager

import android.content.pm.PackageInfo
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(), MapFragment.OnDataPassListener  {
    private lateinit var bottomNavigation: BottomNavigationView
    lateinit var currentCafeName : String
    lateinit var currentCafeUrl : String

    //최근에 뒤로가기 버튼을 누른 시각각
    private var backPressedTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navi)

        replaceFragment(MapFragment(),"map")

        bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.menu_map -> {
                    replaceFragment(MapFragment(), "map")
                }
                R.id.menu_wifi -> {
                    replaceFragment(WifiFragment(), "wifi")
                }
                else -> {
                    replaceFragment(SettingFragment(), "setting")
                }
            }
            true
        }
    }

    private fun replaceFragment(fragmentClass: Fragment, tag: String) {
        val bundle = Bundle()
        bundle.putString("currentCafeName", currentCafeName)
        bundle.putString("currentCafeUrl", currentCafeUrl)
        fragmentClass.arguments = bundle //유저 정보를 넘겨줌

        supportFragmentManager.popBackStackImmediate(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_frame, (fragmentClass), tag)
            .addToBackStack(tag).commit()
    }


    //뒤로가기버튼을 누를때 콜백
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1) {
            val tempTime = System.currentTimeMillis()
            val intervalTime = tempTime - backPressedTime

            //2초이내 한번 더 뒤로가기 눌렀을 때, 종료
            if (!(0 > intervalTime || 2000 < intervalTime)) {
                finishAffinity()
                System.runFinalization()
                exitProcess(0)
            } else {
                backPressedTime = tempTime
                Toast.makeText(this, "'뒤로' 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()
                return
            }
        }
        super.onBackPressed()
        updateBottomMenu()
    }

    //태그를 통해 현재 프래그먼트를 찾아서, 메뉴활성화
    private fun updateBottomMenu() {
        when (nowFragment()) {
            "map" -> bottomNavigation.menu.findItem(R.id.menu_map).isChecked = true
            "wifi" -> bottomNavigation.menu.findItem(R.id.menu_wifi).isChecked = true
            "setting" -> bottomNavigation.menu.findItem(R.id.menu_setting).isChecked = true
        }

    }

    //태그를 통해 현재 프래그먼트를 찾아서, 태그반환
    private fun nowFragment(): String {
        val tag1: Fragment? = supportFragmentManager.findFragmentByTag("map")
        val tag2: Fragment? = supportFragmentManager.findFragmentByTag("wifi")
        val tag3: Fragment? = supportFragmentManager.findFragmentByTag("setting")

        return if (tag1 != null && tag1.isVisible) {
            "map"
        } else if (tag2 != null && tag2.isVisible) {
            "wifi"
        } else if (tag3 != null && tag3.isVisible) {
            "setting"
        } else
            "close"

    }

    //MapFragment 에서 위도 경도 정보 받음
    override fun onDataPass(cafeName: String, cafeUrl: String) {
        currentCafeName = cafeName
        currentCafeUrl = cafeUrl
    }
}