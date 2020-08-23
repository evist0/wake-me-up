package com.pampam.wakemeup.data.model

enum class SessionRange {
    Default,
    Near,
    Far;

    fun toMeters() = when (this) {
        Default -> 250.0
        Near -> 100.0
        Far -> 500.0
    }
}