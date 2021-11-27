package com.jongsip.cafe.adapter

import android.app.AlertDialog
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.jongsip.cafe.R
import com.jongsip.cafe.activity.CafeDetailActivity
import com.jongsip.cafe.model.CafeMenu

class CafeMenuAdapter(private val context: CafeDetailActivity, private val data: ArrayList<CafeMenu>): BaseAdapter() {

    override fun getCount(): Int {
        return data.size
    }

    override fun getItem(position: Int): Any {
        return data[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.item_cafe_menu,parent,false)
        val item = data[position]

        if(item.imgUrl != null){
            view.findViewById<ImageView>(R.id.img_cafe_menu).run{
                visibility = VISIBLE
                Glide.with(context).load(item.imgUrl).into(this)
            }
        }

        if(item.price != null){
            view.findViewById<TextView>(R.id.text_cafe_menu_price).text =
                String.format(context.getString(R.string.item_cafe_price_text),item.price)
        }

        view.findViewById<TextView>(R.id.text_cafe_menu_name).text = item.name

//        view.findViewById<LinearLayout>(R.id.layout_cafe_menu).setOnClickListener {
//            AlertDialog.Builder(context).setTitle("메뉴 이름").setMessage("이 커피는 ~입니다.").show()
//        }

        return view
    }

}