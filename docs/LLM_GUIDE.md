# Generating Appkin configurations with an LLM

This file is the generation contract for Appkin v3. For individual field
definitions, read the [v3 configuration reference](CONFIGURATION_V3.md) and the
[canonical JSON Schema](../schema/v3.schema.json).

## Output contract

When the user asks for a configuration file, return one valid JSON object with no
comments, Markdown fences, or surrounding prose.

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

1. Use only properties defined by `schema/v3.schema.json`. The schema rejects unknown
   properties.
2. Use lowercase enum strings exactly as documented.
3. Make `mainView` an inline layout unless it intentionally references a reusable root
   view.
4. Put reusable screen definitions in root `views` and reusable behavior in root
   `actions`.
5. Use `{ "ref": "id" }` for every view or action reference. Do not generate the
   legacy ID-only reference form.
6. Give every definition a unique, non-empty ID when it will be referenced.
7. Give every action definition a complete `event`.
8. Trace the data available at every node. List templates receive one item object,
   REST continuations receive the response object, and forms produce their submitted
   object.
9. Use `mapping` for a single value or array path. Use `${path}` when composing a
   string or URL from one or more values.
10. For every list, provide `itemView` and a `mapping` selecting an array of objects.
11. If a list has `initData`, ensure its endpoint returns a JSON object containing the
    mapped array.
12. Use HTTPS endpoints. Never write a real API key,
    token, password, or other secret into the JSON. When an API needs a key in its URL
    (a query parameter or a path segment) or in a header, write a named placeholder
    instead: two underscores, an uppercase name, two underscores, for example
    `https://api.example.com/items?api_key=__EXAMPLE_API_KEY__` or
    `"headers": { "Authorization": "Bearer __EXAMPLE_TOKEN__" }`. Appkin asks the user
    for each placeholder's value when the app opens, stores it encrypted on the device,
    and fills it into every request. Use one name per key and reuse it wherever it's
    needed. For a request that can fail in a way the user should understand, add
    `onError` (its data has `error.message`).
13. Make every mapping and placeholder match the real data exactly, including case and
    nesting.
14. Prefer a small working configuration over speculative or undocumented fields.
15. Before returning the result, validate JSON syntax and confirm every reference
    resolves to exactly one definition.

## Data-flow checklist

Before writing JSON, identify:

- Initial static data, if any
- The exact shape of every REST response
- Which object is active when each action runs
- Which array each list maps
- Which payload each form creates
- Which response or item object each destination view receives

Then construct the configuration in this order:

1. Reusable destination views
2. Reusable actions and event chains
3. Main view
4. References
5. Final syntax and schema validation

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
- `save` (saves the current data, such as a form's values, in a collection on the phone; a list with `stored` shows it, filtered with `where` and ordered with `sort`)
- `update` (changes a saved object, by default the one with the current data's `id`)
- `delete` (removes a saved object, or all that match `where`; put it behind an alert to ask first)
- `open` (hands a `https:`, `http:`, `geo:`, `tel:` or `mailto:` address to the browser,
  maps, the dialer or email; nothing else opens)
- `share` (opens the share sheet on a text)

Other enums:

- Orientations: `vertical`, `horizontal` (and `grid`, for lists only)
- Actions: `click`, `longclick`
- HTTP methods: `get`, `post`, `put`, `delete`
- Text styles: `regular`, `italic`, `bold`
- Text inputs: `plain`, `numeric`, `email`, `password`
- Form elements: `text`, `selector`, `date`, `time`, `checkbox`, `hidden`

Do not invent support for PATCH, JavaScript expressions, conditions other than
`visibleIf`, databases beyond `save`, `update`, `delete` and `stored`, arbitrary native
components, or undocumented style properties.

## Common mistakes

- A button shows its `title` (or the value its `mapping` selects). A button's `label` is
  not displayed.
- A text view shows its `label` as a heading, then the `mapping` value or `title` under it as
  a quieter line. Use `textStyle` and `textSize` for emphasis, and `labelStyle` to style the
  label on its own (`"labelStyle": "bold"` over regular text). When a value holds HTML,
  such as a museum's description, add `"format": "html"`. An image's `"scale": "fit"` shows
  the whole picture instead of cropping it; a `show` event's `title` names the screen it
  opens. A web address written out in
  a text view (`https://...`) becomes a link that opens in the browser. For a button that
  opens a page, a map, the dialer or email, use an `open` event.
- A list needs `mapping` (the path to an array of objects) and `itemView`. Its data comes
  from `data` on the list or a parent, or from `initData` for a REST endpoint.
  For an API with pages (a `skip`, `offset` or `page` parameter, or a "next" link), add
  `"nextPage": { "parameter": "skip", "step": 40 }` (the page size) or
  `"nextPage": { "urlFrom": "info.next" }` to `initData`, and the list loads more as it's
  scrolled. `"orientation": "grid"` with `"columns": 2` shows items side by side;
  `emptyText` says what an empty list means.
- Inside `itemView` and in views opened by a list item, mappings and `${...}` refer to the
  fields of that one item, not to the whole response.
- Paths reach array items by index: `meals.0.strMeal`. When a REST response wraps one
  object in an array, open its view with a `show` event whose `mapping` selects it, such
  as `"mapping": "meals.0"`.
- Data from an API is often incomplete. A text view whose `mapping` (or `title`) has nothing
  to show isn't drawn, label included, and a missing `${...}` becomes empty text. To hide any
  other view, such as a button that needs a field, give it `"visibleIf": "field"` (shown only
  when the field has a value) or `"visibleIf": "!field"` (only when it hasn't). It is a path,
  never a comparison.
- In a URL, Appkin percent-encodes what each `${...}` inserts: write `?q=${q}`, never encode
  a placeholder yourself. Fixed text in a URL is sent as written, so encode it
  (`type=Arms%20and%20Armor`).
- `mapping` is a plain path such as `owner.name`; `${owner.name}` is only for building a
  string, such as a URL or a label. A placeholder always starts with `$`: `${title}`, never
  `{title}`.
- An inline action needs both `id` and `event`. To reuse one, define it in the root
  `actions` and reference it with `{ "ref": "id" }`.
- Form fields are `text`, `selector`, `date`, `time`, `checkbox` and `hidden`; there is no
  `input`, `dropdown` or `switch`. Dates are `dd/MM/yyyy`, times `HH:mm`. A selector option
  is a string or `{ "label": "Any type", "value": "" }`. To send something from the screen
  with a form (an item's id), use a `hidden` field: `{ "type": "hidden", "mapping": "id",
  "value": "${id}" }`.
- A URL query parameter that is just a placeholder (`&type=${type}`) is left out when the
  value is empty or missing, so optional filters need no extra work.
- Image and text sizes are numbers (dp and sp), not strings: `"size": { "height": 200 }`.
- Give every app an `icon`: one emoji that fits what it is about, such as `"🏍️"` for a
  motorcycle guide or `"📚"` for a book club. It is the app's icon on the home page and the
  phone's home screen.
- Don't invent image addresses or IDs (such as cover IDs): a made-up one shows a different
  picture or none. Use addresses the user gives, fields of a REST response, or images you
  are certain of; otherwise leave the image out and say so.
- The same goes for every link (images, `rest` URLs, web pages): write an address only in a
  form you know works, exactly as it is published. Never build one by guessing its parts
  (folders, file names, IDs, hashes, sizes): a guessed address almost never loads.
- A screen opened with a `show` event already has the system Back gesture and button:
  don't add "Back" buttons or alerts that only explain how to go back.
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
