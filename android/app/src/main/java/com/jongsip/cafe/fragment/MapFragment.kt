package com.jongsip.cafe.fragment
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jongsip.cafe.R
import com.jongsip.cafe.util.PermissionUtils
import net.daum.mf.map.api.MapView


class MapFragment : Fragment() {
    lateinit var btnMoveHere: FloatingActionButton
    private lateinit var mapView : MapView
    lateinit var locatioNManager : LocationManager

    var longitude : Double = 0.0
    var latitude : Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = MapView(activity)
        val mapViewContainer: ViewGroup = rootView.findViewById(R.id.map_view)
        mapViewContainer.addView(mapView)

        btnMoveHere = rootView.findViewById(R.id.btn_move_here)
        btnMoveHere.setOnClickListener{
            if (checkLocationService()) {
                // GPS가 켜져있을 경우
                when (PermissionUtils.requestLocationPermission(requireActivity())) {
                    PermissionUtils.PERMISSION_CODE_ACCEPTED -> getLocation()
                }
            } else {
                // GPS가 꺼져있을 경우
                Toast.makeText(activity, "GPS를 켜주세요", Toast.LENGTH_SHORT).show()
            }
        }

        return rootView
    }

    // GPS가 켜져있는지 확인
    private fun checkLocationService(): Boolean {
        val locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun getLocation(){
        locatioNManager = (activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?)!!
        var userLocation: Location? = getLatLng()
        if(userLocation != null){
            latitude = userLocation.latitude
            longitude = userLocation.longitude
            Toast.makeText(activity, "현재 내 위치 값: ${latitude}, ${longitude}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLatLng(): Location? {
        var currentLatLng: Location? = null
        var hasFineLocationPermission = ContextCompat.checkSelfPermission(requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION)
        var hasCoarseLocationPermission = ContextCompat.checkSelfPermission(requireActivity(),
            Manifest.permission.ACCESS_COARSE_LOCATION)

        if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED){
            val locationProvider = LocationManager.GPS_PROVIDER
            currentLatLng = locatioNManager?.getLastKnownLocation(locationProvider)
        }else{
            currentLatLng = getLatLng()
        }

        return currentLatLng
    }

    companion object {

    }
}