package org.inquest.uploader.bl.utils

import kotlinx.coroutines.CancellationException

/**
 * Converts [this] into a [Result.success] if non-null.
 */
fun <T> T?.makeResult(): Result<T> = if (this == null) {
    Result.failure(NullPointerException())
} else {
    Result.success(this)
}

/**
 * Converts [this] into a [Result.failure]
 */
fun <E: Throwable, T> E.makeResult(): Result<T> = Result.failure(this)

/**
 * Invokes [this] and returns the result as a [Result].
 *
 * @return [Result] of this invocation
 */
inline fun <reified T> (() -> T?).result(): Result<T> = try {
    this().makeResult()
} catch (ex: Throwable) {
    Result.failure(ex)
}

/**
 * Invokes [this] and returns the result as a [Result].
 *
 * @return [Result] of this invocation
 */
suspend inline fun <reified T> (suspend () -> T?).result(): Result<T> = try {
    this().makeResult()
} catch (cancel: CancellationException) {
    throw cancel
} catch (ex: Throwable) {
    Result.failure(ex)
}