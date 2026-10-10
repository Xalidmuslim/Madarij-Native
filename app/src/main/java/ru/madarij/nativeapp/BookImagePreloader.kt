package ru.madarij.nativeapp

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * A tiny, process-scoped cache for the five existing book bitmaps.
 *
 * BitmapFactory.decodeResource runs only on Dispatchers.IO. The decoded full-color
 * bitmaps are published on the main thread to Compose; no new hardware layers,
 * effects, alpha blending or image scaling changes are introduced.
 */
internal object BookImagePreloader {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = ConcurrentHashMap.newKeySet<Int>()
    private val cached = mutableStateMapOf<Int, ImageBitmap>()
    private val bookResources = intArrayOf(
        R.drawable.reference_paper,
        R.drawable.library_hero,
        R.drawable.navigation_leather,
        R.drawable.paper_light,
        R.drawable.paper_sage
    )

    fun preload(context: Context) {
        // Start the home image and shared paper first, then the secondary screens.
        bookResources.forEach { request(context, it) }
    }

    fun request(context: Context, id: Int) {
        if (cached.containsKey(id) || !inFlight.add(id)) return
        val appContext = context.applicationContext
        scope.launch {
            try {
                val options = BitmapFactory.Options().apply {
                    inScaled = false  // drawable-nodpi: preserve the exact bitmap pixels
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                }
                val bitmap = BitmapFactory.decodeResource(appContext.resources, id, options)
                if (bitmap != null) {
                    val image = bitmap.asImageBitmap()
                    withContext(Dispatchers.Main.immediate) { cached[id] = image }
                }
            } catch (_: OutOfMemoryError) {
                // Existing opaque paper/color placeholders remain readable.
            } catch (_: RuntimeException) {
                // A missing/invalid decoration must never crash the book.
            } finally {
                inFlight.remove(id)
            }
        }
    }

    @Composable
    fun image(id: Int): ImageBitmap? = cached[id]
}

@Composable
internal fun PreloadedBookImage(
    resourceId: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.FillBounds,
    alignment: Alignment = Alignment.Center
) {
    val context = LocalContext.current
    LaunchedEffect(resourceId) { BookImagePreloader.request(context, resourceId) }
    val bitmap = BookImagePreloader.image(resourceId)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
            alignment = alignment
        )
    }
}
