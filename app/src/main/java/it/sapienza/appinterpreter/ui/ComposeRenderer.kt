@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package it.sapienza.appinterpreter.ui

import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.LocalLifecycleOwner
import it.sapienza.appinterpreter.runtime.ListsState
import it.sapienza.appinterpreter.runtime.ScreenViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.util.Date
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
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
import it.sapienza.appinterpreter.extensions.getValue
import it.sapienza.appinterpreter.extensions.matchReplace
import it.sapienza.appinterpreter.extensions.toDate
import it.sapienza.appinterpreter.form.extensions.stringValue
import it.sapienza.appinterpreter.model.Layout
import it.sapienza.appinterpreter.model.LayoutOrientation
import it.sapienza.appinterpreter.model.action.Action
import it.sapienza.appinterpreter.model.action.ActionType
import it.sapienza.appinterpreter.model.view_model.ButtonView
import it.sapienza.appinterpreter.model.view_model.FormView
import it.sapienza.appinterpreter.model.view_model.ImageView
import it.sapienza.appinterpreter.model.view_model.ListView
import it.sapienza.appinterpreter.model.view_model.TextView
import it.sapienza.appinterpreter.model.view_model.form_model.CheckBox
import it.sapienza.appinterpreter.model.view_model.form_model.DateInput
import it.sapienza.appinterpreter.model.view_model.form_model.Selector
import it.sapienza.appinterpreter.model.view_model.form_model.TextInput
import it.sapienza.appinterpreter.model.view_model.form_model.helpers.TextType
import it.sapienza.appinterpreter.model.view_model.helper.MView
import it.sapienza.appinterpreter.model.view_model.helper.TextStyle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

private const val HORIZONTAL_LAZY_THRESHOLD = 8

/** The screen being drawn: its app, state and events. */
val LocalScreen = staticCompositionLocalOf<ScreenViewModel> { error("RendererScreen needs LocalScreen") }

