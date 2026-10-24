package it.sapienza.appinterpreter.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat

/** Ideas to start from: tapping one writes its request in the field. */
private val IDEAS = listOf(
    "📚 Book list" to "A list of my favourite books, with a screen for each: cover, author and my notes",
    "🍝 Recipes" to "My family recipes, searchable, each with its ingredients and steps",
    "🏃 Race sign-up" to "A sign-up form for our 10 km summer race, with name, email, date of birth and T-shirt size",
    "🖼️ Art gallery" to "A gallery of famous paintings from a public museum API, with a page for each"
)

/**
 * Starts working with an AI app: what to ask for (with ideas to start from), whether to change the current app, how
 * the round trip goes, and how much of the AI chat the instructions take. Appkin has no AI inside, which the screen
 * says once, plainly. Share and Copy stay above the keyboard.
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
                    Button(onClick = onShare, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share to AI app…")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Header(changeCurrent)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = request,
                    onValueChange = onRequestChange,
                    label = { Text(if (changeCurrent) "What to change" else "Describe your app") },
                    placeholder = {
                        Text(if (changeCurrent) "Add a screen with the opening hours" else "A list of my favourite books, with a screen for each")
                    },
                    shape = RoundedCornerShape(16.dp),
                    // A height rather than minLines, which Robolectric can't lay out.
                    modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp).testTag("ai-request")
                )
                if (!changeCurrent) Ideas(onRequestChange)
            }
            if (canChangeCurrent) ChangeCurrent(changeCurrent, onChangeCurrentChange)
            HowItWorks()
            Note(Icons.Filled.Info, "About ${NumberFormat.getIntegerInstance().format(tokens)} tokens: your AI app reads them before the chat starts.", textTag = "ai-token-estimate")
        }
    }
}

@Composable
private fun Header(changeCurrent: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) { Text("✨", fontSize = 26.sp) }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (changeCurrent) "What should change?" else "What should your app do?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Your AI app writes it, Appkin runs it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Ideas(onPick: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IDEAS.forEach { (name, request) -> SuggestionChip(onClick = { onPick(request) }, label = { Text(name) }) }
    }
}

/** The switch for sending the current app too; the whole row toggles it. */
@Composable
private fun ChangeCurrent(checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                .testTag("ai-change-current")
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Change the current app", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Your AI app gets this app too, and changes it instead of starting over.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun HowItWorks() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("How it works", style = MaterialTheme.typography.titleSmall)
            Step(1, "Share this to your AI app, such as ChatGPT, Claude or Gemini.")
            Step(2, "Chat there until you like the result.")
            Step(3, "Bring it back: share or copy the answer here, or open the file it makes. Appkin checks it and opens it, ready to run.")
            Note(Icons.Filled.Lock, "Appkin has no AI inside, and sends nothing anywhere else.")
        }
    }
}

@Composable
private fun Step(number: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** A small line with an icon: a fact worth knowing, not something to act on. */
@Composable
private fun Note(icon: ImageVector, text: String, textTag: String? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = textTag?.let { Modifier.testTag(it) } ?: Modifier
        )
    }
}
