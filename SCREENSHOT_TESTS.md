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
