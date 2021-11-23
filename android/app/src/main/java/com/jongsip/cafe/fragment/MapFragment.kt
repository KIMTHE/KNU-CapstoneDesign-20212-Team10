package com.jongsip.cafe.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.jongsip.cafe.R
import net.daum.mf.map.api.MapView


class MapFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(com.jongsip.cafe.R.layout.fragment_map, container, false)

        val mapView = MapView(activity)
        val mapViewContainer: ViewGroup = rootView.findViewById(R.id.map_view)
        mapViewContainer.addView(mapView)

        return rootView
    }

    companion object {

    }
}