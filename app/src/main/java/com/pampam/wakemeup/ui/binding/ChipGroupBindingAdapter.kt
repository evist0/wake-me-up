package com.pampam.wakemeup.ui.binding

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.databinding.InverseBindingMethod
import androidx.databinding.InverseBindingMethods
import com.google.android.material.chip.ChipGroup
import com.pampam.wakemeup.R
import com.pampam.wakemeup.data.model.SessionRange

@InverseBindingMethods(
    InverseBindingMethod(
        type = ChipGroup::class,
        attribute = "android:checkedButton",
        method = "getCheckedChipId"
    )
)
class ChipGroupBindingAdapter {
    companion object {
        @JvmStatic
        @BindingAdapter("android:checkedButton")
        fun setCheckedChip(view: ChipGroup?, range: SessionRange?) {
            val id = when (range) {
                SessionRange.Default -> R.id.defaultDistanceChip
                SessionRange.Near -> R.id.nearDistanceChip
                SessionRange.Far -> R.id.farDistanceChip
                else -> return
            }

            if (id != view?.checkedChipId) {
                view?.check(id)
            }
        }

        @JvmStatic
        @BindingAdapter(
            value = ["android:onCheckedChanged", "android:checkedButtonAttrChanged"],
            requireAll = false
        )
        fun setChipsListeners(
            view: ChipGroup?, listener: ChipGroup.OnCheckedChangeListener?,
            attrChange: InverseBindingListener?
        ) {
            if (attrChange == null) {
                view?.setOnCheckedChangeListener(listener)
            } else {
                view?.setOnCheckedChangeListener { group, checkedId ->
                    listener?.onCheckedChanged(group, checkedId)
                    attrChange.onChange()
                }
            }
        }
    }
}