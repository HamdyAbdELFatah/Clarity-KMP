# Clarity iOS interop headers

This directory holds the hand-written Objective-C ABI shim (`ClarityInterop.h`)
used by the Kotlin/Native `clarityInterop` cinterop definition (`../clarity.def`).

## Why a shim exists

The Microsoft Clarity iOS SDK ships only as a binary `Clarity.xcframework` (Swift
module `Clarity`). Rather than commit that ~133 MB binary into the library, this
project ships a **forward-declaration header** that matches the SDK's
Objective-C surface (as exposed by its generated `-Swift.h`) and relies on
`linkerOpts("-undefined", "dynamic_lookup")` to resolve the real symbols at the
host app's link time. The host iOS app links the real Clarity SDK via Swift
Package Manager or CocoaPods.

## Pinned version

This shim targets **Microsoft Clarity iOS SDK 3.5.3**. It must stay in sync with
the `clarity-ios-sdk` version pinned in `gradle/libs.versions.toml`.

## Why selector correctness is critical

Objective-C dispatches by selector at runtime, not at compile time. Combined with
`dynamic_lookup` linking, **a mismatched return type or nullability (e.g.
declaring a `void` method as `BOOL`) will compile and link cleanly but read an
undefined register or crash at runtime.** There is no compile-time safety net,
so every selector, return type, and nullability annotation below must match the
real `-Swift.h` exactly.

## Upgrade procedure

When bumping the Clarity iOS SDK version:

1. Obtain the new `Clarity.xcframework` and open its generated `-Swift.h`
   (inside the framework bundle).
2. Diff every selector, return type, and nullability in `ClarityInterop.h`
   against the real header. Pay special attention to `BOOL` vs `void` and
   nullable bridging (which determines Kotlin `Boolean` vs `Unit` vs nullable).
3. If the Swift class names changed, update the `objc_runtime_name` attributes
   (these carry the Swift mangling, e.g. `_TtC7Clarity10ClaritySDK`).
4. Update the `clarity-ios-sdk` version in `gradle/libs.versions.toml` and the
   `TARGETS:` comment at the top of `ClarityInterop.h`.
5. Verify:
   - `./gradlew :clarity-kmp:compileKotlinIosSimulatorArm64`
   - `./gradlew :clarity-kmp:iosSimulatorArm64Test`
   - Link the sample app against the new SDK in Xcode and run it.

## Reference

- Microsoft Clarity iOS SDK docs:
  https://learn.microsoft.com/en-us/clarity/mobile-sdk/ios-sdk
- SPM package: https://github.com/microsoft/clarity-apps
