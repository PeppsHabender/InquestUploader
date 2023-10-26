package org.inquest.uploader.logviewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.kodein.rememberScreenModel
import org.apache.commons.lang3.time.DurationFormatUtils
import org.inquest.uploader.api.config.IGW2Info
import org.inquest.uploader.api.entities.ILog
import org.inquest.uploader.api.entities.JsonActorParent
import org.inquest.uploader.api.entities.Player
import org.inquest.uploader.api.entities.Profession
import org.inquest.uploader.api.logs.LogFile
import org.inquest.uploader.api.logs.StoredLogs.Companion.LogState
import org.inquest.uploader.api.persistence.IPersistence
import org.inquest.uploader.spi.ICurrentLog
import org.inquest.uploader.spi.ILogViewerAdapter
import org.inquest.uploader.ui.commons.composables.AsyncImage
import org.inquest.uploader.ui.commons.composables.HyperLink
import org.inquest.uploader.ui.commons.composables.Table
import org.inquest.uploader.ui.commons.utils.DIUtils
import org.inquest.uploader.ui.commons.utils.Spacers.WeightSpacer
import org.inquest.uploader.ui.commons.view.InquestDIView
import java.io.Serial
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.time.temporal.UnsupportedTemporalTypeException
import java.util.Locale

/**
 * Shows an overview of the current log. Currently only dps numbers.
 */
class LogOverview: InquestDIView(), ILogViewerAdapter {
    override val showUploadPanel: Boolean = true

    override fun canHandle(logState: LogState?): Boolean = logState != null && LogState.IDLE != logState

    @Composable
    override fun TheContent() {
        val persistence: IPersistence = DIUtils.localInstance()
        val currLog: LogFile = rememberScreenModel<ICurrentLog>().currentLog ?: return

        when(currLog.state) {
            LogState.UPLOADED, LogState.UPLOADED_ANALZED -> persistence.dpsReportLog(currLog.realFile).onSuccess {
                it.UploadedLogInformation(currLog.state, currLog.realFile, persistence)
            }
            LogState.ANALYZED -> persistence.eiLog(currLog.realFile).onSuccess {
                it.AnalyzedLogInformation()
            }
            else -> {}
        }
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = 1746295097235445345L
    }
}

/**
 * Composable to display an uploaded logs information.
 *
 * @param state Current log state
 * @param from Path where the log was loaded from
 * @param persistence The apps persistence
 */
