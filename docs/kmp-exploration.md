# Kotlin Multiplatform exploration

Branch `explore/kmp-shared`, based on master `66c02ba`. This is an exploration. It isn't
meant to be merged until the owner decides whether Appkin goes multiplatform. It
contains no iOS UI and no Swift code. The goal was to find out whether the engine can be
shared and what that costs, with Android behaving exactly as before.

## Result

- A new `:shared` Kotlin Multiplatform module holds the whole engine: the model, parser,
  validation, runtime state and use cases, the editor logic and the AI prompt builder.
  It has 3,900 lines of common code and only 6 lines of platform code.
- `:shared` compiles for Android, `iosArm64` and `iosSimulatorArm64`. Kotlin 2.4
  cross-compiles the iOS klibs on Linux (`./gradlew :shared:compileKotlinIosArm64`), so
  common code is checked against the real iOS target, not just metadata. Only linking the
  framework needs a Mac.
- Jackson, org.json (in the engine) and networknt are gone from the app. They are
  replaced by kotlinx.serialization, OptimumCode's json-schema-validator and Ktor.
- All tests pass: 97 before, 144 after (86 common, 58 Android). Two pieces of output
  were compared exactly. Validator messages match networknt's, and
  `docs/AI_CONTEXT.md` is reproduced byte for byte.
- The minified release (R8) works on an emulator. Masterpieces, the detail screen, the
  Summer Run form (missing-field error, then submit and alert) and the saved apps from the
  previous build all worked. The current master can't be minified. R8 fails on
  networknt's Jackson 3. With those warnings silenced, it builds but can't parse any
  configuration ("TypeReference constructed without actual type information").

## What moved, what stayed

| In `:shared` (common code) | Stays in `:app` (Android) and why |
|---|---|
| `model/` on kotlinx.serialization | Compose renderer, editor screen and highlighting: UI (an iOS app would use SwiftUI) |
| `AppParser`, `ReferenceValidator`, `ValidatedConfigurationParser` | Activities, menus, share sheet, clipboard, intents |
| `ConfigurationValidator` and the v3 schema, copied in at build time (`V3Schema`) | Koin wiring (`AppModule`) |
| `runtime/`: `ScreenViewModel`, `FormsState`, `ListsState`, use cases, `AppRepository` | `DomainController` (raw-resource examples), `SharedPreferencesStore` |
| `alerts/AlertsViewModel` (StateFlow) | `AlertHost` (Compose dialog) |
| `AppLibrary`, `CurrentApp` over `AppStore` | `EncryptedApiKeyStore` (Keystore) implementing `SecretStore` |
| `ConfigurationKeys` (API keys) over `SecretStore`, `ApiKeyPlaceholders` with a pure-Kotlin SHA-256 | `AndroidCalendarWriter` (insert intent) implementing `CalendarWriter` |
| `CreateCalendarEventUseCase` → `CalendarEntry` | `HomeShortcuts` (pinned shortcuts) implementing `HomeScreenShortcuts` |
| `JsonHttpClient` (Ktor), `ShareLinks` (Ktor) | `HttpClient`: the one OkHttp client shared by Ktor and Coil |
| `editor/`: checker, suggestions, cursor, snippets, `prettyJson` | |
| `ai/AiContext` | `AskAi` (Android resources, share sheet) |

Platform seams are common interfaces with Android implementations: `AppStore`,
`SecretStore`, `CalendarWriter`, `HomeScreenShortcuts` and `RuntimeStrings`. Common
tests run them on in-memory fakes.

The runtime exposes plain `StateFlow`s and has no Compose types. `ScreenViewModel` is a
multiplatform androidx `ViewModel` with `SavedStateHandle` (lifecycle 2.10). A common
test drives a whole screen with no UI, as SwiftUI would.

## Library choices

