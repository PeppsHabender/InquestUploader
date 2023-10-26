package org.inquest.uploader.ui.commons.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

/**
 * Image that can be clicked and contains a text above.
 *
 * @param resPath Path to the image resource
 * @param text Text to show above image
 * @param modifier [Modifier]
 * @param onClick Action to be performed on click
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ClickableImage(
    resPath: String,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }

    var hovered: Boolean by remember { mutableStateOf(false) }
    val scale: Float by animateFloatAsState(if(hovered) 1.5F else 1F)
    val bgAlpha: Float by animateFloatAsState(if(hovered) 0.2F else 0.6F)
    val textAlpha: Float by animateFloatAsState(if(hovered) 0F else 1F)

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .background(Color.Transparent)
            .onPointerEvent(PointerEventType.Enter) {
                hovered = true
            }.onPointerEvent(PointerEventType.Exit) {
                hovered = false
            }.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Image(
            painter = painterResource(resPath),
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.Center) // keep the image centralized into the Box
                .graphicsLayer(
                    // zoom limits
                    scaleX = scale,
                    scaleY = scale,
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color.Black.copy(alpha = bgAlpha))
        )
        Text(
            text,
            color = Color.White.copy(alpha = textAlpha),
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Internal cache for [ImageBitmap]s.
 */
private object ImageCache {
    var cache: Map<String, ImageBitmap> = emptyMap()
}

/**
 * Loads an image asynchronously and stores it's data inside the [ImageCache].
 *
 * @param url Url to load from,
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex.
 * background)
 * @param alignment Optional alignment parameter used to place the [ImageBitmap] in the given
 * bounds defined by the width and height
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be used
 * if the bounds are a different size from the intrinsic size of the [ImageBitmap]
 * @param alpha Optional opacity to be applied to the [ImageBitmap] when it is rendered onscreen
 * @param colorFilter Optional ColorFilter to apply for the [ImageBitmap] when it is rendered
 * onscreen
 * @param filterQuality Sampling algorithm applied to the [bitmap] when it is scaled and drawn
 * into the destination. The default is [FilterQuality.Low] which scales using a bilinear
 * sampling algorithm
 * @param placeholder Composable that is shown whilst the image is loaded in the background
 */
@Composable
fun AsyncImage(
    url: String,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
    placeholder: @Composable () -> Unit = {
        Text("...")
    }
) {
    var img: ImageBitmap? by remember {
        mutableStateOf(ImageCache.cache[url])
    }

    // Image not loaded or we lost track of it
    if(img == null || ImageCache.cache[url] == null) {
        LaunchedEffect(Unit) {
            img = withContext(Dispatchers.IO) {
                try {
                    URI.create(url).toURL().openStream().buffered().use(::loadImageBitmap)
                } catch (_: Throwable) {
                    null
                }
            }?.also {
                ImageCache.cache += url to it
            }
        }
        placeholder()
        return
    }

    if(img != ImageCache.cache[url]) {
        // Update image in the cache if necessary
        img = ImageCache.cache[url]!!
    }

    Image(
        bitmap = img!!,
        contentDescription = null,
        modifier = modifier,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        filterQuality = filterQuality,
    )
}