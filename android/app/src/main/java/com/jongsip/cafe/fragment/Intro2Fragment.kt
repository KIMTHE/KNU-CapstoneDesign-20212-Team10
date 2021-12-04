package com.jongsip.cafe.fragment

import android.view.*
import androidx.fragment.app.Fragment
import com.jongsip.cafe.R

import android.os.Bundle

class Intro2Fragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_intro2, container, false)
    }

    companion object {
        fun newInstance(): Intro2Fragment {
            val fragment= Intro2Fragment()
            val args: Bundle = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}