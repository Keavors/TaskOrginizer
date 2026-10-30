package com.keavors.taskorganizer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.keavors.taskorganizer.data.AppData
import com.keavors.taskorganizer.data.DictionaryTerm

/**
 * Words the recognizer should write correctly. The words themselves go into the
 * recognition prompt; the variants are replaced after recognition.
 */
@Composable
fun DictionaryScreen(vm: MainViewModel, data: AppData, snackbar: SnackbarHostState) {
    var newWord by remember { mutableStateOf("") }
    val add = {
        if (newWord.isNotBlank()) {
            vm.addTerm(newWord, emptyList())
            newWord = ""
        }
    }
    ScreenScaffold("Словарь", onBack = { vm.screen = Screen.SETTINGS }, snackbar = snackbar) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    "Добавьте названия, которые вы часто говорите: GitHub, Docker, имена. Модель будет ждать их и писать правильно. " +
                        "«Варианты» — как она ошибается («гитхаб»): такие слова заменяются автоматически, с любыми окончаниями. " +
                        "Если вы исправите надиктованное слово вручную, приложение само предложит его запомнить.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newWord,
                        onValueChange = { newWord = it },
                        label = { Text("Новое слово") },
                        placeholder = { Text("Например, Kubernetes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { add() }),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalIconButton(onClick = add, enabled = newWord.isNotBlank()) { Icon(Icons.Outlined.Add, "Добавить") }
                }
            }
            if (data.dictionary.isEmpty()) {
                item {
                    Text(
                        "Словарь пока пуст.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 24.dp).fillMaxWidth()
                    )
                }
            }
            items(data.dictionary, key = { it.id }) { term -> TermCard(vm, term) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TermCard(vm: MainViewModel, term: DictionaryTerm) {
    var editWord by remember { mutableStateOf(false) }
    var addVariant by remember { mutableStateOf(false) }
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(term.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { editWord = true }) { Icon(Icons.Outlined.Edit, "Изменить") }
                IconButton(onClick = { vm.deleteTerm(term.id) }) { Icon(Icons.Outlined.Delete, "Удалить") }
            }
            Text("Варианты распознавания", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 12.dp)) {
                term.variants.forEach { v ->
                    InputChip(
                        selected = false,
                        onClick = { vm.updateTerm(term.id, variants = term.variants - v) },
                        label = { Text(v) },
                        trailingIcon = { Icon(Icons.Outlined.Close, "Убрать", Modifier.size(16.dp)) }
                    )
                }
                AssistChip(
                    onClick = { addVariant = true },
                    label = { Text("Вариант") },
                    leadingIcon = { Icon(Icons.Outlined.Add, null, Modifier.size(16.dp)) }
                )
            }
        }
    }
    if (editWord) {
        TextInputDialog("Изменить слово", term.text, "Как писать правильно", onDismiss = { editWord = false }) {
            editWord = false
            vm.updateTerm(term.id, text = it)
        }
    }
    if (addVariant) {
        TextInputDialog("Как модель ошибается", "", "Например, гитхаб", "Добавить", onDismiss = { addVariant = false }) {
            addVariant = false
            vm.updateTerm(term.id, variants = term.variants + it)
        }
    }
}
