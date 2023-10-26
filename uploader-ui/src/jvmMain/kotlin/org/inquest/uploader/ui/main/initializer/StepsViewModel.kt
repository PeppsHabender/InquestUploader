package org.inquest.uploader.ui.main.initializer

import kotlinx.coroutines.CoroutineScope
import org.inquest.uploader.api.logs.GW2EIConfig
import org.inquest.uploader.api.logs.IGW2EIParser
import org.inquest.uploader.api.logs.ILogAnalyzer
import org.inquest.uploader.ui.commons.composables.LoadingMeta
import org.inquest.uploader.ui.commons.utils.ViewModel
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

/**
 * Contains needed methods for storing the ei config and analyzing the latest logs.
 */
class StepsViewModel(
    private val gw2EIParser: IGW2EIParser,
    private val logAnalyzer: ILogAnalyzer,
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
    fun analyzeMetas(): Array<LoadingMeta> =
        mutableListOf<LoadingMeta>().apply {
            this@StepsViewModel.bosses.forEach {  boss ->
                boss.listDirectoryEntries().filter(Path::isRegularFile).maxBy {
                    it.toFile().lastModified()
                }.also {
                    add("Analyzing ${it.absolutePathString()}", 2) {
                        this@StepsViewModel.gw2EIParser.parse(it).getOrThrow()
                    }
                }
            }

            add("Finalizing...", 3) {
                this@StepsViewModel.logAnalyzer.triggerAnalysis()
                this@StepsViewModel.onSuccess()
            }
        }.toTypedArray()

    private fun MutableList<LoadingMeta>.add(text: String, weight: Int, runnable: suspend CoroutineScope.() -> Unit) {
        add(LoadingMeta(text, weight, runnable = runnable))
    }
}