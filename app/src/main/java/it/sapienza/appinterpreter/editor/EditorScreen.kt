package it.sapienza.appinterpreter.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.sapienza.appinterpreter.ai.AiContext
import it.sapienza.appinterpreter.ai.AskAi
import it.sapienza.appinterpreter.ai.AskAiDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The configuration editor: JSON with syntax colours, suggestions from the schema for the cursor, a live check that
 * places every problem in the text, ready-made views to insert, and Run to open the result as the app.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun EditorScreen(
    title: String,
    initialText: String,
    check: suspend (String) -> List<EditorIssue>,
    suggestions: SchemaSuggestions,
    onRun: (String) -> Unit,
    onSaveAs: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(initialText)) }
    var issues by remember { mutableStateOf<List<EditorIssue>?>(null) }
    var issuesOpen by remember { mutableStateOf(false) }
    var insertOpen by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var askAi by remember { mutableStateOf(false) }
    var pasted by remember { mutableStateOf<String?>(null) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scroll = rememberScrollState()
    val focus = remember { FocusRequester() }

    // The live check, once typing pauses.
    LaunchedEffect(value.text) {
        issues = null
        delay(CHECK_DELAY_MS)
        issues = check(value.text)
    }
    val numberKeyboard = remember(value.text, value.selection) {
        value.selection.collapsed && runCatching { suggestions.isNumberAt(value.text, value.selection.start) }.getOrDefault(false)
    }
    val chips = remember(value.text, value.selection) {
        if (value.selection.collapsed) {
            runCatching { suggestions.at(value.text, value.selection.start) }.getOrDefault(Suggestions.NONE)
        } else Suggestions.NONE
    }

    fun replace(start: Int, end: Int, insert: String, cursor: Int) {
        val text = value.text.replaceRange(start, end, insert)
        value = TextFieldValue(text, TextRange(start + cursor))
    }

    fun jumpTo(offset: Int) {
        val target = offset.coerceIn(0, value.text.length)
        value = value.copy(selection = TextRange(target))
        focus.requestFocus()
        layout?.let { scope.launch { scroll.animateScrollTo((it.getLineTop(it.getLineForOffset(target)) - 120f).toInt().coerceAtLeast(0)) } }
    }

    fun leave() = if (value.text != initialText) confirmDiscard = true else onBack()
    BackHandler(onBack = ::leave)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = ::leave) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { insertOpen = true }) { Icon(Icons.Filled.Add, contentDescription = "Insert") }
                    DropdownMenu(expanded = insertOpen, onDismissRequest = { insertOpen = false }) {
                        Snippets.all.forEach { snippet ->
                            DropdownMenuItem(
                                text = { Text(snippet.label) },
                                onClick = {
                                    insertOpen = false
                                    val at = value.selection.start
                                    val indent = Snippets.indentAt(value.text, at)
                                    val insert = Snippets.indented(snippet.json, indent)
                                    replace(at, value.selection.end, insert, insert.length)
                                }
                            )
                        }
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val found = check(value.text)
                            issues = found
                            if (found.isEmpty()) onRun(value.text) else {
                                issuesOpen = true
                                found.firstNotNullOfOrNull { it.offset }?.let(::jumpTo)
                                Toast.makeText(context, problemsLabel(found.size) + " to fix first", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Icon(Icons.Filled.PlayArrow, contentDescription = "Run") }
                    IconButton(onClick = { moreOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                    DropdownMenu(expanded = moreOpen, onDismissRequest = { moreOpen = false }) {
                        DropdownMenuItem(text = { Text("Format") }, onClick = {
                            moreOpen = false
                            val formatted = prettyJson(value.text)
                            if (formatted == value.text && issues?.any { it.path == "$" } == true) {
                                Toast.makeText(context, "Fix the JSON syntax first", Toast.LENGTH_SHORT).show()
                            } else value = TextFieldValue(formatted, TextRange(0))
                        })
                        DropdownMenuItem(text = { Text("Save as file…") }, onClick = { moreOpen = false; onSaveAs(value.text) })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Ask an AI…") }, onClick = { moreOpen = false; askAi = true })
                        DropdownMenuItem(text = { Text("Paste AI answer") }, onClick = {
                            moreOpen = false
                            val json = AskAi.jsonFromClipboard(context)
                            if (json == null) {
                                Toast.makeText(context, "No JSON in the clipboard: copy the AI's answer first", Toast.LENGTH_LONG).show()
                            } else pasted = json
                        })
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val colors = MaterialTheme.colorScheme
            val jsonColors = if (colors.surface.luminance() < 0.5f) JsonColors.Dark else JsonColors.Light
            val problems = issues.orEmpty().mapNotNull { it.offset }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll)) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = Snippets.withAutoIndent(value, it) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp, color = colors.onSurface
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    // Code, not prose: no autocorrect or capitals, and digits where a number goes.
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = if (numberKeyboard) KeyboardType.Number else KeyboardType.Ascii
                    ),
                    visualTransformation = remember(jsonColors, problems) { JsonHighlighting(jsonColors, problems) },
                    onTextLayout = { layout = it },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).focusRequester(focus)
                )
            }

            if (chips.items.isNotEmpty()) {
                HorizontalDivider()
                SuggestionRow(chips) { suggestion ->
                    replace(suggestion.start, suggestion.end, suggestion.insert, suggestion.cursor)
                }
            }

            IssueBar(
                issues = issues,
                open = issuesOpen,
                text = value.text,
                onToggle = { issuesOpen = !issuesOpen },
                onIssue = { issue -> issue.offset?.let(::jumpTo) },
                onCopyForAi = { found ->
                    AskAi.copy(context, AiContext.fixRequest(value.text, found), "Copied: paste it to the AI that wrote the JSON")
                }
            )
        }
    }

    if (askAi) AskAiDialog(currentApp = value.text, changeCurrentByDefault = true, onDismiss = { askAi = false })

    pasted?.let { json ->
        AlertDialog(
            onDismissRequest = { pasted = null },
            title = { Text("Use the AI's answer?") },
            text = { Text("It replaces the text in the editor. The check then shows anything to fix.") },
            confirmButton = {
                TextButton(onClick = {
                    value = TextFieldValue(prettyJson(json), TextRange(0))
                    pasted = null
                }) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { pasted = null }) { Text("Cancel") } }
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("Your edits haven't been run or saved.") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onBack() }) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } }
        )
    }
}

