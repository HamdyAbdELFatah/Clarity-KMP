# Consumer ProGuard/R8 rules for clarity-kmp.
#
# Microsoft Clarity's Android SDK uses reflection and runtime metadata that R8 can
# strip or rename. Because this library bundles Clarity as an implementation dependency,
# consumers cannot add keep rules for it themselves without knowing the transitive
# coordinate. These rules ship with the AAR and are merged into the consuming app's
# minification configuration automatically.

# Keep all Clarity SDK classes and members so that reflection-based initialization
# and session capture continue to work in release builds.
-keep class com.microsoft.clarity.** { *; }
-keepclassmembers class com.microsoft.clarity.** { *; }

# Keep the Clarity SDK's public entry points used by this library's adapter.
# These are already covered by the package-wide rule above, but are listed explicitly
# to make the adapter's contract obvious and resilient to future package refactors.
-keep class com.microsoft.clarity.Clarity { *; }
-keep class com.microsoft.clarity.ClarityConfig { *; }
-keep class com.microsoft.clarity.models.LogLevel { *; }
