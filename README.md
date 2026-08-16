# 📊 Clarity KMP (Compose Multiplatform)

> **Use Microsoft Clarity in Compose Multiplatform with declarative, simple Kotlin APIs.**  
> Automatically track screens, click events, and session tags across Android and iOS.

> **Disclaimer:** This is an **unofficial** community library and is not affiliated with,
> endorsed by, or sponsored by Microsoft. "Clarity" is a trademark of Microsoft Corporation.
> All platform SDKs are the property of their respective owners.

---

## 🧩 Why Clarity KMP?

The raw Microsoft SDK requires you to manage initialization callbacks, thread safety, value
length limits, and lifecycle plumbing yourself — on two different platforms. This wrapper
removes all of that:

| Task | Raw Microsoft SDK | With Clarity KMP |
|---|---|---|
| Initialize | Handle async init + session callback manually | `createClarityClient(config)` — done |
| Tag user before init finishes | Silently dropped ❌ | Buffered & replayed on session start ✅ |
| Track a screen | Manual set/clear on enter & exit | `ClarityScreen("Name") { }` |
| Track a tap | Custom click handling + event call | `Modifier.clarityClickable("name") { }` |
| Tag a session | Manual call on every screen | `Modifier.clarityTag("plan", "gold")` |
| Thread safety | Your responsibility | Enforced (or auto-dispatched to main) |
| Value length limits | Docs only — rejected silently | Validated up front, fail-fast |
| R8/ProGuard keep rules | You find & add them | Shipped with the artifact |
| Compose previews/tests | SDK crashes or no-ops | Safe `noOpClarityClient()` default |
| Check if recording | N/A | `rememberClarityState()` |

---

## ⚡ Quick Start: Add Dependency

Add the Compose Multiplatform artifact to your `commonMain` dependencies in your `build.gradle.kts` file:

```kotlin
commonMain.dependencies {
    // Primary dependency for Compose Multiplatform apps
    implementation("io.github.hamdyabdelfatah:clarity-kmp-compose:0.1.0")
}
```

---

## 🎨 Compose Multiplatform Usage (Primary)

Simply wrap your app content in `ClarityProvider` and use the built-in tracking components:

```kotlin
// 1. Provide the client to your Composable tree
ClarityProvider(clarityClient) {
    
    // 2. Automatically track screen entry & exit
    ClarityScreen("CheckoutPage") {
        
        Button(
            modifier = Modifier
                // 3. Tag the session when this button is composed
                .clarityTag("tier", "gold")            
                // 4. Track click events automatically
                .clarityClickable("buy_now_clicked") { 
                    processPurchase()
                },
        ) {
            Text("Buy Now")
        }
    }
}
```

### 🎯 Additional Compose Helpers

*   **Track One-time Impressions:** Fire an event once when a composable enters the composition (perfect for tracking ads or popups):
    ```kotlin
    TrackClarityEvent("promo_banner_shown")
    ```
*   **Observe Clarity State Reactively:** Check if Clarity is active or paused directly in your UI:
    ```kotlin
    val state by rememberClarityState()
    if (state == ClarityState.Active) {
        Text("Recording session...")
    }
    ```

---

## 🚀 Easy Setup Guide

### 📋 Minimum Requirements

| Requirement | Version |
|---|---|
| Kotlin | 2.x |
| Compose Multiplatform | 1.x |
| Android minSdk | 24 (API 24) — capture needs **API 29+** (Android 10) |
| iOS deployment target | 15.0 |
| Microsoft Clarity iOS SDK | 3.5.3+ (linked via SPM/CocoaPods, see below) |

Below the capture floors (Android 10 / iOS 15) the app runs safely but records nothing —
the client reports `ClarityState.Unsupported`.

### 🤖 Android Setup (Zero Config!)
Gradle pulls Microsoft's native Android Compose SDK automatically. 

1. Create and initialize the client inside your `Application.onCreate` (must be on the main UI thread):
```kotlin
class MyApp : Application() {
    lateinit var clarity: ClarityClient
        private set

    override fun onCreate() {
        super.onCreate()
        clarity = createClarityClient(
            context = this,
            config = ClarityConfig(
                projectId = "YOUR_PROJECT_ID", // Get this from your Clarity Dashboard
                enabled = !BuildConfig.DEBUG,   // Turn off in debug/development builds
                logLevel = ClarityLogLevel.None,
            ),
        )
    }
}
```
2. Pass the `clarity` instance into your shared KMP `App(clarity)` entry point. Done! 🎉

---

### 🍎 iOS Setup (1 Easy Step!)
Because Apple's compiler doesn't let KMP libraries package native compiled binaries inside them, you must link Microsoft's native iOS SDK:

1. **Add Dependency in Xcode:**
   - Open your project in Xcode.
   - Go to your target settings → **Package Dependencies**.
   - Add `https://github.com/microsoft/clarity-apps` (Version `3.5.3` or newer).
   - Tick the **`Clarity`** library checkbox.

2. **Initialize in your iOS Main Controller:**
   In your shared module's `iosMain` source set (e.g., where you define `MainViewController`):
