// Play Store captions, one entry per screenshot (the listing is English only).
// `code` shows a JSON excerpt beside the phone; `prompt` puts a chat bubble above it.
const CAPTIONS = {
  en: {
    gallery: { kicker: "JSON in, app out", headline: "Write a JSON file. Get a real app.",
      code: "gallery" },
    detail: { kicker: "Lists and images", headline: "Tap an item, open its own screen" },
    form: { kicker: "Forms", headline: "Text, email, dates, choices and switches" },
    alert: { kicker: "Actions", headline: "Buttons that answer with your data" },
    ai: { kicker: "Write it or ask an AI", headline: "Describe the app, open the file",
      prompt: "Make a sign-up form for a 10 km run in Rome", code: "form" },
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
