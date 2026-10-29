package it.sapienza.appinterpreter.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.createBitmap

/**
 * [name]'s icon (see [AppIcon]) as an adaptive-icon bitmap for its home-screen shortcut: 108 dp at 4x, of which
 * launchers show about the middle 72 dp, so everything sits in that middle part.
 */
fun AppIcon.bitmap(name: String, icon: String?): Bitmap {
    val size = 432
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    val emoji = emoji(icon)
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    if (emoji != null) {
        canvas.drawColor(softColor(name))
        text.textSize = size * 0.38f
        canvas.drawText(emoji, center, center - (text.descent() + text.ascent()) / 2, text)
    } else {
        canvas.drawColor(AppIcon.CREAM)
        val tile = size * 0.40f
        canvas.drawRoundRect(
            RectF(center - tile / 2, center - tile / 2, center + tile / 2, center + tile / 2),
            tile * 0.28f,
            tile * 0.28f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tileColor(name) }
        )
        text.color = android.graphics.Color.WHITE
        text.textSize = tile * 0.55f
        text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(letter(name), center, center - (text.descent() + text.ascent()) / 2, text)
    }
    return bitmap
}
