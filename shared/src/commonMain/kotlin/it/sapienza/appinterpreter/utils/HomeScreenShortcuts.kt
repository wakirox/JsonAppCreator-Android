package it.sapienza.appinterpreter.utils

/** Home-screen shortcuts that open Appkin straight into one saved app (Android: pinned launcher shortcuts). */
interface HomeScreenShortcuts {
    /** Asks the launcher to pin a shortcut for [app]; false if it can't pin shortcuts. */
    fun pin(app: AppLibrary.SavedApp): Boolean

    /** A deleted app's pinned shortcut stays on the home screen, greyed out, and says so when tapped. */
    fun disable(id: String)
}

/** The letter icon of a saved app, the same in the home page's list and on the home screen. */
object AppIcons {
    /** The colour (ARGB) of [name]'s icon. */
    fun colorOf(name: String): Int = PALETTE[name.hashCode().mod(PALETTE.size)]

    fun letterOf(name: String): String = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

    // Twelve clearly different hues, dark enough for a white letter. With twelve, the five built-in examples all differ.
    private val PALETTE = intArrayOf(
        0xFF1565C0.toInt(), 0xFFC62828.toInt(), 0xFF2E7D32.toInt(), 0xFFAD1457.toInt(),
        0xFF5D4037.toInt(), 0xFF00695C.toInt(), 0xFFE65100.toInt(), 0xFF455A64.toInt(),
        0xFF558B2F.toInt(), 0xFF6A1B9A.toInt(), 0xFF00838F.toInt(), 0xFF283593.toInt()
    )
}
