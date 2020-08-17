package com.pampam.wakemeup.data.model

enum class LocationStatus {
    Unavailable,
    FirstAvailable,
    Available;

    fun isAvailable(): Boolean {
        return this == FirstAvailable || this == Available
    }
}