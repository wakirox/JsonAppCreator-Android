package it.sapienza.appinterpreter.utils

/** Home-screen shortcuts that open Appkin straight into one saved app (Android: pinned launcher shortcuts). */
interface HomeScreenShortcuts {
    /** Asks the launcher to pin a shortcut for [app]; false if it can't pin shortcuts. */
    fun pin(app: AppLibrary.SavedApp): Boolean

    /** A deleted app's pinned shortcut stays on the home screen, greyed out, and says so when tapped. */
    fun disable(id: String)
}
