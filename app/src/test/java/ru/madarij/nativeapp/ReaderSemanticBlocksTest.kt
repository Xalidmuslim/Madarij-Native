package ru.madarij.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderSemanticBlocksTest {
    @Test fun verseWithReferenceIsSeparateFromAuthorExplanation() {
        val input = "Автор разъясняет этот смысл. Всевышний сказал: «Это — прямой путь ко Мне». [Аль-Хиджр, 15:41] Затем он продолжает рассуждение."
        val list = parseReaderRichSegments(input)
        val block = list.filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.VERSE, block.kind)
        assertTrue(block.body.contains("«Это — прямой путь ко Мне»"))
        assertTrue(block.body.contains("[Аль-Хиджр, 15:41]"))
        assertTrue(list.last() is ReaderRichSegment.Paragraph)
        assertEquals(input, list.joinToString(" ") { it.raw })
    }

    @Test fun scholarWordsAreNeverCalledPropheticReport() {
        val list = parseReaderRichSegments("Аль-Хасан сказал: «Знание требует действия».")
        val block = list.filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.SAYING, block.kind)
        assertTrue(block.body.contains("«Знание требует действия»"))
    }

    @Test fun propheticReportIsExplicitOnly() {
        val list = parseReaderRichSegments("Пророк сказал: «Дела оцениваются по намерениям».")
        assertEquals(
            ReaderCitationKind.PROPHETIC_REPORT,
            list.filterIsInstance<ReaderRichSegment.Quote>().single().kind
        )
        val plain = parseReaderRichSegments("Он сказал об этой книге, что надо размышлять.")
        assertTrue(plain.all { it is ReaderRichSegment.Paragraph })
    }

    @Test fun numberedItemsPreserveTextAndOrder() {
        val input = "Здесь возможны два объяснения. Первое: взаимоотношение слов. Второе: смысловое толкование."
        val list = parseReaderRichSegments(input)
        assertEquals(2, list.count { it is ReaderRichSegment.Numbered })
        assertEquals(input, list.joinToString(" ") { it.raw })
    }

    @Test fun quotationAndOrdinaryTextAreNotDuplicated() {
        val input = "Здесь обычный текст. Аль-Хасан сказал: «Истина ясна». Затем следует объяснение."
        val list = parseReaderRichSegments(input)
        assertEquals(input, list.joinToString(" ") { it.raw })
    }
    @Test fun secondVolumePronounWithSurahReferenceIsQuranNotSaying() {
        val text = "Он также сказал: «Они говорят: „Когда исполнится это обещание, если вы правдивы?“ Скажи: „Возможно, часть того, что вы торопите, уже приближается к вам“» (сура «Ан-Намль», 27:71–72)."
        val quote = parseReaderRichSegments(text).filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.VERSE, quote.kind)
        assertTrue(quote.label.isBlank())
        assertTrue(!quote.body.contains("Он также сказал"))
        assertTrue(quote.body.contains("27:71–72"))
        assertTrue(quote.body.contains("Скажи:"))
    }

    @Test fun quoteToCompanionsContainingWordProphetIsStillQuran() {
        val text = "Лучшим Своим творениям — сподвижникам Пророка — Он сказал: «Неужели, когда вас постигло несчастье, хотя вы прежде причинили противнику вдвое больше, вы спросили: „Откуда это?“ Скажи: „От вас самих“» (сура «Аль Имран», 3:165)."
        val segments = parseReaderRichSegments(text)
        val quote = segments.filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.VERSE, quote.kind)
        assertTrue(segments.first() is ReaderRichSegment.Paragraph)
        assertTrue((segments.first() as ReaderRichSegment.Paragraph).raw.contains("сподвижникам Пророка"))
        assertTrue(quote.body.startsWith("«Неужели"))
    }

    @Test fun ibnAbbasIsNamedAndNotConfusedWithHadithOrQuran() {
        val text = "Ибн Аббас, да будет доволен Аллах им и его отцом, сказал: «Поистине, доброе дело оставляет свет в сердце, сияние на лице, силу в теле, прибавление удела и любовь в сердцах людей. А дурной поступок оставляет потемнение лица, мрак в сердце, слабость в теле»."
        val block = parseReaderRichSegments(text).filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.COMPANION_SAYING, block.kind)
        assertTrue(block.label.startsWith("Ибн Аббас"))
        assertTrue(block.body.startsWith("«Поистине"))
        assertTrue(!block.body.startsWith("Ибн Аббас"))
    }

    @Test fun quranSurahCitationsTakePrecedenceOverGenericHeSaid() {
        val text = "И Он сказал: «Всё хорошее, что постигает тебя, — от Аллаха, а всё дурное, что постигает тебя, — от тебя самого» (сура «Ан-Ниса», 4:79)."
        val block = parseReaderRichSegments(text).filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.VERSE, block.kind)
        assertTrue(block.label.isBlank())
    }

    @Test fun literalHeSaidWithoutSourceDoesNotBecomeQuranOrHadith() {
        val text = "Он также сказал: «Это моё объяснение смысла»."
        val segments = parseReaderRichSegments(text)
        assertTrue(segments.none { it is ReaderRichSegment.Quote })
    }

    @Test fun plainQuranReferenceWorksWithoutExplicitDivineIntroduction() {
        val text = "«Поистине, для тех, кто поступал несправедливо, есть наказание и до этого» (сура «Ат-Тур», 52:47)."
        val quote = parseReaderRichSegments(text).filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.VERSE, quote.kind)
    }

    @Test fun printedBracketedReferenceWorksForFirstVolume() {
        val text = "И сказал: «Они — на верном руководстве от своего Господа» [«Аль-Бакара», 2:5]."
        val quote = parseReaderRichSegments(text).filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.VERSE, quote.kind)
    }

    @Test fun aReportedHumanQuoteWithAReferenceIsNotAutomaticallyQuran() {
        val text = "Ибн Аббас сказал: «Вот как я понимал слова этого аята» [«Аль-Бакара», 2:5]."
        val quote = parseReaderRichSegments(text).filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.COMPANION_SAYING, quote.kind)
    }

    @Test fun noUnwarrantedPropheticReportFromMentionOfProphet() {
        val text = "Муджахид, рассказывая о Пророке, сказал: «Таково было моё понимание»."
        val quote = parseReaderRichSegments(text).filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.SAYING, quote.kind)
    }

    @Test fun nestedQuotedQuestionsRemainOneVerse() {
        val text = "Он сказал: «Они говорят: „Когда наступит это?“ Скажи: „Это близко“» (сура «Ан-Намль», 27:71–72). Затем автор продолжил."
        val segments = parseReaderRichSegments(text)
        assertEquals(1, segments.count { it is ReaderRichSegment.Quote })
        assertTrue(segments.last() is ReaderRichSegment.Paragraph)
    }

}
