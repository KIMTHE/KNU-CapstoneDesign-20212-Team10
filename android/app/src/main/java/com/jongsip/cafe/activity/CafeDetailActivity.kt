package com.jongsip.cafe.activity

import android.content.*
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.jongsip.cafe.R
import com.jongsip.cafe.adapter.CafeMenuAdapter
import com.jongsip.cafe.model.CafeMenu
import com.jongsip.cafe.util.CrawlingUtils

class CafeDetailActivity : AppCompatActivity() {
    lateinit var textCafeName: TextView
    lateinit var textCafeAddress: TextView
    lateinit var listCafeMenu: ListView
    lateinit var connectWifi: Button

    lateinit var firestore: FirebaseFirestore

    private var lastSuggestedNetwork: WifiNetworkSuggestion? = null
    var wifiManager: WifiManager? = null

    val context = this
    private var menuInfo: ArrayList<CafeMenu>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cafe_detail)

        val detailUrl = intent.getStringExtra("detailUrl")

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        firestore = FirebaseFirestore.getInstance()

        textCafeName = findViewById(R.id.text_cafe_name)
        textCafeAddress = findViewById(R.id.text_cafe_address)
        listCafeMenu = findViewById(R.id.list_cafe_menu)
        connectWifi = findViewById(R.id.connect_wifi)

        textCafeName.text = intent.getStringExtra("placeName")
        textCafeAddress.text = intent.getStringExtra("addressName")

        connectWifi.visibility = View.INVISIBLE

        //와이파이 연결버튼이 해당 카페 와이파이 정보가 있을경우만 버튼 보임
        var tempUrl = detailUrl?.substring(27)
        var tempId : String? = null
        var tempPw : String? = null
        firestore.collection("wifiInfo").document(tempUrl!!).get().addOnSuccessListener {
            tempId = it.data?.get("id").toString()
            tempPw = it.data?.get("pw").toString()
            if(tempId != "null"){
                connectWifi.visibility = View.VISIBLE
            }
        }

        connectWifi.setOnClickListener {
            connectUsingNetworkSuggestion(ssid = tempId!!, password = tempPw!!)
        }

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

    /*와이파이 연결을 위한 부분*/
    private fun connectUsingNetworkSuggestion(ssid: String, password: String) {
        val wifiNetworkSuggestion = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()
        val intentFilter =
            IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!intent.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    return
                }
                showToast("연결중...")
            }
        }

        registerReceiver(broadcastReceiver, intentFilter)

        lastSuggestedNetwork?.let {
            val status = wifiManager!!.removeNetworkSuggestions(listOf(it))
            Log.i("WifiNetworkSuggestion", "Removing Network suggestions status is $status")
        }
        val suggestionsList = listOf(wifiNetworkSuggestion)

        var status = wifiManager!!.addNetworkSuggestions(suggestionsList)
        Log.i("WifiNetworkSuggestion", "Adding Network suggestions status is $status")
        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE) {
            showToast("이미 등록돼있습니다")
            status = wifiManager!!.removeNetworkSuggestions(suggestionsList)
            Log.i("WifiNetworkSuggestion", "Removing Network suggestions status is $status")
            status = wifiManager!!.addNetworkSuggestions(suggestionsList)
        }
        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            lastSuggestedNetwork = wifiNetworkSuggestion
            showToast("연결성공")
        }
    }

    private fun showToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show()
    }

}