# Consumer ProGuard/R8 rules for clarity-kmp-compose.
#
# This artifact transitively bundles the Microsoft Clarity Compose SDK (which in turn
# contains the core Clarity runtime). Clarity relies on reflection and runtime metadata,
# so these rules keep the underlying SDK classes from being stripped or renamed in
# consumer release builds.

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