- **kotlinx.serialization** for the model. One explicit `TypedSerializer` picks the class
  by `"type"`, with no reflection, so it works on Native. An object without a type is a
  `ViewReference`. `ButtonView` can no longer extend `TextView` (a serializable class
  can't extend another), so both are `TextualView`. The renderer checks that type where
  it treated buttons as text.
- **`JsonText`**, a small strict JSON reader and writer on `JsonElement`. The app relied
  on Jackson for things kotlinx doesn't do: character offsets of every path, reading one
  value from inside a page (shared chats), duplicate-key detection, Jackson-worded syntax
  errors at the same offsets (checked against Jackson 2.22), Jackson's pretty-print
  format, and BigDecimal number output.
- **OptimumCode json-schema-validator 0.5.5**, draft 2020-12 with iOS targets. Its
  messages are reworded to networknt's, because users see them and the editor reads the
  unknown property's name from them.
- **Ktor 3.6** for the app's requests. On Android it runs on the same `OkHttpClient` Coil
  uses (`OkHttp.create { preconfigured = … }`), with the Appkin User-Agent. Wikimedia
  images and requests work in the minified build. **Coil stays on OkHttp.** Images are
  Android UI, an iOS app wouldn't use Coil, and one client keeps one connection pool and
  one User-Agent interceptor.
- **kotlinx-datetime** for form and calendar dates. They are read as strictly as
  `SimpleDateFormat` (lenient = false) did.

## Validator parity

`ValidatorParityTest` runs both validators on 10 bundled configurations and 84
deliberately broken ones. The broken set covers every kind of mistake the schema checks:
types, enums, constants, required and unknown properties, references, `oneOf`, and
`minimum`/`minLength`. The paths and messages are identical, e.g.
`$.mainView.views[1].type: does not have a value in the enumeration [...]`. Raw
OptimumCode output differed in two ways. Its messages were terse, and it has no summary
line for a failed `oneOf`. Both are mapped in `ConfigurationValidator`. networknt stays
as a test-only dependency, as the reference for this test.

## Size and build time

| | master | this branch |
|---|---|---|
| Release APK, unminified (as published) | 36.7 MB | 34.1 MB (−2.7 MB) |
| Release APK, R8 | fails without extra rules; 6.5 MB with `-dontwarn`, but broken at runtime | **5.1 MB, works** |
| Clean `assembleRelease` | 24 s | 23 s |
| Clean R8 `assembleRelease` | n/a | 32 s |
| Clean unit tests | 21 s (97 tests) | 17 s (144 tests) |

Times are single runs on the Rome PC (`--rerun-tasks --no-build-cache`), so read them
as "no noticeable change". The minified build is opt-in (`-Pminify=true`), and the
published release is unchanged. Turning R8 on is now possible, and worth doing whatever
the decision.

## Behaviour notes (kept, or knowingly different)

Kept on purpose: numbers shown as org.json showed them (`1.50` → `1.5`), null form
fields left out of the submitted JSON, and Jackson's date forms in `min`/`max`/`value`.
Also kept: `20.0` accepted as an integer, text after the JSON ignored (as Jackson did),
storage files and keys unchanged (old saved apps open), and a list's odd fallback that
reads its mapping as an array.

Small differences, all on invalid input or edge cases:

- Syntax error and invalid HTTP response texts are worded differently, though the
  editor's plain-words versions are the same.
- RFC 1123 dates ("Fri, 01 May 2026 …") in date fields are no longer accepted.
- A calendar time that falls in a DST gap moves forward instead of being rejected.
- A POST that is redirected is followed with Ktor's rules.

## Risks

- Every rule Jackson and org.json applied implicitly is now explicit code in `JsonText`,
  `TypedSerializer` and the validator's message mapping. The tests pin it down, but it's
  code to own.
- OptimumCode is a smaller project than networknt. The parity test would catch a
  regression after an upgrade.
- It's a wide change: 163 files and +4.3k/−2.3k lines. If the owner decides against
  iOS, the Jackson removal and R8 alone are worth keeping. The `:shared` split costs
  little to keep but is optional.

## Remaining effort for an iOS app

- SwiftUI renderer for the six views, four form fields, lists, alerts and navigation:
  about 2–3 weeks, the bulk of the work.
- SwiftUI editor (highlighting, suggestions UI, problem list) and home page: about
  2 weeks.
- iOS implementations of the seams: `AppStore` (UserDefaults), `SecretStore` (Keychain),
  `CalendarWriter` (EventKitUI), shortcuts (App Intents or none), and `RuntimeStrings`.
  About 3–5 days.
- SKIE (or KMP-NativeCoroutines) so Swift gets `StateFlow` as async sequences and sealed
  classes as enums. Also the Xcode project with the `AppkinShared` framework (already
  declared). About 2–3 days.
- Mac CI to link and test (`iosSimulatorArm64Test`); GitHub's macOS minutes cost 10× and
  the repository is public, so a self-hosted Mac or manual runs. App Store setup, privacy
  manifest, review. About 1 week.

That is roughly 6–8 weeks for a first iOS release with feature parity.

## Recommendation

Keep the engine work regardless. Jackson out, R8 working and a 5 MB APK are wins for
Android alone. Go multiplatform only if an iOS Appkin is a real product goal. The
shared engine removes the risky half (parsing, validation and runtime semantics are
proven identical). What remains is a SwiftUI renderer and editor, which is ordinary
iOS work. If iOS isn't planned for the next months, merge the non-UI parts (Jackson
removal, R8, kotlinx model) and keep `:shared` as the module structure. It costs nothing
on Android and keeps the option open.