```kotlin
val clarity = createClarityClient(
    config = ClarityConfig(projectId = "YOUR_PROJECT_ID")
)

fun MainViewController() = ComposeUIViewController {
    App(clarity)
}
```
3. Done! SwiftUI setup is ready. ✅

---

## 🧭 General API Methods

You can call these general methods on the `clarity` client instance inside your shared code:

### 1. Identify Users
```kotlin
// Tag your user with their login ID
clarity.setCustomUserId("user_abc_123")
```

### 2. Override Session IDs (Link to your own analytics logs)
```kotlin
clarity.setCustomSessionId("checkout-session-99")
```

### 3. Track Custom Events
```kotlin
clarity.trackEvent("purchase_completed")
```

### 4. Tag Sessions
```kotlin
clarity.tag("plan", "premium")
```

### 5. Privacy & GDPR Consent
```kotlin
// Apply user consent preferences
clarity.setConsent(ClarityConsent(analyticsStorage = true))
```

### 6. Tracking from ViewModels / Business Logic (No Compose Required)

The fluent API works anywhere you have the `ClarityClient` — ViewModels, repositories, use cases:

```kotlin
class CheckoutViewModel(private val clarity: ClarityClient) {

    fun checkoutStarted() {
        clarity.withScreen("Checkout") {   // sets screen for the block, resets after
            clarity.tag("plan", "premium") // fluent alias for setCustomTag
        }
    }

    fun purchaseCompleted() {
        clarity.trackEvent("purchase_completed")
        clarity.userId("user_abc_123")     // fluent alias for setCustomUserId
    }

    fun onAppBackgrounded() {
        clarity.ifActive {                 // guard: only runs while recording
            clarity.pause()
        }
    }
}
```

Every call returns `Boolean` (`true` = accepted/applied), so you can log or react to failures
without exceptions. Batch multiple events/tags in one call with `sendEvents(...)` /
`setTags(...)`.

---

## 🤖 AI Assistant / Prompt Guide

If you are using an AI Coding Assistant (such as Copilot, Cursor, or ChatGPT) to write tracking code in your app, copy and paste the prompt block below to train the model on how to use this library:

```markdown
We are using the `Clarity KMP` library to track analytics. Here is how you should write tracking code:
1. Always inject the `ClarityClient` instance to write tracking logic.
2. For screen views, wrap each screen in `ClarityScreen(name) { ... }`.
3. To tag a session from a Composable, use `Modifier.clarityTag(key, value)`.
4. For tracking clicks/taps on layouts, cards, or custom buttons, use the `Modifier.clarityClickable(eventName) { onClick() }` helper instead of a standard `Modifier.clickable`.
5. To log a one-time impression event when a composable shows up, use `TrackClarityEvent(eventName)`.
6. To set custom user IDs, use `clarity.userId(id)`.
7. To track custom events in ViewModels or other shared code, use `clarity.trackEvent(eventName)`.
8. Do not import native com.microsoft.clarity.* classes in commonMain. Use the shared KMP com.hamdy.clarity.* classes.
```

---

## ⚠️ Legacy / Alternative Setup (Non-Compose / Views Only)

If you are working on a legacy Android View-based app, or a pure UIKit/SwiftUI iOS app with **no Compose Multiplatform**, use the core lightweight module instead.

### 1. Add Core Dependency
```kotlin
commonMain.dependencies {
    implementation("io.github.hamdyabdelfatah:clarity-kmp:0.1.0")
}
```

### 2. Manual Screen Tracking
Since you don't have Compose, you must set and clear screen names manually:
```kotlin
// On Screen Enter
clarity.screen("Dashboard")

// On Screen Exit
clarity.screen(null)
```
*Or use the block-scoped helper:*
```kotlin
clarity.withScreen("Dashboard") {
    // Runs block and automatically resets screen name to null when done
}
```

---

## ❓ Simple Troubleshooting

*   **My events/screens aren't showing up?**
    *   First, check the client state in your UI: `val state by rememberClarityState()` — it must be
        `ClarityState.Active` for calls to be recorded. If it stays `InitializationAccepted`, calls
        made before activation are buffered and applied on session start (default behavior).
    *   If state is `Failed` or `Unsupported`, see the entries below. In debug builds, set
        `logLevel = ClarityLogLevel.Debug` on the config to see native SDK logs.
    *   Remember that `projectId` must be your real Clarity dashboard project ID — the sample's
        `YOUR_PROJECT_ID` placeholder records nothing.
*   **Status is `Unsupported`?**
    *   Microsoft Clarity only records data on **Android 10+ (API 29+)** and **iOS 15+**. Below these versions, the app runs safely without crashing, but it won't record anything.
*   **iOS Build fails with `Undefined symbols`?**
    *   Make sure you added `https://github.com/microsoft/clarity-apps` to your Package Dependencies in Xcode.
*   **Duplicate classes errors on Android?**
    *   If you mixed both modules, exclude the duplicate Microsoft core classes in your Android dependencies:
    ```kotlin
    implementation("io.github.hamdyabdelfatah:clarity-kmp") {
        exclude(group = "com.microsoft.clarity", module = "clarity")
    }
    ```
