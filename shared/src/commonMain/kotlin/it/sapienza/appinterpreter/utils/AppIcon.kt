package it.sapienza.appinterpreter.utils

/**
 * A JSON app's icon, the same on the home page and on the phone's home screen: the app's emoji on a soft colour, or,
 * without one, its first letter on a coloured tile over cream, like Appkin's own icon. Colours (ARGB) come from the
 * name. Drawing it is platform code (Android: `AppIcon.bitmap` and `AppIconView`).
 */
object AppIcon {
    /** The cream behind a letter tile: the Appkin icon's background. */
    const val CREAM = 0xFFFFF3EA.toInt()

    /** The tile colour for [name]: twelve clearly different hues, dark enough for a white letter. */
    fun tileColor(name: String): Int = PALETTE[name.hashCode().mod(PALETTE.size)]

    /** The soft colour behind [name]'s emoji: its tile colour, mostly white. */
    fun softColor(name: String): Int = blend(tileColor(name), WHITE, 0.82f)

    fun letter(name: String): String = name.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"

    /** The emoji in [icon], if it holds something to draw. */
    fun emoji(icon: String?): String? = icon?.trim()?.takeIf { it.isNotEmpty() }

    /** [from] mixed with [to] by [ratio], channel by channel, as androidx's ColorUtils.blendARGB does. */
    internal fun blend(from: Int, to: Int, ratio: Float): Int {
        val inverse = 1 - ratio
        fun channel(shift: Int) = ((from ushr shift and 0xFF) * inverse + (to ushr shift and 0xFF) * ratio).toInt()
        return (channel(24) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    private const val WHITE = 0xFFFFFFFF.toInt()

    // With twelve hues, the five built-in examples all differ.
    private val PALETTE = intArrayOf(
        0xFF1565C0.toInt(), 0xFFC62828.toInt(), 0xFF2E7D32.toInt(), 0xFFAD1457.toInt(),
        0xFF5D4037.toInt(), 0xFF00695C.toInt(), 0xFFE65100.toInt(), 0xFF455A64.toInt(),
        0xFF558B2F.toInt(), 0xFF6A1B9A.toInt(), 0xFF00838F.toInt(), 0xFF283593.toInt()
    )
}
