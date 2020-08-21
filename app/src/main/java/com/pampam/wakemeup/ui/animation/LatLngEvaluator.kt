package com.pampam.wakemeup.ui.animation

import android.animation.TypeEvaluator
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil

object LatLngEvaluator : TypeEvaluator<LatLng> {
    override fun evaluate(fraction: Float, startValue: LatLng, endValue: LatLng): LatLng {
        return SphericalUtil.interpolate(startValue, endValue, fraction.toDouble())
    }
}