package com.example.wifitest

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import android.net.wifi.WifiInfo
import android.os.Handler
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.concurrent.timer


class MainActivity : AppCompatActivity() {
    lateinit var button: Button
    lateinit var wifiname: TextView
    private var lastSuggestedNetwork: WifiNetworkSuggestion? = null
    var wifiManager: WifiManager? = null


    companion object {
        const val PERMISSION_CODE_ACCEPTED = 1
        const val PERMISSION_CODE_NOT_AVAILABLE = 0
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        wifiname = findViewById(R.id.wifiname)
        button = findViewById(R.id.connect)
        getWifiSSID()

        timer(period = 1000) {
            runOnUiThread {
                when (requestLocationPermission()) {
                    MainActivity.PERMISSION_CODE_ACCEPTED -> getWifiSSID()
                }
            }
            button.setOnClickListener(View.OnClickListener {
                wifiManager!!.disconnect()
                connectUsingNetworkSuggestion(ssid = "와이파이 아이디", password = "와이파이 비밀번호")
                wifiManager!!.reconnect()
            })
        }
    }

    /*권한 요청*/
    fun requestLocationPermission(): Int {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                // request permission
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    MainActivity.PERMISSION_CODE_ACCEPTED)

            }
        } else {
            // already granted
            return MainActivity.PERMISSION_CODE_ACCEPTED
        }

        // not available
        return MainActivity.PERMISSION_CODE_NOT_AVAILABLE
    }

    /*와이파이 이름을 얻기 위한 부분*/
    fun getWifiSSID() {
        val mWifiManager: WifiManager = (this.getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager)!!
        val info: WifiInfo = mWifiManager.getConnectionInfo()

        if (info.getSupplicantState() === SupplicantState.COMPLETED) {
            val ssid: String = info.getSSID()
            if(ssid == "<unknown ssid>"){
                wifiname.setText("연결안됨")
            }
            else {
                wifiname.setText(ssid)
                Log.d("wifi name", ssid)
            }
        } else {
            Log.d("wifi name", "could not obtain the wifi name")
        }
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
                showToast("Connection Suggestion Succeeded")
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
            showToast("Suggestion Update Needed")
            status = wifiManager!!.removeNetworkSuggestions(suggestionsList)
            Log.i("WifiNetworkSuggestion", "Removing Network suggestions status is $status")
            status = wifiManager!!.addNetworkSuggestions(suggestionsList)
        }
        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            lastSuggestedNetwork = wifiNetworkSuggestion
            showToast("Suggestion Added")
        }
    }

    private fun showToast(s: String) {
        Toast.makeText(applicationContext, s, Toast.LENGTH_LONG).show()
    }
}