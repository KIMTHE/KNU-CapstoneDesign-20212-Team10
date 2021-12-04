package com.jongsip.cafe.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.jongsip.cafe.R
import com.jongsip.cafe.adapter.MyPagerAdapter
import com.merhold.extensiblepageindicator.ExtensiblePageIndicator


class IntroActivity : AppCompatActivity() {
    private lateinit var flexibleIndicator: ExtensiblePageIndicator
    private lateinit var vpIntro: ViewPager
    private lateinit var btnStartMain: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        flexibleIndicator = findViewById<View>(R.id.flexibleIndicator) as ExtensiblePageIndicator
        vpIntro = findViewById(R.id.pager_intro)
        btnStartMain = findViewById(R.id.btn_start_main)

        vpIntro.adapter = MyPagerAdapter(supportFragmentManager)
        flexibleIndicator.initViewPager(vpIntro)

        vpIntro.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
             override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {}
            override fun onPageScrollStateChanged(state: Int) {
                Log.e("sjlkj", "sjahdal")
            }
        })

        btnStartMain.setOnClickListener {
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }
    }



}