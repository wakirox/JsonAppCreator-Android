@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package it.sapienza.appinterpreter.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.sapienza.appinterpreter.runtime.ListsState
import it.sapienza.appinterpreter.runtime.ScreenViewModel
import it.sapienza.appinterpreter.runtime.ViewVisibility
import it.sapienza.appinterpreter.runtime.text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.util.Date
import android.text.format.DateFormat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import it.sapienza.appinterpreter.activity.createIntentPhotoActivity
import it.sapienza.appinterpreter.json.fillAddress
import it.sapienza.appinterpreter.json.getValue
import it.sapienza.appinterpreter.json.matchReplace
import it.sapienza.appinterpreter.json.optString
import it.sapienza.appinterpreter.extensions.toDate
import it.sapienza.appinterpreter.form.extensions.stringValue
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.action.ActionType
import it.sapienza.appinterpreter.model.view_model.ButtonIcon
import it.sapienza.appinterpreter.model.view_model.ButtonView
import it.sapienza.appinterpreter.model.view_model.CheckView
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.ImageView
import it.sapienza.appinterpreter.model.view_model.ImageScale
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.model.view_model.TextView
import it.sapienza.appinterpreter.model.view_model.TextFormat
import it.sapienza.appinterpreter.model.view_model.TextualView
import it.sapienza.appinterpreter.model.view_model.form_model.CheckBox
import it.sapienza.appinterpreter.model.view_model.form_model.DateInput
import it.sapienza.appinterpreter.model.view_model.form_model.HiddenInput
import it.sapienza.appinterpreter.model.view_model.form_model.TimeInput
import it.sapienza.appinterpreter.utils.FormDates
import it.sapienza.appinterpreter.model.view_model.form_model.Selector
import it.sapienza.appinterpreter.model.view_model.form_model.TextInput
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.TextType
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.model.view_model.helper.TextStyle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

private const val HORIZONTAL_LAZY_THRESHOLD = 8

/** The screen's margins: everything lines up on the same left edge, 16 dp in. */
private val PAGE_PADDING = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

/** The space between one view and the next. */
private val VIEW_SPACING = 12.dp

/** The screen being drawn: its app, state and events. */
val LocalScreen = staticCompositionLocalOf<ScreenViewModel> { error("RendererScreen needs LocalScreen") }

@Composable
fun RendererScreen(model: MView, data: JsonObject? = model.data, loading: Boolean) {
    val screen = LocalScreen.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, screen) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) screen.lists.retryFailed()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val refreshing by screen.lists.refreshing.collectAsState()
    val canRefresh by screen.lists.canRefresh.collectAsState()
    val pull = rememberPullToRefreshState()
    // Lists loaded from a request load again when the screen is pulled down.
    Box(
        Modifier.fillMaxSize().testTag("renderer-screen")
            .pullToRefresh(isRefreshing = refreshing, state = pull, enabled = canRefresh, onRefresh = screen.lists::refresh)
    ) {
        RenderRoot(model, data)
        if (canRefresh) {
            PullToRefreshDefaults.Indicator(state = pull, isRefreshing = refreshing, modifier = Modifier.align(Alignment.TopCenter))
        }
        if (loading) {
            Card(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(20.dp)
            ) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
        }
    }
}

