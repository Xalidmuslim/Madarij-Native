package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReminderSpotlightTest {
    @Test fun exactPassageUsesOnlyItsOwnCharacters() {
        val text = "Рассуждение автора. Поклонение соединяет две основы: предельную любовь и предельное смирение и покорность. Следующая мысль."
        val quote = "Поклонение соединяет две основы: предельную любовь и предельное смирение и покорность."
        val match = requireNotNull(reminderSpotlightRange(text, quote))
        assertEquals(quote, text.substring(match.first, match.last + 1))
    }

    @Test fun stablePrefixWorksForReconciledPrintedPage() {
        val quote = "Самое большее, чего достигают дела, если раб вложил в них всю искренность и старание и совершил их наилучшим образом, — это благодарность Аллаху за часть оказанных ему милостей."
        val displayed = quote.take(80) + " — продолжение в объединённом отображении страниц."
        val range = requireNotNull(reminderSpotlightRange(displayed, quote))
        assertTrue(range.first == 0 && range.last > 35)
    }

    @Test fun neverClaimAnUnrelatedOrAmbiguousPassage() {
        val quote = "Понимание — это милость Всевышнего Аллаха к Своему рабу и свет, который Он вселяет в его сердце."
        assertNull(reminderSpotlightRange("Совершенно другой абзац.", quote))
        val duplicated = quote.take(64) + " ..." + quote.take(64)
        assertNull(reminderSpotlightRange(duplicated, quote))
    }

    @Test fun temporaryModeOnlyInvokedFromHandwrittenReminder() {
        val home = File("src/main/java/ru/madarij/nativeapp/HomeContents.kt").readText()
        val main = File("src/main/java/ru/madarij/nativeapp/MainActivity.kt").readText()
        val reader = File("src/main/java/ru/madarij/nativeapp/Reader.kt").readText()
        assertTrue(home.contains("onOpenReminder(reminder)"))
        assertTrue(main.contains("spotlight = true"))
        assertTrue(main.contains("e.arguments?.getBoolean(\"spotlight\") == true"))
        assertTrue(reader.contains("delay(5_000)"))
        assertTrue(reader.contains("showQuoteSpotlight = false"))
        assertTrue(reader.contains("Spotlight"))
        assertTrue(!home.contains("open(reminder.chapterId, reminder.paragraphId)"))
    }

    @Test fun bookCorpusAndSavedPositionRemainUntouched() {
        val reader = File("src/main/java/ru/madarij/nativeapp/Reader.kt").readText()
        val locator = File("src/main/java/ru/madarij/nativeapp/ReminderSpotlight.kt").readText()
        assertTrue(locator.contains("reminderSpotlightRange(text: String, reminder: String)"))
        assertTrue(reader.contains("reminderSpotlightRange(raw, it)"))
        assertTrue(reader.contains("getBoundingBox(range.first).top"))
        assertTrue(reader.contains("spotlightTopY - viewportTopY - inset"))
        assertTrue(reader.contains("savePosition()"))
    }
}
