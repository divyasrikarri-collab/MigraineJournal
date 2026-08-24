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
