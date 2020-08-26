package com.pampam.wakemeup.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.model.SessionStatus
import com.pampam.wakemeup.databinding.ActivityAlarmBinding
import com.pampam.wakemeup.turnScreenOffAndKeyguardOn
import com.pampam.wakemeup.turnScreenOnAndKeyguardOff
import org.koin.androidx.viewmodel.ext.android.viewModel

class AlarmActivity : AppCompatActivity() {
    private val viewModel by viewModel<AlarmActivityViewModel>()

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            window.decorView.apply {
                systemUiVisibility =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE
            }
        }
    }
}