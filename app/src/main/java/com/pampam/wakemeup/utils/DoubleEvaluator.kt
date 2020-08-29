package com.pampam.wakemeup.utils

import android.animation.TypeEvaluator

object DoubleEvaluator : TypeEvaluator<Double> {
    override fun evaluate(fraction: Float, startValue: Double, endValue: Double): Double {
        return startValue + (endValue - startValue) * fraction
    }
}