package com.pampam.wakemeup.ui

import androidx.lifecycle.ViewModel
import com.pampam.wakemeup.data.SessionRepository

class AlarmActivityViewModel(private val sessionRepository: SessionRepository) : ViewModel() {
    val session = sessionRepository.currentSession

    fun accept() {
        session.value = null
    }
}