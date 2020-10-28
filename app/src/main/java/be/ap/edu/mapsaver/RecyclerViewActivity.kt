package com.ap.volders.herhaling.RecyclerView

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import be.ap.edu.mapsaver.MainActivity
import be.ap.edu.mapsaver.R
import kotlinx.android.synthetic.main.activity_recycler_view.*

class RecyclerViewActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycler_view)

        var intent = intent
        val stringArray = intent.getStringArrayListExtra("EXTRA_DATA")
        val intentList = ArrayList<RecyclerViewData>()

        stringArray.forEach {
            intentList!!.add(
                    RecyclerViewData(it.toString()))
        }


        val adapter = RecyclerViewAdapter(intentList)
        rv.adapter = adapter
        rv.layoutManager = LinearLayoutManager(this)

//        btnRecyclerAdd.setOnClickListener {
//            val title = etTodo.text.toString()
//            val lat = etTodo.text.toString()
//            val lon = etTodo.text.toString()
//            val itemTodo = RecyclerViewData(title,lat,lon)
//            todoList.add(itemTodo)
//            adapter.notifyItemInserted(todoList.size - 1)
//        }

        btnBackRecycler.setOnClickListener {
            intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}