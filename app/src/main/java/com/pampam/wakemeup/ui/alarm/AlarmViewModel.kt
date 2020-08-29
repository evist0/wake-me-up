package com.pampam.wakemeup.ui.alarm

import androidx.lifecycle.ViewModel
import com.pampam.wakemeup.data.SessionRepository

class AlarmViewModel(sessionRepository: SessionRepository) : ViewModel() {
    val session = sessionRepository.currentSession

    fun accept() {
        session.value = null
    }
}