package com.pampam.wakemeup.ui/*
package com.pampam.wakemeup.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.model.DestinationPrediction
import com.pampam.wakemeup.data.model.DestinationPredictionSource
import kotlinx.android.synthetic.main.item_destination.view.*

class DestinationPredictionsAdapter(layoutInflater: LayoutInflater) :
    SuggestionsAdapter<DestinationPrediction, DestinationPredictionsAdapter.ViewHolder>(
        layoutInflater
    ) {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val width = view.width
    }

    var onPredictionSelect: (DestinationPrediction) -> Unit = {}
    var onPredictionDelete: (DestinationPrediction) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(layoutInflater.inflate(R.layout.item_destination, parent, false))
    }

    override fun onBindSuggestionHolder(
        prediction: DestinationPrediction,
        holder: ViewHolder,
        position: Int
    ) {
        when (prediction.src) {
            DestinationPredictionSource.Recent -> {
                holder.itemView.recentImageView.visibility = View.VISIBLE
                holder.itemView.remoteImageView.visibility = View.GONE
                holder.itemView.deleteImageView.visibility = View.VISIBLE
            }
            DestinationPredictionSource.Remote -> {
                holder.itemView.remoteImageView.visibility = View.VISIBLE
                holder.itemView.recentImageView.visibility = View.GONE
                holder.itemView.deleteImageView.visibility = View.GONE
            }
        }

        holder.itemView.primaryText.text = prediction.primaryText
        holder.itemView.secondaryText.text = prediction.secondaryText

        holder.itemView.setOnClickListener {
            onPredictionSelect(prediction)
        }
        holder.itemView.deleteImageView.setOnClickListener {
            onPredictionDelete(prediction)
        }
    }

    override fun getSingleViewHeight(): Int {
        return 50
    }

}*/
