package io.github.nikoapps.commons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

const val PIN_LENGTH = 4

/**
 * Shows how many digits are entered as large dots, never the digits themselves.
 * [description] is what TalkBack reads, such as "2 of 4 digits entered".
 */
@Composable
fun PinDots(entered: Int, description: String, modifier: Modifier = Modifier) {
    Text(
        text = (0 until PIN_LENGTH).joinToString(" ") { if (it < entered) "●" else "○" },
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
    )
}

/** Digits only, large keys, with a delete key that TalkBack reads as [deleteLabel]. */
@Composable
fun PinPad(
    onDigit: (Int) -> Unit,
    onDelete: () -> Unit,
    deleteIcon: Painter,
    deleteLabel: String,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { digit -> DigitKey(digit, onDigit, Modifier.weight(1f)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(Modifier.weight(1f))
            DigitKey(0, onDigit, Modifier.weight(1f))
            // An icon fits at any text size; the backspace shape is the one every phone keyboard uses.
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f).heightIn(min = 72.dp)) {
                Icon(
                    painter = deleteIcon,
                    contentDescription = deleteLabel,
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Int, onDigit: (Int) -> Unit, modifier: Modifier) {
    Button(onClick = { onDigit(digit) }, modifier = modifier.heightIn(min = 72.dp)) {
        Text(digit.toString(), style = MaterialTheme.typography.headlineLarge)
    }
}
