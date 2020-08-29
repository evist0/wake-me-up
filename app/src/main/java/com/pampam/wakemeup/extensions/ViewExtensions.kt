package com.pampam.wakemeup.extensions

import android.view.View
import android.view.WindowInsets
import androidx.core.view.updatePadding
import com.pampam.wakemeup.ui.Padding

fun View.doOnApplyWindowInsets(f: (View, WindowInsets, Padding) -> Unit) {
    val initialPadding = recordInitialPaddingForView(this)
    setOnApplyWindowInsetsListener { v, insets ->
        f(v, insets, initialPadding)
        insets
    }

    requestApplyInsetsWhenAttached()
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        requestApplyInsets()
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

fun View.updatePadding(padding: Padding) = updatePadding(
    padding.left,
    padding.top,
    padding.right,
    padding.bottom
)

private fun recordInitialPaddingForView(view: View) = Padding(
    view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom
)