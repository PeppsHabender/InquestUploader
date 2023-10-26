package org.inquest.uploader.bl.utils

import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatters {
    /**
     * Formatter which formats a logfiles prefix.
     */
    val LOG_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss'.zevtc'", Locale.US)

    /**
     * Basic readable date format.
     */
    val READABLE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE dd. MMM yyyy HH:mm", Locale.US)
}