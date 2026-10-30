package com.keavors.taskorganizer.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keavors.taskorganizer.data.AppData
import kotlinx.coroutines.launch

@Composable
fun AppRoot(vm: MainViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    // Asked when the first entry gets a time to remind at.
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) vm.notificationsDenied()
    }
    LaunchedEffect(Unit) {
        vm.notificationRequests.collect {
            if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.notificationsDenied()
        }
    }

    // Messages from any screen; a new one replaces the one on screen.
    LaunchedEffect(Unit) {
        vm.messages.collect { m ->
            launch {
                snackbar.currentSnackbarData?.dismiss()
                val result = snackbar.showSnackbar(
                    message = m.text,
                    actionLabel = m.actionLabel,
                    withDismissAction = m.actionLabel == null,
                    duration = if (m.action != null) SnackbarDuration.Long else SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) m.action?.invoke()
            }
        }
    }

    val current = data
    if (current == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    BackHandler(enabled = vm.screen != Screen.MAIN) {
        vm.screen = if (vm.screen == Screen.DICTIONARY) Screen.SETTINGS else Screen.MAIN
    }

    AnimatedContent(targetState = vm.screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { screen ->
        when (screen) {
            Screen.MAIN -> MainScreen(vm, current, snackbar)
            Screen.SETTINGS -> SettingsScreen(vm, current, snackbar)
            Screen.DICTIONARY -> DictionaryScreen(vm, current, snackbar)
            Screen.SEARCH -> SearchScreen(vm, current)
            Screen.TRASH -> TrashScreen(vm, current, snackbar)
        }
    }
}

@Composable
private fun MainScreen(vm: MainViewModel, data: AppData, snackbar: SnackbarHostState) {
    val tab = data.activeTab
    val focus = LocalFocusManager.current
    // A tap on empty space ends typing: the cursor and the keyboard go away.
    ScreenBackground(Modifier.pointerInput(Unit) { detectTapGestures(onTap = { focus.clearFocus() }) }) {
        Scaffold(
            topBar = { TopBar(vm, data) },
            bottomBar = { Composer(vm, tab) },
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = Color.Transparent
        ) { padding ->
            ItemList(vm, data, tab, padding)
        }
    }
}
