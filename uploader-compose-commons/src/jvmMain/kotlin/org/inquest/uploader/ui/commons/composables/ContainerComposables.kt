package org.inquest.uploader.ui.commons.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.inquest.uploader.ui.commons.utils.ModifierExtensions.ifNotNull

/**
 * Container which is underlined by a bar of width [barFraction].
 *
 * @param showBar If true bar is shown, if false bar is hidden
 * @param barFraction Fraction of the container the bar takes
 * @param modifier Container modifier
 * @param content Content of the container
 */
@Composable
fun UnderlinedContainer(
    showBar: Boolean = true,
    barFraction: Float = 0.5F,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var width: Int by remember {
        mutableStateOf(0)
    }

    Column(
        modifier = modifier.onSizeChanged {
             width = it.width
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
        Spacer(Modifier.height(2.dp))
        if(!showBar) {
            return@Column
        }

        Box(
            modifier = Modifier
                .height(3.5.dp).clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colors.primary)
                .width(width.dp * barFraction)
        )
    }
}

/**
 * Veeeerry basic and non-error prone implementation of a compose table.
 *
 * @param items Items to display
 * @param headers List of headers, should have same length as cells
 * @param cells The cell content composables
 * @param cellHeight Height of each cell
 * @param cellWidths Respective width of the i-th cell
 * @param horizontalArrangement [Arrangement.Horizontal]
 * @param verticalAlignment [Alignment.Vertical]
 * @param extend if true, the table extends itself into the remaining space with blank cells
 * @param modifier [Modifier]
 */
@Composable
fun <T> Table(
    items: List<T>,
    headers: List<String?>,
    vararg cells: @Composable ColumnScope.(T) -> Unit,
    cellHeight: Dp = 50.dp,
    cellWidths: List<Dp?> = (headers.indices).map { null },
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    extend: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Row(
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        modifier = modifier.verticalScroll(scrollState)
    ) {
        val cellMod: Modifier = Modifier.height(cellHeight).padding(horizontal = 5.dp)
        cells.forEachIndexed { i, cell ->
            val cellWidth: Dp? = cellWidths[i]
            val divider = @Composable {
                if(cellWidth != null) {
                    Divider(Modifier.width(cellWidth), Color.White)
                }
            }

            Column(
                modifier = Modifier.ifNotNull(cellWidth, Modifier::width)
            ) {
                divider()

                Box(cellMod, contentAlignment = Alignment.CenterStart) {
                    headers[i]?.let {
                        Text(it, style = MaterialTheme.typography.h6, fontSize = 18.sp)
                    }
                }

                divider()

                items.forEach {
                    Box(cellMod, contentAlignment = Alignment.CenterStart) {
                        this@Column.cell(it)
                    }
                }

                divider()
            }
        }

        if(extend) {
            Column(Modifier.weight(1F, false)) {
                Divider(color = Color.White)
                Box(cellMod)
                Divider(color = Color.White)

                items.indices.forEach { _ ->
                    Box(cellMod)
                }
                Divider(color = Color.White)
            }
        }
    }
}