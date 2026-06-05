package com.localiza.uniforads.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.localiza.uniforads.databinding.ItemAddressBinding
import com.localiza.uniforads.model.Address
import java.util.Collections

class AddressAdapter(
    private var addresses: MutableList<Address>,
    private val onDeleteClick: (Address) -> Unit,
    private val onAddressClick: (Address) -> Unit
) : RecyclerView.Adapter<AddressAdapter.AddressViewHolder>() {

    inner class AddressViewHolder(private val binding: ItemAddressBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(address: Address) {
            binding.tvNeighborhood.text = address.neighborhood
            
            // Exibe Rua + Número ou SN
            val streetDisplay = if (address.number.isNullOrBlank()) {
                "${address.street}, S/N"
            } else {
                "${address.street}, ${address.number}"
            }
            binding.tvStreet.text = streetDisplay

            binding.tvCity.text = address.city
            binding.tvState.text = address.state
            
            binding.btnDelete.setOnClickListener { onDeleteClick(address) }
            
            binding.root.setOnClickListener { onAddressClick(address) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val binding = ItemAddressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AddressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(addresses[position])
    }

    override fun getItemCount(): Int = addresses.size

    fun updateList(newList: List<Address>) {
        addresses.clear()
        addresses.addAll(newList)
        notifyDataSetChanged()
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(addresses, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(addresses, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun getList(): List<Address> = addresses
}