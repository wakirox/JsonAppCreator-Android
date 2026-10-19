package it.sapienza.appinterpreter.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat

/**
 * Starts working with an AI app: says plainly that there is no AI in Appkin, explains the round trip, then shares (or
 * copies) the instructions and the request. The actions stay above the keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskAiScreen(
    request: String,
    onRequestChange: (String) -> Unit,
    canChangeCurrent: Boolean,
    changeCurrent: Boolean,
    onChangeCurrentChange: (Boolean) -> Unit,
    tokens: Int,
    onShare: () -> Unit,
    onCopy: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ask your AI app") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onCopy) { Text("Copy") }
                    Button(onClick = onShare) { Text("Share to AI app…") }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Appkin has no AI inside. It hands instructions to an AI app you already use, " +
                    "such as ChatGPT, Claude or Gemini, and nothing is sent anywhere else.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "1. Describe your app and share it to your AI app.\n" +
                    "2. Chat there until you like the result.\n" +
                    "3. Bring the result back: open the file it makes in Appkin, or share or copy " +
                    "its answer, or a share link of the chat, here. It opens in the editor, checked and ready " +
                    "to run.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = request,
                onValueChange = onRequestChange,
                label = { Text(if (changeCurrent) "What to change" else "Describe your app") },
                placeholder = { Text("A list of my favourite books, with a screen for each") },
                // A height rather than minLines, which Robolectric can't lay out.
                modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).testTag("ai-request")
            )
            if (canChangeCurrent) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = changeCurrent, onCheckedChange = onChangeCurrentChange, modifier = Modifier.testTag("ai-change-current"))
                    Text("Change the current app")
                }
            }
            Text(
                "About ${NumberFormat.getIntegerInstance().format(tokens)} tokens: the AI app reads them before " +
                    "your chat starts, out of the space it has for the whole chat.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("ai-token-estimate")
            )
        }
    }
}
