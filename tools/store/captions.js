// Play Store captions, one entry per screenshot (the listing is English only).
// `code` shows a JSON excerpt beside the phone; `prompt` puts a chat bubble above it.
const CAPTIONS = {
  en: {
    home: { kicker: "Appkin", headline: "Your own little apps, all in one place" },
    ai: { kicker: "With your AI app", headline: "Describe it here, your AI app makes it" },
    gallery: { kicker: "Lists and pictures", headline: "Galleries, catalogues and more from the web" },
    detail: { kicker: "Every item", headline: "Tap one to see it up close" },
    form: { kicker: "Forms", headline: "Sign-ups that check every field" },
    shortcut: { kicker: "On your home screen", headline: "Each app gets its own icon" },
    editor: { kicker: "For the curious", headline: "Every app is one file you can change" },
  },
};

// Excerpts of examples/v3_masterpieces.json and examples/v3_race_signup.json, shortened to fit.
const CODE = {
  gallery: `{
  "name": "Masterpieces",
  "mainView": {
    "type": "layout",
    "views": [{
      "type": "list",
      "mapping": "artworks",
      "action": { "ref": "open_artwork" },
      "itemView": {
        "type": "layout",
        "orientation": "horizontal",
        "views": [
          { "type": "image",
            "url": "\${thumb}" },
          { "type": "text",
            "label": "\${title}",
            "title": "\${artist}" }
        ]
      }
    }]
  }
}`,
  form: `{
  "name": "Summer Run 2026",
  "mainView": {
    "type": "layout",
    "views": [{
      "type": "form",
      "buttonTitle": "Sign me up",
      "formElements": [
        { "type": "text",
          "mapping": "email",
          "textType": "email",
          "title": "Email" },
        { "type": "selector",
          "mapping": "distance",
          "options": ["5 km", "10 km",
                      "Half marathon"] },
        { "type": "date",
          "mapping": "birth" }
      ]
    }]
  }
}`,
};