@Composable
private fun RenderRoot(model: MView, screenData: JsonObject?) {
    val screen = LocalScreen.current
    val resolved = screen.resolve(model)
    val data = remember(resolved, resolved.data, screenData) { screenData ?: resolved.data }

    when {
        resolved is Layout && resolved.orientation != LayoutOrientation.horizontal -> {
            val children = remember(resolved, data) { screen.shownViews(resolved.views, data) }
            val listStates = children.map { child ->
                if (child is ListView && child.orientation != LayoutOrientation.horizontal) {
                    val childData = remember(child, child.data, data) { child.data ?: data }
                    screen.lists.get(child, childData)
                } else {
                    null
                }
            }
            // The lists' content, observed here: the lazy column's items are built from it.
            val listContents = listStates.map { entry -> entry?.let { it.content.collectAsState().value } }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PAGE_PADDING,
                verticalArrangement = Arrangement.spacedBy(VIEW_SPACING)
            ) {
                children.forEachIndexed { index, child ->
                    val listState = listStates[index]
                    val listContent = listContents[index]
                    if (child is ListView && listState != null && listContent != null) {
                        verticalListItems(child, listState, listContent)
                    } else {
                        item(key = "root-$index-${child.id.orEmpty()}") {
                            RenderNode(child, data, columnChildModifier(child))
                        }
                    }
                }
            }
        }
        resolved is ListView && resolved.orientation != LayoutOrientation.horizontal -> {
            val entry = screen.lists.get(resolved, data)
            val content by entry.content.collectAsState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PAGE_PADDING,
                verticalArrangement = Arrangement.spacedBy(VIEW_SPACING)
            ) {
                verticalListItems(resolved, entry, content)
            }
        }
        else -> {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PAGE_PADDING),
                verticalArrangement = Arrangement.spacedBy(VIEW_SPACING)
            ) {
                RenderNode(resolved, data, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun RenderNode(
    model: MView,
    parentData: JsonObject?,
    modifier: Modifier = Modifier,
    allowImagePreview: Boolean = true
) {
    val resolved = LocalScreen.current.resolve(model)
    val data = remember(resolved, resolved.data, parentData) { resolved.data ?: parentData }
    if (!ViewVisibility.isShown(resolved, data, reference = model)) return
    when (resolved) {
        is Layout -> RenderLayout(resolved, data, modifier, allowImagePreview)
        is ButtonView -> RenderButton(resolved, data, modifier)
        is TextView -> RenderText(resolved, data, modifier)
        is ImageView -> RenderImage(resolved, data, modifier, allowImagePreview)
        is ListView -> RenderList(resolved, data, modifier)
        is FormView -> RenderForm(resolved, data, modifier)
        is CheckView -> RenderCheck(resolved, data, modifier)
    }
}

@Composable
private fun RenderLayout(
    layout: Layout,
    data: JsonObject?,
    modifier: Modifier,
    allowImagePreview: Boolean
) {
    val screen = LocalScreen.current
    val resolvedChildren = screen.shownViews(layout.views, data)
    if (layout.orientation == LayoutOrientation.horizontal) {
        // A row with an icon button (such as Delete beside a tick box) centres its views on it; other rows keep them at the top.
        val centred = resolvedChildren.any { it.isIconOnly() }
        Row(
            modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = if (centred) Alignment.CenterVertically else Alignment.Top
        ) {
            resolvedChildren.forEach { child ->
                RenderNode(child, data, rowChildModifier(child), allowImagePreview)
            }
        }
    } else {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(VIEW_SPACING)) {
            resolvedChildren.forEach { child -> RenderNode(child, data, columnChildModifier(child), allowImagePreview) }
        }
    }
}

/** [views] resolved, without those that aren't shown with [data]: they take no space, and no gap either. */
private fun ScreenViewModel.shownViews(views: List<MView>, data: JsonObject?): List<MView> =
    views.mapNotNull { view -> resolve(view).takeIf { ViewVisibility.isShown(it, it.data ?: data, reference = view) } }

private fun columnChildModifier(model: MView): Modifier {
    if (model.isIconOnly()) return Modifier
    val width = model.explicitWidth()
    return if (width == null) Modifier.fillMaxWidth() else Modifier.width(width.dp)
}

private fun RowScope.rowChildModifier(model: MView): Modifier {
    if (model.isIconOnly()) return Modifier
    val width = model.explicitWidth()
    return if (width == null) Modifier.weight(1f) else Modifier.width(width.dp)
}

private fun MView.explicitWidth(): Int? = when (this) {
    is TextualView -> size?.width
    is ImageView -> size?.width
    is Layout -> when (orientation) {
        LayoutOrientation.horizontal -> views.mapNotNull { it.explicitWidth() }.takeIf { it.size == views.size }?.sum()
        else -> views.mapNotNull { it.explicitWidth() }.maxOrNull()
    }
    else -> null
}

@Composable
private fun RenderText(
    view: TextualView,
    data: JsonObject?,
    modifier: Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = 2.dp)
) {
    val text = view.text(data)
    val label = view.label?.matchReplace(data).orEmpty()
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme
    // Under a label, the text is its supporting line: the label leads, the text is quieter.
    val style = when (view.textStyle) {
        TextStyle.bold -> typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        TextStyle.italic -> typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
        else -> typography.bodyLarge
    }.let { base -> view.textSize?.let { base.copy(fontSize = it.sp) } ?: base }
        .let { base -> if (label.isNotEmpty()) base.copy(color = colors.onSurfaceVariant) else base.copy(color = colors.onSurface) }
    val links = linkStyles()
    val body = bodyText(view, text, links)
    val display = buildAnnotatedString {
        if (label.isNotEmpty()) {
            // The label is a heading: a title's size and weight, unless labelStyle (or textSize) says otherwise.
            val labelSpan = SpanStyle(
                fontSize = view.textSize?.sp ?: typography.titleMedium.fontSize,
                fontWeight = when (view.labelStyle) {
                    null -> FontWeight.SemiBold
                    TextStyle.bold -> FontWeight.Bold
                    else -> FontWeight.Normal
                },
                fontStyle = if (view.labelStyle == TextStyle.italic) FontStyle.Italic else FontStyle.Normal,
                color = colors.onSurface
            )
            withStyle(labelSpan) { append(linkified(label, links)) }
            if (body.isNotEmpty()) append("\n")
        }
        append(body)
    }
    val textModifier = modifier
        .then(view.size?.height?.let { Modifier.height(it.dp) } ?: Modifier)
        .then(actionModifier(view.action, data))
        .padding(contentPadding)
    if (view.action == null) {
        Text(text = display, modifier = textModifier, style = style, overflow = TextOverflow.Ellipsis)
    } else {
        // Text that does something on a tap says so, with an arrow at its end, as a list item does.
        Row(textModifier, verticalAlignment = Alignment.CenterVertically) {
            Text(text = display, modifier = Modifier.weight(1f), style = style, overflow = TextOverflow.Ellipsis)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/** A text view's text with its links: simple HTML when its format says so, else plain text with web addresses linked. */
private fun bodyText(view: TextualView, text: String, links: TextLinkStyles): AnnotatedString {
    val body = if (view.format == TextFormat.html) AnnotatedString.fromHtml(text, links) else linkified(text, links)
    val end = body.text.trimEnd().length
    return if (end < body.length) body.subSequence(0, end) else body
}

@Composable
private fun RenderButton(view: ButtonView, data: JsonObject?, modifier: Modifier) {
    val screen = LocalScreen.current
    val action = screen.resolve(view.action)
    val onClick = { if (action?.type != ActionType.longclick) screen.onAction(view.action, data) }
    val longClick = if (action?.type == ActionType.longclick) Modifier.combinedClickable(
        onClick = {},
        onLongClick = { screen.onAction(view.action, data) }
    ) else Modifier
    val icon = view.icon
    if (icon != null && view.isIconOnly()) {
        val description = view.label?.matchReplace(data)?.trim()?.takeIf { it.isNotEmpty() } ?: icon.spokenName()
        FilledTonalIconButton(onClick = onClick, modifier = modifier.then(longClick)) { Icon(icon.vector(), contentDescription = description) }
        return
    }
    // A button on its own is tonal: a form's submit button is the screen's one filled button.
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().then(longClick),
        contentPadding = if (icon != null) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding
    ) {
        if (icon != null) {
            Icon(icon.vector(), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }
        Text(view.text(data))
    }
}

/** A button that shows only its icon: it keeps its own size, and the row it is in is centred on it. */
private fun MView.isIconOnly(): Boolean = this is ButtonView && icon != null && title.isNullOrBlank() && mapping == null

private fun ButtonIcon.vector(): ImageVector = when (this) {
    ButtonIcon.add -> Icons.Filled.Add
    ButtonIcon.check -> Icons.Filled.Check
    ButtonIcon.close -> Icons.Filled.Close
    ButtonIcon.delete -> Icons.Filled.Delete
    ButtonIcon.edit -> Icons.Filled.Edit
    ButtonIcon.email -> Icons.Filled.Email
    ButtonIcon.favorite -> Icons.Filled.Favorite
    ButtonIcon.info -> Icons.Filled.Info
    ButtonIcon.phone -> Icons.Filled.Phone
    ButtonIcon.place -> Icons.Filled.Place
    ButtonIcon.refresh -> Icons.Filled.Refresh
    ButtonIcon.search -> Icons.Filled.Search
    ButtonIcon.settings -> Icons.Filled.Settings
    ButtonIcon.share -> Icons.Filled.Share
    ButtonIcon.star -> Icons.Filled.Star
}

/** What a screen reader says for an icon without a label, such as "Delete". */
private fun ButtonIcon.spokenName(): String = name.replaceFirstChar { it.uppercase() }

/**
 * A tick box: ticked when the data's value is true; ticking it runs the action at once. A ticked title is struck
 * through. Only the box ticks, so a tap on the title reaches the list item (to open it, say).
 */
@Composable
private fun RenderCheck(view: CheckView, data: JsonObject?, modifier: Modifier) {
    val screen = LocalScreen.current
    val checked = data?.getValue(view.mapping) == "true"
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange = { screen.onCheck(view, data, it) })
        Spacer(Modifier.width(12.dp))
        Text(
            view.title?.matchReplace(data).orEmpty(),
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified
        )
    }
}

@Composable
private fun RenderImage(
    view: ImageView,
    data: JsonObject?,
    modifier: Modifier,
    allowImagePreview: Boolean,
    edgeToEdge: Boolean = false,
    fillParentHeight: Boolean = false
) {
    val context = LocalContext.current
    val mapped = view.mapping?.let { data?.getValue(it) }
    // An address with a placeholder that has no value would load something else: the placeholder shows instead.
    val url = (mapped ?: view.url)?.fillAddress(data)?.takeIf { it.isNotBlank() }
    val openPreview: (() -> Unit)? = if (allowImagePreview) {
        { url?.let { context.startActivity(createIntentPhotoActivity(context, it)) } }
    } else null
    val heightModifier = if (fillParentHeight) {
        Modifier.fillMaxHeight()
    } else {
        view.size?.height?.let { Modifier.height(it.dp) } ?: Modifier.height(220.dp)
    }
    val imageModifier = modifier
        .then(heightModifier)
        .clip(if (edgeToEdge) RectangleShape else RoundedCornerShape(16.dp))
        .then(actionModifier(view.action, data, openPreview))
    if (url == null) {
        ImagePlaceholder(imageModifier) { Text("Image unavailable") }
        return
    }
    // A plain AsyncImage, not SubcomposeAsyncImage: list cards size their rows to their content, which a
    // subcomposition can't report. Loading and failure are drawn over it.
    var state by remember(url) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    Box(imageModifier) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = if (view.scale == ImageScale.fit) ContentScale.Fit else ContentScale.Crop,
            onState = { state = it },
            // The tag marks a loaded image, which tests wait for.
            modifier = Modifier.matchParentSize().then(
                if (state is AsyncImagePainter.State.Success) Modifier.testTag("image-${data?.optString("id").orEmpty()}")
                else Modifier
            )
        )
        when (state) {
            is AsyncImagePainter.State.Success -> {}
            is AsyncImagePainter.State.Error -> ImagePlaceholder(Modifier.matchParentSize()) { Text("Image unavailable") }
            else -> ImagePlaceholder(Modifier.matchParentSize()) { CircularProgressIndicator(Modifier.size(32.dp)) }
        }
    }
}

