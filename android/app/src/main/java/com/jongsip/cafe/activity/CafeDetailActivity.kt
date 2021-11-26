package com.jongsip.cafe.activity

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.jongsip.cafe.R
import com.jongsip.cafe.adapter.CafeMenuAdapter
import com.jongsip.cafe.model.CafeMenu
import com.jongsip.cafe.util.CrawlingUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

class CafeDetailActivity : AppCompatActivity() {
    lateinit var textCafeName: TextView
    lateinit var textCafeAddress: TextView
    lateinit var listCafeMenu: ListView

    var menuInfo: ArrayList<CafeMenu>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cafe_detail)

        val detailUrl = intent.getStringExtra("detailUrl")

        textCafeName = findViewById(R.id.text_cafe_name)
        textCafeAddress = findViewById(R.id.text_cafe_address)
        listCafeMenu = findViewById(R.id.list_cafe_menu)

        textCafeName.text = intent.getStringExtra("placeName")
        textCafeAddress.text = intent.getStringExtra("addressName")

        //네트워크 관련은 비동기로 처리해야함, thread 를 blocking
        runBlocking {
            GlobalScope.async {
                menuInfo = CrawlingUtils.crawlingCafeMenu(detailUrl!!)
            }
        }

        if(menuInfo != null){
            listCafeMenu.adapter = CafeMenuAdapter(this, menuInfo!!)
        }

    }
}