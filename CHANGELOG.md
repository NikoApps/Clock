# Changelog

Internal changelog for developers, generated from commit messages with git-cliff. The user-facing notes are in `fastlane/metadata/android/en-US/changelogs/`.

## 0.3.1 - 2026-10-19

### CI

- attach the release APK as niko-clock.apk so latest/download links work

### Docs

- store description covers setup, warnings, widgets and AI assistance
- phone screenshots for the store listing
- feature graphic for the F-Droid listing
- release notes for 0.3.1

## 0.3.0 - 2026-10-18

### Added

- health warnings, last announcement and first-run setup
- date and time-and-date widgets, calmer setup buttons

### Docs

- link the Niko Clock page on the website

### Chores

- release 0.3.0

## 0.2.0 - 2026-10-17

### Added

- add large time display and speak time button
- speak the time as natural phrases in English and Nepali
- pick an offline voice and keep the main screen fixed
- add helper settings storage and per-app language
- add settings for helpers behind an optional PIN
- detect a deliberate shake from accelerometer samples
- speak the time when the phone is shaken
- bring background work back when the app is opened
- let Speaker report when speech ends and set its volume and stream
- speak hourly chimes from exact alarms
- say the time from a quick settings tile and a home-screen widget
- redesign settings and separate voice and screen language
- settings as a plain list with choice dialogs
- speak in Hindi, Spanish and Portuguese, and translate the screen into 9 languages
- speak the date on double tap, voice pitch, info buttons, support and about
- clock card with seconds and date, app name centred at the top
- shake again right after the time to hear the date
- speak date tile and a setting for shake again for the date

### Fixed

- let the app see text-to-speech engines on Android 11+

### Build

- prepare reproducible, optionally signed release builds

### CI

- build release, run unit tests, fail on accessibility lint and Google dependencies
- publish signed, reproducible releases from version tags

### Docs

- say shared code stays in Clock until the second app
- add internal changelog generated with git-cliff
- add CLAUDE.md and project skills for Compose and coroutines
- prepare the first public release

### Chores

- update launcher icon to Niko Clock v2
- rename package to io.github.nikoapps
- ignore local agent worktrees
- release 0.2.0

