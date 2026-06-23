# Contributing

Use JDK 11+, Android SDK 35+, and Xcode with an iOS simulator. Keep platform SDK calls behind `ClaritySdkAdapter`; common code and Compose helpers must depend only on `ClarityClient`.

Before opening a change:

```bash
./gradlew allTests lintRelease apiCheck dokkaGenerate
```

Add a failing test before changing behavior. Run `./gradlew apiDump` only for intentional public API changes and explain the compatibility impact. Do not commit `local.properties`, build outputs, signing material, project IDs, or Microsoft binary artifacts.