@Composable
private fun ILog.DpsLog.UploadedLogInformation(
    state: LogState,
    from: Path,
    persistence: IPersistence
) {
    LogInformation(
        this.encounter.boss ?: "Unknown",
        if (this.encounter.isCm) "Challenge Mode" else "",
        this.encounter.success,
        this.encounterTimeInstant ?: Instant.now(),
        this.uploadTimeInstant,
        this.permalink,
        DurationFormatUtils.formatDuration((this.encounter.duration * 1000).toLong(), "mm'm' ss's' SSS'ms'"),
    ) {
        val professionIcons: Map<Int, String> = rememberProfessionIcons(Profession::id)

        if(LogState.UPLOADED_ANALZED == state) {
            persistence.eiLog(from).onSuccess {
                // Show a detailed dps report
                DpsReportDetailed(it)
                return@LogInformation
            }
        }


        // Not so detailed report with player names and comp dps only
        val players: List<Player> = this@UploadedLogInformation.players.entries.sortedBy { it.key }.map { it.value }.toList()
        Table(
            players,
            listOf(null, "Name", "Account"),
            {
                Box(Modifier.padding(5.dp)) {
                    AsyncImage(
                        professionIcons[it.elite_spec]!!,
                        modifier = Modifier.size(30.dp).align(Alignment.Center),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                }
            },
            {
                Text(it.character_name ?: "Unknown")
            },
            {
                // I'm funny I know
                Text(it.display_name ?: "Unknown.6969")
            },
            cellWidths = listOf(50.dp, 250.dp, 250.dp),
            extend = true,
            modifier = Modifier.fillMaxWidth()
        )
        Text("Comp Dps: ${this@UploadedLogInformation.encounter.compDps}", modifier = Modifier.align(Alignment.End))
    }
}

/**
 * Composable to display an analyzed logs information.
 */
@Composable
private fun ILog.JsonLog.AnalyzedLogInformation() {
    LogInformation(
        this.fightName ?: "Unknown",
        if (this.isCM) "Challenge Mode" else "",
        this.success, this.recordedOn, null, null,
        this.duration
    ) {
        DpsReportDetailed(this@AnalyzedLogInformation)
    }
}

/**
 * Generates a detailed report of everything dps (the most important thing anyways ever, right?)
 * Currently super messy, but this just functions as a prototype really.
 */
@Composable
private fun ColumnScope.DpsReportDetailed(log: ILog.JsonLog) {
    val professionIcons: Map<String, String> = rememberProfessionIcons(Profession::name)

    // Players by dps
    val players: List<JsonActorParent.JsonPlayer> = log.players.sortedByDescending { it.dpsTargets[0][0].dps }
    // You! The uploader
    val user: JsonActorParent.JsonPlayer? = players.firstOrNull{ it.account == log.recordedAccountBy }
    Table(
        players,
        listOf(null, "Name", "Account", "Dps\n(Target)", "PDps\n(Target)", "CDps\n(Target)", "Dps\n(All)", "PDps\n(All)", "CDps\n(All)"),
        {
            Box(Modifier.padding(5.dp)) {
                AsyncImage(
                    professionIcons[it.profession]!!,
                    modifier = Modifier.size(30.dp).align(Alignment.Center),
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }
        },
        {
            Text(it.name ?: "Unknown", color = it.color(user))
        },
        {
            // The funny one again
            Text(it.account ?: "Unknown.6969", color = it.color(user))
        },
        {
            Text(it.dpsTargets[0][0].dps.toString(), color = it.color(user))
        },
        {
            Text(it.dpsTargets[0][0].powerDps.toString(), color = it.color(user))
        },
        {
            Text(it.dpsTargets[0][0].condiDps.toString(), color = it.color(user))
        },
        {
            Text(it.dpsAll[0].dps.toString(), color = it.color(user))
        },
        {
            Text(it.dpsAll[0].powerDps.toString(), color = it.color(user))
        },
        {
            Text(it.dpsAll[0].condiDps.toString(), color = it.color(user))
        },
        cellWidths = listOf(50.dp, 250.dp, 250.dp, 120.dp, 120.dp, 120.dp, 120.dp, 120.dp, 120.dp),
        extend = true,
        modifier = Modifier.fillMaxWidth()
    )
    Text("Comp Dps: ${players.sumOf { it.dpsTargets[0][0].dps!! }}", modifier = Modifier.align(Alignment.End))
}

@Composable
private fun JsonActorParent.JsonPlayer.color(user: JsonActorParent.JsonPlayer?) =
    if(this == user) MaterialTheme.colors.primary else Color.White

@Composable
private fun LogInformation(
    encounterName: String,
    cmStr: String,
    success: Boolean,
    recordedOn: TemporalAccessor,
    uploadedOn: TemporalAccessor?,
    permalink: String?,
    duration: String?,
    dpsReport: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 30.dp, end = 30.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Log for $encounterName $cmStr", fontSize = 22.sp, style = MaterialTheme.typography.h6)

            WeightSpacer()

            if(success) {
                Icon(Icons.Filled.Done, null, modifier = Modifier.size(50.dp), tint = Color(0xFF085C08))
            } else {
                // Boohoo you lost
                Icon(Icons.Filled.Close, null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colors.primary)
            }
        }
        Row {
            Text("Recorded on ${recordedOn.getDateString()}", fontSize = 19.sp)
            WeightSpacer()
            if(uploadedOn != null) {
                Text("Uploaded on ${Helper.DATE_TIME_FORMATTER.format(uploadedOn)}", fontSize = 19.sp)
            }
        }
        if(permalink != null){
            DpsReportLink(permalink)
        }
        if(duration != null){
            Text("Took $duration", fontSize = 19.sp, modifier = Modifier.align(Alignment.Start))
        }

        dpsReport()
    }
}

private fun TemporalAccessor.getDateString(): String = try {
    Helper.DATE_TIME_FORMATTER.format(this)
} catch (_: UnsupportedTemporalTypeException) {
    if(this is Instant) {
        Helper.DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(this, ZoneId.systemDefault()))
    } else {
        "Unknown"
    }
}

@Composable
private fun DpsReportLink(link: String) {
    val uriHandler: UriHandler = LocalUriHandler.current

    Row {
        Text("Log can also be viewed via ", fontSize = 19.sp)
        HyperLink(
            text = link,
            color = MaterialTheme.colors.primary,
            fontSize = 21.sp,
            onClick = uriHandler::openUri
        )
        WeightSpacer()
    }
}

@Composable
private fun <T> rememberProfessionIcons(keyExtractor: (Profession) -> T): Map<T, String> = DIUtils.localInstance<IGW2Info>().let { info ->
    remember {
        info.professions.filter { it.profession_icon != null || it.profession_icon_big != null }.associate {
            if(it.profession_icon_big == null) {
                // Refer to the icon with the better resolution if possible
                keyExtractor(it) to it.profession_icon!!
            } else {
                keyExtractor(it) to it.profession_icon_big!!
            }
        }
    }
}

private object Helper {
    val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE MMM dd yyyy 'at' HH:mm", Locale.US)
}