@Composable
private fun ImagePlaceholder(modifier: Modifier, content: @Composable () -> Unit) {
    Card(modifier) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun RenderList(view: ListView, data: JsonObject?, modifier: Modifier) {
    val screen = LocalScreen.current
    val entry = screen.lists.get(view, data)
    val state by entry.content.collectAsState()

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            state.waiting -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            state.errorMessage != null -> ListLoadError(state.errorMessage.orEmpty()) { screen.lists.retry(entry) }
            state.items.isEmpty() -> EmptyList(view, entry)
            view.orientation == LayoutOrientation.horizontal &&
                (state.items.size > HORIZONTAL_LAZY_THRESHOLD || state.canLoadMore) -> LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val cardWidth = (view.itemView.explicitWidth()?.plus(28) ?: 300).dp
                itemsIndexed(
                    items = state.items,
                    key = { index, item -> listItemKey(view, index, item) }
                ) { _, item ->
                    ListCard(view, item, Modifier.width(cardWidth))
                }
                if (state.canLoadMore || state.moreErrorMessage != null) {
                    item(key = "list-more") { Box(Modifier.width(cardWidth)) { MoreItems(entry, state, automatic = true) } }
                }
            }
            view.orientation == LayoutOrientation.horizontal -> Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val cardWidth = (view.itemView.explicitWidth()?.plus(28) ?: 300).dp
                state.items.forEach { item ->
                    ListCard(view, item, Modifier.width(cardWidth))
                }
            }
            view.columnCount > 1 -> state.items.chunked(view.columnCount).forEachIndexed { index, row ->
                GridRow(view, row, index * view.columnCount)
            }
            else -> state.items.forEachIndexed { index, item ->
                ListCard(
                    view = view,
                    item = item,
                    modifier = Modifier.fillMaxWidth().testTag("list-position-$index")
                )
            }
        }
        // Inside a screen that scrolls as a whole, the next page waits for a tap rather than loading all at once.
        if (view.orientation != LayoutOrientation.horizontal && state.items.isNotEmpty() &&
            (state.canLoadMore || state.moreErrorMessage != null)
        ) {
            MoreItems(entry, state, automatic = false)
        }
    }
}

