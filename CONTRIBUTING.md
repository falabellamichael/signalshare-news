# Contributing

Please open an issue for substantial behavior changes before implementing them. Preserve the existing reader state, offline snapshots, background-fetch behavior and complication identities.

- Keep patches focused and include a clear description of the behavior change.
- Use the Gradle wrapper with JDK 21 and Android SDK 36.
- Run relevant checks with `./gradlew testDebugUnitTest lintDebug assembleDebug`.
- For reader or complication changes, exercise the affected flow on a round Wear OS emulator or physical watch.
- Do not submit API credentials, signing keys, personal data, SDK paths, Gradle caches or generated APK/AAB files.
- Retain third-party notices and comply with the licenses of any added assets or dependencies.

Contributions intentionally submitted for inclusion are provided under this project's Apache-2.0 license, unless explicitly stated otherwise as described by the license. Do not include code or assets that you cannot license for this project.
