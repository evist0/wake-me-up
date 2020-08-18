package com.pampam.wakemeup.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.pampam.wakemeup.R
import kotlinx.android.synthetic.main.popup.view.*

enum class PopupAction {
    ACCEPT,
    DENY
}

class PopupView(context: Context) : FrameLayout(context) {

    private var attachedTo: ViewGroup? = null

    init {
        View.inflate(context, R.layout.popup, this)

        popup_title.text = ""
        popup_message.text = ""
    }

    fun setTitle(title: String): PopupView {
        popup_title.text = title
        return this
    }

    fun setMessage(message: String): PopupView {
        popup_message.text = message

        return this
    }

    fun setCallback(onAction: (PopupAction) -> Unit): PopupView {
        popup_accept.setOnClickListener { onAction(PopupAction.ACCEPT) }
        popup_deni.setOnClickListener { onAction(PopupAction.DENY) }

        return this
    }

    fun show(attachTo: ViewGroup) {
        attachedTo = attachTo

        if (popup_message.text == "") {
            popup_message.visibility = View.GONE
        }

        attachedTo!!.addView(this)
    }

    fun hide() {

        if (attachedTo != null) {
            attachedTo!!.removeView(this)
        } else {
            return
        }
    }
}