private fun LazyListScope.verticalListItems(view: ListView, entry: ListsState.Entry, state: ListsState.Content) {
    when {
        state.waiting -> item(key = "list-loading-${System.identityHashCode(view)}") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.errorMessage != null -> item(key = "list-error-${System.identityHashCode(view)}") {
            val screen = LocalScreen.current
            ListLoadError(state.errorMessage.orEmpty()) { screen.lists.retry(entry) }
        }
        state.items.isEmpty() -> item(key = "list-empty-${System.identityHashCode(view)}") {
            EmptyList(view, entry)
        }
        view.columnCount > 1 -> itemsIndexed(
            items = state.items.chunked(view.columnCount),
            key = { index, row -> listItemKey(view, index * view.columnCount, row.first()) }
        ) { index, row ->
            GridRow(view, row, index * view.columnCount)
        }
        else -> itemsIndexed(
            items = state.items,
            key = { index, item -> listItemKey(view, index, item) }
        ) { _, item ->
            ListCard(view, item, Modifier.fillMaxWidth())
        }
    }
    if (state.items.isNotEmpty() && (state.canLoadMore || state.moreErrorMessage != null)) {
        item(key = "list-more-${System.identityHashCode(view)}") { MoreItems(entry, state, automatic = true) }
    }
}

