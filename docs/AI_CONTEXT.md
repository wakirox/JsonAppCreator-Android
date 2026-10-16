# JSON App Creator: context for AI assistants

JSON App Creator is an Android app that turns a JSON configuration into a native app. You write the configuration; the app validates it against the v3 schema and runs it. This document is the complete contract: use only what it describes.

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

The optional root fields are `author`, `changelog`, `debugMode`, `views`, and
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
12. Use HTTPS endpoints that require no custom headers. Never embed private API keys,
    tokens, passwords, or other secrets.
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

Events:

- `show`
- `rest`
- `alert`

Other enums:

- Orientations: `vertical`, `horizontal`
- Actions: `click`, `longclick`
- HTTP methods: `get`, `post`
- Text styles: `regular`, `italic`, `bold`
- Text inputs: `plain`, `numeric`, `email`, `password`
- Form elements: `text`, `selector`, `date`, `checkbox`

Do not invent support for custom headers, DELETE/PUT/PATCH, JavaScript expressions,
conditional rendering, databases, arbitrary native components, or undocumented style
properties.

## Common mistakes

- A button shows its `title` (or the value its `mapping` selects). A button's `label` is
  not displayed.
- A text view shows its `label` as a first line, then the `mapping` value or `title`, in
  one style. Use `textStyle` and `textSize` for emphasis.
- A list needs `mapping` (the path to an array of objects) and `itemView`. Its data comes
  from `data` on the list or a parent, or from `initData` for a REST endpoint.
- Inside `itemView` and in views opened by a list item, mappings and `${...}` refer to the
  fields of that one item, not to the whole response.
- Paths reach array items by index: `meals.0.strMeal`. When a REST response wraps one
  object in an array, open its view with a `show` event whose `mapping` selects it, such
  as `"mapping": "meals.0"`.
- `mapping` is a plain path such as `owner.name`; `${owner.name}` is only for building a
  string, such as a URL or a label. A placeholder always starts with `$`: `${title}`, never
  `{title}`.
- An inline action needs both `id` and `event`. To reuse one, define it in the root
  `actions` and reference it with `{ "ref": "id" }`.
- Form fields are `text`, `selector`, `date` and `checkbox`; there is no `input`,
  `dropdown` or `switch`. Dates are `dd/MM/yyyy`.
- Image and text sizes are numbers (dp and sp), not strings: `"size": { "height": 200 }`.
- Don't invent image addresses or IDs (such as cover IDs): a made-up one shows a different
  picture or none. Use addresses the user gives, fields of a REST response, or images you
  are certain of; otherwise leave the image out and say so.
- A screen opened with a `show` event already has the system Back gesture and button:
  don't add "Back" buttons or alerts that only explain how to go back.
- JSON has no comments and no trailing commas.

## Reference

Generated from the v3 JSON Schema. Unknown properties are rejected. **Required** properties are marked.

### Root object

Machine-readable contract for a JSON App Creator v3 application. schemaVersion is optional for backward compatibility; new configurations should set it to 3.

- `schemaVersion`: always `3`. Configuration-language version. Optional only for compatibility with configurations created before schema versioning was introduced
- `name` **(required)**: text (not empty)
- `author`: text
- `debugMode`: `true` or `false`
- `version` **(required)**: text (not empty)
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
- `orientation`: one of `"horizontal"`, `"vertical"`. How the child views are arranged; vertical by default
- `views` **(required)**: an array; each item is a view (or `{ "ref": "id" }`). The child views, in order

#### `text`

- `id`: text (not empty)
- `action`: an action (or `{ "ref": "id" }`)
- `mapping`: text. Path of a value in the current data to show as the text, such as owner.name
- `data`: an object with any data
- `textSize`: a number (at least 0). Font size in sp
- `size`: a size, `{ "width": number, "height": number }` in dp
- `textStyle`: one of `"regular"`, `"italic"`, `"bold"`
- `title`: text. The text, when no mapping value is shown
- `label`: text. First line of the text view, shown above the text

#### `button`

