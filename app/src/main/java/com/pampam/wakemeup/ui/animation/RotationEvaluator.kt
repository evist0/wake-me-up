package com.pampam.wakemeup.ui.animation

import android.animation.TypeEvaluator
import kotlin.math.abs

object RotationEvaluator : TypeEvaluator<Float> {
    override fun evaluate(fraction: Float, startValue: Float, endValue: Float): Float {
        return if (abs(startValue - endValue) <= 180) startValue + ((endValue - startValue) * fraction) else
            startValue + 360 - ((endValue - startValue) * fraction)
    }
}