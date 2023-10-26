package org.inquest.uploader.api.config

import org.inquest.uploader.api.entities.Profession

/**
 * Responsible for fetching build information about gw2.
 */
interface IGW2Info {
    /**
     * List of all professions currently in the game.
     */
    val professions: List<Profession>

    /**
     * Pulls all needed entities from the gw2 api.
     */
    fun pullGw2Entities()
}