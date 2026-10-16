package it.sapienza.appinterpreter.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import it.sapienza.appinterpreter.activity.MainActivity

/** Home-screen shortcuts that open JSON App Creator straight into one saved app. */
object HomeShortcuts {
    const val EXTRA_APP_ID = "app_id"

    /** Asks the launcher to pin a shortcut for [app]; false if the launcher can't pin shortcuts. */
    fun pin(context: Context, app: AppLibrary.SavedApp): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val intent = Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(EXTRA_APP_ID, app.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val shortcut = ShortcutInfoCompat.Builder(context, app.id)
            .setShortLabel(app.name.take(25))
            .setLongLabel(app.name)
            .setIcon(IconCompat.createWithAdaptiveBitmap(letterIcon(app.name)))
            .setIntent(intent)
            .build()
        // Re-enable it in case this app was deleted and saved again.
        ShortcutManagerCompat.enableShortcuts(context, listOf(shortcut))
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    /** A deleted app's pinned shortcut stays on the home screen, greyed out, and says so when tapped. */
    fun disable(context: Context, id: String) =
        ShortcutManagerCompat.disableShortcuts(context, listOf(id), "This app was deleted in JSON App Creator")

    /** An adaptive icon: the app's first letter on a colour chosen from its name. */
    private fun letterIcon(name: String): Bitmap {
        val size = 432 // 108 dp at 4x; the visible part is the middle 72 dp
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(PALETTE[Math.floorMod(name.hashCode(), PALETTE.size)])
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val letter = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"
        canvas.drawText(letter, size / 2f, size / 2f - (paint.descent() + paint.ascent()) / 2, paint)
        return bitmap
    }

    private val PALETTE = intArrayOf(
        0xFFE5604F.toInt(), 0xFF3F6E8C.toInt(), 0xFF2E7D32.toInt(), 0xFF8E24AA.toInt(),
        0xFFC25E00.toInt(), 0xFF00838F.toInt(), 0xFF5D4037.toInt(), 0xFF3949AB.toInt()
    )
}
