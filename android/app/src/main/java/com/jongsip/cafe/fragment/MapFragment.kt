package com.jongsip.cafe.fragment
import KakaoAPI
import android.Manifest
import android.content.Context
import android.content.Intent
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
import com.google.android.gms.maps.model.Marker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jongsip.cafe.R
import com.jongsip.cafe.activity.CafeDetailActivity
import com.jongsip.cafe.model.Place
import com.jongsip.cafe.model.ResultSearchKeyword
import com.jongsip.cafe.util.PermissionUtils
import net.daum.mf.map.api.CalloutBalloonAdapter
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MapFragment : Fragment() {
    lateinit var btnMoveHere: FloatingActionButton
    private lateinit var mapView : MapView
    private lateinit var locationManager : LocationManager
    lateinit var mapViewContainer: ViewGroup

    private val eventListener = MarkerEventListener()   // 마커 클릭 이벤트 리스너
    var cafeList = HashMap<String, Place>(45)//카페 장소 정보

    var longitude : Double = 0.0
    var latitude : Double = 0.0

    lateinit var nowCafeName : String

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
        val cardView =rootView.findViewById(R.id.card_view) as LinearLayout

        cardView.visibility = View.GONE

        mapView.setCalloutBalloonAdapter(CustomBalloonAdapter(layoutInflater))  // 커스텀 말풍선 등록
        mapView.setPOIItemEventListener(eventListener)  // 마커 클릭 이벤트 리스너 등록

        btnMoveHere = rootView.findViewById(R.id.btn_move_here)
        btnMoveHere.setOnClickListener{
            if (checkLocationService()) {
                // GPS 가 켜져있을 경우
                when (PermissionUtils.requestLocationPermission(requireActivity())) {
                    PermissionUtils.PERMISSION_CODE_ACCEPTED -> getLocation()
                }
            } else {
                // GPS 가 꺼져있을 경우
                Toast.makeText(activity, "GPS를 켜주세요", Toast.LENGTH_SHORT).show()
            }
        }

        btnMoveHere.performClick()

        return rootView
    }

    // GPS 가 켜져있는지 확인
    private fun checkLocationService(): Boolean {
        val locationManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun getLocation(){
        locationManager = (activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?)!!
        val userLocation: Location? = getLatLng()
        if(userLocation != null){
            latitude = userLocation.latitude
            longitude = userLocation.longitude

            val mapPoint : MapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude)
            mapView.setMapCenterPoint(mapPoint, true)//맵 이동
            mapView.setZoomLevel(1, true)
            searchKeyword("카페")
        }
    }

    //지도에 마커 추가
    private fun addMarker(response: Response<ResultSearchKeyword>) {
        var mapPoint : MapPoint
        Log.d("현재 상황", "${response.body()?.documents!!}" )

        var check : Int = 0
        for (item in response.body()?.documents!!) {
            if(check == 0){//제일 가까운 카페 이름, 해당 웹주소 mainActivty로 보내기
                dataPassListener.onDataPass(item.place_name, item.place_url)
                check = 1
            }
            val marker = MapPOIItem()
            cafeList[item.place_name] = item
            marker.itemName = item.place_name
            mapPoint = MapPoint.mapPointWithGeoCoord(item.y.toDouble(), item.x.toDouble())
            marker.mapPoint = mapPoint
            marker.markerType = MapPOIItem.MarkerType.BluePin
            marker.selectedMarkerType = MapPOIItem.MarkerType.RedPin
            marker.isShowCalloutBalloonOnTouch = true
            mapView.addPOIItem(marker)

        }
    }

    private fun getLatLng(): Location? {
        var currentLatLng: Location? = null
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(requireActivity(),
            Manifest.permission.ACCESS_COARSE_LOCATION)

        currentLatLng = if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
            hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED){
            val locationProvider = LocationManager.GPS_PROVIDER
            locationManager.getLastKnownLocation(locationProvider)
        }else{
            val locationProvider = LocationManager.NETWORK_PROVIDER
            locationManager.getLastKnownLocation(locationProvider)
        }

        if(currentLatLng == null){
            currentLatLng = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
        Log.d("디버깅", "위치:" + currentLatLng)
        return currentLatLng
    }

    private fun searchKeyword(keyword: String) {
        val retrofit = Retrofit.Builder()   // Retrofit 구성
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(KakaoAPI::class.java)   // 통신 인터페이스를 객체로 생성
        val call = api.getSearchKeyword(API_KEY, keyword,longitude.toString(), latitude.toString(), 20000, "distance")   // 검색 조건 입력

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


    // 마커 클릭 말풍선 어댑터
    inner class CustomBalloonAdapter(inflater: LayoutInflater): CalloutBalloonAdapter {
        private val mCallOutBalloon: View = inflater.inflate(R.layout.balloon_latout, null)
        val name: TextView = mCallOutBalloon.findViewById(R.id.ball_tv_name)
        private val address: TextView = mCallOutBalloon.findViewById(R.id.ball_tv_address)

        override fun getCalloutBalloon(poiItem: MapPOIItem?): View {
            // 마커 클릭 시 나오는 말풍선
            Log.d("마커 확인용 ", "${poiItem?.itemName}")
            name.text = poiItem?.itemName   // 해당 마커의 정보 이용 가능
            address.text = "메뉴를 보고싶다면, 클릭해주세요"
            return mCallOutBalloon
        }

        override fun getPressedCalloutBalloon(poiItem: MapPOIItem?): View {
            // 말풍선 클릭 시
            return mCallOutBalloon
        }
    }

    // 마커 클릭 이벤트 리스너
    inner class MarkerEventListener(): MapView.POIItemEventListener {
        override fun onPOIItemSelected(mapView: MapView?, poiItem: MapPOIItem?) {
            // 마커 클릭 시

        }

        override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?) {
            // 말풍선 클릭 시 (Deprecated)
            // 이 함수도 작동하지만 그냥 아래 있는 함수에 작성하자
        }

        override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?, buttonType: MapPOIItem.CalloutBalloonButtonType?) {
            // 말풍선 클릭 시
            Log.d("마커 클릭 시 ", "${poiItem?.itemName}")
            val intent = Intent(activity, CafeDetailActivity::class.java)
            intent.putExtra("detailUrl",cafeList[poiItem!!.itemName]!!.place_url)
            intent.putExtra("placeName",poiItem.itemName)
            intent.putExtra("addressName",cafeList[poiItem.itemName]!!.address_name)
            startActivity(intent)
        }

        override fun onDraggablePOIItemMoved(mapView: MapView?, poiItem: MapPOIItem?, mapPoint: MapPoint?) {
            // 마커의 속성 중 isDraggable = true 일 때 마커를 이동시켰을 경우
        }
    }

    //MainActivity 로 가게이름, 해당 링크 보내기 위한 부분
    interface OnDataPassListener {
        fun onDataPass(currentCafeName: String, currentCafeUrl: String)
    }

    private lateinit var dataPassListener: OnDataPassListener

    //MainActivity 로 가게이름, 해당 링크 보내기 위한 부분
    override fun onAttach(context: Context) {
        super.onAttach(context)
        dataPassListener = context as OnDataPassListener //형변환
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()
    }


    override fun onDestroy() {
        super.onDestroy()
        mapViewContainer.removeView(mapView)
    }
}