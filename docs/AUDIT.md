# Production Readiness Audit

## Resolved

- Replaced the global `expect object` and Android no-op initializer with injectable platform-created clients.
- Added observable lifecycle states and retained SDK success/failure results.
- Delayed initial identifiers and tags until the SDK session callback.
- Added official length validation, nullable screen reset, custom session IDs, multi-value tags, pause/resume, session callbacks, new sessions, session URLs, and consent.
- Added runtime OS support checks and main-thread enforcement.
- Updated Android SDKs to 3.8.2 and added the required Compose SDK without publishing duplicate Android runtimes.
- Removed the 133 MB checked-in Microsoft XCFramework and updated the iOS ABI shim for Clarity 3.5.3.
- Added explicit API mode, binary API dumps, Dokka, tag/property-derived versions, CI, and consumer documentation.
- Replaced iOS tests that initialized the real analytics SDK with adapter-driven common tests.

## Constraints

- iOS applications must link Microsoft's Clarity product through SPM or CocoaPods; Maven metadata cannot express an Xcode package dependency.
- The iOS C interop header is a version-pinned ABI shim. The sample host link is the compatibility check for the official binary.
- Microsoft publishes `clarity` and `clarity-compose` with the same Android namespace. The Compose artifact excludes the core Android runtime and uses `clarity-compose` alone.
- Clarity initialization is asynchronous. An accepted initialization is not an active session until the callback fires.
- Consent APIs differ: Android accepts analytics and ads choices, while iOS 3.5.x accepts analytics only.

## Release Gates

- Android and common tests pass.
- iOS sources and consumer framework link for simulator and device.
- Android lint, API compatibility, Dokka, Maven publications, and sample builds pass.
- The SwiftUI sample resolves Clarity 3.5.3 and links the generated Compose framework.

