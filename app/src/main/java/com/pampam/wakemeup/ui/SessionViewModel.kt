package com.pampam.wakemeup.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.pampam.wakemeup.data.SessionRepository
import com.pampam.wakemeup.data.model.Session
import com.pampam.wakemeup.data.model.SessionRange
import com.pampam.wakemeup.data.model.SessionStatus
import com.pampam.wakemeup.data.model.edit

class SessionViewModel(private val sessionRepository: SessionRepository) : ViewModel() {
    val currentSession: LiveData<Session?> = sessionRepository.currentSession

    fun setSessionRange(range: SessionRange) {
        sessionRepository.currentSession.value = currentSession.value!!.edit(range)
    }

    fun setSessionActive() {
        sessionRepository.currentSession.value =
            currentSession.value!!.edit(SessionStatus.Active)
    }

    fun cancelSession() {
        sessionRepository.currentSession.value = null
    }
}