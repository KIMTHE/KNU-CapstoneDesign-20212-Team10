package com.jongsip.cafe.fragment

import android.view.*
import androidx.fragment.app.Fragment
import com.jongsip.cafe.R

import android.os.Bundle

class Intro3Fragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_intro3, container, false)
    }

    companion object {
        fun newInstance(): Intro3Fragment {
            val fragment = Intro3Fragment()
            val args: Bundle = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}