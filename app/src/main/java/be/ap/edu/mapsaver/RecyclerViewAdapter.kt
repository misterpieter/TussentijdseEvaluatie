package com.ap.volders.herhaling.RecyclerView

import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import be.ap.edu.mapsaver.R
import kotlinx.android.synthetic.main.item_recycler.view.*

class RecyclerViewAdapter(
    var dataList:ArrayList<RecyclerViewData>) :
    RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewAdapterHolder>() {

    inner class RecyclerViewAdapterHolder(personalItemView: View):RecyclerView.ViewHolder(personalItemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewAdapterHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recycler,parent,false)
        return RecyclerViewAdapterHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerViewAdapterHolder, position: Int) {
        holder.itemView.tvRecyclerItemTitle.text = dataList[position].title
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

}