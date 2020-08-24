package com.pampam.wakemeup.ui

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.pampam.wakemeup.R
import kotlinx.android.synthetic.main.popup.view.*

class PopupView(context: Context) : FrameLayout(context) {

    var title: String
        get() = "${popup_title.text}"
        set(value) {
            popup_title.text = value
        }
    var message: String
        get() = "${popup_message.text}"
        set(value) {
            popup_message.text = value
        }

    init {
        View.inflate(context, R.layout.popup, this)
        setOnAcceptClickListener { }
        setOnDenyClickListener { }
        dismiss()
    }

    fun show() {
        this.visibility = View.VISIBLE
    }

    fun dismiss() {
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