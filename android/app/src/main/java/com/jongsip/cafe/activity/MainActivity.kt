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
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class MainActivity : AppCompatActivity(), MapFragment.OnDataPassListener  {
    private lateinit var bottomNavigation: BottomNavigationView
    lateinit var currentCafeName : String
    lateinit var currentCafeUrl : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(R.id.bottom_navi)

        supportFragmentManager.beginTransaction().add(R.id.fragment_frame, MapFragment())
            .commit()

        bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menu_map -> replaceFragment(MapFragment(),"map")
                R.id.menu_wifi -> replaceFragment(WifiFragment(),"wifi")
                else -> replaceFragment(SettingFragment(),"setting")
            }
            true
        }
    }

    private fun replaceFragment(fragmentClass: Fragment, tag: String) {
        val bundle = Bundle()
        bundle.putString("currentCafeName", currentCafeName)
        bundle.putString("currentCafeUrl", currentCafeUrl)
        fragmentClass.arguments = bundle //유저 정보를 넘겨줌

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_frame, (fragmentClass),tag)
            .addToBackStack(tag).commit()
    }


    //뒤로가기버튼을 누를때 콜백
    override fun onBackPressed() {
        super.onBackPressed()
        updateBottomMenu()
    }

    //태그를 통해 현재 프래그먼트를 찾아서, 메뉴활성화
    private fun updateBottomMenu() {
        val tag1: Fragment? = supportFragmentManager.findFragmentByTag("map")
        val tag2: Fragment? = supportFragmentManager.findFragmentByTag("wifi")
        val tag3: Fragment? = supportFragmentManager.findFragmentByTag("setting")

        if (tag1 != null && tag1.isVisible) {
            bottomNavigation.menu.findItem(R.id.menu_map).isChecked = true
        }
        if (tag2 != null && tag2.isVisible) {
            bottomNavigation.menu.findItem(R.id.menu_wifi).isChecked = true
        }
        if (tag3 != null && tag3.isVisible) {
            bottomNavigation.menu.findItem(R.id.menu_setting).isChecked = true
        }
    }

    //MapFragment 에서 위도 경도 정보 받음
    override fun onDataPass(cafeName: String, cafeUrl: String) {
        currentCafeName = cafeName
        currentCafeUrl = cafeUrl
    }
}