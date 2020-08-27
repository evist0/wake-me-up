package com.pampam.wakemeup.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.model.SessionRange
import com.pampam.wakemeup.databinding.FragmentSessionBinding
import kotlinx.android.synthetic.main.fragment_session.*
import org.koin.androidx.viewmodel.ext.android.viewModel


class SessionFragment : Fragment() {

    private val viewModel by viewModel<SessionViewModel>()
    private lateinit var binding: FragmentSessionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_session, container, false
        )

        binding.viewModel = this.viewModel
        binding.lifecycleOwner = this.viewLifecycleOwner

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDistanceChipGroup()
        initAwakeButton()
        initCancelButton()
        observeSession()
    }

    private fun initDistanceChipGroup() {
        detailsDistanceChipGroup.setOnCheckedChangeListener { _, checkedId ->
            val session = viewModel.currentSession.value
            if (session != null) {
                val range = when (checkedId) {
                    R.id.defaultDistanceChip -> SessionRange.Default
                    R.id.nearDistanceChip -> SessionRange.Near
                    R.id.farDistanceChip -> SessionRange.Far
                    else -> null
                }
                if (range != null) {
                    viewModel.setSessionRange(range)
                }
            }
        }
    }

    private fun initAwakeButton() {
        awakeButton.setOnClickListener {
            viewModel.setSessionActive()
        }
    }

    private fun initCancelButton() {
        cancelButton.setOnClickListener {
            viewModel.cancelSession()
        }
    }

    private fun observeSession() {
        viewModel.currentSession.observe(viewLifecycleOwner, Observer { session ->
            if (session != null) {

                val distanceChipId = when (session.range) {
                    SessionRange.Default -> R.id.defaultDistanceChip
                    SessionRange.Near -> R.id.nearDistanceChip
                    SessionRange.Far -> R.id.farDistanceChip
                }
                detailsDistanceChipGroup.check(distanceChipId)

            }
        })
    }
}