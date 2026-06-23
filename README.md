# Clarity KMP

Unofficial Kotlin Multiplatform bindings for the official Microsoft Clarity Android and iOS SDKs. The library supports Android Views, Android Compose, UIKit-hosted Compose Multiplatform, and common tracking code.

This project is not affiliated with or endorsed by Microsoft.

## Support

| Target | Library target / build minimum | Clarity data capture (`isSupported`) |
|---|---|---|
| Android | `minSdk` 24 | API 29–36 |
| iOS | iOS 15 (sample `IPHONEOS_DEPLOYMENT_TARGET`) | iOS 15–18 |

`ClarityClient.isSupported` reports `true` only where Microsoft Clarity captures
data (Android API 29+, iOS 15+). On older OS versions the client constructs and
runs without crashing but reports `ClarityState.Unsupported` and records nothing,
so you may set a lower deployment target than the capture floor if you need to
support those devices.

## Installation

```kotlin
commonMain.dependencies {
    implementation("com.hamdy.clarity:clarity-kmp:0.1.0")
    // Use instead of the core dependency when using Compose Multiplatform:
    implementation("com.hamdy.clarity:clarity-kmp-compose:0.1.0")
}
```

The Compose artifact supplies `clarity-kmp` transitively and uses Microsoft's `clarity-compose` SDK on Android. Do not add both project artifacts or both Microsoft Android SDK artifacts manually.

### Android

Create one client on the main thread in `Application.onCreate`:

```kotlin
class App : Application() {
    lateinit var clarity: ClarityClient
        private set

    override fun onCreate() {
        super.onCreate()
        clarity = createClarityClient(
            context = this,
            config = ClarityConfig(
                projectId = BuildConfig.CLARITY_PROJECT_ID,
                enabled = !BuildConfig.DEBUG,
                logLevel = ClarityLogLevel.None,
            ),
        )
    }
}
```

### iOS

The Maven artifact contains Kotlin bindings, not Microsoft's binary. Link `https://github.com/microsoft/clarity-apps` version `3.5.3` or newer to the host target and select its `Clarity` product. CocoaPods users can add `pod 'Clarity', '~> 3.5'`.

Create the client from the iOS main thread before constructing shared UI:

```kotlin
val clarity = createClarityClient(
    ClarityConfig(projectId = "YOUR_PROJECT_ID")
)
```

See `sample/iosApp` for the complete SwiftUI/SPM host.

## Common Usage

Inject `ClarityClient` into shared code rather than reading global state:

```kotlin
clarity.setOnSessionStartedCallback { sessionId ->
    println("Clarity session: $sessionId")
}
clarity.setCustomUserId("non-pii-user-id")
clarity.setCustomSessionId("checkout-42")
clarity.setCustomTag("plan", setOf("premium", "annual"))
clarity.setCurrentScreenName("Checkout")
clarity.sendCustomEvent("purchase_submitted")
```

Operations return `true` only when accepted. `InitializationAccepted` means the asynchronous SDK initialization request was accepted; wait for `Active` or the session callback before sending session metadata. Initial IDs and tags in `ClarityConfig` are applied automatically when a session starts.

Use `pause()` and `resume()` for runtime capture control. `enabled=false` prevents SDK initialization and returns a disabled client. All APIs must be invoked on the platform main thread.

## Compose

```kotlin
ClarityProvider(clarity) {
    ClarityScreen("Checkout") {
        Button(
            modifier = Modifier.clarityClickable("purchase_clicked"),
            onClick = ::purchase,
        ) { Text("Buy") }
    }
}
```

`LocalClarityClient` defaults to a no-op client, so previews and tests do not record data. `TrackClarityEventOnFirstComposition` emits once per composition lifecycle.

## Privacy

- Never send names, email addresses, phone numbers, credentials, tokens, health data, or other PII.
- Obtain and store consent before calling `setConsent`. Android maps analytics and ads storage; iOS currently supports analytics storage only.
- Mask sensitive screens and controls using the native Microsoft SDK configuration. Compose helpers do not override Microsoft dashboard masking rules.
- Clarity must not be used in apps directed to users under 18. Review Microsoft's current terms and platform documentation before release.

## Development

```bash
./gradlew allTests lintRelease apiCheck dokkaGenerate
./gradlew verifyPublishedConsumers
./gradlew :sample:composeApp:linkDebugFrameworkIosSimulatorArm64
```

See [CONTRIBUTING.md](CONTRIBUTING.md), [docs/RELEASING.md](docs/RELEASING.md), and [docs/AUDIT.md](docs/AUDIT.md).

## License

Apache-2.0. Microsoft Clarity is distributed separately under Microsoft's terms.
