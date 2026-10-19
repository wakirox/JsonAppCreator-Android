package it.sapienza.appinterpreter.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils

/**
 * A JSON app's icon, the same on the home page and on the phone's home screen: the app's emoji on a soft colour, or,
 * without one, its first letter on a coloured tile over cream, like Appkin's own icon. Colours come from the name.
 */
object AppIcon {
    /** The cream behind a letter tile: the Appkin icon's background. */
    const val CREAM = 0xFFFFF3EA.toInt()

    /** The tile colour for [name]: twelve clearly different hues, dark enough for a white letter. */
    fun tileColor(name: String): Int = PALETTE[Math.floorMod(name.hashCode(), PALETTE.size)]

    /** The soft colour behind [name]'s emoji: its tile colour, mostly white. */
    fun softColor(name: String): Int = ColorUtils.blendARGB(tileColor(name), Color.WHITE, 0.82f)

    fun letter(name: String): String = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

    /** The emoji in [icon], if it holds something to draw. */
    fun emoji(icon: String?): String? = icon?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * An adaptive-icon bitmap: 108 dp at 4x, of which launchers show about the middle 72 dp, so everything sits
     * in that middle part.
     */
    fun bitmap(name: String, icon: String?): Bitmap {
        val size = 432
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = size / 2f
        val emoji = emoji(icon)
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
        if (emoji != null) {
            canvas.drawColor(softColor(name))
            text.textSize = size * 0.38f
            canvas.drawText(emoji, center, center - (text.descent() + text.ascent()) / 2, text)
        } else {
            canvas.drawColor(CREAM)
            val tile = size * 0.40f
            canvas.drawRoundRect(
                RectF(center - tile / 2, center - tile / 2, center + tile / 2, center + tile / 2),
                tile * 0.28f, tile * 0.28f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tileColor(name) }
            )
            text.color = Color.WHITE
            text.textSize = tile * 0.55f
            text.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(letter(name), center, center - (text.descent() + text.ascent()) / 2, text)
        }
        return bitmap
    }

    private val PALETTE = intArrayOf(
        0xFF1565C0.toInt(), 0xFFC62828.toInt(), 0xFF2E7D32.toInt(), 0xFFAD1457.toInt(),
        0xFF5D4037.toInt(), 0xFF00695C.toInt(), 0xFFE65100.toInt(), 0xFF455A64.toInt(),
        0xFF558B2F.toInt(), 0xFF6A1B9A.toInt(), 0xFF00838F.toInt(), 0xFF283593.toInt()
    )
}
