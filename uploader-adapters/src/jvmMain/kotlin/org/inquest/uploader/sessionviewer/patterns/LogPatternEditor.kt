package org.inquest.uploader.sessionviewer.patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.inquest.uploader.api.entities.ILog
import org.inquest.uploader.sessionviewer.SessionViewModel
import org.inquest.uploader.ui.commons.composables.HyperLink
import org.inquest.uploader.ui.commons.composables.InquestTextField
import org.inquest.uploader.ui.commons.utils.DIUtils
import org.inquest.uploader.ui.commons.utils.ModifierExtensions.Spacer
import org.inquest.uploader.ui.commons.utils.Spacers.HeightSpacer
import org.inquest.uploader.ui.commons.utils.View
import org.kodein.di.instance
import java.io.Serial

/**
 * Allows to edit the order of patterns that are evaluated for logs in the current session.
 */
data object LogPatternEditor : View {
    @Serial
    private const val serialVersionUID: Long = 1499591786027582445L

    @Composable
    @OptIn(ExperimentalLayoutApi::class)
    override fun Content() {
        val viewModel: SessionViewModel = DIUtils.localDirectDI().instance()

        DisposableEffect(Unit) {
            // Save whener this leaves the screen
            onDispose(viewModel::save)
        }

        Box(Modifier.fillMaxSize().padding(30.dp)) {
            Column {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val size = viewModel.patternLs.size
                    viewModel.patternLs.forEachIndexed { idx, pattern ->
                        pattern.Editor(viewModel, idx == size - 1)
                    }

                    AddButton(viewModel)
                }

                SubstitutionEditor()
            }

            SupportiveInfo()
        }
    }
}

@Composable
private fun BoxScope.SupportiveInfo() {
    val uriHandler: UriHandler = LocalUriHandler.current

    Column(Modifier.align(Alignment.BottomEnd), horizontalAlignment = Alignment.End) {
        Text("More Information on:")
        HyperLink(
            "DpsReport Jsons",
            "https://dps.report/api",
            color = MaterialTheme.colors.primary,
            onClick = uriHandler::openUri
        )
        HyperLink(
            "GW2EI Jsons",
            "https://baaron4.github.io/GW2-Elite-Insights-Parser/Json/index.html",
            color = MaterialTheme.colors.primary,
            onClick = uriHandler::openUri
        )
        HyperLink(
            "JSONata",
            "https://docs.jsonata.org/overview.html",
            color = MaterialTheme.colors.primary,
            onClick = uriHandler::openUri
        )
    }
}

/**
 * Adds a new [Pattern] to the current list.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FlowRowScope.AddButton(viewModel: SessionViewModel) {
    var showPopup: Boolean by remember { mutableStateOf(false) }

    IconButton(
        onClick = { showPopup = true },
        modifier = Modifier.align(Alignment.CenterVertically).size(40.dp)
    ) {
        Icon(
            Icons.Default.AddCircle,
            null,
            modifier = Modifier.fillMaxSize().aspectRatio(1F),
            tint = MaterialTheme.colors.primary,
        )

        if(!showPopup) {
            return@IconButton
        }

        Popup(
            onDismissRequest = { showPopup = false },
            offset = IntOffset(5, 10),
        ) {
            Box(
                Modifier
                    .background(MaterialTheme.colors.background).width(200.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .border(1.5.dp, MaterialTheme.colors.primary)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    DefaultText("Text") {
                        viewModel.patternLs += SimpleText()
                        showPopup = false
                    }

                    DefaultSpacer()

                    DefaultText("Log Info") {
                        viewModel.patternLs += LogInformation()
                        showPopup = false
                    }

                    DefaultSpacer()

                    DefaultText("JSONata") {
                        viewModel.patternLs += Jsonata()
                        showPopup = false
                    }
                }
            }
        }
    }
}

/**
 * Allows to edit all string substitutions.
 */
