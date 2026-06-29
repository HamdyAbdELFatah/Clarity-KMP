@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hamdy.clarity

import platform.Foundation.NSThread
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS [MainThreadAccess] backed by GCD. The `actual` half of [defaultMainThreadAccess]; used by
 * [DefaultClarityClient] to dispatch off-main calls under [ClarityDispatchStrategy.DispatchToMain].
 */
internal class IosMainThreadAccess : MainThreadAccess {
    override val isMainThread: Boolean
        get() = NSThread.isMainThread

    override fun post(action: () -> Unit) {
        // dispatch_async schedules the block onto the main queue; it runs later on the main
        // thread, which is exactly the fire-and-forget hop DefaultClarityClient wants.
        dispatch_async(dispatch_get_main_queue()) { action() }
    }
}

internal actual fun defaultMainThreadAccess(): MainThreadAccess = IosMainThreadAccess()
