# Appkin: context for AI assistants

Appkin is an Android app that turns a JSON configuration into a native app. You write the configuration; the app validates it against the v3 schema and runs it. This document is the complete contract: use only what it describes.

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
- Text inputs: `plain`, `numeric`, `email`, `password`
- Form elements: `text`, `selector`, `date`, `time`, `checkbox`, `hidden`, `photos` (pick photos from the gallery, kept on the phone)

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

## Reference

Generated from the v3 JSON Schema. Unknown properties are rejected. **Required** properties are marked.

### Root object

Machine-readable contract for an Appkin v3 application. schemaVersion is optional for backward compatibility; new configurations should set it to 3.

- `schemaVersion`: always `3`. Configuration-language version. Optional only for compatibility with configurations created before schema versioning was introduced
- `name` **(required)**: text (not empty)
- `author`: text
- `debugMode`: `true` or `false`
- `version` **(required)**: text (not empty)
- `icon`: text. One emoji for the app's icon on the home page and the phone's home screen, such as "🏍️". Without it, the icon is the name's first letter
- `changelog`: text
- `mainView` **(required)**: a view (or `{ "ref": "id" }`)
- `views`: an array; each item is a view. Reusable view definitions. References are not valid in this registry
- `actions`: an array; each item is an action definition. Reusable action definitions. References are not valid in this registry

### Views

Every view has a `type`. Where a view is expected, `{ "ref": "id" }` may reference a view defined in the root `views` instead.

#### `layout`

- `id`: text (not empty)
- `action`: an action (or `{ "ref": "id" }`)
- `mapping`: text
- `data`: an object with any data
- `visibleIf`: text
- `orientation`: one of `"horizontal"`, `"vertical"`. How the child views are arranged; vertical by default
- `views` **(required)**: an array; each item is a view (or `{ "ref": "id" }`). The child views, in order

#### `text`

- `id`: text (not empty)
- `action`: an action (or `{ "ref": "id" }`)
- `mapping`: text. Path of a value in the current data to show as the text, such as owner.name
- `data`: an object with any data
- `visibleIf`: text
- `textSize`: a number (at least 0). Font size in sp
- `size`: a size, `{ "width": number, "height": number }` in dp
- `textStyle`: one of `"regular"`, `"italic"`, `"bold"`
- `title`: text. The text, when no mapping value is shown
- `label`: text. First line of the text view, shown above the text
- `labelStyle`: one of `"regular"`, `"italic"`, `"bold"`. The label's own style, so a bold heading needn't make its text bold; without it the label looks like the text
- `format`: one of `"plain"`, `"html"`. plain (default), or html: simple HTML in the text (<b>, <i>, <em>, <strong>, <u>, <br>, <p>, <a href>), such as a museum's descriptions
- `showWhenEmpty`: `true` or `false`. Show the label even when the mapping or title has nothing to show. By default such a text view isn't drawn at all

#### `button`

- `id`: text (not empty)
- `action`: an action (or `{ "ref": "id" }`). What a tap does
- `mapping`: text. Path of a value in the current data to use as the button's text
- `data`: an object with any data
- `visibleIf`: text
- `textSize`: a number (at least 0)
- `size`: a size, `{ "width": number, "height": number }` in dp
- `textStyle`: one of `"regular"`, `"italic"`, `"bold"`
- `title`: text. The button's text
- `label`: text. Not displayed on buttons: use title. On a button with only an icon, what screen readers say, such as "Delete ${title}"
- `icon`: one of `"add"`, `"check"`, `"close"`, `"copy"`, `"delete"`, `"edit"`, `"email"`, `"favorite"`, `"info"`, `"phone"`, `"place"`, `"refresh"`, `"search"`, `"settings"`, `"share"`, `"star"`. An icon from the phone's standard set, before the title; without a title the button is just the icon, in its own small size

#### `image`

- `id`: text (not empty)
- `action`: an action (or `{ "ref": "id" }`)
- `mapping`: text. Path of an image address in the current data
- `data`: an object with any data
- `visibleIf`: text
- `url`: text. Image address; may use ${field} placeholders
- `size`: a size, `{ "width": number, "height": number }` in dp
- `scale`: one of `"crop"`, `"fit"`. crop (default): the picture fills its space and its edges may be cut; fit: the whole picture shows, inside its space

