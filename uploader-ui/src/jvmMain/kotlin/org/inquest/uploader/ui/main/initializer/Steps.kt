package org.inquest.uploader.ui.main.initializer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import org.inquest.uploader.api.config.ConfigEntity
import org.inquest.uploader.api.config.IConfig
import org.inquest.uploader.api.config.IGW2Info
import org.inquest.uploader.api.logs.GW2EIConfig
import org.inquest.uploader.api.logs.IGW2EIParser
import org.inquest.uploader.bl.utils.StringExtensions.toPath
import org.inquest.uploader.ui.commons.composables.LoadingBar
import org.inquest.uploader.ui.commons.composables.LoadingMeta
import org.inquest.uploader.ui.commons.utils.DIUtils
import org.inquest.uploader.ui.commons.utils.DirectoryDialog
import org.inquest.uploader.ui.commons.utils.PathResult
import org.inquest.uploader.ui.commons.utils.View
import org.kodein.di.DirectDI
import org.kodein.di.instance
import java.io.Serial
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

internal typealias Step = View

/**
 * Fetches the local arcdps folder, downloads elite insights and fetches gw2 build information.
 */
internal class ArcFolderStep(private val onSuccess: () -> Unit): Step {
    @Composable
    override fun Content() {
        val di: DirectDI = DIUtils.localDirectDI()
        val config: IConfig = di.instance()
        val viewModel: StepsViewModel = rememberScreenModel()
        val nav: Navigator = LocalNavigator.currentOrThrow

        viewModel.onSuccess = this.onSuccess
        LoadingBar(
            LoadingMeta("Finding arc dps log folder...", 3, config.firstStart) {
                SelectLogFolder(config)
            },
            LoadingMeta("Downloading GW2 Elite Insights...", 10) {
                di.instance<IGW2EIParser>().downloadLatestRelease()
            },
            LoadingMeta("Downloading GW2 Information...", 6) {
                di.instance<IGW2Info>().pullGw2Entities()

                if(!config.firstStart) {
                    onSuccess()
                }
            },
            LoadingMeta("Collecting stored bosses...", 2, config.firstStart) {
                viewModel.bosses = config.arcdpsPath.listDirectoryEntries().filter(Path::isDirectory)
                delay(1000)
                nav.push(ConfigureUploaderStep)
            }
        )
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = 8673305989592911517L
    }
}

private fun SelectLogFolder(config: IConfig) {
    val found: PathResult = DirectoryDialog(
        title = "Choose ArcDps-Log Folder",
        allowMultiSelection = false,
        initDir = System.getProperty("user.home").toPath().resolve("Documents")
    )

    val path: Path = when (found) {
        is PathResult.None, is PathResult.Multiple -> error("Failed to locate ArcDps log-folder!")
        is PathResult.Single -> found.path
    }

    config.store(arcdpsPath = path)
}

/**
 * Configures the uploader and gets the information the inquest needs.
 */
private data object ConfigureUploaderStep: Step {
    @Serial
    private const val serialVersionUID: Long = -2890054979752325158L

    private val gW2EIConfig: GW2EIConfig = GW2EIConfig().copy(Anonymous = false)

    @Composable
    override fun Content() {
        val nav: Navigator = LocalNavigator.currentOrThrow
        val config: IConfig = DIUtils.localDirectDI().instance()
        val viewModel: StepsViewModel = rememberScreenModel()

        var newEIConfig: GW2EIConfig by remember { mutableStateOf(this.gW2EIConfig) }
        var configEntity: ConfigEntity by remember { mutableStateOf(ConfigEntity()) }
        var uploadLatest: Boolean by remember { mutableStateOf(true) }

        Column(
            modifier = Modifier.fillMaxWidth(0.4F).padding(top = 20.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                "Do you want to",
                style = MaterialTheme.typography.h5
            )
            ConfigItem(
                "...show folder rather than encounter names?",
                configEntity.showFolderNames
            ) {
                configEntity = configEntity.copy(showFolderNames = it)
            }
            ConfigItem(
                "...upload new logs to dps.report automatically?",
                configEntity.uploadToDpsReport
            ) {
                configEntity = configEntity.copy(uploadToDpsReport = it)
            }
            ConfigItem(
                "...analyze logs automatically?",
                configEntity.analyzeAutomatically
            ) {
                configEntity = configEntity.copy(analyzeAutomatically = it)
            }
            ConfigItem(
                "...anonymize logs?",
                newEIConfig.Anonymous
            ) {
                newEIConfig = newEIConfig.copy(Anonymous = it)
            }
            Box(Modifier.background(Color.White).height(2.dp).fillMaxWidth().clip(RoundedCornerShape(5.dp)))
            ConfigItem(
                "...analyze your latest logs in order to initialize the application?",
                uploadLatest
            ) {
                uploadLatest = it
            }
            Text("(might take several minutes)", style = MaterialTheme.typography.caption)

            Button(
                modifier = Modifier.align(Alignment.End),
                onClick = {
                    config.store(
                        showFolderNames = configEntity.showFolderNames,
                        uploadToDpsReport = configEntity.uploadToDpsReport
                    ).getOrThrow()
                    viewModel.storeConfig(newEIConfig)

                    if(uploadLatest) {
                        nav.push(AnalyzeStep)
                    } else {
                        viewModel.onSuccess()
                    }
                }
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
private fun ConfigItem(text: String, initVal: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text)
        Spacer(Modifier.weight(1F))
        Checkbox(
            initVal,
            onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary)
        )
    }
}

/**
 * Analyzes the latest log per boss if the user wanted.
 */
private data object AnalyzeStep: Step {
    @Serial
    private const val serialVersionUID: Long = 2632711606298902976L

    @Composable
    override fun Content() {
        val vm: StepsViewModel = rememberScreenModel()

        LoadingBar(*vm.analyzeMetas())
    }
}