@Composable
fun RendererScreen(model: MView, data: JSONObject? = model.dataObj, loading: Boolean) {
    val screen = LocalScreen.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, screen) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) screen.lists.retryFailed()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    Box(Modifier.fillMaxSize().testTag("renderer-screen")) {
        RenderRoot(model, data)
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
private fun RenderRoot(model: MView, screenData: JSONObject?) {
    val screen = LocalScreen.current
    val resolved = screen.resolve(model)
    val data = remember(resolved, resolved.data, screenData) { screenData ?: resolved.dataObj }

    when {
        resolved is Layout && resolved.orientation != LayoutOrientation.horizontal -> {
            val children = remember(resolved) { resolved.views.map(screen::resolve) }
            val listStates = children.map { child ->
                if (child is ListView && child.orientation != LayoutOrientation.horizontal) {
                    val childData = remember(child, child.data, data) { child.dataObj ?: data }
                    screen.lists.get(child, childData)
                } else {
                    null
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                children.forEachIndexed { index, child ->
                    val listState = listStates[index]
                    if (child is ListView && listState != null) {
                        verticalListItems(child, listState)
                    } else {
                        item(key = "root-$index-${child.id.orEmpty()}") {
                            RenderNode(child, data, columnChildModifier(child))
                        }
                    }
                }
            }
        }
        resolved is ListView && resolved.orientation != LayoutOrientation.horizontal -> {
            val state = screen.lists.get(resolved, data)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                verticalListItems(resolved, state)
            }
        }
        else -> {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RenderNode(resolved, data, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun RenderNode(
    model: MView,
    parentData: JSONObject?,
    modifier: Modifier = Modifier,
    allowImagePreview: Boolean = true
) {
    val resolved = LocalScreen.current.resolve(model)
    val data = remember(resolved, resolved.data, parentData) { resolved.dataObj ?: parentData }
    when (resolved) {
        is Layout -> RenderLayout(resolved, data, modifier, allowImagePreview)
        is ButtonView -> RenderButton(resolved, data, modifier)
        is TextView -> RenderText(resolved, data, modifier)
        is ImageView -> RenderImage(resolved, data, modifier, allowImagePreview)
        is ListView -> RenderList(resolved, data, modifier)
        is FormView -> RenderForm(resolved, data, modifier)
    }
}

@Composable
private fun RenderLayout(
    layout: Layout,
    data: JSONObject?,
    modifier: Modifier,
    allowImagePreview: Boolean
) {
    val screen = LocalScreen.current
    val resolvedChildren = layout.views.map(screen::resolve)
    if (layout.orientation == LayoutOrientation.horizontal) {
        Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            resolvedChildren.forEach { child ->
                RenderNode(child, data, rowChildModifier(child), allowImagePreview)
            }
        }
    } else {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            resolvedChildren.forEach { child -> RenderNode(child, data, columnChildModifier(child), allowImagePreview) }
        }
    }
}

private fun columnChildModifier(model: MView): Modifier {
    val width = model.explicitWidth()
    return if (width == null) Modifier.fillMaxWidth() else Modifier.width(width.dp)
}

private fun RowScope.rowChildModifier(model: MView): Modifier {
    val width = model.explicitWidth()
    return if (width == null) Modifier.weight(1f) else Modifier.width(width.dp)
}

private fun MView.explicitWidth(): Int? = when (this) {
    is TextView -> size?.width
    is ImageView -> size?.width
    is Layout -> when (orientation) {
        LayoutOrientation.horizontal -> views.mapNotNull { it.explicitWidth() }.takeIf { it.size == views.size }?.sum()
        else -> views.mapNotNull { it.explicitWidth() }.maxOrNull()
    }
    else -> null
}

@Composable
private fun RenderText(
    view: TextView,
    data: JSONObject?,
    modifier: Modifier,
    contentPadding: PaddingValues = PaddingValues(6.dp)
) {
    val text = ((view.mapping?.let { data?.getValue(it) } ?: view.title).orEmpty()).matchReplace(data)
    val style = when (view.textStyle) {
        TextStyle.bold -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        TextStyle.italic -> MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
        else -> MaterialTheme.typography.bodyLarge
    }.let { base -> view.textSize?.let { base.copy(fontSize = it.sp) } ?: base }
    val label = view.label?.matchReplace(data).orEmpty()
    val display = listOf(label, text).filter { it.isNotEmpty() }.joinToString("\n").trimEnd()
    val textModifier = modifier
        .then(view.size?.height?.let { Modifier.height(it.dp) } ?: Modifier)
        .then(actionModifier(view.action, data))
        .padding(contentPadding)
    Text(
        text = linkified(display, linkStyles()),
        modifier = textModifier,
        style = style,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RenderButton(view: ButtonView, data: JSONObject?, modifier: Modifier) {
    val screen = LocalScreen.current
    val action = screen.resolve(view.action)
    val title = (view.mapping?.let { data?.getValue(it) } ?: view.title).orEmpty().matchReplace(data)
    Button(
        onClick = { if (action?.type != ActionType.longclick) screen.onAction(view.action, data) },
        modifier = modifier.fillMaxWidth().then(
            if (action?.type == ActionType.longclick) Modifier.combinedClickable(
                onClick = {},
                onLongClick = { screen.onAction(view.action, data) }
            ) else Modifier
        )
    ) { Text(title) }
}

@Composable
private fun RenderImage(
    view: ImageView,
    data: JSONObject?,
    modifier: Modifier,
    allowImagePreview: Boolean,
    edgeToEdge: Boolean = false,
    fillParentHeight: Boolean = false
) {
    val context = LocalContext.current
    val mapped = view.mapping?.let { data?.getValue(it) }
    val url = (mapped ?: view.url)?.matchReplace(data)
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
            contentScale = ContentScale.Crop,
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
private fun RenderList(view: ListView, data: JSONObject?, modifier: Modifier) {
    val screen = LocalScreen.current
    val state = screen.lists.get(view, data)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            state.waiting -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            state.errorMessage != null -> ListLoadError(state.errorMessage.orEmpty()) { screen.lists.retry(state) }
            state.items.isEmpty() -> Text("No data available", style = MaterialTheme.typography.bodyMedium)
            view.orientation == LayoutOrientation.horizontal &&
                state.items.size > HORIZONTAL_LAZY_THRESHOLD -> LazyRow(
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
            else -> state.items.forEachIndexed { index, item ->
                ListCard(
                    view = view,
                    item = item,
                    modifier = Modifier.fillMaxWidth().testTag("list-position-$index")
                )
            }
        }
    }
}

private fun LazyListScope.verticalListItems(view: ListView, state: ListsState.Entry) {
    when {
        state.waiting -> item(key = "list-loading-${System.identityHashCode(view)}") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.errorMessage != null -> item(key = "list-error-${System.identityHashCode(view)}") {
            val screen = LocalScreen.current
            ListLoadError(state.errorMessage.orEmpty()) { screen.lists.retry(state) }
        }
        state.items.isEmpty() -> item(key = "list-empty-${System.identityHashCode(view)}") {
            Text("No data available", style = MaterialTheme.typography.bodyMedium)
        }
        else -> itemsIndexed(
            items = state.items,
            key = { index, item -> listItemKey(view, index, item) }
        ) { _, item ->
            ListCard(view, item, Modifier.fillMaxWidth())
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

private fun listItemKey(view: ListView, index: Int, item: JSONObject): String =
    "${System.identityHashCode(view)}-$index-${item.optString("id")}"

@Composable
private fun ListCard(view: ListView, item: JSONObject, modifier: Modifier) {
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
    item: JSONObject,
    allowImagePreview: Boolean
) {
    val screen = LocalScreen.current
    val resolvedItem = screen.resolve(itemView)
    val resolvedLayout = resolvedItem as? Layout
    val resolvedChildren = resolvedLayout?.views?.map(screen::resolve)

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
                    child is TextView -> RenderText(
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
                        is TextView -> RenderHorizontalCardText(
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
private fun RenderHorizontalCardText(view: TextView, data: JSONObject?, modifier: Modifier) {
    val title = view.label?.matchReplace(data).orEmpty()
    val supportingText = ((view.mapping?.let { data?.getValue(it) } ?: view.title).orEmpty())
        .matchReplace(data)
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
                text = linkified(supportingText, linkStyles()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RenderForm(view: FormView, data: JSONObject?, modifier: Modifier) {
    val screen = LocalScreen.current
    val values = screen.forms.values(view, data)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        view.title?.let { Text(it, style = MaterialTheme.typography.headlineSmall) }
        view.formElements.forEach { item ->
            val onChange: (Any?) -> Unit = { screen.forms.set(view, data, item.mapping, it) }
            when (item) {
                is CheckBox -> BooleanField(item, values[item.mapping] == true, onChange)
                is TextInput -> TextField(item, values[item.mapping] as? String, onChange)
                is DateInput -> DateField(item, values[item.mapping] as? String, onChange)
                is Selector -> SelectorField(item, values[item.mapping] as? String, onChange)
            }
        }
        screen.forms.error(view, data)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { screen.onSubmit(view, data) }, modifier = Modifier.fillMaxWidth()) {
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
        Text(item.title, style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text(value ?: "Choose date") }
        Description(item.description)
    }
    if (!picking) return
    val min = item.min?.let(::utcDay)
    val max = item.max?.let(::utcDay)
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

/** The picker counts days in UTC; the form's dates are local midnights. */
private fun utcDay(date: Date): Long =
    date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun localDay(utcMillis: Long): Date =
    Date.from(Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant())

@Composable
private fun SelectorField(item: Selector, selected: String?, onChange: (Any?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(item.title, style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item.options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onChange(option) },
                    label = { Text(option) }
                )
            }
        }
        Description(item.description)
    }
}

@Composable
private fun Description(description: String?) {
    description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun actionModifier(actionRef: Action?, data: JSONObject?, fallback: (() -> Unit)? = null): Modifier {
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
