package org.inquest.uploader.sessionviewer.patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.transitions.SlideTransition
import org.inquest.uploader.api.entities.ILog
import org.inquest.uploader.ui.commons.utils.View
import java.io.Serial
import java.time.format.DateTimeFormatter
import java.util.Locale

private typealias LogToString = (ILog) -> Any?

/**
 * Pattern to fetch static information of logs in the session.
 */
class LogInformation(
    override var displayName: String = "Fight Name",
    private var asString: LogToString = { l ->
        l.evaluate({it.encounter.boss}) {
            it.fightName
        }
    }
): Pattern() {
    override val isPopup: Boolean = true

    override operator fun invoke(log: ILog): String = asString(log)?.toString() ?: "Unknown"

    @Composable
    override fun Content(dismiss: () -> Unit) {
        Box(
            Modifier
                .background(MaterialTheme.colors.background).width(200.dp)
                .heightIn(max = 500.dp).clip(RoundedCornerShape(5.dp))
                .border(1.5.dp, MaterialTheme.colors.primary)
        ) {
            Navigator(
                TypeSelector { name, block ->
                    this@LogInformation.displayName = name
                    this@LogInformation.asString = block
                    dismiss()
                }
            ) {
                Column {
                    // Allows to go back to the previous screen
                    IconButton(it::pop, enabled = it.canPop) {
                        Icon(Icons.Default.ArrowBack, null)
                    }

                    SlideTransition(it, modifier = Modifier.width(200.dp)) {
                        it.Content()
                    }
                }
            }
        }
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = -8106319705165385210L
    }
}

/**
 * Allows the selection of [Common], [DpsReport].
 */
private data class TypeSelector(private val saveFun: (String, LogToString) -> Unit): View {
    @Composable
    override fun Content() {
        val nav: Navigator = LocalNavigator.currentOrThrow
        DefaultColumn (
            {
                DefaultText("Common") {
                    nav.push(Common(this@TypeSelector.saveFun))
                }
                DefaultText("DPS Report") {
                    nav.push(DpsReport(this@TypeSelector.saveFun))
                }
            }
        )
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = -345269178423409951L
    }

}

private data class Common(private val saveFun: (String, LogToString) -> Unit): View {

    @Composable
    override fun Content() {
        DefaultColumn (
            {
                DefaultText(FIGHT_NAME) {
                    saveFun(FIGHT_NAME) { l ->
                        l.evaluate({ it.encounter.boss }, preferDpsLog = true, ILog.JsonLog::fightName)
                    }
                }
            },
            {
                DefaultText(ENCOUNTER_TIME) {
                    saveFun(ENCOUNTER_TIME) { l ->
                        l.evaluate({ it.encounterTimeInstant?.format(DATE_TIME_FORMATTER) }) {
                            it.recordedOn.let(DATE_TIME_FORMATTER::format)
                        }
                    }
                }
            },
            {
                DefaultText(COMP_DPS) {
                    saveFun(COMP_DPS) { l ->
                        l.evaluate({ it.encounter.compDps }) { log ->
                            log.players.sumOf { player ->
                                player.dpsTargets[0].sumOf { (it.dps ?: 0).toLong() }
                            }
                        }
                    }
                }
            }
        )
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = 300370798100342352L

        private const val FIGHT_NAME = "Fight Name"
        private const val ENCOUNTER_TIME = "Encounter Time"
        private const val COMP_DPS = "Comp Dps"

        private val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE MMM dd yyyy 'at' HH:mm", Locale.US)
    }

}

private data class DpsReport(private val saveFun: (String, LogToString) -> Unit): View {
    @Composable
    override fun Content() {
        DefaultColumn(
            {
                DefaultText(PERMALINK) {
                    saveFun(PERMALINK) { l ->
                        l.evaluate({ it.permalink ?: "" }) { "" }
                    }
                }
            }
        )
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = 1830445309088396170L
        private const val PERMALINK = "Remote URL"
    }

}