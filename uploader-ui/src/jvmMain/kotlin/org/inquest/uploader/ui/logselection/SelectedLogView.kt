package org.inquest.uploader.ui.logselection

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import org.inquest.uploader.api.config.IConfig
import org.inquest.uploader.api.logs.LogFile
import org.inquest.uploader.api.logs.StoredLogs.Companion.LogState
import org.inquest.uploader.bl.utils.IPersistenceExtensions.contains
import org.inquest.uploader.bl.utils.IPersistenceExtensions.get
import org.inquest.uploader.spi.ICurrentLog
import org.inquest.uploader.spi.ILogViewerAdapter
import org.inquest.uploader.ui.commons.utils.ComposeUtils.TabNavigationItem
import org.inquest.uploader.ui.commons.utils.DIUtils
import org.inquest.uploader.ui.commons.view.GlobalViewModel
import org.inquest.uploader.ui.commons.view.InquestDIView
import org.inquest.uploader.ui.logselection.loglist.LogSelection
import org.inquest.uploader.ui.logselection.uploader.LogUploaderAnalyzerView
import org.inquest.uploader.ui.logselection.uploader.LogUploaderAnalyzerViewModel
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import java.io.Serial
import java.nio.file.Path
import java.util.ServiceLoader

/**
 * Container for the currently selected single log.
 */
class SelectedLogView: InquestDIView() {
    @Composable
    override fun TheContent() {
        val currentLog: CurrentLogViewModel = rememberScreenModel()
        val globalViewModel: GlobalViewModel = rememberScreenModel()
        var handledLogs: Set<Path> by remember {
            mutableStateOf(emptySet())
        }

        val addHandledLog: (Path) -> Unit = { handledLogs = handledLogs.plusElement(it) }
        val removeHandledLog: (Path) -> Unit = { handledLogs = handledLogs.minusElement(it) }

        CurrentLogUpdater(currentLog, globalViewModel)

        Row {
            Navigator(LogSelection)
            Box(Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource("/images/inquest/inquest_logo_darkened.png"),
                    contentDescription = null,
                    modifier = Modifier.height(800.dp).align(Alignment.Center),
                    contentScale = ContentScale.FillHeight
                )
                TabNavigator(LOG_VIEW_ADAPTERS[0]) { nav ->
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LOG_VIEW_ADAPTERS.forEach {
                                TabNavigationItem(it)
                            }
                        }

                        val currLog: LogFile? = currentLog.currentLog
                        val currTab: ILogViewerAdapterTab = nav.current as ILogViewerAdapterTab
                        if(currLog == null && !currTab.canHandle(null)) {
                            Box(Modifier.fillMaxSize()) {
                                Text(
                                    "Please select a log to continue!",
                                    style = MaterialTheme.typography.h5,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        } else if(currTab.canHandle(currLog?.state)) {
                            val showUploadPanel: Boolean = currTab.showUploadPanel && currLog?.state != LogState.UPLOADED_ANALZED
                            Box(modifier = Modifier.fillMaxHeight(if(showUploadPanel) 0.9F else 1F)) {
                                currTab.Content()
                            }
                            if(showUploadPanel && currLog != null) {
                                Box(contentAlignment = Alignment.Center) {
                                    LogUploaderAnalyzerView(currLog, handledLogs, addHandledLog, removeHandledLog)
                                }
                            }
                        } else if(currLog != null) {
                            LogUploaderAnalyzerView(currLog, handledLogs, addHandledLog, removeHandledLog)
                        }
                    }
                }
            }
        }
    }

    override fun DI.MainBuilder.initSubDI() {
        bindSingleton { CurrentLogViewModel() }
        bindSingleton<ICurrentLog> { instance<CurrentLogViewModel>() }
        bindProvider {
            LogUploaderAnalyzerViewModel(instance(), instance(), instance())
        }
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = 6256655153026702739L

        val LOG_VIEW_ADAPTERS: List<Tab>
            get() = ServiceLoader.load(ILogViewerAdapter::class.java).mapIndexed { idx, el ->
                object: ILogViewerAdapterTab {
                    @Serial
                    private val serialVersionUID: Long = -8517706360271948546L

                    override val showUploadPanel: Boolean = el.showUploadPanel
                    override val key: ScreenKey = el.key
                    override val options: TabOptions
                        @Composable get() = TabOptions(idx.toUShort(), el.id())

                    override fun canHandle(state: LogState?): Boolean = el.canHandle(state)

                    @Composable
                    override fun Content() {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            el.Content()
                        }
                    }
                }
            }

        private interface ILogViewerAdapterTab: Tab {
            val showUploadPanel: Boolean

            fun canHandle(state: LogState?): Boolean
        }

        private inline fun <reified T: ILogViewerAdapter> T.id(): String =
            (if(this.id == null) this::class.simpleName?.toNormalCase() else this.id)!!

        private fun String.toNormalCase() = fold("") { acc, it ->
            acc + (if(it.isUpperCase()) " " else "") + it
        }
    }
}

@Composable
private fun CurrentLogUpdater(
    viewModel: CurrentLogViewModel,
    globalViewModel: GlobalViewModel,
) {
    if(viewModel.currentLog == null) {
        return
    }

    val config: IConfig = DIUtils.localDirectDI().instance()
    val log: LogFile = viewModel.currentLog!!
    if(globalViewModel.storedLogs.contains(log.realFile, config)) {
        viewModel.currentLog = log.copy(
            state = log.state.updateWith(globalViewModel.storedLogs[log.realFile, config]!!)
        )
    }
}