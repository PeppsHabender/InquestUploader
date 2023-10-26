package org.inquest.uploader.ui.style

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val colors = darkColors(
    primary = Color(red = 237, green = 41, blue = 57),
)

private val shapes = Shapes(
    small = RoundedCornerShape(5.dp),
)

/**
 * The inquest theme.
 */
@Composable
internal fun InquestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = colors,
        shapes = shapes,
        content = content,
    )
}
