package com.hamdy.clarity.sample

import android.app.Application
import android.util.Log
import com.hamdy.clarity.ClarityClient
import com.hamdy.clarity.ClarityConfig
import com.hamdy.clarity.ClarityLogLevel
import com.hamdy.clarity.createClarityClient

class SampleApplication : Application() {
    lateinit var clarityClient: ClarityClient
        private set

    override fun onCreate() {
        super.onCreate()

        // Replace with your actual Clarity project ID.
        // In production, use BuildKonfig or a build config field — never hardcode.
        val projectId = "YOUR_PROJECT_ID"
        if (projectId == PLACEHOLDER_PROJECT_ID) {
            Log.w(TAG, "Clarity projectId is still the placeholder; no data will be captured. Set your real project ID.")
        }
        clarityClient = createClarityClient(
            context = this,
            config = ClarityConfig(
                projectId = projectId,
                enabled = true,
                logLevel = ClarityLogLevel.Debug,
            ),
        )
    }

    private companion object {
        const val TAG = "ClaritySample"
        const val PLACEHOLDER_PROJECT_ID = "YOUR_PROJECT_ID"
    }
}
