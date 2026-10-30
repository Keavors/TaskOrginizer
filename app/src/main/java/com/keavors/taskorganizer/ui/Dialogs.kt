package com.keavors.taskorganizer.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.keavors.taskorganizer.data.TabKind
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Text input in a dialog (tab name, dictionary word). */
@Composable
fun TextInputDialog(
    title: String,
    initial: String = "",
    label: String,
    confirmLabel: String = "Сохранить",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val confirm = { if (value.text.isNotBlank()) onConfirm(value.text.trim()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        },
        confirmButton = { AppButton(confirmLabel, onClick = confirm, kind = ButtonKind.PRIMARY, enabled = value.text.isNotBlank()) },
        dismissButton = { AppButton("Отмена", onClick = onDismiss) }
    )
}

@Composable
fun NewTabDialog(onDismiss: () -> Unit, onCreate: (String, TabKind) -> Unit) {
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(TabKind.TASKS) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val create = { onCreate(title.trim(), kind) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая вкладка") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    placeholder = { Text("Например, Покупки") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (title.isNotBlank()) create() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
                Spacer(Modifier.height(16.dp))
                AppSegmented(listOf(TabKind.TASKS to "Задачи", TabKind.NOTES to "Заметки"), kind, { kind = it })
            }
        },
        confirmButton = { AppButton("Создать", onClick = create, kind = ButtonKind.PRIMARY, enabled = title.isNotBlank()) },
        dismissButton = { AppButton("Отмена", onClick = onDismiss) }
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            AppButton(confirmLabel, onClick = {
                onDismiss()
                onConfirm()
            }, kind = ButtonKind.DANGER)
        },
        dismissButton = { AppButton("Отмена", onClick = onDismiss) }
    )
}

/** 24-hour clock dial. [onClear] adds a "Без времени" button. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(initial: LocalTime, onDismiss: () -> Unit, onClear: (() -> Unit)? = null, onPick: (LocalTime) -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (onClear != null) {
                        AppButton("Без времени", onClick = {
                            onDismiss()
                            onClear()
                        })
                    }
                    Spacer(Modifier.weight(1f))
                    AppButton("Отмена", onClick = onDismiss)
                    Spacer(Modifier.width(4.dp))
                    AppButton("Готово", onClick = {
                        onDismiss()
                        onPick(LocalTime.of(state.hour, state.minute))
                    }, kind = ButtonKind.PRIMARY)
                }
            }
        }
    }
}

/** A reminder interval of the user's own: number + minutes / hours / days. */
@Composable
fun MinutesDialog(title: String, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    var value by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(1) }
    val minutes = value.toIntOrNull()?.takeIf { it in 1..999 }?.let { it * unit }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { v -> value = v.filter { it.isDigit() }.take(3) },
                    label = { Text("Сколько") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
                Spacer(Modifier.height(12.dp))
                AppSegmented(listOf(1 to "мин", 60 to "ч", 24 * 60 to "дн"), unit, { unit = it })
            }
        },
        confirmButton = {
            AppButton("Добавить", onClick = {
                onDismiss()
                minutes?.let(onPick)
            }, kind = ButtonKind.PRIMARY, enabled = minutes != null)
        },
        dismissButton = { AppButton("Отмена", onClick = onDismiss) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueDateDialog(initial: String?, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val initialMillis = (initial?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now())
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppButton("Готово", onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                onDismiss()
            }, kind = ButtonKind.PRIMARY)
        },
        dismissButton = { AppButton("Отмена", onClick = onDismiss) }
    ) {
        DatePicker(state = state)
    }
}
