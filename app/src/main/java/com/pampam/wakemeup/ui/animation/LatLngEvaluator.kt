package com.pampam.wakemeup.ui.animation

import android.animation.TypeEvaluator
import com.google.android.gms.maps.model.LatLng

object LatLngEvaluator : TypeEvaluator<LatLng> {
    override fun evaluate(fraction: Float, startValue: LatLng, endValue: LatLng): LatLng {
        return LatLng(
            startValue.latitude
                    + ((endValue.latitude - startValue.latitude) * fraction),
            startValue.longitude
                    + (endValue.longitude - startValue.longitude) * fraction
        )
    }
}