#### `list`

- `id`: text (not empty)
- `action`: an action (or `{ "ref": "id" }`). What tapping an item does; its event receives that item
- `swipeToStart`: an action (or `{ "ref": "id" }`). What swiping an item towards the start (left) does; its event receives that item, such as an alert asking before a delete. The item slides back unless the action removes it
- `swipeToStartIcon`: one of `"add"`, `"check"`, `"close"`, `"copy"`, `"delete"`, `"edit"`, `"email"`, `"favorite"`, `"info"`, `"phone"`, `"place"`, `"refresh"`, `"search"`, `"settings"`, `"share"`, `"star"`. The icon under an item swiped towards the start; delete by default
- `swipeToEnd`: an action (or `{ "ref": "id" }`). What swiping an item towards the end (right) does; its event receives that item, such as an update marking it done
- `swipeToEndIcon`: one of `"add"`, `"check"`, `"close"`, `"copy"`, `"delete"`, `"edit"`, `"email"`, `"favorite"`, `"info"`, `"phone"`, `"place"`, `"refresh"`, `"search"`, `"settings"`, `"share"`, `"star"`. The icon under an item swiped towards the end; check by default
- `mapping`: text. Path of the array of objects to show, in data or in the initData response
- `data`: an object with any data
- `visibleIf`: text
- `itemView` **(required)**: a view (or `{ "ref": "id" }`). The view repeated for each item; its mappings and placeholders refer to that item
- `initData`: a REST request, `{ "url": ..., "method": "get" | "post" }`. A REST request whose JSON response is the list's data
- `stored`: an object. Shows objects the app saved on the phone with a save event, instead of initData or data; mapping is not used
- `orientation`: one of `"horizontal"`, `"vertical"`, `"grid"`. vertical (default): one item per row; horizontal: a row that scrolls sideways; grid: items side by side in rows of columns
- `columns`: a whole number (at least 1). Items per row of a grid; 2 by default
- `emptyText`: text. What the list says when it has no items, instead of No data available; may use ${field} placeholders

#### `form`

- `id`: text (not empty)
- `action`: an action (or `{ "ref": "id" }`). Runs on submit, with the submitted object as data
- `mapping`: text
- `data`: an object with any data
- `visibleIf`: text
- `title`: text. Heading above the fields
- `buttonTitle`: text. The submit button's text; Submit by default
- `formElements` **(required)**: an array; each item is a form field. The fields; each one puts its value under its mapping in the submitted object

#### `check`

A tick box, ticked when the data's true/false value at mapping is true. Ticking the box runs its action at once with that value turned over, so an update event saves it; a ticked title is struck through. Only the box ticks: a tap on the title reaches the list item.

- `id`: text (not empty)
- `action`: an action (or `{ "ref": "id" }`). Runs when the box is ticked or unticked, with the data where the value at mapping says so; usually an update event
- `mapping` **(required)**: text (not empty). The field holding true or false, such as "done"
- `data`: an object with any data
- `visibleIf`: text
- `title`: text. The text beside the box; may use ${field} placeholders

### Form fields

The items of a form's `formElements`.

#### `text`

- `mapping` **(required)**: text (not empty). Key of the submitted object that receives the value
- `title` **(required)**: text
- `description`: text
- `mandatory`: `true` or `false`. Must be filled in before submitting
- `placeholder`: text
- `value`: text
- `textType`: one of `"plain"`, `"numeric"`, `"email"`, `"password"`

#### `selector`

- `mapping` **(required)**: text (not empty)
- `title` **(required)**: text
- `description`: text
- `mandatory`: `true` or `false`
- `options` **(required)**: an array; each item is text. The choices: each is a string, shown and submitted, or { "label": shown, "value": submitted }, such as { "label": "Any type", "value": "" }
- `selection`: text. The value chosen at first

#### `date`

