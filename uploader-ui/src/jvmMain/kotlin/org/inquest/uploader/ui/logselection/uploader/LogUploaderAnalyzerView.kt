package org.inquest.uploader.ui.logselection.uploader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import org.inquest.uploader.api.logs.LogFile
import org.inquest.uploader.api.logs.StoredLogs.Companion.LogState
import org.inquest.uploader.ui.commons.composables.DynamicText
import org.inquest.uploader.ui.commons.utils.DIUtils
import org.inquest.uploader.ui.commons.utils.ModifierExtensions.Spacer
import org.inquest.uploader.ui.commons.utils.Spacers.WeightSpacer
import org.kodein.di.instance
import java.nio.file.Path

/**
 * Composable which enables the user to upload or analyze the [currLog].
 */
@Composable
fun LogUploaderAnalyzerView(currLog: LogFile, handled: Set<Path>, handle: (Path) -> Unit, finishHandling: (Path) -> Unit) {
    val viewModel: LogUploaderAnalyzerViewModel = DIUtils.localDirectDI().instance()

    when(currLog.state) {
        LogState.UPLOADED_ANALZED -> Text("Seems like your selected tab cannot display anything for this log")
        else -> NotUploadedLog(viewModel, currLog, handled, handle, finishHandling)
    }
}

@Composable
private fun NotUploadedLog(
    viewModel: LogUploaderAnalyzerViewModel,
    currLog: LogFile,
    handled: Set<Path>,
    handle: (Path) -> Unit,
    finishHandling: (Path) -> Unit
) {
    val loading: Boolean = currLog.realFile in handled
    var currError: Throwable? by remember { mutableStateOf(null) }

    val onError: (Throwable) -> Unit = { currError = it }
    val onSuccess: () -> Unit = {finishHandling(currLog.realFile) }

    currError?.let {
        ErrorPopup(it) {
            currError = null
            finishHandling(currLog.realFile)
        }
    }

    val upload: MutableState<Boolean> = remember { mutableStateOf(false) }
    val analyze: MutableState<Boolean> = remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 30.dp).fillMaxSize()
    ) {
        Text(
            currLog.state.infoStr(currLog.realFile),
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.align(Alignment.End)
        ) {
            TextCheckBox("Upload to\ndps.report", !currLog.state.isUploaded(), upload)
            TextCheckBox("Analyze using\nGW2EI", !currLog.state.isAnalyzed(), analyze)

            Button(
                onClick = {
                    handle(currLog.realFile)
                    if(upload.value) {
                        viewModel.upload(currLog.realFile, onSuccess, onError)
                    }
                    if(analyze.value) {
                        viewModel.parse(currLog.realFile, onSuccess, onError)
                    }
                },
                enabled = !loading && (upload.value || analyze.value)
            ) {
                Icon(Icons.Default.Upload, null)
            }

            if(loading) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun TextCheckBox(text: String, visible: Boolean, state: MutableState<Boolean>) {
    if(!visible) {
        return
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            state.value,
            { state.value = it },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.primary)
        )
        Text(text, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ErrorPopup(ex: Throwable, hide: () -> Unit) {
    Popup(
        alignment = Alignment.BottomEnd,
    ) {
        Column(
            modifier = Modifier
                .width(300.dp).wrapContentHeight()
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colors.primary),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Failed to upload or analyze!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.background
                )
                WeightSpacer()
                Text(
                    "X",
                    modifier = Modifier.padding(2.dp).clickable(onClick = hide),
                    color = MaterialTheme.colors.background,
                    fontWeight = FontWeight.Bold
                )
            }
            Modifier.height(1.5.dp).background(MaterialTheme.colors.background).Spacer()
            DynamicText(ex.message ?: "Unknown Error", color = MaterialTheme.colors.background)
        }
    }
}

private fun LogState.infoStr(path: Path): String {
    var str = "Selected log $path has not yet been "
    if(!isUploaded()) {
        str += "uploaded "
    }

    if(this == LogState.IDLE) {
        str += "nor "
    }

    if(!isAnalyzed()) {
        str += "analyzed "
    }

    return str + "by the application..."
}