/** One row of a grid list: its items share the width, and a short last row keeps their size. */
@Composable
private fun GridRow(view: ListView, row: List<JsonObject>, firstIndex: Int) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        row.forEachIndexed { offset, item ->
            ListCard(view, item, Modifier.weight(1f).fillMaxHeight().testTag("list-position-${firstIndex + offset}"))
        }
        repeat(view.columnCount - row.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun EmptyList(view: ListView, entry: ListsState.Entry) {
    Text(
        view.emptyText?.matchReplace(entry.data)?.takeIf { it.isNotBlank() } ?: "No data available",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp).testTag("list-empty")
    )
}

/**
 * The end of a list with more pages: [automatic], it fetches the next one as soon as it comes into view (a lazy list
 * composes it only near its end); otherwise a button does. Then a wait, or why the page couldn't load.
 */
@Composable
private fun MoreItems(entry: ListsState.Entry, state: ListsState.Content, automatic: Boolean) {
    val screen = LocalScreen.current
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("list-more"), contentAlignment = Alignment.Center) {
        when {
            state.moreErrorMessage != null -> ListLoadError(state.moreErrorMessage.orEmpty()) { screen.lists.loadMore(entry) }
            state.loadingMore || automatic -> {
                if (automatic) LaunchedEffect(entry, state.items.size) { screen.lists.loadMore(entry) }
                CircularProgressIndicator(Modifier.size(32.dp))
            }
            else -> OutlinedButton(onClick = { screen.lists.loadMore(entry) }) { Text("Show more") }
        }
    }
}

@Composable
private fun ListLoadError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("list-load-error"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onRetry) { Text("Retry") }
    }
}

