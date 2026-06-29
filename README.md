# Clarity KMP

**Use Microsoft Clarity in Kotlin Multiplatform with one shared API.**

Microsoft ships two separate native SDKs — one for Android, one for iOS — with
different APIs and different return types. This library wraps both behind a single
`ClarityClient` interface, so you write your analytics code **once** in `commonMain`
and it runs on both platforms.

> Not affiliated with or endorsed by Microsoft.

---

## ✨ What you get

- **One API on both platforms** — `sendCustomEvent("…")`, `setCustomTag(…)`, `setCurrentScreenName(…)` all work from shared Kotlin.
- **No more dual maintenance** — no per-platform wrapper code for Clarity.
- **Safe by default** — a no-op client for previews/tests, input validation, a real state machine, and main-thread enforcement.
- **Compose helpers** — declarative screen + tap tracking for Compose Multiplatform (`clarity-kmp-compose`).

## 📋 Requirements

| Platform | Min target | Clarity captures data from |
|---|---|---|
| Android | `minSdk` 24 | API 29+ |
| iOS | iOS 15 | iOS 15+ |

Below the capture floor the client still constructs and runs without crashing, but reports `ClarityState.Unsupported` and records nothing — so you can keep a lower deployment target than the capture floor.

## 📦 Installation

Add **one** of the two artifacts to your `commonMain`:

**Option A — Core (views / no Compose):**

```kotlin
commonMain.dependencies {
    implementation("com.hamdy.clarity:clarity-kmp:0.1.0")
}
```

**Option B — Compose Multiplatform (recommended for Compose apps):**

```kotlin
commonMain.dependencies {
    implementation("com.hamdy.clarity:clarity-kmp-compose:0.1.0")
}
```

The Compose artifact already includes `clarity-kmp` transitively. **Don't add both** artifacts — pick one.

---

## 🚀 Setup

### Android (automatic)

Nothing to configure. When you add the dependency, Gradle pulls Microsoft's
Android Clarity SDK automatically — there is no manual step.

1. Create the client on the main thread in `Application.onCreate`:

```kotlin
class App : Application() {
    lateinit var clarity: ClarityClient
        private set

    override fun onCreate() {
        super.onCreate()
        clarity = createClarityClient(
            context = this,
            config = ClarityConfig(
                projectId = "YOUR_PROJECT_ID",
                enabled = !BuildConfig.DEBUG,   // off in debug builds
                logLevel = ClarityLogLevel.None,
            ),
        )
    }
}
```

2. Pass it into your shared code. Done. ✅

> **Using the Compose artifact on Android?** Your app module must exclude the
> standalone `com.microsoft.clarity:clarity` to avoid duplicate classes (the
> Compose artifact already bundles them):
> ```kotlin
> implementation("com.hamdy.clarity:clarity-kmp") {
>     exclude(group = "com.microsoft.clarity", module = "clarity")
> }
> ```

### iOS (one manual step)

**Why one extra step?** A Kotlin Multiplatform library ships as an `.xcframework`
of *compiled Kotlin*. Apple's tooling does not let a KMP library re-distribute a
third-party native SDK binary inside it. So the Maven artifact contains only the
Kotlin bindings — you must link Microsoft's iOS Clarity binary once, by hand. This
is a platform constraint, not a library limitation; every KMP wrapper around a
native iOS SDK (Firebase, Mapbox, …) works the same way.

1. **Link Microsoft's iOS Clarity SDK.** In Xcode, go to your app target →
   **Package Dependencies** → add `https://github.com/microsoft/clarity-apps`
   (version `3.5.3` or newer), and tick the **`Clarity`** product.

   CocoaPods alternative: `pod 'Clarity', '~> 3.5'`.

2. **Create the client from the iOS main thread** (in your shared module's
   `iosMain` entry point, before building the UI):

```kotlin
val clarity = createClarityClient(
    ClarityConfig(projectId = "YOUR_PROJECT_ID")
)
```

3. Done. ✅ See `sample/iosApp` for a complete SwiftUI host.

---

## 🧭 Track from shared code

Inject `ClarityClient` into your shared code rather than using globals:

```kotlin
// Works on Android AND iOS — written once in commonMain
clarity.setOnSessionStartedCallback { sessionId ->
    println("Clarity session: $sessionId")
}
clarity.setCustomUserId("non-pii-user-id")
clarity.setCustomSessionId("checkout-42")
clarity.setCustomTag("plan", setOf("premium", "annual"))
clarity.setCurrentScreenName("Checkout")
clarity.sendCustomEvent("purchase_submitted")
```

**Rules of thumb:**

- Every mutating method returns `Boolean` — `true` means accepted, `false` means the client isn't active yet or the input was invalid. It never throws.
- With `bufferUntilActive` (default), metadata calls made before `Active` are buffered and replayed on session start, so you don't need to wait for `Active` to tag/identify a user. Point-in-time calls (`sendCustomEvent`, `pause`/`resume`) are never buffered and still require `Active`.
- Initial `customUserId` / `customSessionId` / `customTags` set in `ClarityConfig` are re-applied automatically on every new session.
- `pause()` / `resume()` control capture at runtime; `enabled = false` in config gives you a disabled client that records nothing.
- **All APIs run on the platform main thread.** Under the default `EnforceMainThread` strategy, off-main calls throw; switch to `DispatchToMain` to make them safe from background threads.

