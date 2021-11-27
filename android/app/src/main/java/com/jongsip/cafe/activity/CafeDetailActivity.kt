package com.jongsip.cafe.activity

import android.content.ClipData
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jongsip.cafe.R
import com.jongsip.cafe.adapter.CafeMenuAdapter
import com.jongsip.cafe.model.CafeMenu
import com.jongsip.cafe.util.CrawlingUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup

class CafeDetailActivity : AppCompatActivity() {
    lateinit var textCafeName: TextView
    lateinit var textCafeAddress: TextView
    lateinit var listCafeMenu: ListView

    val context = this
    private var menuInfo: ArrayList<CafeMenu>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cafe_detail)

        val detailUrl = intent.getStringExtra("detailUrl")

        textCafeName = findViewById(R.id.text_cafe_name)
        textCafeAddress = findViewById(R.id.text_cafe_address)
        listCafeMenu = findViewById(R.id.list_cafe_menu)

        textCafeName.text = intent.getStringExtra("placeName")
        textCafeAddress.text = intent.getStringExtra("addressName")

        //네트워크를 통한 작업이기 때문에 비동기식으로 구현을 해야 한다.
        Thread {
            menuInfo = CrawlingUtils.crawlingCafeMenu(detailUrl!!)

            this@CafeDetailActivity.runOnUiThread {
                if (menuInfo != null) {
                    listCafeMenu.adapter = CafeMenuAdapter(context, menuInfo!!)
                }
            }
        }.start()

    }
}