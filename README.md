# Migraine Journal

An Android app for tracking migraines alongside sleep, diet, hydration, stress and exercise.
Local-first: everything lives in a Room database on the device, there is no account, no sync
and no analytics.

Built to `MigraineJournal_Spec.md`.

## Stack

| Concern | Choice |
| --- | --- |
| UI | Jetpack Compose, Material 3 |
| Storage | Room (SQLite) |
| Navigation | Navigation Compose |
| Background work | WorkManager (daily reminder) |
| Preferences | DataStore Preferences |
| Charts | Compose `Canvas` + layout, no charting dependency |
| PDF | `android.graphics.pdf.PdfDocument` |
| DI | A hand-rolled `AppContainer` |

Min SDK 26, target/compile SDK 35, Kotlin 2.0.21, AGP 8.7.3.

## Building

```bash
./gradlew assembleDebug     # APK
./gradlew test              # JVM unit tests
./gradlew lint              # Android Lint
```

The Gradle wrapper is checked in. The build needs the Android SDK (set `ANDROID_HOME`, or let
Android Studio create `local.properties`) and network access to `dl.google.com` and Maven
Central for dependency resolution.

### Cloud builds

`.github/workflows/android.yml` runs the unit tests and builds a debug APK on every push, so a
working Android SDK is not needed locally. Open the repository's **Actions** tab, pick the most
recent run, and download the `migrainejournal-debug-apk` artifact.

