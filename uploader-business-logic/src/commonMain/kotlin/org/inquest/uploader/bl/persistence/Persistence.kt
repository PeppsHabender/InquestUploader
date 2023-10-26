package org.inquest.uploader.bl.persistence

import org.inquest.uploader.api.config.IConfig
import org.inquest.uploader.api.entities.ILog
import org.inquest.uploader.api.persistence.IPersistence
import org.inquest.uploader.api.persistence.ISession
import org.inquest.uploader.api.persistence.get
import org.inquest.uploader.bl.utils.AnyExtensions.LOG
import org.inquest.uploader.bl.utils.InquestUploaderConstants
import org.inquest.uploader.bl.utils.PathExtensions.createDirSafe
import org.inquest.uploader.bl.utils.PathExtensions.localLogPath
import org.inquest.uploader.bl.utils.SerializationUtils.deserializeFromByteArray
import org.inquest.uploader.bl.utils.SerializationUtils.serializeToByteArray
import org.inquest.uploader.bl.utils.makeResult
import org.inquest.uploader.bl.utils.result
import org.rocksdb.RocksDB
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.reflect.KClass

/**
 * Implementation of [IPersistence].
 *
 * @param config Used to fetch a files boss
 * @param session New logs will be added here
 */
internal class Persistence(
    private val config: IConfig,
    private val session: ISession
): IPersistence {
    init {
        RocksDB.loadLibrary()
    }

    override fun <T : Any, U : Any> set(key: T, keyC: KClass<T>, value: U, valueC: KClass<U>): Result<Unit> = {
        LOG.debug("Storing {} under {}...", value, key)
        INQUEST_PERS.put(key.serializeToByteArray(keyC), value.serializeToByteArray(valueC))
    }.result().onSuccess {
        LOG.debug("Successfully stored {} under {}.", value, key)
    }.onFailure {
        LOG.debug("Failed to store {} under {}!", value, key, it)
    }

    override fun <U: Any, T: Any> fetch(key: U, keyC: KClass<U>, valueC: KClass<T>): Result<T> = INQUEST_PERS[key, keyC, valueC]

    override fun eiLog(from: Path): Result<ILog.JsonLog> = from.localLogPath(this.config).let { id ->
        if(id in EI_CACHE) {
            EI_CACHE[id].makeResult()
        } else {
            GW2EI_PERS.get<String, ILog.JsonLog>(id).onSuccess {
                EI_CACHE[id] = it
            }.getOrThrow().makeResult()
        }
    }

    override fun storeEiLog(from: Path, log: ILog.JsonLog): Result<Unit> = {
        GW2EI_PERS[from.localLogPath(this.config)] = log
        this.session.add(from, log)
    }.result()

    override fun dpsReportLog(from: Path): Result<ILog.DpsLog> = from.localLogPath(this.config).let { id ->
        if(id in DPS_REPORT_CACHE) {
            DPS_REPORT_CACHE[id].makeResult()
        } else {
            get<ILog.DpsLog>(id).onSuccess {
                DPS_REPORT_CACHE[id] = it
            }
        }
    }

    override fun storeDpsReportLog(from: Path, log: ILog.DpsLog): Result<Unit> = {
        INQUEST_PERS[from.localLogPath(this.config)] = log
        this.session.add(from, log)
    }.result()

    companion object {
        private val INQUEST_PATH: Path = InquestUploaderConstants.LOCAL_STORAGE.resolve("inquest.pers").createDirSafe()
        private val INQUEST_PERS: RocksDB = RocksDB.open(INQUEST_PATH.absolutePathString())

        private val GW2EI_PATH: Path = InquestUploaderConstants.LOCAL_STORAGE.resolve("gw2ei.pers").createDirSafe()
        private val GW2EI_PERS: RocksDB = RocksDB.open(GW2EI_PATH.absolutePathString())

        private val DPS_REPORT_CACHE: MutableMap<String, ILog.DpsLog> = mutableMapOf()
        private val EI_CACHE: MutableMap<String, ILog.JsonLog> = mutableMapOf()

        private operator fun <U: Any, T: Any> RocksDB.get(key: U, keyC: KClass<U>, valueC: KClass<T>): Result<T> = try {
            getImpl(key, keyC, valueC)
        } catch (ex: Exception) {
            Result.failure(ex)
        }

        private fun <U: Any, T: Any> RocksDB.getImpl(key: U, keyC: KClass<U>, valueC: KClass<T>): Result<T> =
            get(key.serializeToByteArray(keyC))?.let {
                deserializeFromByteArray(it, valueC)
            }.makeResult()

        private inline operator fun <reified U: Any, reified T: Any> RocksDB.get(key: U): Result<T> = {
            get(key.serializeToByteArray())?.let {
                deserializeFromByteArray<T>(it)
            }
        }.result()

        private inline operator fun <reified T: Any, reified U: Any> RocksDB.set(key: T, value: U) {
            LOG.debug("Storing {} under {}...", value, key)
            put(key.serializeToByteArray(), value.serializeToByteArray())
            LOG.debug("Successfully stored {} under {}...", value, key)
        }
    }
}
