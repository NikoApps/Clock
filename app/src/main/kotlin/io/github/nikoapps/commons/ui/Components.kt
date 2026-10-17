package io.github.nikoapps.commons.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The primary action on a screen: full width, at least 96dp tall. */
@Composable
fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** A secondary action: full width, at least 64dp tall, outlined so it reads as less important. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/**
 * A screen title that stays at the top while the content scrolls, with a back arrow.
 * The arrow is labelled for TalkBack and sits next to the title, so it is never a lone icon.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(title: String, backIcon: Painter, backLabel: String, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.size(64.dp)) {
                Icon(painter = backIcon, contentDescription = backLabel, modifier = Modifier.size(28.dp))
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

// Settings list type scale. Three levels only: group header, row title, row value.
private val GroupHeaderStyle = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
private val RowTitleStyle = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal)
private val RowValueStyle = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal)

private val RowPadding = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)

/** A group of settings: a short header in the accent colour, rows, then a divider. */
@Composable
fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = GroupHeaderStyle,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 4.dp)
                .semantics { heading() },
        )
        content()
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
    }
}

/** What settings rows need from the app for their info buttons. */
data class InfoStrings(val icon: Painter, val moreAbout: (String) -> String, val ok: String)

/** Provided once by a settings screen, so rows can show an info button without passing it around. */
val LocalInfoStrings = staticCompositionLocalOf<InfoStrings?> { null }

@Composable
private fun RowText(title: String, value: String?, valueColor: Color, info: String?, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = RowTitleStyle, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f, fill = false))
            if (info != null) InfoButton(title, info)
        }
        if (value != null) Text(value, style = RowValueStyle, color = valueColor)
    }
}

/**
 * A small (i) next to a setting's title. Tapping it explains the setting in a few sentences,
 * so the list itself can stay short.
 */
@Composable
private fun InfoButton(title: String, info: String) {
    val strings = LocalInfoStrings.current ?: return
    var open by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = Modifier.size(48.dp)) {
        Icon(
            painter = strings.icon,
            contentDescription = strings.moreAbout(title),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
    if (!open) return
    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(info, style = RowTitleStyle, color = MaterialTheme.colorScheme.onSurface) },
        confirmButton = {
            TextButton(onClick = { open = false }, modifier = Modifier.heightIn(min = 56.dp)) {
                Text(strings.ok, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

/**
 * An on/off setting. The whole row is one touch target and one TalkBack stop,
 * read as a switch.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    info: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .then(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RowText(title, value, MaterialTheme.colorScheme.onSurfaceVariant, info, Modifier.weight(1f))
        // The row handles the click; the switch only shows the state.
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.background,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}

/**
 * A setting that shows its current value under the title. Tapping it does [onClick], such as
 * opening a choice dialog or a phone settings page.
 */
@Composable
fun ValueRow(
    title: String,
    value: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    trailing: Painter? = null,
    leading: Painter? = null,
    valueIsWarning: Boolean = false,
    info: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .then(RowPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (leading != null) {
            Icon(painter = leading, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
        }
        RowText(
            title = title,
            value = value,
            valueColor = if (valueIsWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            info = info,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            Icon(painter = trailing, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * A setting with a few plain options. The row shows the current one; tapping opens a dialog
 * with every option on its own large row. Picking one applies it and closes the dialog.
 */
@Composable
fun <T> ChoiceRow(
    title: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    cancelLabel: String,
    modifier: Modifier = Modifier,
    info: String? = null,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    ValueRow(title = title, value = optionLabel(selected), onClick = { open = true }, modifier = modifier, info = info)
    if (!open) return
    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .selectable(
                                selected = option == selected,
                                role = Role.RadioButton,
                                onClick = {
                                    onSelect(option)
                                    open = false
                                },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.outline,
                            ),
                        )
                        Text(optionLabel(option), style = RowTitleStyle, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { open = false }, modifier = Modifier.heightIn(min = 56.dp)) {
                Text(cancelLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}