The same run publishes a `room-schemas` artifact. Download it and commit its contents to
`app/schemas/` before the first Play release — see [Room schema](#room-schema) for why that
matters.

A second job, **Release build (R8)**, runs `assembleRelease` on every push. That is the only
automated check on minification: `isMinifyEnabled` applies to the `release` build type only, so
a green debug build says nothing about whether R8 and `proguard-rules.pro` are correct. The job
builds unsigned — no keystore is present on the runner — and publishes the R8 `mapping.txt` as
an artifact. It still cannot catch a *runtime* reflection failure; only installing a release
build on a device does that.

### Publishing to Play from CI

`.github/workflows/publish.yml` builds a signed bundle and uploads it to the **internal** track.
It is triggered by a version tag, not by a push to a branch:

```bash
git tag v1.0.1 && git push origin v1.0.1
```

It cannot perform the **first** upload. The Google Play Developer API cannot create a listing,
and refuses uploads for a package until one bundle has been uploaded by hand in the Play
Console. Release 1 is manual; this workflow covers release 2 onward.

Required repository secrets:

| Secret | Value |
| --- | --- |
| `PLAY_SERVICE_ACCOUNT_JSON` | Service account key JSON, granted release permission in Play Console |
| `KEYSTORE_BASE64` | `base64 -w0 migrainejournal-upload.jks` |
| `KEYSTORE_PASSWORD` | |
| `KEY_ALIAS` | `upload` |
| `KEY_PASSWORD` | |

`versionCode` comes from the CI run number and `versionName` from the tag, because Play rejects
any upload whose `versionCode` does not exceed the previous one. Locally both fall back to the
values in `app/build.gradle.kts`; set `VERSION_CODE`/`VERSION_NAME` in the environment to
override. The workflow uploads to `internal` and stops there — promotion to production is a
deliberate step in the Console.

### Trying it in a browser

To run the app without an Android device, feed that debug APK to a browser-based emulator such
as [Appetize.io](https://appetize.io) (free tier, minute-limited):

1. Download `migrainejournal-debug-apk` from the Actions run and unzip it.
2. Upload `app-debug.apk` on Appetize's upload page.
3. Pick a device running **API 33 or higher** and open the link it gives you.

API 33+ matters: that is where `POST_NOTIFICATIONS` became a runtime permission, so it is the
only way to exercise the request flow in `SettingsScreen.kt`. The link is shareable, which makes
it a cheap way to show the app to closed-test recruits before they install anything.

Two caveats. The debug APK is not the artifact you ship — `isMinifyEnabled` applies only to
`release`, so a browser emulator running the debug build proves nothing about how R8 treats the
release build. And a hosted emulator is a poor test of the daily reminder, which is a
WorkManager job that fires on a real clock; test that on a real device.

## Releasing

### One-time: create an upload key

```bash
keytool -genkeypair -v -keystore ~/keys/migrainejournal-upload.jks \
  -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Copy `keystore.properties.example` to `keystore.properties` and fill it in. Both the keystore
and that file are gitignored. Back them up somewhere durable — without the upload key you
cannot ship an update to an existing listing except through Google's key reset process.

When `keystore.properties` is absent the release build still assembles, unsigned, so machines
that only run `test` and `lint` need no signing material.

### Each release

1. Bump `versionCode` (must increase on every upload) and `versionName` in `app/build.gradle.kts`.
2. `./gradlew test lint`
3. `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`, the artifact
   Play accepts.
4. Install the minified build on a real device and exercise it before uploading:

   ```bash
   ./gradlew assembleRelease   # app/build/outputs/apk/release/
   ```

   `isMinifyEnabled` is on, so R8 runs. `proguard-rules.pro` keeps the Room entities and
   `ReminderWorker`, the two things reflected over by name — but a release build can still
   fail at runtime where a debug build passes. Check the daily reminder fires and that CSV
   and PDF export both open in the share sheet.
5. Commit `app/schemas/` if the build regenerated it (see below).

### Room schema

`exportSchema = true` writes the schema JSON to `app/schemas/` on each build, and
`MigraineDatabase` deliberately does *not* call `fallbackToDestructiveMigration()` — a health
log should never silently drop the user's history. That makes the exported schema the input to
every future `Migration`, so **commit `app/schemas/` before the first Play release**; without
the v1 JSON on disk you cannot write or test the migration to v2.

### Privacy policy and listing copy

`docs/` holds the privacy policy page and the store listing text.

Play requires the privacy policy at a public URL. To host it from this repo for free, merge
`docs/` to the default branch, then in the repository's **Settings → Pages** set the source to
that branch and the `/docs` folder. The policy is then served at:

```
https://<owner>.github.io/MigraineJournal/privacy-policy.html
```

Before publishing, replace `CONTACT_EMAIL_PLACEHOLDER` in `docs/privacy-policy.html` with the
support address you want shown publicly — the page is world-readable, so use an address you
are willing to publish. Play also asks for a contact email in the Console; that one is not
shown on the listing.

`docs/store-listing.md` has the app name, short and full descriptions, "what's new" text, the
Data safety answers with the code that backs each one, and the list of graphics that still
need to be produced.

### Play Console checklist

- Data safety form. Nothing is transmitted off device, nothing is shared, and export is
  user-initiated through the system share sheet; `data_extraction_rules.xml` backs the
  "not backed up to cloud" answer.
- Privacy policy at a public URL — required for a health app, and it must be a hosted URL.
- Health apps declaration: this app claims no medical device functionality. Keep the in-app
  disclaimer visible.
- Content rating questionnaire, 512×512 icon, 1024×500 feature graphic, ≥2 phone screenshots.
- Personal developer accounts must run a closed test with a minimum number of opted-in
  testers for 14 continuous days before applying for production access. Check the current
  threshold in the Console and start it early; it is the longest item on the schedule.
- `targetSdk` must meet Play's current floor for new uploads, which rises each August. This
  app targets 35 (Android 15) — verify that is still accepted before planning a release date.

## Layout

```
com.divyasrikarri.migrainejournal
├── data
│   ├── local        Room entities, DAOs, database, type converters
│   ├── model        Option vocabularies, trigger seed list, view models of insights
│   └── repository   MigraineRepository, SettingsRepository, InsightsCalculator
├── ui
│   ├── home         Dashboard
│   ├── logmigraine  Fast-entry / edit form
│   ├── dailycheckin Evening check-in incl. food log
│   ├── history      Month calendar + list, day detail
│   ├── insights     Charts and rule-based callouts
│   ├── settings     Reminder, units, export, data management
│   ├── components   Shared chips, sliders, charts, pickers
│   ├── navigation   Route constants and bottom-bar destinations
│   └── theme        Colour, type, Material 3 theme
├── notification     WorkManager reminder + notification channel
├── export           CSV and PDF writers, share intents
└── util             Date and unit helpers
```

## Screens

- **Home** — a full-width *Log migraine now* button, an "in progress" card when an attack is
  still open, today's check-in status, quick stats (this month, average pain, days
  migraine-free) and the three most recent entries.
- **Log migraine** — pain slider, start time (defaults to now), location and type chips,
  symptom chips, aura toggle, optional medication with a quick-add row of recent meds, and
  notes. Save is sticky. "Still ongoing" skips the end time; reopening the entry from History
  adds the end time and a medication effectiveness rating.
- **Daily check-in** — sleep hours and quality, water with quick-tap glasses, stress,
  exercise, a food log with autocomplete over previously logged foods, and optional screen
  time / barometric pressure / cycle day.
- **History** — month grid where a day's fill is that day's worst pain level and a dot marks a
  completed check-in; tapping a day shows its migraines plus the full daily log. A segmented
  control swaps the calendar for a flat list.
- **Insights** — frequency bars (weekly or monthly), a pain-level trend line, and rule-based
  callouts.
- **Settings** — reminder toggle and time, cycle tracking, ml/oz, PDF and CSV export, clear all
  data, about.

## Notable decisions

**Charts are hand-drawn rather than pulled from a library.** The spec asks for one bar chart and
one line chart. Both are a few dozen lines of Compose layout and `Canvas`, and writing them
directly keeps the rendering theme-aware and drops a dependency (and its transitive
`ViewGroup`-interop cost) from a small app. If the chart requirements grow — zooming, multiple
series, interactive tooltips — Vico is the natural upgrade, as the spec suggests.

**Insights are counts, not claims.** `InsightsCalculator` is pure Kotlin with no Android
dependencies, so it is directly unit-tested. Every callout states its own denominator ("2 of 3
migraines that had a sleep log recorded for the same day"), percentages are suppressed below
three samples, and the trigger-food callout says in as many words that it is a co-occurrence
count and not a cause. Migraine tracking apps make people anxious about food; overstating a
three-sample coincidence would be the easiest way to do harm here.

**Health data does not leave the device.** No network permission is declared. Cloud backup and
device-to-device transfer are switched off in `data_extraction_rules.xml` — the trade-off is
that a user who changes phones starts fresh, which is why export is one tap from Settings.
Exports are written to app-specific external storage and only travel if the user picks a target
in the system share sheet.

**The reminder deep-link uses an intent extra, not a public URI.** A `migrainejournal://checkin`
scheme would let any installed app launch straight into the user's health log; the notification's
`PendingIntent` carries an extra instead, and `MainActivity` translates it into a route.

**Water is stored in millilitres regardless of the display unit**, so switching ml ↔ oz in
Settings never rewrites stored data.

**List columns are joined with `|`, not a comma**, so a pain location or symptom containing a
comma still round-trips through the Room type converter.

## Tests

`./gradlew test` covers the parts where a mistake is silent rather than loud:

- `InsightsCalculatorTest` — bucket boundaries (Monday-start weeks, empty buckets, calendar
  months), the denominators behind each callout, the minimum-sample suppression, and the rule
  that a trigger food only counts on days that actually had a migraine.
- `CsvExporterTest` — RFC 4180 escaping, stable column counts when notes contain commas.
- `ConvertersTest` — round-tripping list columns including values with commas.
- `DateUtilsTest` — half-open day bounds, the boundary the calendar and range queries depend on.
- `ReminderSchedulerTest` — the next-occurrence delay, including the exactly-at-reminder-time case.
- `UnitUtilsTest`, `TriggerFoodsTest`.

## Not in v1

Cloud sync, automatic weather/barometric capture, and any form of ML or predictive trigger
analysis — all out of scope per the spec.

## Disclaimer

A personal log, not a medical device. Nothing it displays is a diagnosis or treatment advice.
