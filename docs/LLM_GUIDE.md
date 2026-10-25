# Generating Appkin configurations with an LLM

This file is the generation contract for Appkin v3. For individual field
definitions, read the [v3 configuration reference](CONFIGURATION_V3.md) and the
[canonical JSON Schema](../schema/v3.schema.json).

## Output contract

Return the configuration as one valid JSON object, with no comments, Markdown fences or
prose inside or around it. When you can create a file, put the JSON in the file; a short
note beside it may say what you left out and why, such as an image you couldn't verify.

Every new configuration must begin with these root fields:

```json
{
  "schemaVersion": 3,
  "name": "Application name",
  "version": "1.0.0",
  "mainView": {
    "type": "layout",
    "views": []
  }
}
```

The optional root fields are `author`, `icon`, `changelog`, `debugMode`, `views`, and
`actions`.

## Generation rules

1. Use only the properties this document describes, with lowercase enum values exactly
   as written. The schema rejects unknown properties.
2. Make `mainView` an inline view unless it intentionally references a reusable root
   view. Put reusable screens in root `views` and reusable behaviour in root `actions`.
3. Reference a reusable view or action with `{ "ref": "id" }`, never `{ "id": "..." }`
   alone. Give every referenced definition a unique, non-empty `id`.
4. Give every action definition a complete `event`.
5. Trace the data at every node: a list's `itemView` gets one item, a `rest` event's
   `thenDo` gets the response, a form's action gets the submitted values, and a `show`
   event's view gets the current data.
6. Use `mapping` for a path to one value or array; use `${path}` to build text or a URL
   from values. Match the real data exactly, including case and nesting.
7. Every list needs an `itemView` and one source of items: `mapping` to an array of
   objects (in `data`, or in the `initData` response), or `stored` for objects saved on
   the phone (no `mapping`).
8. Use HTTPS. Never write a real API key, token, password or other secret into the
   JSON: write a placeholder of two underscores, an uppercase name and two underscores,
   such as `?api_key=__EXAMPLE_API_KEY__` or
   `"headers": { "Authorization": "Bearer __EXAMPLE_TOKEN__" }`. Appkin asks the user for
   each value when the app opens, stores it encrypted and fills it into every request.
   Use one name per key.
9. Prefer a small working configuration over speculative or undocumented fields.
10. Before answering, check the JSON syntax and that every reference resolves to exactly
    one definition.

## Data-flow checklist

Before writing JSON, identify:

- The fixed data, if any, and the data saved on the phone (which collections)
- The exact shape of every REST response
- Which object is the current data when each action runs
- Which array, or which collection, each list shows
- Which values each form submits
- Which object each opened view receives

Then build the configuration in this order: reusable views, reusable actions and event
chains, the main view, references, and a final syntax and schema check.

## Supported language

Views:

- `layout`
- `text`
- `button`
- `image`
- `list`
- `form`
- `check` (a tick box for a true/false field; ticking it runs its action at once, usually `update`)

Events:

