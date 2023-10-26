package org.inquest.uploader.api.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

sealed interface ILog {
    @Serializable
    data class DpsLog(
        val id: String? = null,
        val permalink: String? = null,
        private val uploadTime: Long? = null,
        private val encounterTime: Long? = null,
        val generator: String? = null,
        val generatorId: Int? = null,
        val generatorVersion: Int? = null,
        val language: String? = null,
        val languageId: Int? = null,
        val userToken: String? = null,
        val error: String? = null,
        val encounter: Encounter = Encounter(),
        val evtc: EVTC = EVTC(),
        val players: Map<String, Player> = mapOf(),
        val report: Report = Report()
    ): ILog {
        @Transient
        @JsonIgnore
        val uploadTimeInstant: ZonedDateTime? = this.uploadTime?.let {
            Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault())
        }
        @JsonIgnore
        @Transient
        val encounterTimeInstant: ZonedDateTime? = this.encounterTime?.let {
            Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault())
        }
    }

    @Serializable
    data class JsonLog(
        val eliteInsightsVersion: String? = null,
        val triggerID: Long? = null,
        val eiEncounterID: Long? = null,
        val fightName: String? = null,
        val fightIcon: String? = null,
        val arcVersion: String? = null,
        val gW2Build: Long? = null,
        val language: String? = null,
        val fractalScale: Int? = null,
        val languageID: Byte? = null,
        val recordedBy: String? = null,
        val recordedAccountBy: String? = null,
        val timeStart: String? = null,
        val timeEnd: String? = null,
        val timeStartStd: String? = null,
        val timeEndStd: String? = null,
        val duration: String? = null,
        val durationMS: Long? = null,
        val logStartOffset: Long? = null,
        val durationStartOffset: Long? = null,
        val success: Boolean = false,
        val isCM: Boolean = false,
        val anonymous: Boolean? = null,
        val detailedWvW: Boolean? = null,
        val targets: List<JsonActorParent.JsonNPC> = listOf(),
        val players: List<JsonActorParent.JsonPlayer> = listOf(),
        val phases: List<JsonPhase> = listOf(),
        val mechanics: List<JsonMechanics> = listOf(),
        val uploadLinks: List<String> = listOf(),
        val skillMap: Map<String, SkillDesc> = mapOf(),
        val buffMap: Map<String, BuffDesc> = mapOf(),
        val damageModMap: Map<String, DamageModDesc> = mapOf(),
        val personalBuffs: Map<String, List<Long>> = mapOf(),
        val presentFractalInstabilities: List<Long> = listOf(),
        val presentInstanceBuffs: List<List<Long>> = listOf(),
        val logErrors: List<String> = listOf(),
        val usedExtensions: List<ExtensionDesc> = listOf(),
        val combatReplayMetaData: JsonCombatReplayMetaData? = null,
    ): ILog {
        @Transient
        @JsonIgnore
        val recordedOn: TemporalAccessor = FORMATTER.parse(this.timeStartStd)

        companion object {
            private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss zzz")
        }
    }

    class Both(first: ILog, second: ILog): ILog {
        val dpsLog: DpsLog = if (first is DpsLog) first else second as DpsLog
        val jsonLog: JsonLog = if(first is JsonLog) first else second as JsonLog
    }
}