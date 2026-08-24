@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package it.sapienza.appinterpreter.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import it.sapienza.appinterpreter.activity.MainActivity
import it.sapienza.appinterpreter.activity.createIntentPhotoActivity
import it.sapienza.appinterpreter.AppRatioApplication
import it.sapienza.appinterpreter.custom_view.EventManager
import it.sapienza.appinterpreter.extensions.getArray
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Calendar
import java.util.function.Consumer

private class ListUiState(initiallyWaiting: Boolean) {
    val items = mutableStateListOf<JSONObject>()
    var waiting by mutableStateOf(initiallyWaiting)
}

private const val HORIZONTAL_LAZY_THRESHOLD = 8

@Composable
fun RendererScreen(model: MView, data: JSONObject? = model.dataObj, loading: Boolean) {
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
    val context = LocalContext.current
    val app = (context.applicationContext as AppRatioApplication).app
    val resolved = app?.viewBy(model) ?: model
    val data = remember(resolved, resolved.data, screenData) { screenData ?: resolved.dataObj }

    when {
        resolved is Layout && resolved.orientation != LayoutOrientation.horizontal -> {
            val children = remember(resolved, app) {
                resolved.views.map { app?.viewBy(it) ?: it }
            }
            val listStates = children.map { child ->
                if (child is ListView && child.orientation != LayoutOrientation.horizontal) {
                    val childData = remember(child, child.data, data) { child.dataObj ?: data }
                    rememberListUiState(child, childData)
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
            val state = rememberListUiState(resolved, data)
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
    val context = LocalContext.current
    val app = (context.applicationContext as AppRatioApplication).app
    val resolved = app?.viewBy(model) ?: model
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
    val context = LocalContext.current
    val resolvedChildren = layout.views.map { (context.applicationContext as AppRatioApplication).app?.viewBy(it) ?: it }
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
        text = display,
        modifier = textModifier,
        style = style,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RenderButton(view: ButtonView, data: JSONObject?, modifier: Modifier) {
    val context = LocalContext.current
    val action = resolveAction(view.action)
    val title = (view.mapping?.let { data?.getValue(it) } ?: view.title).orEmpty().matchReplace(data)
    Button(
        onClick = { if (action?.type != ActionType.longclick) executeAction(context as MainActivity, action, data) },
        modifier = modifier.fillMaxWidth().then(
            if (action?.type == ActionType.longclick) Modifier.combinedClickable(
                onClick = {},
                onLongClick = { executeAction(context as MainActivity, action, data) }
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
    edgeToEdge: Boolean = false
) {
    val context = LocalContext.current
    val mapped = view.mapping?.let { data?.getValue(it) }
    val url = (mapped ?: view.url)?.matchReplace(data)
    var imageFailed by remember(url) { mutableStateOf(false) }
    val bitmap by produceState<ImageBitmap?>(null, url) {
        imageFailed = false
        value = url?.let { imageUrl ->
            var loaded: ImageBitmap? = null
            repeat(3) { attempt ->
                loaded = runCatching {
                    withContext(Dispatchers.IO) {
                        Glide.with(context).asBitmap().load(imageUrl).submit().get().asImageBitmap()
                    }
                }.getOrNull()
                if (loaded != null) return@let loaded
                if (attempt < 2) delay(200L * (attempt + 1))
            }
            null
        }.also { loaded ->
            if (url != null && loaded == null) {
                imageFailed = true
            }
        }
    }
    val openPreview: (() -> Unit)? = if (allowImagePreview) {
        { url?.let { context.startActivity(createIntentPhotoActivity(context, it)) } }
    } else null
    val imageModifier = modifier
        .then(view.size?.height?.let { Modifier.height(it.dp) } ?: Modifier.height(220.dp))
        .clip(if (edgeToEdge) RectangleShape else RoundedCornerShape(16.dp))
        .then(actionModifier(view.action, data, openPreview))
    if (bitmap == null) {
        Card(imageModifier) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (url != null && !imageFailed) CircularProgressIndicator(Modifier.size(32.dp)) else Text("Image unavailable")
            }
        }
    } else {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = imageModifier.testTag("image-${data?.optString("id").orEmpty()}")
        )
    }
}

@Composable
private fun RenderList(view: ListView, data: JSONObject?, modifier: Modifier) {
    val state = rememberListUiState(view, data)

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            state.waiting -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
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

@Composable
private fun rememberListUiState(view: ListView, data: JSONObject?): ListUiState {
    val context = LocalContext.current
    val dataKey = data?.toString().orEmpty()
    val state = remember(view, dataKey) { ListUiState(view.initData != null) }
    DisposableEffect(view, dataKey) {
        state.items.clear()
        state.waiting = view.initData != null
        val accept: (JSONObject) -> Unit = { response ->
            val array = view.mapping?.let { response.getArray(it) } ?: view.dataObjArr
            (context as MainActivity).runOnUiThread {
                state.items.clear()
                array?.let { values ->
                    for (index in 0 until values.length()) {
                        state.items.add(values.getJSONObject(index))
                    }
                }
                state.waiting = false
            }
        }
        val call = view.initData?.let { event ->
            EventManager.manage(
                context = context,
                event = event,
                data = data,
                callbackConsumer = Consumer { accept(it) },
                showLoading = false
            )
        }
        if (call == null) accept(data ?: JSONObject())
        onDispose { call?.cancel() }
    }
    return state
}

private fun LazyListScope.verticalListItems(view: ListView, state: ListUiState) {
    when {
        state.waiting -> item(key = "list-loading-${System.identityHashCode(view)}") {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
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

private fun listItemKey(view: ListView, index: Int, item: JSONObject): String =
    "${System.identityHashCode(view)}-$index-${item.optString("id")}"

@Composable
private fun ListCard(view: ListView, item: JSONObject, modifier: Modifier) {
    Card(
        modifier.then(actionModifier(view.action, item))
            .testTag("list-item-${item.optString("id")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListCardContent(view.itemView, item)
    }
}

@Composable
private fun ListCardContent(itemView: MView, item: JSONObject) {
    val context = LocalContext.current
    val app = (context.applicationContext as AppRatioApplication).app
    val resolvedItem = app?.viewBy(itemView) ?: itemView
    val resolvedLayout = resolvedItem as? Layout
    val resolvedChildren = resolvedLayout?.views?.map { app?.viewBy(it) ?: it }

    if (
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RenderImage(
                view = image,
                data = item,
                modifier = image.size?.width?.let { Modifier.width(it.dp) } ?: Modifier.width(112.dp),
                allowImagePreview = false,
                edgeToEdge = true
            )
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
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RenderForm(view: FormView, data: JSONObject?, modifier: Modifier) {
    val context = LocalContext.current
    val action = resolveAction(view.action)
    var error by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        view.title?.let { Text(it, style = MaterialTheme.typography.headlineSmall) }
        view.formElements.forEach { item ->
            when (item) {
                is CheckBox -> BooleanField(item, data)
                is TextInput -> TextField(item, data)
                is DateInput -> DateField(item, data)
                is Selector -> SelectorField(item, data)
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            if (view.formElements.all { it.isSet() }) {
                error = null
                action?.event?.let { EventManager.evaluateEvent(context, it, formJson(view)) }
            } else error = "Please complete all required fields."
        }, modifier = Modifier.fillMaxWidth()) { Text(view.buttonTitle ?: "Submit") }
    }
}

@Composable
private fun BooleanField(item: CheckBox, data: JSONObject?) {
    var checked by remember(item, data) {
        mutableStateOf(data?.getValue(item.mapping)?.toBooleanStrictOrNull() ?: item.value)
    }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Switch(checked, { checked = it; item.value = it })
            Spacer(Modifier.width(12.dp))
            Text(item.title, style = MaterialTheme.typography.titleMedium)
        }
        Description(item.description)
    }
}

@Composable
private fun TextField(item: TextInput, data: JSONObject?) {
    var value by remember(item, data) { mutableStateOf(data?.getValue(item.mapping) ?: item.value.orEmpty()) }
    val keyboardType = when (item.textType) {
        TextType.numeric -> KeyboardType.Number
        TextType.email -> KeyboardType.Email
        TextType.password -> KeyboardType.Password
        else -> KeyboardType.Text
    }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it; item.value = it.ifEmpty { null } },
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
private fun DateField(item: DateInput, data: JSONObject?) {
    val context = LocalContext.current
    var value by remember(item, data) { mutableStateOf(data?.getValue(item.mapping) ?: item.value?.stringValue()) }
    Column {
        Text(item.title, style = MaterialTheme.typography.titleSmall)
        OutlinedButton(onClick = {
            val calendar = Calendar.getInstance().apply { value?.toDate()?.let { time = it } }
            DatePickerDialog(context, { _, year, month, day ->
                calendar.set(year, month, day)
                item.value = calendar.time
                value = calendar.time.stringValue()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
                item.min?.let { datePicker.minDate = it.time }
                item.max?.let { datePicker.maxDate = it.time }
            }.show()
        }, modifier = Modifier.fillMaxWidth()) { Text(value ?: "Choose date") }
        Description(item.description)
    }
}

@Composable
private fun SelectorField(item: Selector, data: JSONObject?) {
    var selected by remember(item, data) { mutableStateOf(data?.getValue(item.mapping) ?: item.selection) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(item.title, style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item.options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { selected = option; item.selection = option },
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

private fun formJson(view: FormView): JSONObject {
    val json = JSONObject()
    view.formElements.forEach { element ->
        val parts = element.mapping.split('.')
        var target = json
        parts.dropLast(1).forEach { key ->
            if (!target.has(key)) target.put(key, JSONObject())
            target = target.getJSONObject(key)
        }
        target.put(parts.last(), element.value())
    }
    return json
}

@Composable
private fun actionModifier(actionRef: Action?, data: JSONObject?, fallback: (() -> Unit)? = null): Modifier {
    val context = LocalContext.current
    val action = resolveAction(actionRef)
    return if (action != null || fallback != null) Modifier.combinedClickable(
        onClick = {
            when {
                action != null && action.type != ActionType.longclick -> executeAction(context as MainActivity, action, data)
                action == null -> fallback?.invoke()
            }
        },
        onLongClick = if (action?.type == ActionType.longclick) {{ executeAction(context as MainActivity, action, data) }} else null
    ) else Modifier
}

@Composable
private fun resolveAction(action: Action?): Action? {
    val app = (LocalContext.current.applicationContext as AppRatioApplication).app
    return action?.let { app?.actionBy(it) }
}

private fun executeAction(context: MainActivity, action: Action?, data: JSONObject?) {
    val payload = action?.dataObj ?: data
    action?.event?.let { EventManager.evaluateEvent(context, it, payload) }
}
