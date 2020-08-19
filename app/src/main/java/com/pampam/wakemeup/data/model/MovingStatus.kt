package com.pampam.wakemeup.data.model

enum class MovingStatus {
    Moving,
    Standing;

    fun isMoving(): Boolean {
        return this == Moving
    }
}