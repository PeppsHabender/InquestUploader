package org.inquest.uploader.sessionviewer.patterns

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.inquest.uploader.api.entities.ILog
import org.inquest.uploader.ui.commons.composables.InquestTextField
import java.io.Serial

/**
 * Pattern to display a simple text.
 */
class SimpleText(
    override var displayName: String = "Some Text"
): Pattern() {
    override val isPopup: Boolean = false

    override operator fun invoke(log: ILog): String = this.displayName

    @Composable
    override fun Content(dismiss: () -> Unit) {
        var text: String by remember {
            mutableStateOf(this.displayName)
        }

        InquestTextField(
            text,
            onValueChange = {
                text = it
                this.displayName = text
            },
            modifier = Modifier.fillMaxHeight()
        )
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = 8045019837791549452L
    }
}