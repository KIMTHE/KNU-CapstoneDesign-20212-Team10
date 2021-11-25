package com.jongsip.cafe.fragment
import KakaoAPI
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.*
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jongsip.cafe.R
import com.jongsip.cafe.model.ResultSearchKeyword
import com.jongsip.cafe.util.PermissionUtils
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.*


class MapFragment : Fragment() {
    lateinit var btnMoveHere: FloatingActionButton
    private lateinit var mapView : MapView
    lateinit var locatioNManager : LocationManager
    lateinit var mapViewContainer: ViewGroup
    var  urlList = arrayOfNulls<String>(15)//카페 url 담을 배열

    var longitude : Double = 0.0
    var latitude : Double = 0.0

    companion object {
        const val BASE_URL = "https://dapi.kakao.com/"
        const val API_KEY = "KakaoAK 84a60e48f5913e29b5d156b3a15da41f"  // REST API 키
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = MapView(activity)
        mapViewContainer = rootView.findViewById(R.id.map_view)
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

        btnMoveHere.performClick()

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

            var mapPoint : MapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude)
            mapView.setMapCenterPoint(mapPoint, true)//맵 이동
            mapView.setZoomLevel(1, true)
            searchKeyword("카페")
        }
    }

    //지도에 마커 추가
    private fun addMarker(response: Response<ResultSearchKeyword>) {
        val marker = MapPOIItem()
        var mapPoint : MapPoint
        var i : Int = 0

        for (item in response.body()?.documents!!) {
            urlList[i] = item.place_url
            i += 1
            marker.itemName = item.place_name
            mapPoint = MapPoint.mapPointWithGeoCoord(item.y.toDouble(), item.x.toDouble())
            marker.mapPoint = mapPoint
            marker.markerType = MapPOIItem.MarkerType.BluePin
            marker.selectedMarkerType = MapPOIItem.MarkerType.RedPin
            mapView.addPOIItem(marker)
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

    private fun searchKeyword(keyword: String) {
        val retrofit = Retrofit.Builder()   // Retrofit 구성
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(KakaoAPI::class.java)   // 통신 인터페이스를 객체로 생성
        val call = api.getSearchKeyword(API_KEY, keyword,longitude.toString(), latitude.toString(), 500)   // 검색 조건 입력

        // API 서버에 요청
        call.enqueue(object: Callback<ResultSearchKeyword> {
            override fun onResponse(
                call: Call<ResultSearchKeyword>,
                response: Response<ResultSearchKeyword>
            ) {
                // 통신 성공 (검색 결과는 response.body()에 담겨있음)
                Log.d("Test", "Raw: ${response.raw()}")
                Log.d("Test", "Body: ${response.body()?.documents}")
                addMarker(response)
            }
            override fun onFailure(call: Call<ResultSearchKeyword>, t: Throwable) {
                // 통신 실패
                Log.w("MainActivity", "통신 실패: ${t.message}")
            }
        })
    }
    override fun onStop() {
        super.onStop()
        mapViewContainer.removeView(mapView)
    }
}