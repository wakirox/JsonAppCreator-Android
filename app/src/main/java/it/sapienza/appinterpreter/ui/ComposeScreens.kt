package it.sapienza.appinterpreter.ui

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.core.util.Separators
import com.fasterxml.jackson.databind.ObjectMapper

private val LightColorScheme = lightColorScheme()
private val DarkColorScheme = darkColorScheme()

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                content()
            }
        }
    }
}

@Composable
fun PlainTextScreen(text: String, onBack: () -> Unit) {
    SecondaryScreen(title = "Changelog", onBack = onBack) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** The active configuration as its author wrote it: pretty-printed, monospaced and coloured, without escaped slashes. */
@Composable
fun JsonScreen(json: String, onBack: () -> Unit) {
    val keyColor = MaterialTheme.colorScheme.primary
    val stringColor = MaterialTheme.colorScheme.tertiary
    val punctuationColor = MaterialTheme.colorScheme.onSurfaceVariant
    val pretty = remember(json) { prettyJson(json) }
    val highlighted = remember(pretty, keyColor, stringColor, punctuationColor) {
        highlightJson(pretty, keyColor, stringColor, punctuationColor)
    }
    SecondaryScreen(title = "Configuration", onBack = onBack) {
        Text(
            text = highlighted,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SecondaryScreen(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        SelectionContainer(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) { content() }
    }
}

private fun prettyJson(json: String): String = runCatching {
    val printer = DefaultPrettyPrinter().withSeparators(
        Separators.createDefaultInstance().withObjectFieldValueSpacing(Separators.Spacing.AFTER)
    )
    ObjectMapper().writer(printer).writeValueAsString(ObjectMapper().readTree(json))
}.getOrDefault(json)

private val JSON_TOKEN = Regex(""""(?:[^"\\]|\\.)*"(\s*:)?|[{}\[\],]""")

private fun highlightJson(json: String, key: Color, string: Color, punctuation: Color): AnnotatedString =
    buildAnnotatedString {
        append(json)
        JSON_TOKEN.findAll(json).forEach { match ->
            val color = when {
                match.value.startsWith("\"") && match.groupValues[1].isNotEmpty() -> key
                match.value.startsWith("\"") -> string
                else -> punctuation
            }
            val end = if (match.groupValues[1].isNotEmpty()) match.range.last + 1 - match.groupValues[1].length else match.range.last + 1
            addStyle(SpanStyle(color = color), match.range.first, end)
        }
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PhotoScreen(url: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, url) {
        value = runCatching {
            url?.let { withContext(Dispatchers.IO) { Glide.with(context).asBitmap().load(it).submit().get().asImageBitmap() } }
        }.getOrNull()
    }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 6f)
        offset += pan
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }.transformable(transform)
                )
            } ?: Text("Image unavailable", modifier = Modifier.padding(24.dp))
        }
    }
}
