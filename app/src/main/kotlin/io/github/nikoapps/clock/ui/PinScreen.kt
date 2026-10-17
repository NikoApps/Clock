package io.github.nikoapps.clock.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.nikoapps.clock.R
import io.github.nikoapps.clock.settings.HelperPin
import io.github.nikoapps.commons.ui.PIN_LENGTH
import io.github.nikoapps.commons.ui.PinDots
import io.github.nikoapps.commons.ui.PinPad
import io.github.nikoapps.commons.ui.SecondaryButton

/** Asks for the helper PIN before settings open. */
@Composable
fun UnlockPinScreen(pinHash: String, onUnlocked: () -> Unit, onCancel: () -> Unit) {
    var entered by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }
    PinLayout(
        title = stringResource(R.string.pin_enter_title),
        message = if (wrong) stringResource(R.string.pin_wrong) else stringResource(R.string.pin_enter_message),
        entered = entered.length,
        onDigit = { digit ->
            if (entered.length < PIN_LENGTH) entered += digit
            if (entered.length == PIN_LENGTH) {
                if (HelperPin.matches(entered, pinHash)) onUnlocked() else wrong = true
                entered = ""
            }
        },
        onDelete = { entered = entered.dropLast(1) },
        onCancel = onCancel,
    )
}

/** Sets a new PIN: enter it, then enter it again to confirm. */
@Composable
fun SetPinScreen(onPinSet: (String) -> Unit, onCancel: () -> Unit) {
    var first by rememberSaveable { mutableStateOf("") }
    var entered by rememberSaveable { mutableStateOf("") }
    var mismatch by rememberSaveable { mutableStateOf(false) }
    val confirming = first.isNotEmpty()
    PinLayout(
        title = stringResource(R.string.pin_set_title),
        message = when {
            mismatch -> stringResource(R.string.pin_mismatch)
            confirming -> stringResource(R.string.pin_confirm_message)
            else -> stringResource(R.string.pin_set_message)
        },
        entered = entered.length,
        onDigit = { digit ->
            if (entered.length < PIN_LENGTH) entered += digit
            if (entered.length == PIN_LENGTH) {
                when {
                    !confirming -> { first = entered; mismatch = false }
                    entered == first -> onPinSet(entered)
                    else -> { first = ""; mismatch = true }
                }
                entered = ""
            }
        },
        onDelete = { entered = entered.dropLast(1) },
        onCancel = onCancel,
    )
}

@Composable
private fun PinLayout(
    title: String,
    message: String,
    entered: Int,
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        // Read out by TalkBack whenever it changes, such as after a wrong PIN.
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        PinDots(entered = entered, description = stringResource(R.string.pin_progress, entered, PIN_LENGTH))
        PinPad(onDigit = onDigit, onDelete = onDelete, deleteLabel = stringResource(R.string.pin_delete))
        SecondaryButton(text = stringResource(R.string.back_to_clock), onClick = onCancel)
    }
}
