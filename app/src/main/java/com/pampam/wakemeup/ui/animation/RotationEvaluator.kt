package com.pampam.wakemeup.ui.animation

import android.animation.TypeEvaluator
import kotlin.math.*

object RotationEvaluator : TypeEvaluator<Float> {
    override fun evaluate(fraction: Float, startValue: Float, endValue: Float): Float {
        return evaluateRad(
            fraction,
            startValue * PI.toFloat() / 180,
            endValue * PI.toFloat() / 180
        ) / PI.toFloat() * 180
    }

    private fun evaluateRad(w: Float, x: Float, y: Float): Float {

        val dot = cos(x) * cos(y) + sin(x) * sin(y)
        val theta = acos(dot) * w
        var relativeX = cos(y) - cos(x) * dot
        var relativeY = sin(y) - sin(x) * dot
        val relativeAbs = hypot(relativeX, relativeY)
        relativeX /= relativeAbs
        relativeY /= relativeAbs

        return atan2(
            sin(x) * cos(theta) + relativeY * sin(theta),
            cos(x) * cos(theta) + relativeX * sin(theta)
        )
    }
}