package com.hamdy.clarity

import android.content.Context
import android.os.Build
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig as MicrosoftClarityConfig
import com.microsoft.clarity.models.LogLevel

/**
 * Creates and initializes a Clarity client.
 *
 * Thread safety is governed by [ClarityConfig.dispatchStrategy], enforced centrally by the
 * returned client: under [ClarityDispatchStrategy.EnforceMainThread] (the default) an off-main
 * call throws, and under [ClarityDispatchStrategy.DispatchToMain] it is posted to the Android
 * main thread. The adapter below no longer enforces the main thread per-call, so the dispatch
 * strategy can actually take effect.
 */
public fun createClarityClient(context: Context, config: ClarityConfig): ClarityClient {
    return DefaultClarityClient(config, AndroidClaritySdkAdapter(context.applicationContext))
}

internal class AndroidClaritySdkAdapter(
    private val context: Context,
) : ClaritySdkAdapter {
    // NOTE: main-thread enforcement used to live here (per-method checkMainThread). It now lives
    // once, in DefaultClarityClient, so that ClarityDispatchStrategy.DispatchToMain can hop calls
    // onto the main thread instead of throwing. The adapter is therefore thread-agnostic by
    // design; callers reach it only through the client, which guarantees main-thread execution.

    override val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun initialize(projectId: String, logLevel: ClarityLogLevel): Boolean =
        runCatching {
            Clarity.initialize(
                context,
                MicrosoftClarityConfig(projectId).apply { this.logLevel = logLevel.toAndroidLogLevel() },
            ) == true
        }.getOrDefault(false)

    override fun setOnSessionStartedCallback(callback: (String) -> Unit): Boolean =
        runCatching { Clarity.setOnSessionStartedCallback { callback(it) } == true }.getOrDefault(false)

    override fun setCustomUserId(value: String): Boolean = Clarity.setCustomUserId(value) == true

    override fun setCustomSessionId(value: String): Boolean = Clarity.setCustomSessionId(value) == true

    override fun setCurrentScreenName(value: String?): Boolean = Clarity.setCurrentScreenName(value) == true

    override fun sendCustomEvent(value: String): Boolean = Clarity.sendCustomEvent(value)

    override fun setCustomTag(key: String, values: Set<String>): Boolean =
        Clarity.setCustomTag(key, *values.toTypedArray())

    override fun pause(): Boolean = Clarity.pause() == true
    override fun resume(): Boolean = Clarity.resume() == true
    override fun isPaused(): Boolean = Clarity.isPaused() == true

    override fun startNewSession(callback: (String) -> Unit): Boolean =
        runCatching { Clarity.startNewSession { callback(it) } == true }.getOrDefault(false)

    override fun getCurrentSessionUrl(): String? = Clarity.getCurrentSessionUrl()

    override fun setConsent(adsStorage: Boolean, analyticsStorage: Boolean): Boolean =
        Clarity.consent(adsStorage, analyticsStorage) == true
}

internal fun ClarityLogLevel.toAndroidLogLevel(): LogLevel = when (this) {
    ClarityLogLevel.None -> LogLevel.None
    ClarityLogLevel.Error -> LogLevel.Error
    ClarityLogLevel.Warning -> LogLevel.Warning
    ClarityLogLevel.Info -> LogLevel.Info
    ClarityLogLevel.Debug -> LogLevel.Debug
    ClarityLogLevel.Verbose -> LogLevel.Verbose
}
