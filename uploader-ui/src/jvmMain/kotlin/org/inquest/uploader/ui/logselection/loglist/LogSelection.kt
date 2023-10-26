package org.inquest.uploader.ui.logselection.loglist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.kodein.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.SlideTransition
import org.inquest.uploader.api.logs.Boss
import org.inquest.uploader.api.logs.FolderToBoss
import org.inquest.uploader.bl.utils.AnyExtensions.LOG
import org.inquest.uploader.ui.commons.composables.ClickableImage
import org.inquest.uploader.ui.commons.utils.DIUtils
import org.inquest.uploader.ui.commons.utils.View
import org.inquest.uploader.ui.commons.view.GlobalViewModel
import org.inquest.uploader.ui.commons.view.InquestDIView
import org.inquest.uploader.ui.logselection.CurrentLogViewModel
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.instance
import java.io.Serial
import kotlin.io.path.name

/**
 * Display all logs for the given [boss].
 */
internal data class LogSelectionView(
    private val boss: Boss
) : View {

    @Composable
    override fun Content() {
        val model: LogSelectionViewModel = rememberScreenModel()
        val navigator: Navigator = LocalNavigator.currentOrThrow
        val currLogModel: CurrentLogViewModel = DIUtils.localInstance()

        Column {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                tint = MaterialTheme.colors.primary,
                modifier = Modifier.size(30.dp).clickable(onClick = navigator::pop),
                contentDescription = null
            )

            LazyColumn(
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                items(model.loadLogs(this@LogSelectionView.boss)) {
                    TextButton(
                        onClick = {
                            currLogModel.currentLog = it
                        }
                    ) {
                        Text(
                            it.readableName,
                            color = if (currLogModel.currentLog?.realFile == it.realFile) MaterialTheme.colors.primary else Color.White
                        )
                    }
                }
            }
        }
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = 8365146334049514322L
    }
}

/**
 * Displays all bosses that were found in the configured folder.
 */
internal data object BossSelectionView : View {
    @Serial
    private const val serialVersionUID: Long = 2029507014636684117L

    @Composable
    override fun Content() {
        val nav: Navigator = LocalNavigator.currentOrThrow
        val model: LogSelectionViewModel = rememberScreenModel()
        val folderToBoss: FolderToBoss = DIUtils.localInstance<GlobalViewModel>().folderToBoss

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.Top,
            horizontalArrangement = Arrangement.Center,
        ) {
            items(items = model.fetchBosses()) {
                val commonMod: Modifier = Modifier.size(100.dp, 150.dp).padding(2.dp)

                val icon: String? = if(it.name in folderToBoss.folderToBoss) "/images/bosses/${folderToBoss[it.name]}.png" else null
                if(icon == null || this::class.java.getResource(icon!!) == null) {
                    it.NoImageBossButton(commonMod, nav)
                    return@items
                }

                Box(commonMod) {
                    ClickableImage(
                        icon,
                        text = it.readableName,
                        modifier = Modifier.fillMaxSize(),
                        onClick = {
                            nav.push(LogSelectionView(it))
                        }
                    )
                }
            }
        }
    }
}

/**
 * Button to be displayed when no image was found for a boss.
 */
@Composable
private fun Boss.NoImageBossButton(modifier: Modifier, navigator: Navigator) {
    Box(modifier) {
        TextButton(
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
            modifier = Modifier.fillMaxSize(),
            onClick = {
                navigator.push(LogSelectionView(this@NoImageBossButton))
            },
        ) {
            Text(
                readableName,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Navigator hosting [LogSelectionView] and [BossSelectionView].
 */
internal data object LogSelection : InquestDIView() {
    @Serial
    private const val serialVersionUID: Long = -3898347437443021234L

    override fun DI.MainBuilder.initSubDI() {
        bindSingleton {
            LogSelectionViewModel(instance(), instance())
        }
    }

    @Composable
    override fun TheContent() {
        val globalViewModel: GlobalViewModel = rememberScreenModel()
        LOG.debug("Updated stored logs: {}", globalViewModel.storedLogs)

        Navigator(BossSelectionView) { nav ->
            SlideTransition(nav, modifier = Modifier.width(300.dp)) { screen ->
                Scaffold(
                    content = { screen.Content() },
                    modifier = Modifier.fillMaxHeight(),
                )
            }
        }
    }
}
