package org.inquest.uploader.sessionviewer.patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.api.jsonata4java.expressions.Expressions
import com.fasterxml.jackson.databind.ObjectMapper
import org.inquest.uploader.api.entities.ILog
import org.inquest.uploader.bl.utils.result
import org.inquest.uploader.ui.commons.composables.InquestTextField
import java.io.Serial

/**
 * Pattern which uses the [JSONata](https://jsonata.org/) api in order to fetch information about
 * the logs in the session.
 */
class Jsonata(
    private var jsonataDpsLog: String = "id",
    private var jsonataJsonLog: String = "eiEncounterId"
): Pattern() {
    override val displayName: String = "JSONata"
    override val isPopup: Boolean = true

    override operator fun invoke(log: ILog): String = when (log) {
        is ILog.DpsLog -> evaluate(this.jsonataDpsLog, log).getOrDefault(EVAL_ERROR)
        is ILog.JsonLog -> evaluate(this.jsonataJsonLog, log).getOrDefault(EVAL_ERROR)
        // Try for the dps log first (should take less time to eval)
        is ILog.Both -> evaluate(this.jsonataDpsLog, log.dpsLog).let {
            if(it.isSuccess) {
                it
            } else {
                evaluate(this.jsonataJsonLog, log.jsonLog)
            }
        }.getOrDefault(EVAL_ERROR)
    }

    private fun evaluate(expr: String, log: ILog): Result<String> = {
        Expressions.parse(expr).evaluate(OBJECT_MAPPER.valueToTree(log))
    }.result().map {
        it.asText().ifEmpty(it::toString)
    }

    @Composable
    override fun Content(dismiss: () -> Unit) {
        var selected: Boolean by remember { mutableStateOf(true) }
        var jsonataDpsLogText by remember { mutableStateOf(this.jsonataDpsLog) }
        var jsonataJsonLogText by remember { mutableStateOf(this.jsonataJsonLog) }

        Box(
            Modifier
                .background(MaterialTheme.colors.background)
                .height(300.dp).clip(RoundedCornerShape(5.dp))
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { selected = true }) {
                        Text("Dps Log", color = if(selected) MaterialTheme.colors.primary else Color.White)
                    }

                    Spacer(Modifier.width(1.dp).height(30.dp).background(Color.White))

                    TextButton(onClick = { selected = false }) {
                        Text("Analyzed Log", color = if(selected) Color.White else MaterialTheme.colors.primary)
                    }
                }

                if(selected) {
                    InquestTextField(
                        jsonataDpsLogText,
                        onValueChange = {
                            jsonataDpsLogText = it
                            this@Jsonata.jsonataDpsLog = it
                        },
                        modifier = Modifier.widthIn(min = 500.dp, max = 1000.dp).height(300.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = Color.White)
                    )
                } else {
                    InquestTextField(
                        jsonataJsonLogText,
                        onValueChange = {
                            jsonataJsonLogText = it
                            this@Jsonata.jsonataJsonLog = it
                        },
                        modifier = Modifier.widthIn(min = 500.dp, max = 1000.dp).height(300.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = Color.White)
                    )
                }
            }
        }
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = 5737151148072447327L
        private const val EVAL_ERROR = "EvaluationError"

        private val OBJECT_MAPPER = ObjectMapper()
    }
}