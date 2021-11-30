package com.jongsip.cafe.util

import android.view.Menu
import com.jongsip.cafe.model.CafeMenu
import org.json.JSONObject
import org.jsoup.Jsoup

object CrawlingUtils {

    // 카카오맵 상세정보 url 을 받아서, 카페의 메뉴를 크롤링
    fun crawlingCafeMenu(detailUrl: String): ArrayList<CafeMenu>? {
        val jsonUrl = detailUrl.split("com/")[0]+"com/main/v/"+detailUrl.split("com/")[1].trim()

        val doc = Jsoup.connect(jsonUrl)
            .timeout(3000)
            .ignoreContentType(true)
            .get()
            .body()
        val str = doc.toString().split("<body>")[1].split("</body>")[0].trim()

        if(str.contains("\"menuInfo\":")){
            val menuInfoStr = str.split("\"menuInfo\":")[1].split("}]")[0].trim()+"}]}"

            val jsonObject = JSONObject(menuInfoStr)
            val jsonMenuArray = jsonObject.getJSONArray("menuList")

            val menuInfo = ArrayList<CafeMenu>()
            for (i in 0 until jsonMenuArray.length()) {
                val iObject = jsonMenuArray.getJSONObject(i)

                val name: String = iObject.optString("menu")
                var price: String? = iObject.optString("price")
                var imgUrl: String? = iObject.optString("img")


                if(price == "") price = null
                if(imgUrl == "") imgUrl = null

                menuInfo.add(CafeMenu(name,price,imgUrl))

            }

            return menuInfo
        }

        return null
    }


}