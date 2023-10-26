package org.inquest.uploader.ui.commons.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Loading bar which loads every [LoadingMeta] sequentially and displays a
 * bar dependent on their [LoadingMeta.weight]s. Show the designated text under
 * the loading bar.
 *
 * @param metas Loading jobs
 */
@Composable
fun LoadingBar(vararg metas: LoadingMeta) {
    val totalWeight: Float = metas.map(LoadingMeta::weight).sum().toFloat()

    var currTask: String by remember { mutableStateOf("") }
    var progress: Float by remember { mutableStateOf(0F) }

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            for(meta in metas.filter(LoadingMeta::execute)) {
                currTask = meta.text
                meta.runnable(this)
                progress += meta.weight.toFloat() / totalWeight
            }
        }
    }

    Column(Modifier.padding(top = 5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(10.dp).padding(horizontal = 30.dp).clip(RoundedCornerShape(10.dp))
        )
        Text(
            currTask,
            modifier = Modifier.padding(start = 30.dp)
        )
    }
}

/**
 * Describes a loading job for the [LoadingBar]
 *
 * @param text Textual description
 * @param weight Importance of this job
 * @param execute Wether this should be executed or not
 * @param runnable The job
 */
data class LoadingMeta(
    val text: String,
    val weight: Int,
    val execute: Boolean = true,
    val runnable: suspend CoroutineScope.() -> Unit,
)