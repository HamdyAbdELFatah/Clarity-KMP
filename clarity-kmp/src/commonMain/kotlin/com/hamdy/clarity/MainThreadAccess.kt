package com.hamdy.clarity

/**
 * The platform abstraction over the main thread. The production binding (Android/iOS `actual`s
 * behind [defaultMainThreadAccess]) queries and hops to the platform main thread; tests inject
 * a fake that records posts and runs them synchronously, so the pure-common tests stay
 * dependency-free and deterministic.
 *
 * This mirrors the [ClaritySdkAdapter] DI seam: an `internal` interface in common code that the
 * client depends on, with a platform-provided default and an injectable test double. The
 * platform default is supplied by an `expect`/`actual` factory ([defaultMainThreadAccess]) —
 * that expect/actual pair is what carries the real `postToMain` behavior per platform.
 */
internal interface MainThreadAccess {
    /** `true` when the calling thread is the platform main (UI) thread. */
    val isMainThread: Boolean

    /**
     * Runs [action] on the platform main thread, asynchronously. If already on the main
     * thread callers should run inline instead of calling this — this always schedules.
     */
    fun post(action: () -> Unit)
}

/**
 * Platform-specific production [MainThreadAccess]: Android `Handler` on the main `Looper`,
 * iOS `dispatch_async(main)`. Declared `expect` so the common client takes no hard dependency
 * on `android.os` / `platform.Foundation`.
 */
internal expect fun defaultMainThreadAccess(): MainThreadAccess
