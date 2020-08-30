package com.pampam.wakemeup.ui.alarm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.model.SessionStatus
import com.pampam.wakemeup.databinding.ActivityAlarmBinding
import com.pampam.wakemeup.extensions.turnScreenOffAndKeyguardOn
import com.pampam.wakemeup.extensions.turnScreenOnAndKeyguardOff
import org.koin.androidx.viewmodel.ext.android.viewModel

class AlarmActivity : AppCompatActivity() {
    private val viewModel by viewModel<AlarmViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DataBindingUtil.setContentView<ActivityAlarmBinding>(
            this,
            R.layout.activity_alarm
        )
            .apply {
                viewModel = this@AlarmActivity.viewModel
                lifecycleOwner = this@AlarmActivity
            }

        turnScreenOnAndKeyguardOff()

        viewModel.session.observe(this, Observer { session ->
            if (session?.status != SessionStatus.Active) {
                finish()
            }
        })

        hideSystemUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        turnScreenOffAndKeyguardOn()
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}