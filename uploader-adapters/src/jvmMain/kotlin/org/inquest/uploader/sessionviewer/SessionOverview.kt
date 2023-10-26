package org.inquest.uploader.sessionviewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.inquest.uploader.api.logs.StoredLogs.Companion.LogState
import org.inquest.uploader.api.persistence.ISession
import org.inquest.uploader.sessionviewer.patterns.LogPatternEditor
import org.inquest.uploader.spi.ILogViewerAdapter
import org.inquest.uploader.ui.commons.composables.HyperLink
import org.inquest.uploader.ui.commons.utils.DIUtils
import org.inquest.uploader.ui.commons.utils.View
import org.inquest.uploader.ui.commons.view.InquestDIView
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import java.io.Serial

/**
 * Generates an overview of the current logs in [ISession]. Uses the list of patterns in
 * [SessionViewModel] in order to evaluate them against the logs and provide a string value to
 * copy.
 */
class SessionOverview: InquestDIView(), ILogViewerAdapter {
    override fun canHandle(logState: LogState?): Boolean = true

    @Composable
    override fun TheContent() {
        val viewModel: SessionViewModel = DIUtils.localInstance()
        val clipboardManager: ClipboardManager = LocalClipboardManager.current
        val clipboardBuilder: StringBuilder = remember { StringBuilder() }

        Navigator(Overview(DIUtils.localInstance(), clipboardBuilder)) { nav ->
            Box(Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.TopStart) {
                CurrentScreen()

                Row(modifier = Modifier.align(Alignment.TopEnd)) {
                    if(viewModel.patternLs.isNotEmpty() && !nav.canPop) {
                        Icon(
                            Icons.Default.ContentCopy, null,
                            modifier = Modifier.size(30.dp).clickable {
                                clipboardManager.setText(AnnotatedString(clipboardBuilder.toString()))
                            }
                        )
                    }

                    Icon(
                        Icons.Default.Settings,
                        null,
                        tint = if(!nav.canPop) Color.White else MaterialTheme.colors.primary,
                        modifier = Modifier.size(30.dp).clickable {
                            if(nav.canPop) {
                                nav.pop()
                            } else {
                                nav.push(LogPatternEditor)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun DI.MainBuilder.initSubDI() {
        bindSingleton {
            SessionViewModel(instance())
        }
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = 7257595140830920837L
    }
}

/**
 * Provides the overview by evaluating all logs in [session] and provide their string content via
 * the given [clipboardBuilder].
 */
private data class Overview(private val session: ISession, private val clipboardBuilder: StringBuilder): View {
    @Composable
    override fun Content() {
        val viewModel: SessionViewModel = DIUtils.localDirectDI().instance()
        val uriHandler: UriHandler = LocalUriHandler.current

        var currSessionLogs: List<List<String>> by remember { mutableStateOf(emptyList()) }
        LaunchedEffect(Unit) {
            launch(Dispatchers.IO) {
                clipboardBuilder.clear()
                currSessionLogs = this@Overview.session.currentLogs.reversed().map { log ->
                    viewModel.patternLs.mapIndexed { idx, pattern ->
                        val value: String = pattern(log)

                        if(value.isNotEmpty() && value != "\"\"") {
                            clipboardBuilder.append(if (idx == 0) value else " $value")
                        }

                        value
                    }.also {
                        clipboardBuilder.append(System.lineSeparator())
                    }
                }
            }
        }

        Column(Modifier.fillMaxSize()) {
            currSessionLogs.forEach { patterns ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    patterns.forEach inner@{ pattern ->
                        if(pattern.isEmpty()) {
                            return@inner
                        }

                        if(pattern.matches(URL_REGEX)) {
                            HyperLink(
                                text = pattern,
                                style = LocalTextStyle.current,
                                color = MaterialTheme.colors.primary,
                                onClick = uriHandler::openUri)
                        } else if(pattern != "\"\"") {
                            Text(viewModel.substitute(pattern))
                        }
                    }
                }
            }

            if(session.currentLogs.isEmpty()) {
                Text("Session currently contains no logs!")
            } else if(currSessionLogs.isEmpty()) {
                CircularProgressIndicator()
            }
        }
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = 8784846008882217165L
        private val URL_REGEX: Regex = "https?:\\/\\/(?:www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_\\+.~#?&\\/=]*)".toRegex()
    }
}