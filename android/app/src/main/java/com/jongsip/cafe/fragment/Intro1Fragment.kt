package com.jongsip.cafe.fragment

import android.view.*
import androidx.fragment.app.Fragment

import android.os.Bundle
import com.jongsip.cafe.R

class Intro1Fragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_intro1, container, false)
    }

    companion object {
        fun newInstance(): Intro1Fragment {
            val fragment = Intro1Fragment()
            val args: Bundle = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}