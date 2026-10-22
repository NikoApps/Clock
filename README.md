# Niko Clock

<img src="fastlane/metadata/android/en-US/images/icon.png" width="96" alt="Niko Clock icon">

Hear the time out loud. Niko Clock is for people who can't easily read a screen: people who can't read, older people and people with low vision. Part of [Niko](https://nikoapps.github.io): simple, free Android apps with no ads and no tracking.

More about the app, in plain words: **[nikoapps.github.io/clock](https://nikoapps.github.io/clock/)**

**Status:** first public test release. Not on F-Droid yet.

## Download

Get the latest APK from [Releases](https://github.com/NikoApps/Clock/releases), or add this repo to [Obtainium](https://github.com/ImranR98/Obtainium) to get updates. Android 8.0 or newer.

## What it does

- Shows the time with seconds and the date in large text
- Speaks the time when you tap the clock or press the big button. Tap twice for the date
- Says the time by itself every hour, half hour or quarter hour, with quiet hours at night and a volume of its own
- Shake the phone to hear the time, even with the screen off. Shake again for the date
- Quick settings tiles for the time and the date, and a home-screen widget
- Speaks English, Nepali, Hindi, Spanish and Portuguese in natural sentences, using the phone's own voice
- Screen text in English, Nepali, Hindi, Bengali, Urdu, Arabic, Spanish, Portuguese, French and Indonesian
- Settings for helpers behind an optional PIN: languages, speed, pitch, text size and more
- No internet permission, no ads, no tracking

## Building

Requires JDK 17 and the Android SDK. Shared code comes from [Niko Commons](https://github.com/NikoApps/Commons) as a git submodule, so clone with `--recursive`, or run `git submodule update --init` in an existing clone.

```sh
git clone --recursive https://github.com/NikoApps/Clock.git
./gradlew assembleDebug
```

Release builds are reproducible and signed in CI from version tags.

## Contributing

See the [contributing guide](https://github.com/NikoApps/.github/blob/main/CONTRIBUTING.md). Report problems in [Issues](https://github.com/NikoApps/Clock/issues). Testing with someone who would actually use the app is the most useful help right now.

Much of the code is written with AI assistance and reviewed and tested on real phones by the maintainer.

## Licence

GNU General Public License v3.0. See [LICENSE](LICENSE).
