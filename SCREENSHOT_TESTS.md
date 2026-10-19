# TMDb screenshot tests

The TMDb visual regression suite runs on the host JVM with Robolectric. It does
not require an emulator, a connected phone, a TMDb API key, or internet access.

The suite covers these paths at a fixed 448 x 900 viewport:

- home
- search results
- movie details
- actor details

Local JSON responses and image fixtures are served by MockWebServer. This keeps
the screenshots deterministic and makes failures reflect app UI changes rather
than live TMDb content changes.

## Verify screenshots

```sh
./gradlew testDebugUnitTest --tests '*TmdbScreenshotTest'
```

When a comparison fails, the actual and highlighted diff images are written to
`app/build/outputs/tmdb-screenshots/`.

## Record intentional changes

After reviewing an intentional interface change, update all golden images with:

```sh
./gradlew testDebugUnitTest --tests '*TmdbScreenshotTest' -PrecordScreenshots=true
```

The version-controlled baselines are in
`app/src/test/resources/goldens/`. Run verification once more after recording so
the new baselines are checked.

# App icon screenshot tests

`AppIconScreenshotTest` covers the home page's app icons (`AppIconView`) at
xxhdpi, the same way and with the same record flag:

- `app_icon_emoji`: emoji on each app's soft colour, including a padded emoji
- `app_icon_letter`: the letter tile over cream, a digit, and the `?` fallback
- `app_icon_sizes`: emoji and letter at 24, 40 and 64 dp
- `app_icon_dark`: both kinds on the dark theme

```sh
./gradlew testDebugUnitTest --tests '*AppIconScreenshotTest'
./gradlew testDebugUnitTest --tests '*AppIconScreenshotTest' -PrecordScreenshots=true
```

Failures write the actual and diff images to
`app/build/outputs/app-icon-screenshots/`.
