package io.github.nikoapps.clock.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import io.github.nikoapps.clock.R
import io.github.nikoapps.clock.chime.GREETING_TIMES
import io.github.nikoapps.clock.settings.Settings
import io.github.nikoapps.clock.settings.ShakeSensitivity
import io.github.nikoapps.commons.ui.ChoiceRow
import io.github.nikoapps.commons.ui.SwitchRow
import io.github.nikoapps.commons.ui.ValueRow
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import io.github.nikoapps.commons.R as CommonsR

// Clock's rows shared by the settings screen and first-run setup, so both always look and behave the same.
// Rows every Niko app has, such as voice and PIN, are in Commons.

/** Languages Niko Clock can speak the time in. Each has hand-written phrases. */
val VOICE_LANGUAGES: List<String?> = listOf(null, "en", "ne-NP", "hi-IN", "bn", "ur", "es", "pt-BR", "fr", "id")

private val CHIME_INTERVALS = listOf(60, 30, 15)
private val CHIME_VOLUMES = listOf(50, 80, 100)
private val QUIET_STARTS = listOf(LocalTime.of(21, 0), LocalTime.of(22, 0), LocalTime.of(23, 0))
private val QUIET_ENDS = listOf(LocalTime.of(6, 0), LocalTime.of(7, 0), LocalTime.of(8, 0))

/** The chimes switch, and the rows that only matter while it's on. */
@Composable
fun ChimeRows(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit, onNeedNotifications: () -> Unit) {
    val cancel = stringResource(CommonsR.string.cancel)
    SwitchRow(
        title = stringResource(R.string.setting_chime),
        info = stringResource(R.string.info_chime),
        checked = settings.chimeEnabled,
        onCheckedChange = { checked ->
            if (checked) onNeedNotifications()
            onUpdate { it.copy(chimeEnabled = checked) }
        },
    )
    if (!settings.chimeEnabled) return
    ChoiceRow(
        title = stringResource(R.string.setting_chime_interval),
        options = CHIME_INTERVALS,
        selected = settings.chimeIntervalMinutes,
        optionLabel = { minutes ->
            stringResource(
                when (minutes) {
                    60 -> R.string.chime_every_hour
                    30 -> R.string.chime_every_half_hour
                    else -> R.string.chime_every_quarter_hour
                },
            )
        },
        onSelect = { minutes -> onUpdate { it.copy(chimeIntervalMinutes = minutes) } },
        cancelLabel = cancel,
    )
    ChoiceRow(
        title = stringResource(R.string.setting_chime_volume),
        options = CHIME_VOLUMES,
        selected = CHIME_VOLUMES.minBy { kotlin.math.abs(it - settings.chimeVolumePercent) },
        optionLabel = { percent ->
            stringResource(
                when (percent) {
                    50 -> R.string.volume_quiet
                    80 -> R.string.volume_medium
                    else -> R.string.volume_loud
                },
            )
        },
        onSelect = { percent -> onUpdate { it.copy(chimeVolumePercent = percent) } },
        cancelLabel = cancel,
    )
    SwitchRow(
        title = stringResource(R.string.setting_quiet_hours),
        info = stringResource(R.string.info_quiet_hours),
        value = if (settings.quietHoursEnabled) {
            "${clockTime(settings.quietStart)} – ${clockTime(settings.quietEnd)}"
        } else {
            stringResource(CommonsR.string.value_off)
        },
        checked = settings.quietHoursEnabled,
        onCheckedChange = { checked -> onUpdate { it.copy(quietHoursEnabled = checked) } },
    )
    if (settings.quietHoursEnabled) {
        ChoiceRow(
            title = stringResource(R.string.setting_quiet_start),
            options = QUIET_STARTS,
            selected = QUIET_STARTS.firstOrNull { it == settings.quietStart } ?: QUIET_STARTS[1],
            optionLabel = { clockTime(it) },
            onSelect = { time -> onUpdate { it.copy(quietStart = time) } },
            cancelLabel = cancel,
        )
        ChoiceRow(
            title = stringResource(R.string.setting_quiet_end),
            options = QUIET_ENDS,
            selected = QUIET_ENDS.firstOrNull { it == settings.quietEnd } ?: QUIET_ENDS[1],
            optionLabel = { clockTime(it) },
            onSelect = { time -> onUpdate { it.copy(quietEnd = time) } },
            cancelLabel = cancel,
        )
    }
    SwitchRow(
        title = stringResource(R.string.setting_follow_dnd),
        info = stringResource(R.string.info_dnd),
        checked = settings.chimeFollowsDoNotDisturb,
        onCheckedChange = { checked -> onUpdate { it.copy(chimeFollowsDoNotDisturb = checked) } },
    )
}

