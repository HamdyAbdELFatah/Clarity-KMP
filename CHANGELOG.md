# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-08-16

### Added

- **`clarity-kmp` core module**: a testable, platform-neutral `ClarityClient` for Microsoft Clarity
  covering Android and iOS, constructed via the platform `createClarityClient` factories.
- Observable lifecycle state machine (`ClarityState`: `NotInitialized` → `InitializationAccepted` →
  `Active` ⇄ `Paused`, plus terminal `Disabled` / `Unsupported` / `Failed`), with
  `observeState` / cancellable `ObserverHandle` subscriptions.
- Metadata buffering (`ClarityConfig.bufferUntilActive`): idempotent setters made before the first
  session becomes active are replayed on session start, in the order config → runtime → callback.
- Main-thread enforcement with `ClarityDispatchStrategy`: `EnforceMainThread` (fail-fast default)
  or `DispatchToMain` (off-main calls are posted to the platform main thread).
- Official Microsoft length validation (255 chars for values, 254 for event names) enforced at
  construction and per call, plus public string validation/truncation helpers.
- Full tracking surface: custom user/session ids, screen names (nullable reset), custom events,
  single- and multi-value tags, pause/resume, new sessions with callbacks, session replay URLs,
  and GDPR consent (`ClarityConsent`).
- Fluent extensions: `trackEvent`, `userId`, `sessionId`, `screen`, `tag`, batch `sendEvents` /
  `setTags`, scoped `withScreen`, and `ifActive` / `ifReady` guards.
- **`clarity-kmp-compose` module**: `ClarityProvider` (composition-local client), `ClarityScreen`
  (auto screen tracking with transition-safe reset), `Modifier.clarityClickable`,
  `Modifier.clarityTag`, `TrackClarityEvent` (one-shot impressions), and `rememberClarityState`
  (reactive state observation).
- Android adapter on the official Microsoft Clarity Android SDK (API 29+ capture floor), iOS
  adapter over a version-pinned cinterop ABI shim for the Clarity iOS SDK 3.5.3 (iOS 15+ floor),
  with `-undefined dynamic_lookup` test linking — consumers link the real iOS SDK via SPM/CocoaPods.
- Consumer ProGuard/R8 keep rules shipped with both artifacts so the transitive Microsoft SDK
  survives release minification.
- Binary API validation (KLIB dumps + `apiCheck`), Dokka documentation, a `consumer-tests` build
  that verifies the published artifacts from a consumer perspective, CI workflows (Android +
  Apple), and an interactive Compose Multiplatform sample app (Android + iOS).

