package org.inquest.uploader.sessionviewer.patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.inquest.uploader.api.entities.ILog
import org.inquest.uploader.sessionviewer.SessionOverview
import org.inquest.uploader.ui.commons.utils.ModifierExtensions.Spacer

/**
 * Default column composable for within the [SessionOverview].
 */
@Composable
internal fun DefaultColumn(vararg blocks: @Composable LazyItemScope.() -> Unit) = LazyColumn(
    modifier = Modifier.padding(start = 10.dp, bottom = 10.dp, end = 10.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp)
) {
    itemsIndexed(blocks) { idx, block ->
        block()

        if(idx != blocks.size - 1) {
            Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colors.primary).Spacer()
        }
    }
}

/**
 * Default text composable for within the [SessionOverview].
 */
@Composable
internal fun DefaultText(text: String, onClick: () -> Unit) = Text(
    text,
    fontSize = 18.sp,
    textAlign = TextAlign.Center,
    modifier = Modifier.clickable(onClick = onClick).fillMaxWidth()
)

/**
 * Default spacer composable for within the [SessionOverview].
 */
@Composable
internal fun DefaultSpacer() =
    Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colors.primary).Spacer()

/**
 * Evaluates the given [ILog] against the given blocks.
 *
 * @param dpsLogBlock Block to evaluate a [ILog.DpsLog]
 * @param jsonLogBlock Block to evaluate a [ILog.JsonLog]
 * @param preferDpsLog If true, a [ILog.DpsLog] is preferred when actually [ILog.Both] are present
 */
internal inline fun <T> ILog.evaluate(
    dpsLogBlock: (ILog.DpsLog) -> T,
    preferDpsLog: Boolean = true,
    jsonLogBlock: (ILog.JsonLog) -> T,
) = when(this) {
    is ILog.DpsLog -> dpsLogBlock(this)
    is ILog.JsonLog -> jsonLogBlock(this)
    is ILog.Both -> if(preferDpsLog) dpsLogBlock(this.dpsLog) else jsonLogBlock(this.jsonLog)
}