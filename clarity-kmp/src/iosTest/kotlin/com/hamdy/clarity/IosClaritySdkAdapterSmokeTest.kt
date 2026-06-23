@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hamdy.clarity

import platform.Foundation.NSProcessInfo
import kotlinx.cinterop.useContents
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Smoke test for the iOS adapter glue.
 *
 * The real Microsoft Clarity binary is not linked into the test runtime, so the
 * live SDK methods are not exercised here (they are validated by linking and
 * running the sample app in Xcode). This test instead provides:
 *
 *  1. **Selector presence** — the `iosTest` source set compiles against the
 *     cinterop bindings generated from `ClarityInterop.h`. Building
 *     `:clarity-kmp:compileTestKotlinIosSimulatorArm64` confirms every selector
 *     the adapter references still resolves.
 *  2. **`isSupported` floor** — [isHostIosSupported] reflects the host's actual
 *     OS version via [NSProcessInfo] against [MIN_IOS_MAJOR_VERSION].
 */
class IosClaritySdkAdapterSmokeTest {

    @Test
    fun isSupportedReflectsTheHostIosVersion() {
        val hostMajor = NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion }

        assertEquals(hostMajor >= MIN_IOS_MAJOR_VERSION, isHostIosSupported())
    }

    @Test
    fun captureFloorIsIosFifteen() {
        // Microsoft Clarity captures on iOS 15 and above.
        assertEquals(15, MIN_IOS_MAJOR_VERSION)
        // The CI host (macos-15) runs at least iOS 15 in the simulator.
        assertTrue(isHostIosSupported())
    }
}
