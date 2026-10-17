package io.github.nikoapps.clock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import io.github.nikoapps.commons.speech.Speaker
import io.github.nikoapps.commons.theme.NikoTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            NikoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    ClockScreen()
                }
            }
        }
    }
}

@Composable
private fun ClockScreen() {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalTime.now()) }
    var voiceMissing by remember { mutableStateOf(false) }

    val speaker = remember { Speaker(context) }
    DisposableEffect(speaker) {
        onDispose { speaker.shutdown() }
    }

    // Tick at the start of each second so the minute flips on time.
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }

    val timeText = now.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    val announcement = stringResource(R.string.time_announcement, timeText)

    ClockContent(
        timeText = timeText,
        voiceMissing = voiceMissing,
        onSpeak = {
            voiceMissing = !speaker.setLanguage(Locale.getDefault())
            if (!voiceMissing) speaker.speak(announcement)
        },
    )
}

@Composable
private fun ClockContent(timeText: String, voiceMissing: Boolean, onSpeak: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            if (voiceMissing) {
                Text(
                    text = stringResource(R.string.voice_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
        Button(
            onClick = onSpeak,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp),
        ) {
            Text(text = stringResource(R.string.speak_time), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ClockContentPreview() {
    NikoTheme {
        ClockContent(timeText = "10:30 AM", voiceMissing = false, onSpeak = {})
    }
}