- `show`
- `rest`
- `alert`
- `calendar` (opens the phone's calendar app on a new event, for the user to save)
- `save` (saves the current data, such as a form's values, in a collection on the phone)
- `update` (changes a saved object, by default the one with the current data's `id`)
- `delete` (removes a saved object, or all that match `where`; put it behind an alert to ask first)
- `back` (closes the current screen, such as after a form saves an edit)
- `copy` (copies a text to the clipboard, to paste in a chat)
- `open` (hands a `https:`, `http:`, `geo:`, `tel:` or `mailto:` address to the browser,
  maps, the dialer or email; nothing else opens)
- `share` (opens the share sheet on a text)

Other enums:

- Orientations: `vertical`, `horizontal` (and `grid`, for lists only)
- Actions: `click`, `longclick`
- HTTP methods: `get`, `post`, `put`, `delete`
- Text styles: `regular`, `italic`, `bold`
- Text inputs: `plain`, `numeric`, `email`, `password`, `url`
- Form elements: `text`, `selector`, `date`, `time`, `checkbox`, `hidden`, `photos` (pick photos from the gallery, kept on the phone), `tags` (labels a stored list can filter on)

Do not invent support for PATCH, JavaScript expressions, conditions other than
`visibleIf`, databases beyond `save`, `update`, `delete` and `stored`, arbitrary native
components, or undocumented style properties.

## Common mistakes

### Text, buttons and images

- A button shows its `title` (or the value its `mapping` selects); its `label` is not
  displayed.
- A text view shows its `label` as a heading, then the `mapping` value or `title` under it
  as a quieter line. Use `textStyle` and `textSize` for emphasis, and `labelStyle` to style
  the label alone (`"labelStyle": "bold"`). For a value holding HTML, add
  `"format": "html"`. A web address written in a text view becomes a link.
- An image's `"scale": "fit"` shows the whole picture instead of cropping it.
- Sizes are numbers (dp and sp), not strings: `"size": { "height": 200 }`.
- Give every app an `icon`: one emoji that fits it, such as `"🏍️"` for a motorcycle guide.
  It is the app's icon on the home page and the phone's home screen.

### Lists and data

- A list's fixed items go in `data` on the list or a parent; a REST list uses `initData`.
  For an API with pages (a `skip`, `offset` or `page` parameter, or a "next" link), add
  `"nextPage": { "parameter": "skip", "step": 40 }` (the page size) or
  `"nextPage": { "urlFrom": "info.next" }` to `initData`. `"orientation": "grid"` with
  `"columns": 2` shows items side by side; `emptyText` says what an empty list means.
- Inside `itemView`, and in views opened from an item, mappings and `${...}` refer to that
  one item's fields, not to the whole response.
- Paths reach array items by index: `meals.0.strMeal`. When a response wraps one object in
  an array, open its view with a `show` event whose `mapping` is `"meals.0"`.
- `mapping` is a plain path such as `owner.name`; `${owner.name}` only builds text. A
  placeholder always starts with `$`: `${title}`, never `{title}`.
- Data is often incomplete. A text view with nothing to show isn't drawn, label included,
  and a missing `${...}` becomes empty text. To hide another view, such as a button that
  needs a field, use `"visibleIf": "field"` (shown when it has a value) or
  `"visibleIf": "!field"`. It is a path, never a comparison.
- An event's `data`, like an action's, replaces the current data it runs with; a form's
  submitted values still come first.

### Forms

- Form fields are `text`, `selector`, `date`, `time`, `checkbox`, `hidden`, `photos` and
  `tags`; there is no `input`, `dropdown` or `switch`. Dates are `dd/MM/yyyy`, times
  `HH:mm`. A selector option is a string or `{ "label": "Any type", "value": "" }`.
- To send something from the screen with a form, such as an item's id, use a `hidden`
  field: `{ "type": "hidden", "mapping": "id", "value": "${id}" }`.

### Saved on the phone

- For a list the user ticks off or edits, such as places to visit or things to pack, use a
  `stored` list with `initialItems`: the full objects, each with every field its views
  show and a `key`, plus `"unique": "key"`. The list saves them itself the first time, and
  later versions' new items are added without undoing ticks; never add a button that
  saves them. Tick with a `check` whose action is an `update` of that collection.
- Order a stored list with `"sort": [{ "by": "visited" }, { "by": "order" }]`: `false`
  comes before `true`. `order` lists values to put first, always as text
  (`["High", "Low"]`, `["false", "true"]`); `"descending": true` turns it round.

### Links and requests

- In a URL, Appkin percent-encodes what each `${...}` inserts: write `?q=${q}`, never
  encode a placeholder yourself. Fixed text is sent as written, so encode it
  (`type=Arms%20and%20Armor`). A query parameter that is only a placeholder
  (`&type=${type}`) is left out when the value is empty.
- Never invent or guess an address (images, `rest` URLs, web pages) or its parts (folders,
  file names, IDs, sizes): it almost never loads, or shows the wrong thing. Use addresses
  the user gives, fields of a REST response, or ones you are certain of; otherwise leave
  them out.
- For a request that can fail in a way the user should understand, add `onError` (its
  data has `error.message`).
- For a button that opens a page, a map, the dialer or email, use an `open` event.

### Structure

- An inline action needs both `id` and `event`; to reuse one, define it in the root
  `actions` and reference it with `{ "ref": "id" }`.
- A screen opened with `show` already has Back: don't add Back buttons or alerts that
  explain it. Its `title` names the screen.
- JSON has no comments and no trailing commas.

## Validate generated output

When repository tools are available, run:

```sh
./gradlew :app:validateConfig -Pconfig=path/to/generated.json
```

Treat every reported path as a required correction. Do not suppress validation errors
by deleting useful definitions or weakening references; repair the configuration's
structure and data flow.

In the app, the editor's live check shows the same problems; **Send problems to your AI
app…** shares them back to the model as a message.

## Recommended prompt

The app has no AI inside. Its **Make an app with your AI app…** command shares a complete,
self-contained prompt to an AI app the user picks: these rules, a reference of every view,
field and event generated from the schema, and worked examples. The user shares the
answer back to the app.
The same context is in [AI_CONTEXT.md](AI_CONTEXT.md) for use outside the app; attach it
to a prompt like this one:

```text
Create an Appkin v3 configuration for the following application.

Requirements:
<describe screens, data, endpoints, and interactions>

Follow the attached AI_CONTEXT.md. Use schemaVersion 3 and explicit
{ "ref": "..." } references. Return only valid JSON, without Markdown or commentary.
```