private fun listItemKey(view: ListView, index: Int, item: JsonObject): String =
    "${System.identityHashCode(view)}-$index-${item.optString("id")}"

@Composable
private fun ListCard(view: ListView, item: JsonObject, modifier: Modifier) {
    Card(
        modifier.then(actionModifier(view.action, item))
            .testTag("list-item-${item.optString("id")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListCardContent(
            itemView = view.itemView,
            item = item,
            allowImagePreview = view.action == null
        )
    }
}

@Composable
private fun ListCardContent(
    itemView: MView,
    item: JsonObject,
    allowImagePreview: Boolean
) {
    val screen = LocalScreen.current
    val resolvedItem = screen.resolve(itemView)
    val resolvedLayout = resolvedItem as? Layout
    val resolvedChildren = resolvedLayout?.views?.let { screen.shownViews(it, item) }

    if (resolvedItem is ImageView) {
        RenderImage(
            view = resolvedItem,
            data = item,
            modifier = Modifier.fillMaxWidth(),
            allowImagePreview = allowImagePreview,
            edgeToEdge = true
        )
    } else if (
        resolvedLayout?.orientation == LayoutOrientation.vertical &&
        resolvedChildren?.firstOrNull() is ImageView
    ) {
        Column(Modifier.fillMaxWidth()) {
            resolvedChildren.forEachIndexed { index, child ->
                when {
                    index == 0 && child is ImageView -> RenderImage(
                        view = child,
                        data = item,
                        modifier = Modifier.fillMaxWidth(),
                        allowImagePreview = false,
                        edgeToEdge = true
                    )
                    // A button among a card's texts is drawn as text, as it always was (ButtonView was a TextView).
                    child is TextualView -> RenderText(
                        view = child,
                        data = item,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    )
                    else -> Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        RenderNode(child, item, Modifier.fillMaxWidth(), allowImagePreview = false)
                    }
                }
            }
        }
    } else if (
        resolvedLayout?.orientation == LayoutOrientation.horizontal &&
        resolvedChildren?.firstOrNull() is ImageView
    ) {
        val image = resolvedChildren.first() as ImageView
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = (image.size?.width?.let { Modifier.width(it.dp) } ?: Modifier.width(112.dp))
                    .fillMaxHeight()
            ) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(image.size?.height?.dp ?: 220.dp)
                )
                RenderImage(
                    view = image,
                    data = item,
                    modifier = Modifier.matchParentSize(),
                    allowImagePreview = false,
                    edgeToEdge = true,
                    fillParentHeight = true
                )
            }
            Column(Modifier.weight(1f)) {
                resolvedChildren.drop(1).forEach { child ->
                    when (child) {
                        is TextualView -> RenderHorizontalCardText(
                            view = child,
                            data = item,
                            modifier = Modifier.fillMaxWidth()
                        )
                        else -> Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            RenderNode(child, item, Modifier.fillMaxWidth(), allowImagePreview = false)
                        }
                    }
                }
            }
        }
    } else {
        Column(Modifier.padding(14.dp)) {
            RenderNode(resolvedItem, item, Modifier.fillMaxWidth(), allowImagePreview = false)
        }
    }
}

/** Links in a text view: the theme's primary color, underlined. */
@Composable
private fun linkStyles() = TextLinkStyles(
    SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
)

