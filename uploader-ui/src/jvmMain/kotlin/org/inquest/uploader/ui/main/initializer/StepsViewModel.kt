package org.inquest.uploader.ui.main.initializer

import kotlinx.coroutines.CoroutineScope
import org.inquest.uploader.api.logs.GW2EIConfig
import org.inquest.uploader.api.logs.IGW2EIParser
import org.inquest.uploader.api.logs.ILogAnalyzer
import org.inquest.uploader.api.persistence.ISession
import org.inquest.uploader.bl.utils.InquestUploaderConstants
import org.inquest.uploader.ui.commons.composables.LoadingMeta
import org.inquest.uploader.ui.commons.utils.ViewModel
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.walk

/**
 * Contains needed methods for storing the ei config and analyzing the latest logs.
 */
class StepsViewModel(
    private val gw2EIParser: IGW2EIParser,
    private val logAnalyzer: ILogAnalyzer,
    private val session: ISession
): ViewModel {
    lateinit var bosses: List<Path>
    lateinit var onSuccess: () -> Unit

    /**
     * Stores the given [config].
     */
    fun storeConfig(config: GW2EIConfig) = this.gw2EIParser.storeConfig(config)

    /**
     * Analyzes the latest metas in the selected arcdps folder.
     */
    @OptIn(ExperimentalPathApi::class)
    fun analyzeMetas(): Array<LoadingMeta> = mutableListOf<LoadingMeta>().apply {
        this@StepsViewModel.bosses.forEach { boss ->
            boss.walk()
                .filter { it.extension in InquestUploaderConstants.LOG_FILE_EXTENSIONS_STR }
                .maxByOrNull(Path::getLastModifiedTime)?.also {
                    add("Analyzing ${it.absolutePathString()}", 2) {
                        this@StepsViewModel.gw2EIParser.parse(it).getOrThrow()
                    }
                }
        }

        add("Finalizing...", 3) {
            this@StepsViewModel.logAnalyzer.triggerAnalysis()
            this@StepsViewModel.session.clear()
            this@StepsViewModel.onSuccess()
        }
    }.toTypedArray()

    private fun MutableList<LoadingMeta>.add(text: String, weight: Int, runnable: suspend CoroutineScope.() -> Unit) {
        add(LoadingMeta(text, weight, runnable = runnable))
    }
}