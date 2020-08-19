package com.pampam.wakemeup.ui.animation

import android.animation.TypeEvaluator
import android.util.Log
import kotlin.math.abs

object RotationEvaluator : TypeEvaluator<Float> {
    override fun evaluate(fraction: Float, startValue: Float, endValue: Float): Float {
        val rotation =
            if (abs(endValue - startValue) <= 180) startValue + ((endValue - startValue) * fraction)
            else {
                if(startValue < endValue) {
                    startValue + 360 - ((startValue + 360 - endValue) * fraction)
                } else {
                    startValue + ((endValue + 360 - startValue) * fraction)
                }
            }

        return rotation % 360
    }
}