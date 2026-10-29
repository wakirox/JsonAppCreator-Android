# Store review

What to tell Google Play and App Store reviewers, and what the listing must not show. Paste the
reviewer notes into Play Console (App content → App access / notes for review) and App Store
Connect (App Review Information → Notes).

## Reviewer notes

> Appkin runs small apps that the user describes in a JSON file: screens of text, pictures, lists,
> forms and buttons, built only from components inside Appkin. The user gets the JSON from the AI
> app they already use (Appkin has no AI inside and sends nothing anywhere), writes it in the
> built-in editor, or opens a file someone sent them.
>
> There is no account, no server and no public gallery: apps exist only on the user's phone, and
> travel only as files that people choose to share. Appkin is free, has no ads, no in-app purchases
> and collects no data.
>
> To try it, open one of the examples on the home page: **Masterpieces** (public-domain paintings
> from Wikimedia Commons), **App ideas** (a notebook saved on the phone, with photos, tags, a PDF and
> a Markdown export) or **Movies** (film data from TMDB). No sign-in is needed.
>
> Content: the examples show only public-domain works (credited in About) and TMDB data, used under
> TMDB's terms with its notice and logo in About. What a user's own app loads is the user's
> responsibility under the Terms of use (About → Terms of use). Anyone can report an app or its
> content from About → Report a problem, which writes to info@wakirox.app.

### App Store guideline 2.5.2 (downloaded code)

> A JSON file only arranges Appkin's own native screens and says which built-in action a button
> runs (show a screen, open a link, save on the phone, …). It contains no executable code: there is
> no script engine, no web view running the app, and nothing changes what Appkin itself can do. The
> same JSON is plain data that the app validates against a published schema before showing it.

## Listing checklist

- [ ] Screenshots, feature graphic and icon show only our own content and public-domain works: no
      TMDB posters, film names or the name "The Movie DB", no trademarks (Pokémon, brands), no real
      people's personal data.
- [ ] The description names only the bundled examples (Masterpieces, App ideas, Movies) and
      features that are in the release.
- [ ] Privacy policy: https://wakirox.app (Appkin collects no data).
- [ ] Terms of use: in the app (About → Terms of use); the same text may be linked from the
      listing once it is on wakirox.app.
- [ ] Contact: info@wakirox.app (About → Report a problem).

## TMDB

Appkin must stay free with no ads, which TMDB's non-commercial terms require. Any ads, payments or
subscription need a written agreement with TMDB first. Keep the notice and logo in About, and the
image cache cleared every 90 days.
