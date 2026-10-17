# Niko Clock

A talking clock for people who can't easily read a screen. Part of Niko: simple, free, offline Android apps with no ads and no tracking.

## Where the context lives

The private Hub repo at `../Hub` (relative to this repo) is the source of truth. Read the relevant doc before changing behaviour, and don't repeat its reasoning here.

- Clock spec: `../Hub/docs/02-product/apps/01-clock/01-spec.md`
- Design system (colour, type, layout, components, speech): `../Hub/docs/04-design-system/`
- Words on screen and spoken phrases: `../Hub/docs/03-brand/04-voice-and-tone.md`
- Stack, text-to-speech and time phrasing: `../Hub/docs/06-engineering/01-platform-and-stack.md`
- Background work (alarms, shake, battery savers): `../Hub/docs/06-engineering/05-background-reliability.md`
- Release, signing, versioning: `../Hub/docs/06-engineering/03-release-and-distribution.md`
- Decisions: `../Hub/docs/07-decisions/`. Anything that constrains future work gets a new ADR there.

If code and the Hub disagree, flag it instead of guessing which is right.

## Stack

- Kotlin, Jetpack Compose, Material 3 as the base layer with Niko's own components on top. No XML views.
- One activity. ViewModel + StateFlow for screen state. DataStore for settings. No dependency injection framework.
- Dependencies: AndroidX, Kotlin libraries and small GPLv3-compatible open source libraries only, pinned in `gradle/libs.versions.toml`. Never Google Play Services, Firebase, Play libraries, analytics or crash reporting. CI fails on them.
- Shared code (theme, speech, later UI components) lives in `io.github.nikoapps.commons` inside this repo until the second app starts (ADR 0020). Keep it free of Clock-specific logic so it can move as is.

Project skills in `.claude/skills/`: `compose-patterns` and `kotlin-coroutines-flows`.

## Hard rules

- No new permissions without asking. Never the internet permission.
- Every control has a spoken label. Touch targets at least 64dp, primary actions at least 96dp. Text in `sp`, usable at 200% font size. Contrast 7:1 in light and dark.
- The main screen never scrolls, and nothing on it moves or appears based on state.
- Speech gets words, never digits or formatted times like "3:15 PM". Each language has a hand-written phrase template.
- No toasts or snackbars for anything important, no icon-only buttons, no swipe-only or long-press-only actions.
- All user-facing text goes in `strings.xml`. Plain everyday words, no exclamation marks, never blame the user.
- Builds must stay reproducible: no timestamps, git hashes or dates in the build, no `+` dependency versions.

## Commands

JDK 17 (Temurin, in `~/.jdks`) and the Android SDK (`~/Android/Sdk`) are installed on the founder's machine; `JAVA_HOME` and `ANDROID_HOME` are set in `~/.zshrc`.

```sh
./gradlew assembleDebug assembleRelease testDebugUnitTest lint
uvx git-cliff -o CHANGELOG.md   # regenerate the internal changelog
scripts/release.sh 0.2.0        # bump version, changelog, commit and tag
```

## Git and releases

- Conventional commits (`feat:`, `fix:`, `docs:`, `chore:`, `build:`, `ci:`, `test:`, `refactor:`). They generate `CHANGELOG.md`.
- Work on a branch (`feat/...`, `fix/...`, `docs/...`, `chore/...`) and merge by pull request with rebase, so each commit stays in the history. Work is tracked in GitHub Issues.
- No AI attribution in commits, pull requests or changelogs.
- Version lives in `gradle.properties` (`VERSION_NAME`, `VERSION_CODE`). Tags are the bare version name, no `v`.
- User-facing release notes are written by hand in `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`: plain words, 500 bytes max. They are not generated from commits.
- Releasing: write `changelogs/<next versionCode>.txt`, run `scripts/release.sh <version>` on `main`, then push `main` and the tag. The Release workflow checks the build is reproducible, signs it and publishes the GitHub release.
- The release key lives outside the repo. Never print, copy or commit it or its password.
- The package ID `io.github.nikoapps.clock` must never change.
- This repo is private. Never change its visibility.
