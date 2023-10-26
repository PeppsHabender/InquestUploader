package org.inquest.uploader.api.entities

import kotlinx.serialization.Serializable

@Serializable
data class Encounter(
    val uniqueId: String? = null,
    val success: Boolean = false,
    val duration: Double = 0.0,
    val compDps: Long = 0,
    val numberOfPlayers: Int = 0,
    val numberOfGroups: Int = 0,
    val bossId: Long? = null,
    val boss: String? = null,
    val isCm: Boolean = false,
    val gw2Build: Long? = null,
    val jsonAvailable: Boolean = false,
)

@Serializable
data class EVTC(val type: String? = null, val version: String? = null, val bossId: Long? = null)
@Serializable
data class Player(
    val display_name: String? = null,
    val character_name: String? = null,
    val profession: Int? = null,
    val elite_spec: Int? = null
)
@Serializable
data class Report(val anonymous: Boolean = false, val detailed: Boolean = false)