package com.pampam.wakemeup.data

import androidx.lifecycle.MutableLiveData
import com.pampam.wakemeup.data.model.Session

class SessionRepository {
    val currentSession = MutableLiveData<Session?>(null)
}