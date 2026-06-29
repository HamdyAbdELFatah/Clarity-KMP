package com.hamdy.clarity

import android.os.Handler
import android.os.Looper

/**
 * Android [MainThreadAccess] backed by a [Handler] bound to the main [Looper]. The `actual`
 * half of [defaultMainThreadAccess]; used by [DefaultClarityClient] to dispatch off-main calls
 * under [ClarityDispatchStrategy.DispatchToMain].
 *
 * `Looper.getMainLooper()` is accessed defensively: on a real device it always exists, but the
 * pure-JVM host tests (Robolectric isn't on the `commonTest` classpath) see an unmocked stub
 * that throws. We treat "can't determine the main looper" as "we're effectively on the only
 * thread there is", i.e. report `isMainThread = true` so the fast inline path runs — which is
 * exactly correct for a single-threaded test. The [Handler] is created lazily for the same
 * reason, and only ever reached when a caller actually posts off-main.
 */
internal class AndroidMainThreadAccess : MainThreadAccess {
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    override val isMainThread: Boolean
        get() = mainLooperOrNull()?.let { Looper.myLooper() == it } ?: true

    override fun post(action: () -> Unit) {
        mainHandler.post(action)
    }

    private fun mainLooperOrNull(): Looper? = try {
        Looper.getMainLooper()
    } catch (expected: RuntimeException) {
        // Unmocked stub in pure-JVM host tests — there is no main thread to distinguish from.
        null
    }
}

internal actual fun defaultMainThreadAccess(): MainThreadAccess = AndroidMainThreadAccess()
