package com.pampam.wakemeup.ui.search

import androidx.recyclerview.widget.DiffUtil
import com.pampam.wakemeup.data.model.Prediction

open class PredictionDiffCallback<T : Prediction> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldPrediction: T, newPrediction: T): Boolean {
        return oldPrediction.placeId == newPrediction.placeId
    }

    override fun areContentsTheSame(oldPrediction: T, newPrediction: T): Boolean {
        return oldPrediction.primaryText == newPrediction.primaryText
                && oldPrediction.secondaryText == newPrediction.secondaryText
    }
}

class PredictionsDiffCallback<T : Prediction>(
    private val oldPredictions: List<T>,
    private val newPredictions: List<T>
) : DiffUtil.Callback() {
    private val predictionDiffCallback = PredictionDiffCallback<T>()

    override fun getOldListSize(): Int = oldPredictions.size

    override fun getNewListSize(): Int = newPredictions.size

    override fun areItemsTheSame(oldPredictionPosition: Int, newPredictionPosition: Int): Boolean {
        val oldPrediction = oldPredictions[oldPredictionPosition]
        val newPrediction = newPredictions[newPredictionPosition]
        return predictionDiffCallback.areItemsTheSame(oldPrediction, newPrediction)
    }

    override fun areContentsTheSame(
        oldPredictionPosition: Int,
        newPredictionPosition: Int
    ): Boolean {
        val oldPrediction = oldPredictions[oldPredictionPosition]
        val newPrediction = newPredictions[newPredictionPosition]
        return predictionDiffCallback.areContentsTheSame(oldPrediction, newPrediction)
    }
}