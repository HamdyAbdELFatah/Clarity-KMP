package com.hamdy.clarity.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hamdy.clarity.ClarityClient
import com.hamdy.clarity.ClarityState
import com.hamdy.clarity.*
import com.hamdy.clarity.compose.ClarityProvider
import com.hamdy.clarity.compose.ClarityScreen
import com.hamdy.clarity.compose.TrackClarityEvent
import com.hamdy.clarity.compose.clarityClickable
import com.hamdy.clarity.compose.clarityTag
import com.hamdy.clarity.compose.rememberClarityState

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

private data class LogEntry(
    val action: String,
    val result: String,
    val index: String,
)

@Composable
private fun SampleHomeScreen(clarityClient: ClarityClient) {
    TrackClarityEvent("home_screen_viewed")

    val clarityState by rememberClarityState()
    val eventLog = remember { mutableStateListOf<LogEntry>() }

    var eventName by remember { mutableStateOf("button_clicked") }
    var userId by remember { mutableStateOf("sample_user_123") }
    var sessionId by remember { mutableStateOf("checkout-42") }
    var tagKey by remember { mutableStateOf("plan") }
    var tagValue by remember { mutableStateOf("premium") }
    var screenName by remember { mutableStateOf("DetailScreen") }
    var sessionUrl by remember { mutableStateOf<String?>(null) }

    fun log(action: String, result: Boolean) {
        eventLog.add(0, LogEntry(action, result.toString(), nextLogIndex()))
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HeaderSection(clarityState)
            }

            item {
                IdentitySection(
                    userId = userId,
                    onUserIdChange = { userId = it },
                    sessionId = sessionId,
                    onSessionIdChange = { sessionId = it },
                    onSetUserId = {
                        val result = clarityClient.userId(userId)
                        log("userId(\"$userId\")", result)
                    },
                    onSetSessionId = {
                        val result = clarityClient.sessionId(sessionId)
                        log("sessionId(\"$sessionId\")", result)
                    },
                )
            }

            item {
                TaggingSection(
                    tagKey = tagKey,
                    onTagKeyChange = { tagKey = it },
                    tagValue = tagValue,
                    onTagValueChange = { tagValue = it },
                    onSetSingleTag = {
                        val result = clarityClient.tag(tagKey, tagValue)
                        log("tag(\"$tagKey\", \"$tagValue\")", result)
                    },
                    onSetMultiTag = {
                        val result = clarityClient.tag(tagKey, setOf(tagValue, "annual"))
                        log("tag(\"$tagKey\", setOf(\"$tagValue\", \"annual\"))", result)
                    },
                )
            }

            item {
                CaptureControlSection(
                    isPaused = clarityState is ClarityState.Paused,
                    screenName = screenName,
                    onScreenNameChange = { screenName = it },
                    onSetScreenName = {
                        val result = clarityClient.screen(screenName)
                        log("screen(\"$screenName\")", result)
                    },
                    onPause = {
                        val result = clarityClient.pause()
                        log("pause()", result)
                    },
                    onResume = {
                        val result = clarityClient.resume()
                        log("resume()", result)
                    },
                    onNewSession = {
                        val result = clarityClient.startNewSession { id ->
                            log("startNewSession callback", true)
                        }
                        log("startNewSession()", result)
                    },
                )
            }

            item {
                SessionUrlSection(
                    sessionUrl = sessionUrl,
                    onGetUrl = {
                        val url = clarityClient.getCurrentSessionUrl()
                        sessionUrl = url
                        log("getCurrentSessionUrl()", url != null)
                    },
                )
            }

            item {
                ComposeHelpersSection(
                    onTagClicked = {
                        log("Modifier.clarityTag applied", true)
                    },
                    onClickTracked = {
                        log("Modifier.clarityClickable fired", true)
                    },
                )
            }

            item {
                CustomEventSection(
                    eventName = eventName,
                    onEventNameChange = { eventName = it },
                    onSendEvent = {
                        val result = clarityClient.trackEvent(eventName)
                        log("trackEvent(\"$eventName\")", result)
                    },
                )
            }

            item {
                EventLogSection(eventLog)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun HeaderSection(state: ClarityState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Clarity KMP Demo",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))
        StateBadge(state)
    }
}

@Composable
private fun StateBadge(state: ClarityState) {
    val (label, color) = when (state) {
        ClarityState.Active -> "Active" to Color(0xFF2E7D32)
        ClarityState.Paused -> "Paused" to Color(0xFFE65100)
        ClarityState.InitializationAccepted -> "Initializing" to Color(0xFF1565C0)
        ClarityState.NotInitialized -> "Not Initialized" to Color(0xFF757575)
        ClarityState.Disabled -> "Disabled" to Color(0xFF9E9E9E)
        ClarityState.Unsupported -> "Unsupported" to Color(0xFFC62828)
        is ClarityState.Failed -> "Failed" to Color(0xFFB71C1C)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (state) {
                    is ClarityState.Failed -> "$label: ${state.reason}"
                    else -> label
                },
                color = color,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun IdentitySection(
    userId: String,
    onUserIdChange: (String) -> Unit,
    sessionId: String,
    onSessionIdChange: (String) -> Unit,
    onSetUserId: () -> Unit,
    onSetSessionId: () -> Unit,
) {
    SectionCard(title = "Identity") {
        OutlinedTextField(
            value = userId,
            onValueChange = onUserIdChange,
            label = { Text("User ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onSetUserId,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Set User ID")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = sessionId,
            onValueChange = onSessionIdChange,
            label = { Text("Custom Session ID") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onSetSessionId,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Set Session ID")
        }
    }
}

@Composable
private fun TaggingSection(
    tagKey: String,
    onTagKeyChange: (String) -> Unit,
    tagValue: String,
    onTagValueChange: (String) -> Unit,
    onSetSingleTag: () -> Unit,
    onSetMultiTag: () -> Unit,
) {
    SectionCard(title = "Tagging") {
        OutlinedTextField(
            value = tagKey,
            onValueChange = onTagKeyChange,
            label = { Text("Tag Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = tagValue,
            onValueChange = onTagValueChange,
            label = { Text("Tag Value") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onSetSingleTag,
                modifier = Modifier.weight(1f),
            ) {
                Text("Single Tag")
            }
            Button(
                onClick = onSetMultiTag,
                modifier = Modifier.weight(1f),
            ) {
                Text("Multi-Value Tag")
            }
        }
    }
}

@Composable
private fun CaptureControlSection(
    isPaused: Boolean,
    screenName: String,
    onScreenNameChange: (String) -> Unit,
    onSetScreenName: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onNewSession: () -> Unit,
) {
    SectionCard(title = "Capture Control") {
        OutlinedTextField(
            value = screenName,
            onValueChange = onScreenNameChange,
            label = { Text("Screen Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onSetScreenName,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Set Screen Name")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onPause,
                modifier = Modifier.weight(1f),
                enabled = !isPaused,
            ) {
                Text("Pause")
            }
            OutlinedButton(
                onClick = onResume,
                modifier = Modifier.weight(1f),
                enabled = isPaused,
            ) {
                Text("Resume")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onNewSession,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start New Session")
        }
    }
}

@Composable
private fun SessionUrlSection(
    sessionUrl: String?,
    onGetUrl: () -> Unit,
) {
    SectionCard(title = "Session URL") {
        Button(
            onClick = onGetUrl,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Get Session URL")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = sessionUrl ?: "Not available yet",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = if (sessionUrl != null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
            )
        }
    }
}

@Composable
private fun ComposeHelpersSection(
    onTagClicked: () -> Unit,
    onClickTracked: () -> Unit,
) {
    SectionCard(title = "Compose Helpers") {
        Text(
            text = "Demonstrates Modifier.clarityTag and Modifier.clarityClickable.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onTagClicked,
            modifier = Modifier
                .fillMaxWidth()
                .clarityTag("demo_button", "compose_helpers"),
        ) {
            Text("Tagged Button (clarityTag)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clarityClickable("demo_click_tracked", onClick = onClickTracked),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Text(
                text = "Tracked Surface (clarityClickable)",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun CustomEventSection(
    eventName: String,
    onEventNameChange: (String) -> Unit,
    onSendEvent: () -> Unit,
) {
    SectionCard(title = "Custom Event") {
        OutlinedTextField(
            value = eventName,
            onValueChange = onEventNameChange,
            label = { Text("Event Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onSendEvent,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Send Event")
        }
    }
}

@Composable
private fun EventLogSection(entries: List<LogEntry>) {
    SectionCard(title = "Event Log (${entries.size})") {
        if (entries.isEmpty()) {
            Text(
                text = "No actions yet. Tap a button above to see Clarity API calls logged here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                entries.take(20).forEach { entry ->
                    LogEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.index,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.width(56.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = entry.action,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (entry.result == "true") {
                    Color(0xFF2E7D32).copy(alpha = 0.12f)
                } else {
                    Color(0xFFC62828).copy(alpha = 0.12f)
                },
            ) {
                Text(
                    text = entry.result,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = if (entry.result == "true") {
                        Color(0xFF2E7D32)
                    } else {
                        Color(0xFFC62828)
                    },
                )
            }
        }
    }
}

private var logIndex = 0

private fun nextLogIndex(): String = "#${++logIndex}"