/** The shake switch, and the rows that only matter while it's on. */
@Composable
fun ShakeRows(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit, onNeedNotifications: () -> Unit) {
    SwitchRow(
        title = stringResource(R.string.setting_shake),
        info = stringResource(R.string.info_shake),
        value = stringResource(R.string.setting_shake_value),
        checked = settings.shakeEnabled,
        onCheckedChange = { checked ->
            if (checked) onNeedNotifications()
            onUpdate { it.copy(shakeEnabled = checked) }
        },
    )
    if (!settings.shakeEnabled) return
    SwitchRow(
        title = stringResource(R.string.setting_shake_date),
        info = stringResource(R.string.info_shake_date),
        checked = settings.shakeAgainForDate,
        onCheckedChange = { checked -> onUpdate { it.copy(shakeAgainForDate = checked) } },
    )
    ChoiceRow(
        title = stringResource(R.string.setting_shake_sensitivity),
        options = listOf(ShakeSensitivity.High, ShakeSensitivity.Medium, ShakeSensitivity.Low),
        selected = settings.shakeSensitivity,
        optionLabel = { stringResource(it.label()) },
        onSelect = { s -> onUpdate { it.copy(shakeSensitivity = s) } },
        cancelLabel = stringResource(CommonsR.string.cancel),
    )
}

/** Bikram Sambat, the helper's note and the morning greeting: what the person hears and sees about today. */
@Composable
fun DayAndDateRows(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit, onNeedNotifications: () -> Unit) {
    SwitchRow(
        title = stringResource(R.string.setting_bikram_sambat),
        info = stringResource(R.string.info_bikram_sambat),
        value = stringResource(R.string.setting_bikram_sambat_value),
        checked = settings.bikramSambat,
        onCheckedChange = { checked -> onUpdate { it.copy(bikramSambat = checked) } },
    )
    NoteRow(note = settings.helperNote, onNote = { note -> onUpdate { it.copy(helperNote = note) } })
    SwitchRow(
        title = stringResource(R.string.setting_greeting),
        info = stringResource(R.string.info_greeting),
        value = if (settings.greetingEnabled) clockTime(settings.greetingTime) else stringResource(CommonsR.string.value_off),
        checked = settings.greetingEnabled,
        onCheckedChange = { checked ->
            if (checked) onNeedNotifications()
            onUpdate { it.copy(greetingEnabled = checked) }
        },
    )
    if (settings.greetingEnabled) {
        ChoiceRow(
            title = stringResource(R.string.setting_greeting_time),
            options = GREETING_TIMES,
            selected = GREETING_TIMES.firstOrNull { it == settings.greetingTime } ?: GREETING_TIMES[1],
            optionLabel = { clockTime(it) },
            onSelect = { time -> onUpdate { it.copy(greetingTime = time) } },
            cancelLabel = stringResource(CommonsR.string.cancel),
        )
    }
}

/** The day clock switch, and showing it over the lock screen while it's on. */
@Composable
fun DayClockRows(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit) {
    SwitchRow(
        title = stringResource(R.string.setting_day_clock),
        info = stringResource(R.string.info_day_clock),
        value = stringResource(R.string.setting_day_clock_value),
        checked = settings.dayClock,
        onCheckedChange = { checked -> onUpdate { it.copy(dayClock = checked) } },
    )
    if (settings.dayClock) {
        SwitchRow(
            title = stringResource(R.string.setting_lock_screen),
            info = stringResource(R.string.info_lock_screen),
            checked = settings.dayClockOnLockScreen,
            onCheckedChange = { checked -> onUpdate { it.copy(dayClockOnLockScreen = checked) } },
        )
    }
}

/** Longest note, so it fits on the clock in large text. */
private const val NOTE_MAX_LENGTH = 80

/** The helper's note: the row shows it, tapping opens a dialog to write, change or remove it. */
@Composable
private fun NoteRow(note: String?, onNote: (String?) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    ValueRow(
        title = stringResource(R.string.setting_note),
        info = stringResource(R.string.info_note),
        value = note ?: stringResource(R.string.note_none),
        trailing = painterResource(CommonsR.drawable.ic_chevron_right),
        onClick = { open = true },
    )
    if (!open) return
    var text by rememberSaveable { mutableStateOf(note.orEmpty()) }
    val buttonText = @Composable { label: String ->
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(stringResource(R.string.setting_note), style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(NOTE_MAX_LENGTH) },
                label = { Text(stringResource(R.string.note_label)) },
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onNote(text.trim().ifEmpty { null })
                    open = false
                },
                modifier = Modifier.heightIn(min = 64.dp),
            ) { buttonText(stringResource(R.string.note_save)) }
        },
        dismissButton = {
            Row {
                if (note != null) {
                    TextButton(
                        onClick = {
                            onNote(null)
                            open = false
                        },
                        modifier = Modifier.heightIn(min = 64.dp),
                    ) { buttonText(stringResource(R.string.note_remove)) }
                }
                TextButton(onClick = { open = false }, modifier = Modifier.heightIn(min = 64.dp)) {
                    buttonText(stringResource(CommonsR.string.cancel))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

/** "9:00 PM" or "21:00", following the phone's clock style. */
fun clockTime(time: LocalTime): String = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

fun ShakeSensitivity.label(): Int = when (this) {
    ShakeSensitivity.Low -> R.string.sensitivity_low
    ShakeSensitivity.Medium -> R.string.sensitivity_medium
    ShakeSensitivity.High -> R.string.sensitivity_high
}
