package it.sapienza.appinterpreter.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * The "Ask your AI app" screen: the request, whether to change the app it was opened with, whether to send all the
 * instructions or the short ones with links (remembered for next time), and how many tokens they take. Sharing and copying go back to the activity as effects. The choices are kept in [saved], so they
 * survive rotation and the process being stopped.
 */
class AskAiViewModel(
    private val saved: SavedStateHandle,
    loadAppToChange: LoadAppToChangeUseCase,
    private val buildPrompt: BuildAiPromptUseCase,
    private val instructions: AiInstructionsChoice,
) : ViewModel() {

    sealed interface Effect {
        class Share(val prompt: String) : Effect
        class Copy(val prompt: String) : Effect
    }

    private val subject = subject(saved)
    private val appToChange: String? = loadAppToChange(subject)

    /** Whether there is an app the AI app could change, so the screen offers "Change the current app". */
    val canChangeCurrent: Boolean = appToChange != null

    var request by mutableStateOf(saved[KEY_REQUEST] ?: "")
        private set

    var changeCurrent by mutableStateOf(saved[KEY_CHANGE_CURRENT] ?: (subject is AskAiSubject.EditorText && canChangeCurrent))
        private set

    /** Whether to send the short instructions, which link to the full reference, instead of all of them. */
    var withLinks by mutableStateOf(instructions.withLinks)
        private set

    // The instructions (and the app) are measured once per choice; the request is added as it's typed.
    private val baseTokens = mutableMapOf<Pair<Boolean, Boolean>, Int>()

    /** About how many tokens the instructions take, rounded (see [TokenEstimate]). */
    val tokens: Int
        get() {
            val base = baseTokens.getOrPut(changeCurrent to withLinks) { TokenEstimate.of(buildPrompt(app(), null, withLinks)) }
            return TokenEstimate.rounded(base + TokenEstimate.of(request))
        }

    private val effectChannel = Channel<Effect>(Channel.BUFFERED)
    val effects: Flow<Effect> = effectChannel.receiveAsFlow()

    fun onRequestChange(text: String) {
        request = text
        saved[KEY_REQUEST] = text
    }

    fun onChangeCurrentChange(checked: Boolean) {
        changeCurrent = checked && canChangeCurrent
        saved[KEY_CHANGE_CURRENT] = changeCurrent
    }

    fun onWithLinksChange(short: Boolean) {
        withLinks = short
        instructions.withLinks = short
    }

    fun share() {
        effectChannel.trySend(Effect.Share(prompt()))
    }

    fun copy() {
        effectChannel.trySend(Effect.Copy(prompt()))
    }

    private fun app() = appToChange.takeIf { changeCurrent }

    private fun prompt() = buildPrompt(app(), request, withLinks)

    companion object {
        /** Intent extras, read through the SavedStateHandle: which app, and the path of the editor's text. */
        const val EXTRA_SUBJECT = "ask_ai_subject"
        const val EXTRA_TEXT_PATH = "ask_ai_text_path"
        const val SUBJECT_CURRENT_APP = "current_app"
        const val SUBJECT_EDITOR_TEXT = "editor_text"

        private const val KEY_REQUEST = "request"
        private const val KEY_CHANGE_CURRENT = "change_current"

        private fun subject(saved: SavedStateHandle): AskAiSubject = when (saved.get<String>(EXTRA_SUBJECT)) {
            SUBJECT_CURRENT_APP -> AskAiSubject.CurrentApp
            SUBJECT_EDITOR_TEXT -> saved.get<String>(EXTRA_TEXT_PATH)?.let(AskAiSubject::EditorText) ?: AskAiSubject.NewApp
            else -> AskAiSubject.NewApp
        }
    }
}
