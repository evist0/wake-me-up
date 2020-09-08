package com.pampam.wakemeup.ui.session

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.model.SessionRange
import com.pampam.wakemeup.databinding.FragmentSessionBinding
import com.pampam.wakemeup.ui.MainViewModel
import com.pampam.wakemeup.ui.Padding
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.fragment_session.*
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SessionFragment : Fragment() {
    private val viewModel by viewModel<SessionViewModel>()
    private lateinit var binding: FragmentSessionBinding

    private val mainViewModel by sharedViewModel<MainViewModel>()

    private lateinit var cancelSessionDialog: AlertDialog

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

        cancelSessionDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.cancel_session_dialog_title)
            .setMessage(R.string.cancel_session_dialog_message)
            .setNegativeButton(R.string.dialog_negative_text) { _, _ ->
                viewModel.onNegativeCancelSessionDialog()
            }
            .setPositiveButton(R.string.dialog_positive_text) { _, _ ->
                viewModel.onPositiveCancelSessionDialog()
            }
            .create()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navController = findNavController()

        sessionCardView.apply {
            addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                mainViewModel.setMapPadding(
                    Padding(
                        left = rootView.systemWindowsAwareLayout.paddingLeft + sessionCardView.marginLeft,
                        top = rootView.systemWindowsAwareLayout.paddingTop + rootView.systemWindowsAwareLayout.adView.height,
                        right = rootView.systemWindowsAwareLayout.paddingRight + sessionCardView.marginRight,
                        bottom = rootView.systemWindowsAwareLayout.paddingBottom + sessionCardView.marginBottom + sessionCardView.height
                    )
                )
            }
        }

        detailsDistanceChipGroup.setOnCheckedChangeListener { _, checkedId ->
            val session = viewModel.session.value
            if (session != null) {
                val range = when (checkedId) {
                    R.id.defaultDistanceChip -> SessionRange.Default
                    R.id.nearDistanceChip -> SessionRange.Near
                    R.id.farDistanceChip -> SessionRange.Far
                    else -> null
                }
                if (range != null) {
                    viewModel.onSessionRangeSelect(range)
                }
            }
        }

        awakeButton.setOnClickListener {
            viewModel.onAwakeButtonClick()
        }

        cancelButton.setOnClickListener {
            viewModel.onCancelButtonClick()
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            viewModel.onBackPressed()
        }

        with(viewModel) {
            session.observe(viewLifecycleOwner) { session ->
                if (session != null) {
                    val distanceChipId = when (session.range) {
                        SessionRange.Default -> R.id.defaultDistanceChip
                        SessionRange.Near -> R.id.nearDistanceChip
                        SessionRange.Far -> R.id.farDistanceChip
                    }
                    detailsDistanceChipGroup.check(distanceChipId)
                } else {
                    navController.popBackStack()
                }
            }

            cancelSessionDialogVisible.observe(viewLifecycleOwner) { visible ->
                with(cancelSessionDialog) {
                    if (visible) {
                        show()
                    } else {
                        dismiss()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        cancelSessionDialog.dismiss()
    }
}