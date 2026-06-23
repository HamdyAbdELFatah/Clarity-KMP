package com.hamdy.clarity.sample

import androidx.compose.ui.window.ComposeUIViewController
import com.hamdy.clarity.ClarityConfig
import com.hamdy.clarity.ClarityLogLevel
import com.hamdy.clarity.createClarityClient
import platform.Foundation.NSLog
import platform.UIKit.UIViewController

private const val PLACEHOLDER_PROJECT_ID = "YOUR_PROJECT_ID"

/**
 * iOS entry point for the sample app.
 *
 * Initializes Clarity using the common API and launches the Compose UI.
 *
 * **Important:** The host iOS app must link the official Microsoft Clarity
 * iOS SDK (via CocoaPods or SPM) for tracking to work.
 */
fun MainViewController(): UIViewController {
    // Replace with your actual Clarity project ID.
    val projectId = PLACEHOLDER_PROJECT_ID
    if (projectId == PLACEHOLDER_PROJECT_ID) {
        NSLog("Clarity projectId is still the placeholder; no data will be captured. Set your real project ID.")
    }
    val clarityClient = createClarityClient(
        config = ClarityConfig(
            projectId = projectId,
            enabled = true,
            logLevel = ClarityLogLevel.Debug,
        ),
    )

    return ComposeUIViewController {
        App(clarityClient)
    }
}
