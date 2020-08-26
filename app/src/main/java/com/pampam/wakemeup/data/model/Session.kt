package com.pampam.wakemeup.data.model

data class Session(
    val details: DestinationDetails? = null,
    val status: SessionStatus = SessionStatus.Fetching,
    val range: SessionRange = SessionRange.Default
)

fun Session.edit(details: DestinationDetails?): Session =
    Session(details, status, range)

fun Session.edit(status: SessionStatus): Session =
    Session(details, status, range)

fun Session.edit(range: SessionRange): Session =
    Session(details, status, range)
