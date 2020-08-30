package com.pampam.wakemeup.ui.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pampam.wakemeup.data.LocationRepository
import com.pampam.wakemeup.data.SessionRepository
import com.pampam.wakemeup.data.model.Session
import com.pampam.wakemeup.data.model.SessionRange
import com.pampam.wakemeup.data.model.SessionStatus
import com.pampam.wakemeup.extensions.isDismissRationaleRequired
import com.pampam.wakemeup.extensions.toLatLng

class SessionViewModel(
    private val locationRepository: LocationRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    val session: LiveData<Session?> = sessionRepository.currentSession

    private val mCancelSessionDialogVisible = MutableLiveData<Boolean>()
    val cancelSessionDialogVisible: LiveData<Boolean> = mCancelSessionDialogVisible

    fun onSessionRangeSelect(range: SessionRange) {
        sessionRepository.currentSession.value = session.value!!.copy(range = range)
    }

    fun onAwakeButtonClick() {
        locationRepository.requireLocation {
            sessionRepository.currentSession.value =
                session.value!!.copy(status = SessionStatus.Active)
        }
    }

    fun onCancelButtonClick() {
        val session = session.value!!
        val location = locationRepository.location.value
        if (session.isDismissRationaleRequired(location?.toLatLng())) {
            mCancelSessionDialogVisible.value = true
        } else {
            sessionRepository.currentSession.value = null
        }
    }

    fun onBackPressed() {
        val session = session.value!!
        val location = locationRepository.location.value
        if (session.isDismissRationaleRequired(location?.toLatLng())) {
            mCancelSessionDialogVisible.value = true
        } else {
            sessionRepository.currentSession.value = null
        }
    }

    fun onPositiveCancelDialog() {
        sessionRepository.currentSession.value = null
    }
}