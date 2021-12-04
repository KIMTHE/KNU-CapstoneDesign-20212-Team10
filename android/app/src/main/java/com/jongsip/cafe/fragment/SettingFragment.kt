package com.jongsip.cafe.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.jongsip.cafe.R

class SettingFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var rootView = inflater.inflate(R.layout.fragment_setting, container, false)

        var goPlayStore : TextView = rootView.findViewById(R.id.go_play_store)
        //앱 평가하기 텍스트를 눌렀을 때
        goPlayStore.setOnClickListener {
            var intent : Intent = Intent(Intent(Intent.ACTION_VIEW))
            intent.setData(Uri.parse("market://details?id=" + "com.kakao.talk")) //com.kakao.talk 부분에 패키지 이름으로 변경 후 출시하면 동작가능
            context?.startActivity(intent)
        }

        var emailAddress : TextView = rootView.findViewById(R.id.email_address)
        //문의사항 텍스트를 눌렀을 때
        emailAddress.setOnClickListener {
            val clipboardManager : ClipboardManager = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("strName","jongsip@gmail.com")
            clipboardManager?.setPrimaryClip(clipData)

            Toast.makeText(context, "이메일 주소가 복사됐습니다", Toast.LENGTH_SHORT).show()
        }

        return rootView
    }

    companion object {

    }
}