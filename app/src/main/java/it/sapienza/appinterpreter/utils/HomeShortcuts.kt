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

    /** The colour of [name]'s icon; the home page's list uses it too, so an app looks the same in both places. */
    fun colorOf(name: String): Int = PALETTE[Math.floorMod(name.hashCode(), PALETTE.size)]

    fun letterOf(name: String): String = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

    /** An adaptive icon: the app's first letter on a colour chosen from its name. */
    private fun letterIcon(name: String): Bitmap {
        val size = 432 // 108 dp at 4x; the visible part is the middle 72 dp
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(colorOf(name))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = size * 0.38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(letterOf(name),size / 2f, size / 2f - (paint.descent() + paint.ascent()) / 2, paint)
        return bitmap
    }

    // Twelve clearly different hues, dark enough for a white letter. With twelve, the five built-in examples all differ.
    private val PALETTE = intArrayOf(
        0xFF1565C0.toInt(), 0xFFC62828.toInt(), 0xFF2E7D32.toInt(), 0xFFAD1457.toInt(),
        0xFF5D4037.toInt(), 0xFF00695C.toInt(), 0xFFE65100.toInt(), 0xFF455A64.toInt(),
        0xFF558B2F.toInt(), 0xFF6A1B9A.toInt(), 0xFF00838F.toInt(), 0xFF283593.toInt()
    )
}
