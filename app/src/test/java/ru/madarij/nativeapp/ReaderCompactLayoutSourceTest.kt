package ru.madarij.nativeapp

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guard visual density and cold-reader optimizations without changing book content. */
class ReaderCompactLayoutSourceTest {
    @Test fun numberedLabelsStayCompact() {
        val src = File("src/main/java/ru/madarij/nativeapp/Reader.kt").readText()
        assertTrue(src.contains("Modifier.padding(horizontal = 8.dp, vertical = 4.dp)"))
        assertTrue(src.contains("fontSize = 13.sp, lineHeight = 17.sp"))
        assertTrue(src.contains("horizontalArrangement = Arrangement.spacedBy(7.dp)"))
    }

    @Test fun notesAreCompactAndStillExpandable() {
        val src = File("src/main/java/ru/madarij/nativeapp/Reader.kt").readText()
        assertTrue(src.contains("shape = RoundedCornerShape(14.dp)"))
        assertTrue(src.contains("onClick = { expanded = !expanded }"))
        assertTrue(!src.contains("Развёрнутое объяснение и дополнительные детали"))
    }

    @Test fun restoredCorpusSkipsSecondJsonParse() {
        val src = File("src/main/java/ru/madarij/nativeapp/data/BookRepository.kt").readText()
        val fast = src.indexOf("dao.contentVersion()?.sha256 == hash")
        val parse = src.indexOf("val root = JSONObject(bytes.toString(Charsets.UTF_8))")
        assertTrue(fast in 0 until parse)
    }

    @Test fun readingPreparationDoesNotBlockComposeThread() {
        val src = File("src/main/java/ru/madarij/nativeapp/Reader.kt").readText()
        assertTrue(src.contains("withContext(Dispatchers.Default)"))
        assertTrue(src.contains("buildReaderDisplayRussian(loadedParagraphs)"))
        assertTrue(src.contains("delay(190)"))
        assertTrue(!src.contains("delay(650)"))
    }
}