- `id`: text (not empty)
- `action`: an action (or `{ "ref": "id" }`). What a tap does
- `mapping`: text. Path of a value in the current data to use as the button's text
- `data`: an object with any data
- `textSize`: a number (at least 0)
- `size`: a size, `{ "width": number, "height": number }` in dp
- `textStyle`: one of `"regular"`, `"italic"`, `"bold"`
- `title`: text. The button's text
- `label`: text. Not displayed on buttons: use title

#### `image`

- `id`: text (not empty)
- `action`: an action (or `{ "ref": "id" }`)
- `mapping`: text. Path of an image address in the current data
- `data`: an object with any data
- `url`: text. Image address; may use ${field} placeholders
- `size`: a size, `{ "width": number, "height": number }` in dp

#### `list`

- `id`: text (not empty)
- `action`: an action (or `{ "ref": "id" }`). What tapping an item does; its event receives that item
- `mapping`: text. Path of the array of objects to show, in data or in the initData response
- `data`: an object with any data
- `itemView` **(required)**: a view (or `{ "ref": "id" }`). The view repeated for each item; its mappings and placeholders refer to that item
- `initData`: a REST request, `{ "url": ..., "method": "get" | "post" }`. A REST request whose JSON response is the list's data
- `orientation`: one of `"horizontal"`, `"vertical"`

#### `form`

- `id`: text (not empty)
- `action`: an action (or `{ "ref": "id" }`). Runs on submit, with the submitted object as data
- `mapping`: text
- `data`: an object with any data
- `title`: text. Heading above the fields
- `buttonTitle`: text. The submit button's text; Submit by default
- `formElements` **(required)**: an array; each item is a form field. The fields; each one puts its value under its mapping in the submitted object

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
- `options` **(required)**: an array; each item is text
- `selection`: text

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

### Events

What an action does: the `event` of an action definition.

#### `rest`

- `url` **(required)**: text (not empty). HTTPS address; may use ${field} placeholders from the current data
- `method`: one of `"get"`, `"post"`
- `thenDo`: an event. The event to run next, with the JSON response as data
- `mapping`: text
- `data`: an object with any data

#### `show`

- `view` **(required)**: a view (or `{ "ref": "id" }`). The view to open; it receives the current data
- `mapping`: text. Path of the part of the current data the view receives
- `data`: an object with any data

#### `alert`

- `value`: text. The message; may use ${field} placeholders
- `thenDoOK`: an event. The event to run when OK is tapped
- `thenDoKO`: an event. The event to run when the alert is cancelled
- `mapping`: text
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
- `method`: one of `"get"`, `"post"`
- `thenDo`: an event
- `mapping`: text
- `data`: an object with any data

#### Size (`size`)

- `width`: a whole number (at least 0)
- `height`: a whole number (at least 0)

## Example 1: a static list with a detail screen

The list's items are in `data`; tapping one shows the reusable `artwork` view with that item.

