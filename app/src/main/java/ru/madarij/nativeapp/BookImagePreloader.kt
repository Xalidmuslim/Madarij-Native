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
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
    // Bound decoding to avoid seven simultaneous 32-bit bitmaps competing
    // with Compose, Room startup and text layout for CPU and heap.
    private val decodeGate = Semaphore(2)
    private val inFlight = ConcurrentHashMap.newKeySet<Int>()
    private val cached = mutableStateMapOf<Int, ImageBitmap>()
    private val bookResources = intArrayOf(
        R.drawable.reference_paper,
        R.drawable.library_hero,
        R.drawable.navigation_leather,
        R.drawable.paper_light,
        R.drawable.paper_sage,
        R.drawable.card_paper,
        R.drawable.cta_leather
    )

    fun preload(context: Context) {
        // Home's backdrop and navigation are prioritized for first-frame paint.
        // Delay secondary paper themes until after first content composition.
        request(context, R.drawable.reference_paper)
        request(context, R.drawable.navigation_leather)
        request(context, R.drawable.card_paper)
        scope.launch {
            delay(300)
            for (id in bookResources) {
                if (id !in setOf(R.drawable.reference_paper,
                        R.drawable.navigation_leather, R.drawable.card_paper)) {
                    request(context, id)
                }
            }
        }
    }

    fun request(context: Context, id: Int) {
        if (cached.containsKey(id) || !inFlight.add(id)) return
        val appContext = context.applicationContext
        scope.launch {
            try {
                decodeGate.withPermit {
                    val options = BitmapFactory.Options().apply {
                        inScaled = false // Preserve original paper texture pixels
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    }
                    val bitmap = BitmapFactory.decodeResource(appContext.resources, id, options)
                    if (bitmap != null) {
                        val image = bitmap.asImageBitmap()
                        withContext(Dispatchers.Main.immediate) { cached[id] = image }
                    }
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
