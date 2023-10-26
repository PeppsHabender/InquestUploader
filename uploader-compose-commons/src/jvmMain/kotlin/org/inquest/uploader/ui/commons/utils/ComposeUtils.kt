package org.inquest.uploader.ui.commons.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import org.inquest.uploader.ui.commons.composables.UnderlinedContainer
import org.inquest.uploader.ui.commons.utils.ModifierExtensions.Spacer
import org.kodein.di.DirectDI
import org.kodein.di.compose.localDI
import org.kodein.di.direct
import org.kodein.di.instance
import java.awt.Toolkit

// I just like those name better..
typealias View = Screen

typealias ViewModel = ScreenModel

/**
 * Utils related to compose and voyager.
 */
object ComposeUtils {
    /**
     * @return The current screen size as a [DpSize]
     */
    fun screenSize(): DpSize {
        val screenSize = Toolkit.getDefaultToolkit().screenSize

        return DpSize(screenSize.width.dp, screenSize.height.dp)
    }

    /**
     * Item for navigating an underlying [TabNavigator].
     *
     * @param tab Tab to create item for
     */
    @Composable
    fun TabNavigationItem(tab: Tab) {
        val nav: TabNavigator = LocalTabNavigator.current
        val selected: Boolean = nav.current.key == tab.key
        val baseModifier: Modifier = if(selected) Modifier else Modifier.clickable {
            nav.current = tab
        }

        UnderlinedContainer(
            modifier = baseModifier,
            showBar = selected,
        ) {
            Text(
                tab.options.title,
                color = if (selected) MaterialTheme.colors.primary else Color.White,
            )
        }
    }

    /**
     * Darkens this color by [factor].
     */
    fun Color.darker(factor: Float = 0.7f) = this.copy(
        red = this.red * factor,
        green = this.green * factor,
        blue = this.blue * factor,
    )
}

/**
 * Utils related to dependency injection.
 */
object DIUtils {
    /**
     * Current local di.
     */
    @Composable
    fun localDirectDI(): DirectDI = localDI().direct

    /**
     * Current local instance of [T].
     */
    @Composable
    inline fun <reified T: Any> localInstance(): T = localDirectDI().instance()
}

/**
 * Spacers..
 *
 * Duh
 */
object Spacers {
    /**
     * Spacer with height [height].
     */
    @Composable
    fun HeightSpacer(height: Dp) = Modifier.height(height).Spacer()

    /**
     * Spacer with [width].
     */
    @Composable
    fun WidthSpacer(width: Dp) = Modifier.width(width).Spacer()

    /**
     * Spacer with weight [weight].
     */
    @Composable
    fun RowScope.WeightSpacer(weight: Float = 1F) = Modifier.weight(weight).Spacer()

    /**
     * Spacer with height [height].
     */
    @Composable
    fun ColumnScope.HeightSpacer(height: Dp = 1.5.dp) = Modifier.height(height).Spacer()

    /**
     * Spacer with weight [weight].
     */
    @Composable
    fun ColumnScope.WeightSpacer(weight: Float = 1F) = Modifier.weight(weight).Spacer()
}

/**
 * Extensions on [Modifier].
 */
object ModifierExtensions {
    /**
     * Converts this modifier into a [Spacer].
     */
    @Composable
    fun Modifier.Spacer() = androidx.compose.foundation.layout.Spacer(this)

    /**
     * Performs [block] with [value] when value is not null.
     */
    inline fun <T> Modifier.ifNotNull(value: T?, block: Modifier.(T) -> Modifier): Modifier = value?.let {
        this.block(it)
    } ?: this
}