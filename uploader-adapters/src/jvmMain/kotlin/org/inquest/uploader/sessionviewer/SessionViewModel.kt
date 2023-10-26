package org.inquest.uploader.sessionviewer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.inquest.uploader.api.persistence.IPersistence
import org.inquest.uploader.api.persistence.get
import org.inquest.uploader.api.persistence.set
import org.inquest.uploader.sessionviewer.patterns.Jsonata
import org.inquest.uploader.sessionviewer.patterns.LogInformation
import org.inquest.uploader.sessionviewer.patterns.Pattern
import org.inquest.uploader.sessionviewer.patterns.SimpleText
import org.inquest.uploader.sessionviewer.patterns.evaluate
import org.inquest.uploader.ui.commons.utils.ViewModel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serial
import java.io.Serializable
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * View model for a [SessionOverview]. Contains all current patterns and substitutes.
 */
class SessionViewModel(private val persistence: IPersistence): ViewModel {
    var patternLs: List<Pattern> by mutableStateOf(
        this.persistence.get<ByteArray>(PATTERN_KEY).let {
            var found = Pattern.Companion.PatternList()
            if(it.isSuccess) {
                found = it.getOrThrow().toObject()
            }

            if(found.isEmpty()) DEFAULT_PATTERNS else found
        }
    )
    var substitutions: Map<String, String> by mutableStateOf(
        this.persistence.get<ByteArray>(SUBSTITUTIONS_KEY).let {
            if(it.isSuccess) {
                it.getOrThrow().toObject()
            } else { DEFAULT_SUBSTITUTIONS }
        }
    )

    /**
     * Substitutes [str] for ui display.
     */
    fun substitute(str: String) = this.substitutions.getOrDefault(str, str)

    /**
     * Saves the current list of patterns and substitutions.
     */
    fun save() {
        this.persistence[PATTERN_KEY] = Pattern.Companion.PatternList().apply {
            addAll(this@SessionViewModel.patternLs)
        }.toByteArray()
        this.persistence[SUBSTITUTIONS_KEY] = SubstitutionMap().also {
            it.putAll(this.substitutions)
        }.toByteArray()
    }

    companion object {
        private const val PATTERN_KEY = "%LOG_PATTERN%"
        private const val SUBSTITUTIONS_KEY = "%STRING_SUBSTITUTES%"

        private val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE MMM dd yyyy 'at' HH:mm", Locale.US)
        private val DEFAULT_PATTERNS: List<Pattern> by lazy {
            listOf(
                Jsonata(
                    "encounter.success ? \":white_check_mark:\" : \":x:\"",

                    "success ? \":white_check_mark:\" : \":x:\""
                ),
                SimpleText("-"),
                LogInformation(),
                Jsonata(
                    "encounter.cm ? \"[CM]\" : \"\"",
                    "cm ? \"[CM]\" : \"\""
                ),
                SimpleText("-"),
                LogInformation(
                    "Encounter Time",
                ) { l ->
                    l.evaluate({ it.encounterTimeInstant?.format(DATE_TIME_FORMATTER) }) {
                        it.recordedOn.let(DATE_TIME_FORMATTER::format)
                    }
                },
                SimpleText("-"),
                LogInformation("Remote URL") { l ->
                    l.evaluate({ it.permalink ?: "" }) { "" }
                }
            )
        }
        private val DEFAULT_SUBSTITUTIONS: Map<String, String> = mapOf(
            ":white_check_mark:" to "✅",
            ":x:" to "❌"
        )

        private fun Serializable.toByteArray(): ByteArray = ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use {
                it.writeObject(this)
                it.flush()
            }

            baos.toByteArray()
        }

        private inline fun <reified T: Serializable> ByteArray.toObject(): T = ByteArrayInputStream(this).use { bais ->
            ObjectInputStream(bais).use {
                it.readObject() as T
            }
        }
    }
}

/**
 * Serializable map of strings.
 */
@kotlinx.serialization.Serializable
private class SubstitutionMap: LinkedHashMap<String, String>() {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -497185796166637881L
    }
}