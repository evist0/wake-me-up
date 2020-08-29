package com.pampam.wakemeup.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.model.RemotePrediction
import kotlinx.android.synthetic.main.item_remote_prediction.view.*

class RemotePredictionsAdapter : RecyclerView.Adapter<RemotePredictionsAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private var remotePredictions = ArrayList(emptyList<RemotePrediction>())

    var onRemotePredictionSelect: ((RemotePrediction) -> Unit)? = null

    fun updateRemotePredictions(newRemotePredictions: List<RemotePrediction>) {
        val diff =
            DiffUtil.calculateDiff(PredictionsDiffCallback(remotePredictions, newRemotePredictions))
        remotePredictions.apply {
            clear()
            addAll(newRemotePredictions)
        }
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_remote_prediction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val remotePrediction = remotePredictions[position]
        holder.itemView.apply {
            primaryText.text = remotePrediction.primaryText
            secondaryText.text = remotePrediction.secondaryText
            setOnClickListener {
                onRemotePredictionSelect?.invoke(remotePrediction)
            }
        }
    }

    override fun getItemCount(): Int {
        return remotePredictions.size
    }
}