## 🎨 Compose helpers (`clarity-kmp-compose`)

```kotlin
ClarityProvider(clarity) {                       // make client available below
    ClarityScreen("Checkout") {                  // auto-reports this screen; resets on exit
        Button(
            modifier = Modifier.clarityTag("plan", "premium")   // tag the session
                .clarityClickable("purchase_clicked"),          // track taps
            onClick = ::purchase,
        ) { Text("Buy") }
    }
}

// Reactive lifecycle state in any composable
val state by rememberClarityState()
when (state) {
    ClarityState.Active -> Text("Recording")
    else -> Text("Idle")
}

TrackClarityEvent("home_viewed")                 // fire an event once on first composition
```

- `LocalClarityClient` defaults to a no-op client, so **previews and tests record nothing** unless you provide a real one.
- `ClarityScreen` reports its name on enter and, by default (`restoreOnExit = true`), clears it on exit so a navigated-away screen stops reporting.
- `rememberClarityState()` turns `ClarityClient.state` into reactive `State`, backed by `observeState` and disposed automatically.

## 📖 API reference

| API | Purpose |
|---|---|
| `createClarityClient(config)` (iOS) / `(context, config)` (Android) | Construct the client on the main thread |
| `noOpClarityClient()` | Safe no-op for previews/tests |
| `state` | Lifecycle: `NotInitialized → InitializationAccepted → Active ⇄ Paused` (or `Disabled` / `Unsupported` / `Failed`) |
| `isSupported` | `true` where Clarity captures data (Android API 29+, iOS 15+) |
| `setCustomUserId(id)` | Stitch sessions across devices for a user |
| `setCustomSessionId(id)` | Override the auto-generated session id |
| `setCurrentScreenName(name)` | Record current screen (dashboard) |
| `sendCustomEvent(name)` | Attach a custom event to the session |
| `setCustomTag(key, value(s))` | Tag the session (single or multi-value) |
| `pause()` / `resume()` | Runtime capture control |
| `startNewSession { }` | End the current session and start a fresh one |
| `getCurrentSessionUrl()` | Replay URL of the current session, or `null` |
| `setOnSessionStartedCallback { }` | Called on every new session |
| `setConsent(ClarityConsent)` | Apply GDPR consent (Android: analytics + ads; iOS: analytics only) |
| `observeState { }` | Subscribe to state transitions (immediate current value, then every change); returns a cancellable handle |

### Configuration options (`ClarityConfig`)

| Option | Default | Purpose |
|---|---|---|
| `bufferUntilActive` | `true` | Buffer idempotent metadata calls made before `Active` and replay them on session start (prevents data loss during async init) |
| `dispatchStrategy` | `EnforceMainThread` | `EnforceMainThread` throws on off-main calls (fail-fast); `DispatchToMain` posts them to the main thread and returns `true`/`null` instead |

### Compose helpers (`clarity-kmp-compose`)

| Helper | Purpose |
|---|---|
| `ClarityScreen(name, restoreOnExit = true)` | Report a screen name; clear it on exit by default |
| `rememberClarityState()` | `ClarityClient.state` as reactive Compose `State` |
| `Modifier.clarityTag(key, value)` | Tag the session from any node |
| `Modifier.clarityClickable(event)` | Track an event on click |
| `TrackClarityEvent(name)` | Fire an event once on first composition |

Limits (Microsoft's): IDs / tags / screen names ≤ 255 chars; event names ≤ 254 chars.

---

## ❓ Troubleshooting

- **`state` is `Unsupported`** — your OS is below the capture floor (Android < 29, iOS < 15). The SDK is present but records nothing.
- **`state` is `InitializationAccepted` but calls return `false`** — initialization was accepted but no session has started yet. Wait for `Active` / the session callback before sending metadata.
- **iOS build fails with undefined `Clarity*` symbols** — the host app didn't link Microsoft's iOS Clarity SDK. Redo the iOS step 1 (add the SPM package and select the `Clarity` product).
- **Duplicate `com.microsoft.clarity.*` classes on Android** — you added both the core and Compose artifacts, or both Microsoft Android SDKs. Use only `clarity-kmp-compose` and apply the `exclude` snippet above.

## 🔒 Privacy

- **Never send PII** — no names, emails, phone numbers, credentials, tokens, or health data.
- Obtain and store consent before calling `setConsent`. Android honors analytics **and** ads storage; iOS supports analytics storage only.
- Mask sensitive screens/controls via Microsoft's native SDK config. The Compose helpers do **not** override Microsoft dashboard masking rules.
- Clarity must not be used in apps directed to users under 18. Review Microsoft's current terms before release.

## 🧪 Sample app

A full working example lives in [`sample/`](sample/) — an Android app, a Compose Multiplatform module, and a SwiftUI iOS host that links the real Clarity SDK via SPM.

## 🛠️ Development

```bash
./gradlew allTests lintRelease apiCheck dokkaGenerate
./gradlew verifyPublishedConsumers
./gradlew :sample:composeApp:linkDebugFrameworkIosSimulatorArm64
```

See [CONTRIBUTING.md](CONTRIBUTING.md), [docs/RELEASING.md](docs/RELEASING.md), and [docs/AUDIT.md](docs/AUDIT.md).

## 📄 License

Apache-2.0. Microsoft Clarity is distributed separately under Microsoft's terms.