- `mapping` **(required)**: text (not empty)
- `title` **(required)**: text
- `description`: text
- `mandatory`: `true` or `false`
- `min`: a date, `dd/MM/yyyy`
- `max`: a date, `dd/MM/yyyy`
- `value`: a date, `dd/MM/yyyy`

#### `checkbox`

- `mapping` **(required)**: text (not empty). Key of the submitted object that receives true or false
- `title` **(required)**: text
- `description`: text
- `mandatory`: `true` or `false`. Must be checked before submitting
- `value`: `true` or `false`

#### `time`

A time of day, picked on a clock and submitted as HH:mm, as a calendar event's time takes it.

- `mapping` **(required)**: text (not empty)
- `title` **(required)**: text
- `description`: text
- `mandatory`: `true` or `false`
- `value`: text. The time at first, HH:mm

#### `hidden`

A value the form sends without showing it, such as the id of the item on screen.

- `mapping` **(required)**: text (not empty). Key of the submitted object that receives the value
- `value`: text. The value, sent as text; may use ${field} placeholders filled in from the screen's data. Without it, the data's value at mapping is sent

#### `photos`

Photos picked from the phone's gallery, kept on the phone with the app. It submits a list of photo references; an image view whose mapping is that list shows them in a row.

- `mapping` **(required)**: text (not empty). Where the list of photos goes, such as "photos"
- `title` **(required)**: text (not empty)
- `description`: text
- `mandatory`: `true` or `false`. At least one photo is needed
- `max`: a whole number (at least 1). How many photos it takes; 10 by default

### Events

What an action does: the `event` of an action definition.

#### `rest`

- `url` **(required)**: text (not empty). HTTPS address; may use ${field} placeholders from the current data
- `method`: one of `"get"`, `"post"`, `"put"`, `"delete"`. get (default), post, put or delete; post and put send the current data as a JSON body
- `headers`: an object
- `thenDo`: an event. The event to run next, with the JSON response as data
- `onError`: an event. The event to run when the request fails, instead of an alert; its data is the request's, plus error.message and, for an HTTP error, error.status
- `mapping`: text
- `data`: an object with any data

#### `show`

- `view` **(required)**: a view (or `{ "ref": "id" }`). The view to open; it receives the current data
- `mapping`: text. Path of the part of the current data the view receives
- `data`: an object with any data
- `title`: text. The opened screen's title in the app bar, instead of the app's name; may use ${field} placeholders from the data it receives

#### `alert`

- `value`: text. The message; may use ${field} placeholders
- `thenDoOK`: an event. The event to run when OK is tapped
- `thenDoKO`: an event. The event to run when the alert is cancelled
- `mapping`: text
- `data`: an object with any data

#### `calendar`

Opens the phone's calendar app on a new event, filled in; the user reviews and saves it there.

- `title` **(required)**: text (not empty). The event's title; may use ${field} placeholders
- `date` **(required)**: text (not empty). The day, dd/MM/yyyy; may be a placeholder such as ${date} from a form's date field
- `time`: text. The start time, HH:mm; without it the event lasts all day
- `durationMinutes`: a whole number (at least 1). How long a timed event lasts; 60 by default
- `location`: text. Where it happens; may use ${field} placeholders
- `description`: text. Notes for the event; may use ${field} placeholders
- `mapping`: text
- `data`: an object with any data

#### `open`

Hands an address to another app on the phone: https or http opens the browser, geo: a map, tel: the dialer (nothing is called) and mailto: an email app. No other kinds of address are opened.

- `url` **(required)**: text. An https:, http:, geo:, tel: or mailto: address, or a placeholder holding one, such as ${url}; values placeholders insert are percent-encoded
- `mapping`: text. Path of the part of the current data the address is filled in from
- `data`: an object with any data

#### `share`

Opens the phone's share sheet on a text, for the user to send to any app.

- `text` **(required)**: text (not empty). What is shared; may use ${field} placeholders, such as "${title} ${url}"
- `mapping`: text. Path of the part of the current data the text is filled in from
- `data`: an object with any data

#### `save`