@Composable
private fun ColumnScope.SubstitutionEditor() {
    val viewModel: SessionViewModel = DIUtils.localDirectDI().instance()
    var expanded: Boolean by remember { mutableStateOf(false) }

    HeightSpacer(15.dp)

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { expanded = !expanded }.padding(bottom = 5.dp)
    ) {
        Text("Edit string substitutes", fontSize = 19.sp)

        Icon(
            if(expanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropUp,
            null,
            modifier = Modifier.size(20.dp),
        )
    }

    if(!expanded) {
        return
    }

    LazyColumn(
        modifier = Modifier.weight(0.5F).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(viewModel.substitutions.entries.toList()) {
            Substitution(it.key, it.value)
        }

        item {
            Substitution("", "", true)
        }
    }
}

/**
 * Enables the editing of one substitute.
 */
@Composable
private fun Substitution(
    prevKey: String,
    prevValue: String,
    addBtn: Boolean = false
) {
    val viewModel: SessionViewModel = DIUtils.localInstance()

    var key: String by remember { mutableStateOf(prevKey) }
    var value: String by remember { mutableStateOf(prevValue) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(50.dp)
    ) {
        Text("Value:")
        InquestTextField(key, { key = it })
        Text("Substitute:")
        InquestTextField(value, { value = it })

        IconButton({ viewModel.substitutions -= prevKey }) {
            Icon(
                Icons.Default.DeleteOutline, null,
                modifier = Modifier.size(30.dp),
            )
        }

        if(addBtn) {
            IconButton(
                {
                    viewModel.substitutions -= prevKey
                    viewModel.substitutions += key to value
                },
                modifier = Modifier.size(30.dp),
                enabled = key.isNotEmpty() && value.isNotEmpty()
            ) {
                Icon(Icons.Default.AddCircle, null)
            }
        }
    }
}

/**
 * General pattern which describes how a log is evaluated for the session.
 */
sealed class Pattern: View {
    /**
     * Name to display in the pattern list
     */
    abstract val displayName: String

    /**
     * When true, a popup is shown rather than expanding the composable which this is triggered from.
     */
    abstract val isPopup: Boolean

    /**
     * Evaluates the [log] to a string value.
     */
    abstract operator fun invoke(log: ILog): String

    @Composable
    final override fun Content() {
        Content {}
    }

    @Composable
    abstract fun Content(dismiss: () -> Unit)

    companion object {
        @Serial
        private const val serialVersionUID: Long = -3652420443332146955L

        /**
         * Serializable list of [Pattern]s.
         */
        class PatternList: ArrayList<Pattern>() {
            companion object {
                @Serial
                private const val serialVersionUID: Long = 3360083128553977269L
            }
        }
    }
}

/**
 * Enables the editing of a pattern.
 */
@Composable
private fun Pattern.Editor(viewModel: SessionViewModel, isLast: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(50.dp)
    ) {
        var edited: Boolean by remember { mutableStateOf(false) }

        if(edited) {
            // Edited -> Show the content of this padding
            if(isPopup) {
                // Editor is a popup
                Popup(
                    onDismissRequest = { edited = false },
                    properties = PopupProperties(focusable = true),
                    offset = IntOffset(5, 10)
                ) {
                    Content {
                        edited = false
                    }
                }
            } else {
                Content()
            }
        } else {
            // Not edited -> Show the display name
            Text(this@Editor.displayName)
        }

        IconButton(
            onClick = { edited = !edited }
        ) {
            Icon(
                Icons.Default.Edit,
                null,
                tint = if (edited) MaterialTheme.colors.primary else Color.White
            )
        }

        IconButton(
            onClick = { viewModel.patternLs -= this@Editor }
        ) {
            Icon(
                Icons.Default.Delete,
                null
            )
        }

        if(!isLast) {
            Modifier.width(1.dp).fillMaxHeight(0.8F).background(Color.White).Spacer()
        }
    }
}