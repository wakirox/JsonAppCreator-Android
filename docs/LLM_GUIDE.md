# Generating JSON App Creator configurations with an LLM

This file is the generation contract for JSON App Creator v3. For individual field
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

## Validate generated output

When repository tools are available, run:

```sh
./gradlew :app:validateConfig -Pconfig=path/to/generated.json
```

Treat every reported path as a required correction. Do not suppress validation errors
by deleting useful definitions or weakening references; repair the configuration's
structure and data flow.

## Recommended prompt

```text
Create a JSON App Creator v3 configuration for the following application.

Requirements:
<describe screens, data, endpoints, and interactions>

Follow docs/LLM_GUIDE.md and schema/v3.schema.json. Use schemaVersion 3 and explicit
{ "ref": "..." } references. Return only valid JSON, without Markdown or commentary.
```
