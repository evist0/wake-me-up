package com.pampam.wakemeup.data.model

enum class MyLocationStatus {
    Unavailable,
    FirstAvailable,
    Available;

    fun isAvailable(): Boolean {
        return this == FirstAvailable || this == Available
    }
}