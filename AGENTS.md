# Notes for AI agents

- **Asked to install or set up the app on a TV?** Follow [`INSTALL.md`](INSTALL.md) step by step.
- **Working on the code?**
  - Kotlin + Jetpack Compose for TV (`androidx.tv:tv-material` 1.0), single `app` module, no DI, no database.
  - `data/`: `FilmRepository` (Cinemeta catalog, TMDB/OMDb extras, Wikipedia photos), `LocalStore` (JSON file), models.
  - `ui/`: `AppViewModel` holds navigation (`backStack`) and all state; one file per screen; `Theme.kt` has the
    palette, Inter typography and shared components (`PillButton`, `BlurredBackdrop`, badges).
  - Build: `scripts/build.sh` (debug) or `scripts/build.sh release`; JDK 17. Install: `scripts/install.sh <tv-ip>`.
  - The UI is driven by a D-pad: every new screen needs an initial `FocusRequester`, and anything the user should
    reach must be focusable. Test on a real TV or an Android TV emulator with `adb shell input keyevent …` and
    `adb exec-out screencap -p`.
  - Keep all UI text in English.
- **Never commit** `local.properties`, `.signing/`, `dist/` or any API key. Release APKs are built with `NO_KEYS=1`.
