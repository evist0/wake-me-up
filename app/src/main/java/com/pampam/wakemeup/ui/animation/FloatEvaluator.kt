package com.pampam.wakemeup.ui.animation

import android.animation.TypeEvaluator

object FloatEvaluator : TypeEvaluator<Float> {
    override fun evaluate(fraction: Float, startValue: Float, endValue: Float): Float {
        return startValue + ((endValue - startValue) * fraction)
    }
}