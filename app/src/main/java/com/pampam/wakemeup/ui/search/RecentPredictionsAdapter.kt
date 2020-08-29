package com.pampam.wakemeup.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.model.RecentPrediction
import kotlinx.android.synthetic.main.item_recent_prediction.view.*
import kotlinx.android.synthetic.main.item_remote_prediction.view.primaryText
import kotlinx.android.synthetic.main.item_remote_prediction.view.secondaryText

class RecentPredictionsAdapter :
    PagedListAdapter<RecentPrediction, RecentPredictionsAdapter.ViewHolder>(PredictionDiffCallback()) {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    var onRecentPredictionSelect: ((RecentPrediction) -> Unit)? = null
    var onRecentPredictionDelete: ((RecentPrediction) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recent_prediction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recentPrediction = getItem(position)
        holder.itemView.apply {
            primaryText.text = recentPrediction?.primaryText ?: ""
            secondaryText.text = recentPrediction?.secondaryText ?: ""
            setOnClickListener {
                if (recentPrediction != null) {
                    onRecentPredictionSelect?.invoke(recentPrediction)
                }
            }
            deleteImageView.setOnClickListener {
                if (recentPrediction != null) {
                    onRecentPredictionDelete?.invoke(recentPrediction)
                }
            }

            if (recentPrediction == null) {
                if (!shimmerLayout.isShimmerVisible) {
                    shimmerLayout.showShimmer(true)
                }
            } else if (shimmerLayout.isShimmerVisible) {
                shimmerLayout.hideShimmer()
            }
        }
    }
}