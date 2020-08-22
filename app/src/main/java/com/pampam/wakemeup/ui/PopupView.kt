package com.pampam.wakemeup.ui

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.pampam.wakemeup.R
import kotlinx.android.synthetic.main.popup.view.*

class PopupView(context: Context) : FrameLayout(context) {

    lateinit var title: String
    lateinit var message: String

    init {
        View.inflate(context, R.layout.popup, this)
        dismiss()
    }

    fun show() {
        popup_title.text = title
        popup_message.text = message
        this.visibility = View.VISIBLE
    }

    fun dismiss() {
        title = ""
        message = ""
        popup_accept.setOnClickListener { dismiss() }
        popup_deny.setOnClickListener { dismiss() }

        this.visibility = View.GONE
    }

    fun setOnAcceptClickListener(acceptListener: () -> Unit) =
        popup_accept.setOnClickListener {
            acceptListener()
            dismiss()
        }

    fun setOnDenyClickListener(denyListener: () -> Unit) =
        popup_deny.setOnClickListener {
            denyListener()
            dismiss()
        }
}