@Composable
private fun RenderHorizontalCardText(view: TextualView, data: JsonObject?, modifier: Modifier) {
    val title = view.label?.matchReplace(data).orEmpty()
    val supportingText = view.text(data)
    Column(
        modifier = modifier
            .then(view.size?.height?.let { Modifier.height(it.dp) } ?: Modifier)
            .then(actionModifier(view.action, data))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (supportingText.isNotEmpty()) {
            Text(
                text = bodyText(view, supportingText, linkStyles()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RenderForm(view: FormView, data: JsonObject?, modifier: Modifier) {
    val screen = LocalScreen.current
    val form = screen.forms.state(view, data)
    val values by form.values.collectAsState()
    val error by form.error.collectAsState()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // A form's title is a heading like a text view's label.
        view.title?.let { Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        view.formElements.forEach { item ->
            val onChange: (Any?) -> Unit = { screen.forms.set(view, data, item.mapping, it) }
            when (item) {
                is CheckBox -> BooleanField(item, values[item.mapping] == true, onChange)
                is TextInput -> TextField(item, values[item.mapping] as? String, onChange)
                is DateInput -> DateField(item, values[item.mapping] as? String, onChange)
                is Selector -> SelectorField(item, values[item.mapping] as? String, onChange)
                is TimeInput -> TimeField(item, values[item.mapping] as? String, onChange)
                // A hidden field only sends its value.
                is HiddenInput -> Unit
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { screen.onSubmit(view, data) }, modifier = Modifier.align(Alignment.End)) {
            Text(view.buttonTitle ?: "Submit")
        }
    }
}

@Composable
private fun BooleanField(item: CheckBox, checked: Boolean, onChange: (Any?) -> Unit) {
    Column {
        // The whole row toggles, label included.
        Row(
            Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = { onChange(it) }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(checked, onCheckedChange = null)
            Spacer(Modifier.width(12.dp))
            Text(item.title, style = MaterialTheme.typography.titleMedium)
        }
        Description(item.description)
    }
}

@Composable
private fun TextField(item: TextInput, value: String?, onChange: (Any?) -> Unit) {
    val keyboardType = when (item.textType) {
        TextType.numeric -> KeyboardType.Number
        TextType.email -> KeyboardType.Email
        TextType.password -> KeyboardType.Password
        else -> KeyboardType.Text
    }
    OutlinedTextField(
        value = value.orEmpty(),
        onValueChange = { onChange(it.ifEmpty { null }) },
        label = { Text(item.title) },
        placeholder = { item.placeholder?.let { Text(it) } },
        supportingText = if (item.description == null) null else {{ Text(item.description!!) }},
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (item.textType == TextType.password) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DateField(item: DateInput, value: String?, onChange: (Any?) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Column {
        FieldTitle(item.title)
        OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text(value ?: "Choose date") }
        Description(item.description)
    }
    if (!picking) return
    val min = item.min?.let { utcDay(Date(it)) }
    val max = item.max?.let { utcDay(Date(it)) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = value?.toDate()?.let(::utcDay),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                (min == null || utcTimeMillis >= min) && (max == null || utcTimeMillis <= max)
        }
    )
    DatePickerDialog(
        onDismissRequest = { picking = false },
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onChange(localDay(it).stringValue()) }
                picking = false
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(android.R.string.cancel)) } }
    ) { DatePicker(state) }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TimeField(item: TimeInput, value: String?, onChange: (Any?) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Column {
        FieldTitle(item.title)
        OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text(value ?: "Choose time") }
        Description(item.description)
    }
    if (!picking) return
    val (hour, minute) = value?.let(FormDates::parseTime) ?: (12 to 0)
    val state = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    )
    AlertDialog(
        onDismissRequest = { picking = false },
        confirmButton = {
            TextButton(onClick = {
                onChange(FormDates.formatTime(state.hour, state.minute))
                picking = false
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(android.R.string.cancel)) } },
        text = { TimePicker(state) }
    )
}

/** The picker counts days in UTC; the form's dates are local midnights. */
private fun utcDay(date: Date): Long =
    date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun localDay(utcMillis: Long): Date =
    Date.from(Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant())

@Composable
private fun SelectorField(item: Selector, selected: String?, onChange: (Any?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldTitle(item.title)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item.options.forEach { option ->
                FilterChip(
                    selected = selected == option.value,
                    onClick = { onChange(option.value) },
                    label = { Text(option.label) }
                )
            }
        }
        Description(item.description)
    }
}

/** The title above a date, time or choice field, in the field labels' style. */
@Composable
private fun FieldTitle(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Description(description: String?) {
    description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun actionModifier(actionRef: Action?, data: JsonObject?, fallback: (() -> Unit)? = null): Modifier {
    val screen = LocalScreen.current
    val action = screen.resolve(actionRef)
    return if (action != null || fallback != null) Modifier.combinedClickable(
        onClick = {
            when {
                action != null && action.type != ActionType.longclick -> screen.onAction(actionRef, data)
                action == null -> fallback?.invoke()
            }
        },
        onLongClick = if (action?.type == ActionType.longclick) {{ screen.onAction(actionRef, data) }} else null
    ) else Modifier
}
