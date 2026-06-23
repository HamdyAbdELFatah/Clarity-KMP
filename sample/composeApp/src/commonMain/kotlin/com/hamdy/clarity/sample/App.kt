package com.hamdy.clarity.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hamdy.clarity.ClarityClient
import com.hamdy.clarity.compose.ClarityProvider
import com.hamdy.clarity.compose.ClarityScreen
import com.hamdy.clarity.compose.TrackClarityEventOnFirstComposition

@Composable
fun App(clarityClient: ClarityClient) {
    ClarityProvider(clarityClient) {
        MaterialTheme {
            ClarityScreen(name = "SampleHome") {
                SampleHomeScreen(clarityClient)
            }
        }
    }
}

@Composable
private fun SampleHomeScreen(clarityClient: ClarityClient) {
    TrackClarityEventOnFirstComposition("home_screen_viewed")

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Clarity KMP Sample",
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "State: ${clarityClient.state}",
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = {
                clarityClient.sendCustomEvent("start_workout_clicked")
            }) {
                Text("Track Event")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                clarityClient.setCustomUserId("sample_user_123")
            }) {
                Text("Set User ID")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                clarityClient.setCustomTag("plan", "premium")
            }) {
                Text("Set Tag")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                clarityClient.setCurrentScreenName("DetailScreen")
            }) {
                Text("Set Screen Name")
            }
        }
    }
}
