package org.inquest.uploader.api.entities

import kotlinx.serialization.Serializable

@Serializable
data class Build(val id: Long = -1)

@Serializable
data class Profession(
    val id: Int,
    val name: String,
    val profession: String,
    val profession_icon: String? = null,
    val profession_icon_big: String? = null,
    val elite: Boolean,
    val icon: String,
    val minor_traits: List<Int> = listOf(),
    val major_traits: List<Int> = listOf(),
    val weapon_trait: Int? = null,
    val background: String,
)

@Serializable
data class GW2(
    val build: Build = Build(),
    val professions: List<Profession> = listOf(),
)