package com.hamdy.clarity

import android.content.Context
import android.os.Build
import android.os.Looper
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig as MicrosoftClarityConfig
import com.microsoft.clarity.models.LogLevel

/** Creates and initializes a Clarity client. Call this on the Android main thread. */
public fun createClarityClient(context: Context, config: ClarityConfig): ClarityClient {
    checkMainThread()
    return DefaultClarityClient(config, AndroidClaritySdkAdapter(context.applicationContext))
}

internal class AndroidClaritySdkAdapter(
    private val context: Context,
) : ClaritySdkAdapter {
    override val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun initialize(projectId: String, logLevel: ClarityLogLevel): Boolean = onMainThread {
        runCatching {
            Clarity.initialize(
                context,
                MicrosoftClarityConfig(projectId).apply { this.logLevel = logLevel.toAndroidLogLevel() },
            ) == true
        }.getOrDefault(false)
    }

    override fun setOnSessionStartedCallback(callback: (String) -> Unit): Boolean = onMainThread {
        runCatching { Clarity.setOnSessionStartedCallback { callback(it) } == true }.getOrDefault(false)
    }

    override fun setCustomUserId(value: String): Boolean = onMainThread {
        Clarity.setCustomUserId(value) == true
    }

    override fun setCustomSessionId(value: String): Boolean = onMainThread {
        Clarity.setCustomSessionId(value) == true
    }

    override fun setCurrentScreenName(value: String?): Boolean = onMainThread {
        Clarity.setCurrentScreenName(value) == true
    }

    override fun sendCustomEvent(value: String): Boolean = onMainThread {
        Clarity.sendCustomEvent(value)
    }

    override fun setCustomTag(key: String, values: Set<String>): Boolean = onMainThread {
        Clarity.setCustomTag(key, *values.toTypedArray())
    }

    override fun pause(): Boolean = onMainThread { Clarity.pause() == true }
    override fun resume(): Boolean = onMainThread { Clarity.resume() == true }
    override fun isPaused(): Boolean = onMainThread { Clarity.isPaused() == true }

    override fun startNewSession(callback: (String) -> Unit): Boolean = onMainThread {
        runCatching { Clarity.startNewSession { callback(it) } == true }.getOrDefault(false)
    }

    override fun getCurrentSessionUrl(): String? = onMainThread { Clarity.getCurrentSessionUrl() }

    override fun setConsent(adsStorage: Boolean, analyticsStorage: Boolean): Boolean = onMainThread {
        Clarity.consent(adsStorage, analyticsStorage) == true
    }
}

private inline fun <T> onMainThread(block: () -> T): T {
    checkMainThread()
    return block()
}

private fun checkMainThread() {
    check(Looper.myLooper() == Looper.getMainLooper()) { "Clarity APIs must be called on the Android main thread." }
}

internal fun ClarityLogLevel.toAndroidLogLevel(): LogLevel = when (this) {
    ClarityLogLevel.None -> LogLevel.None
    ClarityLogLevel.Error -> LogLevel.Error
    ClarityLogLevel.Warning -> LogLevel.Warning
    ClarityLogLevel.Info -> LogLevel.Info
    ClarityLogLevel.Debug -> LogLevel.Debug
    ClarityLogLevel.Verbose -> LogLevel.Verbose
}
