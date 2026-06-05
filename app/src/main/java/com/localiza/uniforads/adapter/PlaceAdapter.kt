package com.localiza.uniforads.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.localiza.uniforads.R
import com.localiza.uniforads.databinding.ItemPlaceBinding
import com.localiza.uniforads.model.Place

class PlaceAdapter(private val places: List<Place>) :
    RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder>() {

    class PlaceViewHolder(private val binding: ItemPlaceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(place: Place) {
            binding.tvPlaceName.text = itemView.context.getString(R.string.nome_place_label, place.name)
            binding.tvPlaceType.text = itemView.context.getString(R.string.tipo_place_label, place.type)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val binding = ItemPlaceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(places[position])
    }

    override fun getItemCount(): Int = places.size
}