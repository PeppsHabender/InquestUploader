package org.inquest.uploader.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.inquest.uploader.api.config.IConfig
import org.inquest.uploader.api.logs.IGW2EIParser
import org.inquest.uploader.api.logs.ILogAnalyzer
import org.inquest.uploader.api.logs.ILogLoader
import org.inquest.uploader.ui.commons.utils.ComposeUtils.screenSize
import org.inquest.uploader.ui.commons.utils.DIUtils
import org.inquest.uploader.ui.commons.view.GlobalViewModel
import org.inquest.uploader.ui.commons.view.InquestDIView
import org.inquest.uploader.ui.logselection.SelectedLogView
import org.inquest.uploader.ui.main.initializer.InitializeView
import org.inquest.uploader.ui.style.InquestTheme
import org.kodein.di.DirectDI
import org.kodein.di.instance
import java.io.Serial
import kotlin.concurrent.thread


/**
 * Main entry point to the compose part of the application.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ApplicationScope.InquestUploader() {
    val windowState = InitialWindowState()
    val di: DirectDI = DIUtils.localDirectDI()
    var open: Boolean by remember {
        mutableStateOf(true)
    }

    val cancelWatchers: () -> Unit = remember {
        startWatching(di)
    }

    val openApp: () -> Unit = remember {
        {
            open = true

            val globalVm: GlobalViewModel = di.instance()
            globalVm.updateStoredLogs()
            globalVm.updateBossList()
        }
    }

    Tray(icon = painterResource("/images/inquest/inquest_logo.svg"), onAction = openApp) {
        Item("Open Uploader", onClick = openApp)

        Item("Close Uploader") {
            cancelWatchers()
            exitApplication()
        }
    }

    if(!open) {
        return
    }

    CompositionLocalProvider(
        LocalWindowExceptionHandlerFactory provides WindowExceptionFunctionHandler {
            cancelWatchers()
            exitApplication()
        }
    ) {
        Window(
            onCloseRequest = { open = false },
            undecorated = true,
            resizable = false,
            state = windowState,
            icon = painterResource("/images/inquest/inquest_logo.svg")
        ) {
            InquestTheme {
                DecoratedWindow(
                    closeApp = { open = false },
                    modifier = Modifier.fillMaxSize(),
                    minimizeApp = { windowState.isMinimized = true },
                ) {
                    MainView()
                }
            }
        }
    }
}

private fun startWatching(di: DirectDI): (() -> Unit) {
    val globalViewModel: GlobalViewModel = di.instance()

    val analyzerJob: Thread = thread {
        runBlocking {
            di.instance<ILogAnalyzer>().startAnalyzing(globalViewModel::updateBossList, globalViewModel::updateStoredLogs)
        }
    }
    val loaderJob: Thread = thread {
        runBlocking {
            di.instance<ILogLoader>().startWatching(globalViewModel::updateBossList, globalViewModel::updateStoredLogs)
        }
    }

    return {
        runBlocking {
            analyzerJob.interrupt()
            loaderJob.interrupt()

            analyzerJob.join()
            loaderJob.join()
        }
    }
}

internal data object MainView: InquestDIView() {
    @Serial
    private const val serialVersionUID: Long = 2337828936097972006L

    @Composable
    override fun TheContent() {
        Column(
            modifier = Modifier.padding(5.dp),
        ) {
            Navigator(SelectedLogView())
        }
    }
}

@Composable
private fun MainView() {
    val di: DirectDI = DIUtils.localDirectDI()
    var firstStart: Boolean by remember {
        mutableStateOf(di.instance<IConfig>().firstStart)
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            firstStart = di.instance<IGW2EIParser>().needsUpdate().getOrDefault(false)
        }
    }

    if(firstStart) {
        InitializeView {
            firstStart = false
        }.Content()
    } else {
        MainView.Content()
    }
}

@Composable
private fun InitialWindowState(): WindowState {
    val windowState = rememberWindowState()
    val screenSize = screenSize()

    // Full screen looks kinda.. meh
    windowState.size = screenSize * 0.7f
    windowState.position = WindowPosition(
        (screenSize.width - windowState.size.width) / 2,
        (screenSize.height - windowState.size.height) / 2,
    )

    return windowState
}

@Composable
private fun DecoratedWindow(
    closeApp: () -> Unit,
    minimizeApp: () -> Unit,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .background(MaterialTheme.colors.background)
                .height(40.dp)
                .padding(2.dp)
                .fillMaxWidth(),
        ) {
            TextButton(
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.clip(RoundedCornerShape(5.dp)).size(25.dp),
                onClick = minimizeApp,
            ) {
                Text(
                    "_",
                    color = Color.White
                )
            }
            Spacer(Modifier.width(5.dp))
            TextButton(
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.primary),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.clip(RoundedCornerShape(5.dp)).size(25.dp),
                onClick = closeApp,
            ) {
                Text(
                    "x",
                    color = MaterialTheme.colors.background,
                )
            }
        }

        Scaffold(modifier = modifier) {
            content()
        }
    }
}