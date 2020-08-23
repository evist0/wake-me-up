package com.pampam.wakemeup.data.model

data class Session(
    val details: DestinationDetails? = null,
    val status: SessionStatus = SessionStatus.Fetching,
    val range: SessionRange = SessionRange.Default
)