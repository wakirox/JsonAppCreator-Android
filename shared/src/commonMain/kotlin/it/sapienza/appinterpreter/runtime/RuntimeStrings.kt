package it.sapienza.appinterpreter.runtime

/** The runtime's messages in the user's language (Android: the app's string resources). */
interface RuntimeStrings {
    fun viewMissing(id: String): String
    fun configurationInvalid(detail: String): String
    val errorUnrecognized: String
    fun screenDataUnrestorable(detail: String): String
}

/** The English messages, for tests and as a fallback. */
object EnglishRuntimeStrings : RuntimeStrings {
    override fun viewMissing(id: String) = "View '$id' does not exist in this configuration"
    override fun configurationInvalid(detail: String) = "The saved configuration is invalid\n$detail"
    override val errorUnrecognized = "Unrecognized error"
    override fun screenDataUnrestorable(detail: String) = "Unable to restore screen data\n$detail"
}
