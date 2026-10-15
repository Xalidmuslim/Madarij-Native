package ru.madarij.nativeapp

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * Eight actual miniature images from the user's generated antique icon sheet.
 * Drawn as ordinary bitmap painters: no shader, doubled stroke or GPU layer.
 */
internal object AntiqueBookIcons {
    @DrawableRes
    fun drawable(kind: String): Int? = when (kind) {
        "home" -> R.drawable.book_icon_home
        "contents" -> R.drawable.book_icon_contents
        "bookmarks" -> R.drawable.book_icon_bookmarks
        "search" -> R.drawable.book_icon_search
        "study" -> R.drawable.book_icon_study
        "glossary" -> R.drawable.book_icon_glossary
        "notes" -> R.drawable.book_icon_notes
        "more", "toc" -> R.drawable.book_icon_more
        else -> null
    }
}

@Composable
internal fun AntiqueBookIcon(kind: String, modifier: Modifier = Modifier) {
    val asset = AntiqueBookIcons.drawable(kind) ?: return
    Image(
        painter = painterResource(asset),
        contentDescription = null, // Clickable parent provides accessibility semantics.
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
