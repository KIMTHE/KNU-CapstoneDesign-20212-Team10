package com.jongsip.cafe.adapter

import android.view.ViewGroup
import android.R
import android.content.Context
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.PagerAdapter
import com.jongsip.cafe.fragment.Intro1Fragment
import com.jongsip.cafe.fragment.Intro2Fragment
import com.jongsip.cafe.fragment.Intro3Fragment
import org.w3c.dom.Text


class MyPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
    private val mData = ArrayList<Fragment>()

    init{
        mData.add(Intro1Fragment.newInstance())
        mData.add(Intro2Fragment.newInstance())
        mData.add(Intro3Fragment.newInstance())
    }

    override fun getCount(): Int {
        return mData.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return "${position+1} 번째"
    }

    override fun getItem(position: Int): Fragment {
        return mData[position]
    }


}