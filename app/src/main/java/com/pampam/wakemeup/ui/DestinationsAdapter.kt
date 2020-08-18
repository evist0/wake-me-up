package com.pampam.wakemeup.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mancj.materialsearchbar.adapter.SuggestionsAdapter
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.model.Destination
import com.pampam.wakemeup.data.model.DestinationSource
import kotlinx.android.synthetic.main.item_destination.view.*

class DestinationsAdapter(layoutInflater: LayoutInflater) :
    SuggestionsAdapter<Destination, DestinationsAdapter.ViewHolder>(layoutInflater) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(layoutInflater.inflate(R.layout.item_destination, parent, false))
    }

    override fun onBindSuggestionHolder(
        suggestion: Destination,
        holder: ViewHolder,
        position: Int
    ) {
        when (suggestion.src) {
            DestinationSource.Recent -> {
                holder.itemView.recentImageView.visibility = View.VISIBLE
                holder.itemView.remoteImageView.visibility = View.GONE
                holder.itemView.deleteImageView.visibility = View.VISIBLE
            }
            DestinationSource.Remote -> {
                holder.itemView.remoteImageView.visibility = View.VISIBLE
                holder.itemView.recentImageView.visibility = View.GONE
                holder.itemView.deleteImageView.visibility = View.GONE
            }
        }

        holder.itemView.text.text = suggestion.fullText
    }

    override fun getSingleViewHeight(): Int {
        return 50
    }

}