# Play Store listing copy

Paste-ready text for the Play Console. Character limits are Play's; the counts in
parentheses are what these drafts actually use. Re-check counts after any edit.

---

## App name (limit 30)

```
Migraine Journal
```

## Short description (limit 80)

```
Track migraines with sleep, food, stress and water. Private, offline, free.
```

## Full description (limit 4000)

```
Migraine Journal is a private, offline diary for migraine attacks and the daily habits that
sit around them.

Log an attack in seconds, fill in a short check-in each evening, and see your own patterns
over time — without an account, without ads, and without your health data leaving your phone.

LOG AN ATTACK FAST
When a migraine starts, one tap from the home screen opens the log. Set the pain level, mark
where it hurts and what kind of pain it is, pick your symptoms, note aura, and record any
medication you took. If it is still going, save it as ongoing and close it out later — you
can add the end time and rate how well the medication worked once you know.

A DAILY CHECK-IN THAT TAKES A MINUTE
Sleep hours and quality, water with quick-tap glasses, stress, exercise, and a food log with
autocomplete over foods you have already entered. Screen time, barometric pressure and cycle
day are there if you want them and hidden if you do not. Every field is optional, so a
half-filled check-in still saves.

SEE YOUR OWN HISTORY
A month calendar shades each day by that day's worst pain level, with a dot for a completed
check-in. Tap any day for the full picture: the attacks, the medication, and the daily log
that went with it. Or switch to a plain list and scroll back.

INSIGHTS THAT DO NOT OVERSTATE
Frequency bars by week or month, a pain-level trend line, and plain-language callouts. Every
callout shows its own sample size, so "2 of 3 migraines" reads as exactly that. Percentages
are hidden below three entries, and food callouts say plainly that they are a co-occurrence
count and not a cause. This app counts what you logged; it does not guess at why.

BUILT FOR YOUR APPOINTMENT
Export a PDF summary covering the last year — attacks, frequency, medication and patterns —
formatted to hand to a neurologist. Or export the raw CSV if you want to keep your own copy
or work with it in a spreadsheet.

YOUR DATA STAYS ON YOUR PHONE
No account. No sign-up. No analytics. No ads. The app does not request internet access at
all, so it cannot send your health information anywhere even in principle. Cloud backup and
device-to-device transfer are switched off deliberately. Exports go where you send them and
nowhere else, and "Clear all data" in Settings means it.

ALSO
- Optional daily reminder at a time you pick
- Millilitres or ounces
- Dark theme
- Free, with no in-app purchases

Migraine Journal is a personal log, not a medical device. Nothing it records or displays is
a diagnosis, a treatment recommendation, or medical advice. Talk to a clinician about your
headaches.
```

## What's new — first release (limit 500)

```
First release.

Log migraine attacks with pain level, location, symptoms, aura and medication. Add a daily
check-in for sleep, water, stress, exercise and food. Review your history on a month calendar,
see frequency and pain trends, and export a PDF summary for your doctor or a CSV of everything.

Fully offline — no account, no ads, no analytics, and no internet permission.
```

---

## Data safety form — answers

Play's Data safety section asks whether data is *collected* (sent off the device) or
*shared*. For this app both are **No** across every category. Data stored only in the app's
local database is not "collected" under Play's definition.

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **No** |
| Is all user data encrypted in transit? | N/A — no data is transmitted |
| Do you provide a way for users to request data deletion? | **Yes** — Settings → Clear all data, and uninstalling removes everything |
| Privacy policy URL | The GitHub Pages URL for `docs/privacy-policy.html` |

Supporting facts, if review asks:

- `AndroidManifest.xml` declares no `INTERNET` permission — the only permission is
  `POST_NOTIFICATIONS`, for the optional daily reminder.
- `res/xml/data_extraction_rules.xml` excludes every domain from both cloud backup and
  device-to-device transfer.
- Export writes to app-specific storage and hands a `FileProvider` URI to the system share
  sheet; the file moves only if the user picks a destination.

## Health apps declaration

The app claims **no medical device functionality**: it records what the user types and
displays counts and averages of it. It does not diagnose, screen, monitor, treat, or offer
recommendations. The disclaimer appears in Settings → About, in the store description, and in
the privacy policy.

## Category and tags

- Category: **Health & Fitness**
- Content rating: expect **Everyone** — no user-generated content that is shared, no ads, no
  purchases, no data collection.

## Graphics still needed

These cannot be generated from the codebase and have to be produced as images:

- App icon: 512 × 512 PNG, 32-bit, no transparency
- Feature graphic: 1024 × 500 PNG or JPEG, no transparency
- Phone screenshots: at least 2 (8 max), 16:9 or 9:16, min 320 px on the short edge —
  Home, Log migraine, History calendar and Insights are the four that show the app best
- Optional: 7-inch and 10-inch tablet screenshots
