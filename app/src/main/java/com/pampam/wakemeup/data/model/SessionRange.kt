package com.pampam.wakemeup.data.model

enum class SessionRange {
    Default,
    Near,
    Far;

    fun toMeters() = when (this) {
        Default -> 500.0
        Near -> 250.0
        Far -> 1000.0
    }
}