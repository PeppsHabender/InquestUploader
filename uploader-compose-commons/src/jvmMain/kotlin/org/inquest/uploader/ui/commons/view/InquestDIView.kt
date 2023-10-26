package org.inquest.uploader.ui.commons.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.inquest.uploader.api.logs.BossToName
import org.inquest.uploader.api.logs.FolderToBoss
import org.inquest.uploader.api.logs.StoredLogs
import org.inquest.uploader.api.persistence.IPersistence
import org.inquest.uploader.bl.utils.IPersistenceExtensions.bossToName
import org.inquest.uploader.bl.utils.IPersistenceExtensions.folderToBoss
import org.inquest.uploader.bl.utils.IPersistenceExtensions.storedLogs
import org.inquest.uploader.ui.commons.utils.View
import org.inquest.uploader.ui.commons.utils.ViewModel
import org.kodein.di.DI
import org.kodein.di.compose.localDI
import org.kodein.di.compose.withDI
import org.kodein.di.subDI
import java.io.Serial

/**
 * Abstract view that enables dependency injection specific to its tree using
 * [createSubDI].
 */
abstract class InquestDIView: View {

    @Composable
    final override fun Content() = withDI(localDI().createSubDI()) {
        TheContent()
    }

    @Composable
    protected abstract fun TheContent()

    private fun DI.createSubDI(): DI = subDI(
        parentDI = this,
        allowSilentOverride = true,
    ) {
        initSubDI()
    }

    protected open fun DI.MainBuilder.initSubDI() {}

    companion object {
        @Serial
        private const val serialVersionUID: Long = -8153996393224494800L
    }
}

/**
 * The global view model of the application.
 */
class GlobalViewModel(
    private val persistence: IPersistence
): ViewModel {
    /**
     * Reference to [IPersistence.bossToName].
     */
    var bossToName: BossToName by mutableStateOf(this.persistence.bossToName)
        private set
    /**
     * Reference to [IPersistence.folderToBoss].
     */
    var folderToBoss: FolderToBoss by mutableStateOf(this.persistence.folderToBoss)
        private set
    /**
     * Reference to [IPersistence.storedLogs].
     */
    var storedLogs: StoredLogs by mutableStateOf(this.persistence.storedLogs)
        private set

    /**
     * Updates the model to the newest values in the persistence.
     */
    fun updateBossList() {
        this.bossToName = this.persistence.bossToName
        this.folderToBoss = this.persistence.folderToBoss
    }

    /**
     * Updates the stored logs in the persistence.
     */
    fun updateStoredLogs() {
        this.storedLogs = this.persistence.storedLogs
    }
}