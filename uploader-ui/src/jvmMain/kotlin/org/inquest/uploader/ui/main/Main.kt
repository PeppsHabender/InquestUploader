package org.inquest.uploader.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import androidx.compose.ui.window.application
import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.DarculaTheme
import com.github.weisj.darklaf.theme.spec.AccentColorRule
import org.inquest.uploader.api.config.IConfig
import org.inquest.uploader.api.entities.ILog
import org.inquest.uploader.api.persistence.ISession
import org.inquest.uploader.bl.inquestBlModule
import org.inquest.uploader.bl.utils.AnyExtensions.LOG
import org.inquest.uploader.bl.utils.PathExtensions.localLogPath
import org.inquest.uploader.ui.commons.view.GlobalViewModel
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.compose.withDI
import org.kodein.di.instance
import java.awt.Color
import java.awt.Window
import java.nio.file.Path

internal fun main() = application {
    val darculaTheme = DarculaTheme()

    LafManager.install(darculaTheme.derive(
        darculaTheme.fontSizeRule,
        darculaTheme.fontPrototype,
        AccentColorRule.fromColor(Color(237, 41, 57).darker(), Color(237, 41, 57))
    ))

    withDI(inquestBlModule, DI.Module(name = "GlobalVM") {
        bindSingleton<ISession> { Session(instance()) }

        bindSingleton {
            GlobalViewModel(instance())
        }
    }) {
        InquestUploader()
    }
}

/**
 * Exception handler which solely prints the exception as a log and executes [block].
 */
@JvmInline
@OptIn(ExperimentalComposeUiApi::class)
value class WindowExceptionFunctionHandler(private val block: () -> Unit): WindowExceptionHandlerFactory {
    override fun exceptionHandler(window: Window): WindowExceptionHandler = WindowExceptionHandler {
        LOG.error("An uncaught exception was encountered while running the application!", it)
        block()
    }

}

private data class Session(private val config: IConfig): ISession {
    private var currentLogsImpl: Map<String, ILog> by mutableStateOf(
        linkedMapOf()
    )

    override val currentLogs: Collection<ILog>
        get() = this.currentLogsImpl.values

    override fun add(from: Path, log: ILog) = from.localLogPath(this.config).let {
        this.currentLogsImpl += it to this.currentLogsImpl[it].merge(log)
    }

    private fun ILog?.merge(log: ILog): ILog = if(this == null) {
        log
    } else if(this::class != log::class) {
        ILog.Both(this, log)
    } else log

    override fun clear() {
        this.currentLogsImpl = linkedMapOf()
    }
}