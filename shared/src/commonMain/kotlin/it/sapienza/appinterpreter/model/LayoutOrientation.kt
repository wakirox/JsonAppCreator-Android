package it.sapienza.appinterpreter.model

import kotlinx.serialization.Serializable

@Serializable
enum class LayoutOrientation {
    horizontal, vertical,
    /** Lists only: items side by side in rows of ListView.columns. */
    grid
}
