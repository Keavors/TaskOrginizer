package com.keavors.taskorganizer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keavors.taskorganizer.data.Tab
import com.keavors.taskorganizer.data.TabKind
import com.keavors.taskorganizer.speech.DictationPhase
import com.keavors.taskorganizer.text.Entries
import kotlinx.coroutines.delay

/** Inserts dictated text at the cursor; returns the new field value and the inserted piece. */
private fun insertDictation(current: TextFieldValue, raw: String, kind: TabKind): Pair<TextFieldValue, String> {
    val start = current.selection.min
    val end = current.selection.max
    val before = current.text.substring(0, start)
    val after = current.text.substring(end)
    var piece = if (before.isBlank()) Entries.tidy(raw) else raw.trim()
    if (kind == TabKind.TASKS && current.text.isBlank()) piece = Entries.stripLonePeriod(piece)
    val gap = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
    val tail = if (after.isNotEmpty() && !after.first().isWhitespace()) " " else ""
    val text = before + gap + piece + tail + after
    return TextFieldValue(text, TextRange((before + gap + piece).length)) to piece
}

/** Bottom panel: learning card, voice bar with the live text and the input with the microphone. */
@Composable
fun Composer(vm: MainViewModel, tab: Tab) {
    val dictation = vm.services.dictation
    val phase by dictation.phase.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var field by rememberSaveable(tab.id, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var dictated by remember(tab.id) { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val submit: (String) -> Unit = { text ->
        if (vm.addEntries(tab.id, text, dictated) > 0) {
            field = TextFieldValue("")
            dictated = null
        }
    }

    val autoSubmit by rememberUpdatedState(settings.speech.autoSubmit)
    LaunchedEffect(tab.id, tab.kind) {
        dictation.results.collect { raw ->
            val wasEmpty = field.text.isBlank()
            val (next, piece) = insertDictation(field, raw, tab.kind)
            dictated = listOfNotNull(dictated, piece).joinToString(" ")
            if (autoSubmit && wasEmpty) submit(next.text) else field = next
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.notify("Микрофон разрешён. Нажмите на кнопку ещё раз и говорите.")
        else vm.notify("Без доступа к микрофону голосовой ввод не работает. Разрешите его в настройках телефона.")
    }
    val pressMic = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            dictation.press()
            true
        } else {
            permission.launch(Manifest.permission.RECORD_AUDIO)
            false
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 10.dp)
    ) {
        LearnCard(vm)
        AnimatedVisibility(phase != DictationPhase.Idle, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            VoiceBar(vm, phase)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = field,
                        onValueChange = {
                            field = it
                            if (it.text.isBlank()) dictated = null
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = if (tab.kind == TabKind.TASKS) ImeAction.Done else ImeAction.Default
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit(field.text) }),
                        maxLines = 6,
                        modifier = Modifier.weight(1f).padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
                        decorationBox = { inner ->
                            Box {
                                if (field.text.isEmpty()) {
                                    Text(
                                        if (tab.kind == TabKind.TASKS) "Новая задача" else "Новая заметка",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                inner()
                            }
                        }
                    )
                    AnimatedVisibility(field.text.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
                        IconButton(onClick = { submit(field.text) }) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, "Добавить", tint = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            MicButton(phase, dictation.level.collectAsStateWithLifecycle().value, onPress = pressMic, onRelease = { dictation.release() })
        }
    }
}

@Composable
private fun MicButton(phase: DictationPhase, level: Float, onPress: () -> Boolean, onRelease: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val listening = phase is DictationPhase.Listening
    val busy = phase == DictationPhase.Finishing
    val ring by animateFloatAsState(if (listening) 1f + level * 0.55f else 1f, label = "ring")
    val container = if (listening) colors.error else colors.primary
    val content = if (listening) colors.onError else colors.onPrimary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
        if (listening) {
            Box(
                Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        scaleX = ring
                        scaleY = ring
                    }
                    .clip(CircleShape)
                    .background(container.copy(alpha = 0.22f))
            )
        }
        Surface(
            shape = CircleShape,
            color = container,
            contentColor = content,
            shadowElevation = 3.dp,
            modifier = Modifier
                .size(56.dp)
                .pointerInput(busy) {
                    if (busy) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown()
                        val started = onPress()
                        waitForUpOrCancellation()
                        if (started) onRelease()
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                when {
                    busy -> CircularProgressIndicator(Modifier.size(24.dp), color = content, strokeWidth = 2.5.dp)
                    listening -> Icon(Icons.Rounded.Stop, "Остановить", Modifier.size(26.dp))
                    else -> Icon(Icons.Rounded.Mic, "Надиктовать: нажмите или удерживайте", Modifier.size(26.dp))
                }
            }
        }
    }
}

/** State of the dictation and the words recognized so far. */
@Composable
private fun VoiceBar(vm: MainViewModel, phase: DictationPhase) {
    val colors = MaterialTheme.colorScheme
    val dictation = vm.services.dictation
    val partial by dictation.partial.collectAsStateWithLifecycle()
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Column(Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (phase is DictationPhase.Listening) {
                    val pulse = rememberInfiniteTransition(label = "pulse")
                    val alpha by pulse.animateFloat(1f, 0.25f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dot")
                    Box(Modifier.size(10.dp).graphicsLayer { this.alpha = alpha }.clip(CircleShape).background(colors.error))
                    Spacer(Modifier.width(10.dp))
                    Text("Говорите", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            delay(250)
                            now = SystemClock.elapsedRealtime()
                        }
                    }
                    Text(
                        formatClock((now - phase.startedAt).coerceAtLeast(0) / 1000),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    LevelBars(vm, Modifier.weight(1f).height(28.dp))
                } else {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Распознаю…", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
                }
                TextButton(onClick = { dictation.cancel() }) { Text("Отмена") }
            }
            if (partial.isNotBlank()) {
                // The newest words matter most: long text is cut from the start.
                Text(
                    if (partial.length > LIVE_TEXT_CHARS) "…" + partial.takeLast(LIVE_TEXT_CHARS) else partial,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 10.dp, bottom = 8.dp)
                )
            }
        }
    }
}

private const val LIVE_TEXT_CHARS = 200

@Composable
private fun LevelBars(vm: MainViewModel, modifier: Modifier) {
    val level by vm.services.dictation.level.collectAsStateWithLifecycle()
    val history = remember { mutableStateListOf<Float>().apply { repeat(28) { add(0f) } } }
    LaunchedEffect(level) {
        history.removeAt(0)
        history.add(level)
    }
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val gap = 3.dp.toPx()
        val barWidth = (size.width - gap * (history.size - 1)) / history.size
        history.forEachIndexed { i, v ->
            val h = (0.15f + v * 0.85f) * size.height
            drawRoundRect(
                color = color.copy(alpha = 0.35f + v * 0.65f),
                topLeft = Offset(i * (barWidth + gap), (size.height - h) / 2),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}

@Composable
private fun LearnCard(vm: MainViewModel) {
    val learn = vm.learn
    AnimatedVisibility(learn.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Запомнить исправления для голосового ввода?",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { vm.learn = emptyList() }) { Icon(Icons.Outlined.Close, "Не нужно") }
                }
                learn.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.from, style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.padding(horizontal = 6.dp).size(16.dp))
                        Text(s.to, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.rememberCorrections(listOf(s)) }) { Text("Запомнить") }
                    }
                }
                if (learn.size > 1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { vm.rememberCorrections(learn) }) { Text("Запомнить все") }
                    }
                }
            }
        }
    }
}
