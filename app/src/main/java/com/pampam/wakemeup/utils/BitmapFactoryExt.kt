package com.pampam.wakemeup.utils

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory

class BitmapFactoryExt(private val resources: Resources) {
    fun decodeResourceScaled(id: Int, scaledWidth: Int, scaledHeight: Int): Bitmap =
        Bitmap.createScaledBitmap(
            BitmapFactory.decodeResource(resources, id),
            scaledWidth,
            scaledHeight,
            true
        )
}