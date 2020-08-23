package com.pampam.wakemeup.ui.animation

import android.animation.TypeEvaluator

object DoubleEvaluator : TypeEvaluator<Double> {
    override fun evaluate(fraction: Float, startValue: Double, endValue: Double): Double {
        return startValue + (endValue - startValue) * fraction
    }
}