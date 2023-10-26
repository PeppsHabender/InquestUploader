package org.inquest.uploader.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.application
import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.DarculaTheme
import com.github.weisj.darklaf.theme.spec.AccentColorRule
import org.inquest.uploader.api.config.IConfig
import org.inquest.uploader.api.entities.ILog
import org.inquest.uploader.api.persistence.ISession
import org.inquest.uploader.bl.inquestBlModule
import org.inquest.uploader.bl.utils.PathExtensions.localLogPath
import org.inquest.uploader.ui.commons.view.GlobalViewModel
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.compose.withDI
import org.kodein.di.instance
import org.slf4j.LoggerFactory
import java.awt.Color
import java.nio.file.Path

internal fun main() = try {
    application()
} catch (ex: Throwable) {
    LoggerFactory.getLogger("MAIN").error("Application failed with error {}!", ex.localizedMessage, ex)
}

private fun application() = application {
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