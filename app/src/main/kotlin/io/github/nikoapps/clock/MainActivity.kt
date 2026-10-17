package io.github.nikoapps.clock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import io.github.nikoapps.commons.theme.NikoTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
private fun ClockScreen(viewModel: ClockViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    // Tick at the start of each second so the minute flips on time.
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }

    ClockContent(
        timeText = now.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
        dayText = now.format(DateTimeFormatter.ofPattern("EEEE")),
        voice = state.voice,
        onSpeak = viewModel::onSpeak,
    )
}

@Composable
private fun ClockContent(timeText: String, dayText: String, voice: VoiceStatus, onSpeak: () -> Unit) {
    val speakLabel = stringResource(R.string.speak_time)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Tapping anywhere on the time speaks it.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(onClickLabel = speakLabel, onClick = onSpeak),
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
            Text(
                text = dayText,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        // Always shown, in the same place, so nothing on the screen moves when the voice changes.
        Text(
            text = stringResource(voice.message()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            minLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        )
        Button(
            onClick = onSpeak,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp),
        ) {
            Text(text = speakLabel, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@StringRes
private fun VoiceStatus.message(): Int = when (this) {
    VoiceStatus.Starting, VoiceStatus.Ready -> R.string.status_ready
    VoiceStatus.NoVoice -> R.string.status_no_voice
    VoiceStatus.NoEngine -> R.string.status_no_engine
    VoiceStatus.LanguageNotSupported -> R.string.status_language_not_supported
}

@Preview(showBackground = true)
@Composable
private fun ClockContentPreview() {
    NikoTheme {
        ClockContent(timeText = "10:30 AM", dayText = "Saturday", voice = VoiceStatus.Ready, onSpeak = {})
    }
}
