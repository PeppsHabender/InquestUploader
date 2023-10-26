package org.inquest.uploader.bl.gw2

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.http.client.HttpClient
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.inquest.uploader.api.config.IGW2Info
import org.inquest.uploader.api.entities.Build
import org.inquest.uploader.api.entities.GW2
import org.inquest.uploader.api.entities.Profession
import org.inquest.uploader.api.persistence.IPersistence
import org.inquest.uploader.api.persistence.get
import org.inquest.uploader.api.persistence.set
import org.inquest.uploader.bl.utils.AnyExtensions.LOG
import org.inquest.uploader.bl.utils.HttpClientExtensions.doGetRequest

/**
 * Implementation of [IGW2Info].
 *
 * @param persistence Used to store information
 */
class GW2Info(
    private val persistence: IPersistence
): IGW2Info {
    override val professions: List<Profession> by lazy {
        this.persistence.get<GW2>(GW2_INFO_KEY).getOrDefault(GW2()).professions
    }

    override fun pullGw2Entities() {
        LOG.info("Downloading from guild wars 2 api...")
        val build: Build = CLIENT.doGetRequest(BUILD_EP) {
            OBJECT_MAPPER.readValue(it)
        }
        LOG.debug("Downloaded build id {}.", build.id)
        val professions: List<Profession> = CLIENT.doGetRequest(PROFESSIONS_EP) {
            OBJECT_MAPPER.readValue(it)
        }
        LOG.debug("Downloaded professions list.")

        this.persistence.set(
            GW2_INFO_KEY, GW2(
                build,
                professions
            )
        ).onSuccess {
            LOG.info("Successfully downloaded guild wars 2 info.")
        }.onFailure {
            LOG.error("Failed to download guild wars 2 info!", it)
        }
    }

    companion object {
        private const val ENDPOINT = "https://api.guildwars2.com/v2/"
        private const val BUILD_EP = "${ENDPOINT}build"
        private const val PROFESSIONS_EP = "${ENDPOINT}specializations?ids=all"
        private const val GW2_INFO_KEY = "%GW2INFO%"

        private val OBJECT_MAPPER: ObjectMapper = ObjectMapper().registerKotlinModule()
        private val REQUEST_CONFIG: RequestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
        private val CLIENT: HttpClient = HttpClientBuilder.create().setDefaultRequestConfig(REQUEST_CONFIG).build();
    }
}