/**
 * The suggestions above the keyboard. With a heading they are the attributes the object can still take, required
 * ones first and highlighted; tapping one adds it.
 */
@Composable
private fun SuggestionRow(suggestions: Suggestions, onPick: (Suggestion) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        suggestions.heading?.let {
            Text(
                "$it:",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        suggestions.items.forEach { suggestion ->
            SuggestionChip(
                onClick = { onPick(suggestion) },
                label = {
                    Text(
                        if (suggestion.required) "${suggestion.label} *" else suggestion.label,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (suggestion.required) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = if (suggestion.required) {
                    SuggestionChipDefaults.suggestionChipColors(
                        containerColor = colors.errorContainer,
                        labelColor = colors.onErrorContainer
                    )
                } else SuggestionChipDefaults.suggestionChipColors(),
                border = if (suggestion.required) null else SuggestionChipDefaults.suggestionChipBorder(enabled = true)
            )
        }
    }
}

/** The live check's result: a summary line that opens the list of problems; tapping one moves the cursor to it. */
@Composable
private fun IssueBar(
    issues: List<EditorIssue>?,
    open: Boolean,
    text: String,
    onToggle: () -> Unit,
    onIssue: (EditorIssue) -> Unit,
    onCopyForAi: (List<EditorIssue>) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainer) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable(enabled = !issues.isNullOrEmpty(), onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    issues == null -> Text("Checking…", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
                    issues.isEmpty() -> {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("No problems: ready to run", style = MaterialTheme.typography.labelLarge)
                    }
                    else -> {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = colors.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            problemsLabel(issues.size) + if (open) "" else ": " + issues.first().message,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (open && !issues.isNullOrEmpty()) {
                TextButton(onClick = { onCopyForAi(issues) }, modifier = Modifier.padding(start = 4.dp)) {
                    Text("Copy problems for AI")
                }
                LazyColumn(Modifier.heightIn(max = 220.dp)) {
                    items(issues) { issue ->
                        Column(Modifier.fillMaxWidth().clickable { onIssue(issue) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            val line = issue.offset?.let { offset -> text.take(offset).count { it == '\n' } + 1 }
                            Text(
                                listOfNotNull(line?.let { "Line $it" }, issue.path.takeIf { it != "$" }).joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurfaceVariant
                            )
                            Text(issue.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

private fun problemsLabel(count: Int) = if (count == 1) "1 problem" else "$count problems"

private const val CHECK_DELAY_MS = 450L