```json
{
  "schemaVersion": 3,
  "name": "Masterpieces",
  "version": "1.0.0",
  "author": "JSON App Creator",
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
          "thumb": "https://commons.wikimedia.org/wiki/Special:FilePath/Tsunami_by_hokusai_19th_century.jpg?width=300",
          "image": "https://commons.wikimedia.org/wiki/Special:FilePath/Tsunami_by_hokusai_19th_century.jpg?width=1000",
          "about": "A giant wave curls over three boats while Mount Fuji sits small in the distance."
        },
        {
          "title": "Girl with a Pearl Earring",
          "artist": "Johannes Vermeer",
          "year": "c. 1665",
          "medium": "Oil on canvas",
          "where": "Mauritshuis, The Hague",
          "thumb": "https://commons.wikimedia.org/wiki/Special:FilePath/1665_Girl_with_a_Pearl_Earring.jpg?width=300",
          "image": "https://commons.wikimedia.org/wiki/Special:FilePath/1665_Girl_with_a_Pearl_Earring.jpg?width=1000",
          "about": "A girl in a blue and yellow turban turns toward us, a large pearl catching the light."
        },
        {
          "title": "Paris Street; Rainy Day",
          "artist": "Gustave Caillebotte",
          "year": "1877",
          "medium": "Oil on canvas",
          "where": "Art Institute of Chicago",
          "thumb": "https://commons.wikimedia.org/wiki/Special:FilePath/Gustave_Caillebotte_-_Paris_Street;_Rainy_Day_-_Google_Art_Project.jpg?width=300",
          "image": "https://commons.wikimedia.org/wiki/Special:FilePath/Gustave_Caillebotte_-_Paris_Street;_Rainy_Day_-_Google_Art_Project.jpg?width=1000",
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
  "version": "1.0.0",
  "author": "JSON App Creator",
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
  "name": "KANTO POKÉDEX",
  "version": "1.0",
  "author": "JsonAppCreator",
  "changelog": "A Pokédex for the original 151 Pokémon. Catalog data comes from the open PokemonGO-Pokedex dataset, with artwork provided by the PokéAPI sprites repository.",
  "mainView": {
    "id": "pokedex_home",
    "type": "layout",
    "views": [
      {
        "type": "text",
        "label": "Kanto Pokédex",
        "title": "Explore the original 151 Pokémon. Tap a Pokémon to open its full entry.",
        "textStyle": "bold",
        "textSize": 18
      },
      {
        "type": "list",
        "mapping": "pokemon",
        "initData": {
          "url": "https://raw.githubusercontent.com/Biuni/PokemonGO-Pokedex/master/pokedex.json",
          "method": "get"
        },
        "action": {
          "id": "show_pokemon_detail"
        },
        "itemView": {
          "id": "pokemon_list_item",
          "type": "layout",
          "orientation": "horizontal",
          "views": [
            {
              "type": "image",
              "url": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/${id}.png",
              "size": {
                "width": 112,
                "height": 112
              }
            },
            {
              "type": "text",
              "label": "#${num}  ${name}",
              "title": "Type: ${type}\n${height}  •  ${weight}"
            }
          ]
        }
      },
      {
        "type": "text",
        "title": "Data: PokemonGO-Pokedex • Artwork: PokéAPI sprites",
        "textStyle": "italic"
      }
    ]
  },
  "views": [
    {
      "id": "pokemon_detail",
      "type": "layout",
      "views": [
        {
          "type": "image",
          "url": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/${id}.png",
          "size": {
            "height": 300
          }
        },
        {
          "type": "text",
          "label": "#${num}  ${name}",
          "title": "${type}",
          "textStyle": "bold",
          "textSize": 24
        },
        {
          "type": "layout",
          "orientation": "horizontal",
          "views": [
            {
              "type": "text",
              "label": "Height",
              "mapping": "height"
            },
            {
              "type": "text",
              "label": "Weight",
              "mapping": "weight"
            }
          ]
        },
        {
          "type": "text",
          "label": "Candy",
          "mapping": "candy"
        },
        {
          "type": "text",
          "label": "Weaknesses",
          "mapping": "weaknesses"
        },
        {
          "type": "layout",
          "orientation": "horizontal",
          "views": [
            {
              "type": "text",
              "label": "Egg",
              "mapping": "egg"
            },
            {
              "type": "text",
              "label": "Spawn time",
              "mapping": "spawn_time"
            }
          ]
        },
        {
          "type": "text",
          "label": "Spawn chance",
          "title": "${spawn_chance}%"
        }
      ]
    }
  ],
  "actions": [
    {
      "id": "show_pokemon_detail",
      "type": "click",
      "event": {
        "type": "show",
        "view": {
          "id": "pokemon_detail"
        }
      }
    }
  ]
}
```

## Your task

Create a JSON App Creator v3 configuration for this app: <describe the app: its screens, data and actions>

If you can create files, give me the complete configuration as a downloadable file named app.json, containing only the JSON. If you can't, reply with only the complete JSON, starting with { and ending with }: no comments or explanations. I will open your file or share your answer in the JSON App Creator app, which runs it; if it reports problems, I will send them to you to fix.
