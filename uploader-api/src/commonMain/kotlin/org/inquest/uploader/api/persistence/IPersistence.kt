package org.inquest.uploader.api.persistence

import org.inquest.uploader.api.entities.ILog
import java.nio.file.Path
import kotlin.reflect.KClass

/**
 * Responsible for handling the persisting of all data needed/taken by the inquest.
 */
interface IPersistence {
    /**
     * Stores the given [value] of type [valueC] under the [key] of type [keyC].
     */
    operator fun <T: Any, U: Any> set(key: T, keyC: KClass<T>, value: U, valueC: KClass<U>): Result<Unit>

    /**
     * Fetches a value of type [valueC] from the [key] of type [keyC].
     */
    fun <U: Any, T: Any> fetch(key: U, keyC: KClass<U>, valueC: KClass<T>): Result<T>

    /**
     * Fetches the elite insights [ILog.JsonLog] for the given path.
     */
    fun eiLog(from: Path): Result<ILog.JsonLog>

    /**
     * Stores the given elite insights [log] under the given path.
     */
    fun storeEiLog(from: Path, log: ILog.JsonLog): Result<Unit>

    /**
     * Fetches the dps.report [ILog.DpsLog] for the given path.
     */
    fun dpsReportLog(from: Path): Result<ILog.DpsLog>

    /**
     * Stores the given dps.report [log] under the given path.
     */
    fun storeDpsReportLog(from: Path, log: ILog.DpsLog): Result<Unit>
}

/**
 * Contains all logs, that were uploaded/analyzed since the inquest
 * started collecting data.
 */
interface ISession {
    val currentLogs: Collection<ILog>

    /**
     * Adds a new [log] from [from] to the session.
     */
    fun add(from: Path, log: ILog)

    /**
     * Clears the current session.
     */
    fun clear()
}

/**
 * Reified version of [IPersistence.fetch].
 */
inline operator fun <reified U: Any> IPersistence.get(key: String): Result<U> = fetch(key, String::class, U::class)

/**
 * Reified version of [IPersistence.set].
 */
inline operator fun <reified U: Any> IPersistence.set(key: String, value: U): Result<Unit> = set(key, String::class, value, U::class)