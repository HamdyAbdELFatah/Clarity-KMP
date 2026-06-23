package com.hamdy.clarity

import android.os.Build
import com.microsoft.clarity.models.LogLevel
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks the Android adapter's platform-glue logic that is testable without the
 * real Microsoft Clarity SDK on a device:
 *  - the exhaustive [ClarityLogLevel] -> [LogLevel] mapping (catches SDK enum drift),
 *  - the [Build.VERSION_CODES.Q] capture floor reflected by [ClaritySdkAdapter.isSupported].
 *
 * The real SDK methods are exercised only by the instrumented sample app; they are
 * intentionally not hit here because they require a live capture session on a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM]) // SDK 35 — Robolectric 4.15 supports up to 35
class AndroidClaritySdkAdapterTest {

    @Test
    fun mapsEveryClarityLogLevelToTheMicrosoftEquivalent() {
        // Exhaustive: a new ClarityLogLevel that isn't mapped would fail to compile
        // (the `when` in toAndroidLogLevel() is exhaustive). This test pins the
        // resulting values so a silent remapping is caught.
        val expected = mapOf(
            ClarityLogLevel.None to LogLevel.None,
            ClarityLogLevel.Error to LogLevel.Error,
            ClarityLogLevel.Warning to LogLevel.Warning,
            ClarityLogLevel.Info to LogLevel.Info,
            ClarityLogLevel.Debug to LogLevel.Debug,
            ClarityLogLevel.Verbose to LogLevel.Verbose,
        )

        ClarityLogLevel.entries.forEach { level ->
            assertEquals(expected.getValue(level), level.toAndroidLogLevel())
        }
    }

    @Test
    fun isSupportedReflectsTheCaptureFloor() {
        // Microsoft Clarity captures on Android API 29 (Q) and above.
        val adapter = AndroidClaritySdkAdapter(org.robolectric.RuntimeEnvironment.getApplication())
        assertEquals(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q, adapter.isSupported)
    }
}