Saves the current data (a form's submitted values) on the phone as a new object of a collection; a list with stored shows the collection. Each object gets an id and createdAt (epoch milliseconds). A form saved this way starts empty again.

- `collection` **(required)**: text (not empty). The collection to add to, such as "ideas"; each app has its own
- `fields`: an object. Values to add to the object; text may use ${field} placeholders from the data the form showed (the screen's, or a list row's), then from the submitted values, such as { "appId": "${id}" }
- `unique`: text. A field that tells saved objects apart, such as "key": saving an object with the same value there replaces the one already saved (keeping its id) instead of adding another
- `thenDo`: an event. The event to run next, with the saved object as data
- `mapping`: text. Path of the part of the current data to save
- `data`: an object with any data

#### `update`

Changes a saved object: the current data (a form's submitted values, or a ticked check's item) and fields replace its values. Its id and createdAt never change.

- `collection` **(required)**: text (not empty). The collection the object is in
- `id`: text (not empty). The id of the object to change; "${id}" (the current data's id) by default
- `fields`: an object. Values to set too; text may use ${field} placeholders, filled as in a save event
- `thenDo`: an event. The event to run next, with the changed object as data
- `mapping`: text. Path of the part of the current data to save
- `data`: an object with any data

#### `delete`

Removes saved objects: the one whose id is id (the current data's by default), or every one where matches. Put it behind an alert's thenDoOK to ask first.

- `collection` **(required)**: text (not empty). The collection to remove from
- `id`: text (not empty). The id of the object to remove; "${id}" (the current data's id) by default
- `where`: an object. Instead of id: removes every object whose fields have these values, with ${field} placeholders from the current data, such as { "appId": "${id}" }
- `thenDo`: an event. The event to run next, with the current data
- `mapping`: text. Path of the part of the current data the id or where is filled in from
- `data`: an object with any data

#### `back`

Closes the current screen and goes back to the one that opened it, as the phone's Back does; such as after saving an edit.

- `mapping`: text
- `data`: an object with any data

#### `copy`

Copies a text to the clipboard, to paste anywhere (a chat, a note); the phone confirms it.

- `text` **(required)**: text (not empty). What is copied; may use ${field} placeholders, and a line whose placeholders all have no value is left out, such as "${title}\n${description}"
- `mapping`: text. Path of the part of the current data the text is filled in from
- `data`: an object with any data

### Actions and references

Where an action is expected, write an action definition or `{ "ref": "id" }` referencing one in the root `actions`.

#### Action definition

- `id` **(required)**: text (not empty)
- `data`: an object with any data
- `type`: one of `"click"`, `"longclick"`
- `event` **(required)**: an event

#### REST data for a list (`initData`)

- `url` **(required)**: text (not empty)
- `method`: one of `"get"`, `"post"`, `"put"`, `"delete"`. get (default), post, put or delete; post and put send the current data as a JSON body
- `headers`: an object
- `thenDo`: an event
- `mapping`: text
- `data`: an object with any data
- `nextPage`: a value. How the list asks for more items when it nears its end. Either parameter and step: that query parameter (such as skip) grows by step, the page size, until a page has fewer items. Or urlFrom: the path in the response of the next page's URL, until there is none

#### Saved data for a list (`stored`)

The objects a save event put in a collection, oldest first; it shows them again when that collection changes.

- `collection` **(required)**: text (not empty). The collection's name, as in the save event
- `where`: an object. Only the objects whose fields have these values; values may use ${field} placeholders from the list's data, such as { "appId": "${id}" }
- `sort`: an object. The order: one sort, or a list where each later sort orders what the earlier ones leave tied (such as done last, then by priority)

#### Size (`size`)

- `width`: a whole number (at least 0)
- `height`: a whole number (at least 0)

## Example 1: a static list with a detail screen

The list's items are in `data`; tapping one shows the reusable `artwork` view with that item.

```json
{
  "schemaVersion": 3,
  "name": "Masterpieces",
  "icon": "🖼️",
  "version": "1.0.0",
  "author": "Appkin",
  "changelog": "Public-domain paintings; images from Wikimedia Commons.",
  "mainView": {
    "type": "layout",
    "data": {
      "artworks": [
        {
          "title": "The Great Wave off Kanagawa",
          "artist": "Katsushika Hokusai",
          "year": "c. 1831",
          "medium": "Woodblock print",
          "where": "Japan",
          "thumb": "https://commons.wikimedia.org/wiki/Special:FilePath/Tsunami_by_hokusai_19th_century.jpg?width=330",
          "image": "https://commons.wikimedia.org/wiki/Special:FilePath/Tsunami_by_hokusai_19th_century.jpg?width=960",
          "about": "A giant wave curls over three boats while Mount Fuji sits small in the distance."
        },
        {
          "title": "Girl with a Pearl Earring",
          "artist": "Johannes Vermeer",
          "year": "c. 1665",
          "medium": "Oil on canvas",
          "where": "Mauritshuis, The Hague",
          "thumb": "https://commons.wikimedia.org/wiki/Special:FilePath/1665_Girl_with_a_Pearl_Earring.jpg?width=330",
          "image": "https://commons.wikimedia.org/wiki/Special:FilePath/1665_Girl_with_a_Pearl_Earring.jpg?width=960",
          "about": "A girl in a blue and yellow turban turns toward us, a large pearl catching the light."
        },
        {
          "title": "Paris Street; Rainy Day",
          "artist": "Gustave Caillebotte",
          "year": "1877",
          "medium": "Oil on canvas",
          "where": "Art Institute of Chicago",
          "thumb": "https://commons.wikimedia.org/wiki/Special:FilePath/Gustave_Caillebotte_-_Paris_Street;_Rainy_Day_-_Google_Art_Project.jpg?width=330",
          "image": "https://commons.wikimedia.org/wiki/Special:FilePath/Gustave_Caillebotte_-_Paris_Street;_Rainy_Day_-_Google_Art_Project.jpg?width=960",
          "about": "Umbrellas, wet cobblestones and new boulevards: Paris on a grey afternoon."
        }
      ]
    },
    "views": [
      {
        "type": "text",
        "label": "Masterpieces",
        "title": "Eight paintings everyone knows. Tap one to see it up close.",
        "textStyle": "bold",
        "textSize": 20
      },
      {
        "type": "list",
        "mapping": "artworks",
        "action": {
          "ref": "open_artwork"
        },
        "itemView": {
          "type": "layout",
          "orientation": "horizontal",
          "views": [
            {
              "type": "image",
              "url": "${thumb}",
              "size": {
                "width": 112,
                "height": 112
              }
            },
            {
              "type": "text",
              "label": "${title}",
              "title": "${artist}\n${year}"
            }
          ]
        }
      },
      {
        "type": "text",
        "title": "Public-domain works · images from Wikimedia Commons",
        "textStyle": "italic"
      }
    ]
  },
  "views": [
    {
      "id": "artwork",
      "type": "layout",
      "views": [
        {
          "type": "image",
          "url": "${image}",
          "size": {
            "height": 340
          }
        },
        {
          "type": "text",
          "label": "${title}",
          "title": "${artist}, ${year}",
          "textStyle": "bold",
          "textSize": 22
        },
        {
          "type": "text",
          "label": "About",
          "mapping": "about"
        },
        {
          "type": "layout",
          "orientation": "horizontal",
          "views": [
            {
              "type": "text",
              "label": "Medium",
              "mapping": "medium"
            },
            {
              "type": "text",
              "label": "Where to see it",
              "mapping": "where"
            }
          ]
        }
      ]
    }
  ],
  "actions": [
    {
      "id": "open_artwork",
      "type": "click",
      "event": {
        "type": "show",
        "view": {
          "ref": "artwork"
        }
      }
    }
  ]
}
```

## Example 2: a form with required fields and an alert

Submitting runs the form's action with the filled-in values, so the alert can use `${name}`.

```json
{
  "schemaVersion": 3,
  "name": "Summer Run 2026",
  "icon": "🏃",
  "version": "1.0.0",
  "author": "Appkin",
  "mainView": {
    "type": "layout",
    "views": [
      {
        "type": "text",
        "label": "Summer Run · 10 km",
        "title": "Saturday 13 June, 8:00 · Villa Borghese, Rome",
        "textStyle": "bold",
        "textSize": 20
      },
      {
        "type": "form",
        "buttonTitle": "Sign me up",
        "action": {
          "ref": "confirm"
        },
        "formElements": [
          {
            "type": "text",
            "mapping": "name",
            "title": "Full name",
            "placeholder": "Your name",
            "mandatory": true
          },
          {
            "type": "text",
            "mapping": "email",
            "title": "Email",
            "placeholder": "you@example.com",
            "textType": "email",
            "mandatory": true,
            "description": "We send your race number here"
          },
          {
            "type": "selector",
            "mapping": "distance",
            "title": "Distance",
            "options": [
              "5 km",
              "10 km",
              "Half marathon"
            ],
            "selection": "10 km"
          },
          {
            "type": "selector",
            "mapping": "shirt",
            "title": "T-shirt size",
            "options": [
              "S",
              "M",
              "L",
              "XL"
            ],
            "selection": "M"
          },
          {
            "type": "date",
            "mapping": "birth",
            "title": "Date of birth",
            "description": "Runners must be 16 or older"
          },
          {
            "type": "checkbox",
            "mapping": "rules",
            "title": "I accept the race rules",
            "mandatory": true
          }
        ]
      }
    ]
  },
  "actions": [
    {
      "id": "confirm",
      "type": "click",
      "event": {
        "type": "alert",
        "value": "See you at the start line, ${name}!"
      }
    }
  ]
}
```

## Example 3: a list loaded from a REST API, with a detail screen

`initData` loads the JSON; `mapping` selects its array; each item's fields fill the item view and the detail.

```json
{
  "schemaVersion": 3,
  "name": "PUBLIC DOMAIN ART",
  "version": "1.0",
  "author": "Appkin",
  "changelog": "Public-domain paintings from the Cleveland Museum of Art. Data and images from its Open Access API (CC0).",
  "mainView": {
    "id": "art_home",
    "type": "layout",
    "views": [
      {
        "type": "text",
        "label": "Public domain art",
        "title": "Paintings from the Cleveland Museum of Art. Tap one to see it larger.",
        "textStyle": "bold",
        "textSize": 18
      },
      {
        "type": "list",
        "mapping": "data",
        "initData": {
          "url": "https://openaccess-api.clevelandart.org/api/artworks/?cc0=1&has_image=1&type=Painting&limit=30&fields=id,title,creation_date,technique,creators,images",
          "method": "get"
        },
        "action": {
          "id": "show_artwork_detail"
        },
        "itemView": {
          "id": "artwork_list_item",
          "type": "layout",
          "orientation": "horizontal",
          "views": [
            {
              "type": "image",
              "url": "${images.web.url}",
              "size": {
                "width": 112,
                "height": 112
              }
            },
            {
              "type": "text",
              "label": "${title}",
              "title": "${creation_date}"
            }
          ]
        }
      },
      {
        "type": "text",
        "title": "Data and images: Cleveland Museum of Art Open Access (CC0)",
        "textStyle": "italic"
      }
    ]
  },
  "views": [
    {
      "id": "artwork_detail",
      "type": "layout",
      "views": [
        {
          "type": "image",
          "url": "${images.web.url}",
          "size": {
            "height": 300
          }
        },
        {
          "type": "text",
          "label": "${title}",
          "title": "${creation_date}",
          "textStyle": "bold",
          "textSize": 24
        },
        {
          "type": "text",
          "label": "Artist",
          "mapping": "creators.0.description"
        },
        {
          "type": "text",
          "label": "Technique",
          "mapping": "technique"
        }
      ]
    }
  ],
  "actions": [
    {
      "id": "show_artwork_detail",
      "type": "click",
      "event": {
        "type": "show",
        "view": {
          "id": "artwork_detail"
        }
      }
    }
  ]
}
```

## Your task

Create an Appkin v3 configuration for this app: <describe the app: its screens, data and actions>

If you can create files, give me the complete configuration as a downloadable file named app.json, containing only the JSON. If you can't, reply with only the complete JSON, starting with { and ending with }: no comments or explanations. I will open your file or share your answer in the Appkin app, which runs it; if it reports problems, I will send them to you to fix.
