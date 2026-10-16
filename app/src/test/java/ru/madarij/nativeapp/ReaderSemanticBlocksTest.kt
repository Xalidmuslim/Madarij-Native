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

    @Test fun firstVolumeCurlyQuotationIsQuranWhenPrintedSourceFollows() {
        val text = "И Он говорит: “Поистине, к Нам их возвращение, а затем на Нас — их расчёт” [“Аль-Гашия”, 88:25–26]."
        val quote = parseReaderRichSegments(text).filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.VERSE, quote.kind)
        assertTrue(quote.body.contains("88:25–26"))
    }

    @Test fun ibnAbbasWordsWithCurlyQuotationStayHumanWords() {
        val text = "Ибн Аббас сказал: “Здесь требуется мудрость и терпение”."
        val quote = parseReaderRichSegments(text).filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.COMPANION_SAYING, quote.kind)
    }

    @Test fun explanatorySentenceIsNotSwallowedByQuranFrame() {
        val text = "Что же до сопоставления аль-Кисаи со словами «Поистине, твой Господь — в засаде» [«Аль-Фаджр», 89:14], то различие между ними по контексту и смыслу очевидно."
        val blocks = parseReaderRichSegments(text)
        val verses = blocks.filterIsInstance<ReaderRichSegment.Quote>()
        assertEquals(1, verses.size)
        val verse = verses.single()
        assertEquals(ReaderCitationKind.VERSE, verse.kind)
        assertEquals("«Поистине, твой Господь — в засаде» [«Аль-Фаджр», 89:14],", verse.body)
        assertTrue(!verse.body.contains("то различие между ними"))
        val plain = blocks.filterIsInstance<ReaderRichSegment.Paragraph>()
        assertEquals(2, plain.size)
        assertTrue(plain.first().raw.startsWith("Что же до сопоставления"))
        assertTrue(plain.last().raw.startsWith("то различие между ними"))
        assertEquals(text, blocks.joinToString(" ") { it.raw })
    }

    @Test fun quranReferenceFollowedByFullAuthorExplanationStaysOutsideQuote() {
        val text = "Всевышний сказал: «Он ведёт, кого пожелает» (сура «Аль-Бакара», 2:272). Здесь автор разъясняет смысл указания."
        val blocks = parseReaderRichSegments(text)
        val verse = blocks.filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.VERSE, verse.kind)
        assertTrue(verse.body.endsWith("(сура «Аль-Бакара», 2:272)."))
        assertTrue(!verse.body.contains("Здесь автор"))
        assertEquals("Здесь автор разъясняет смысл указания.", blocks.last().raw)
        assertEquals(text, blocks.joinToString(" ") { it.raw })
    }

    @Test fun twoVersesAndAuthorExplanationRemainThreeDifferentBlocks() {
        val text = "«Первый аят» (сура «Ан-Нахль», 16:97). Также Он сказал: «Второй аят» (сура «Аш-Шура», 42:30). Таков вывод автора."
        val blocks = parseReaderRichSegments(text)
        assertEquals(2, blocks.count { it is ReaderRichSegment.Quote && it.kind == ReaderCitationKind.VERSE })
        assertTrue(blocks.last() is ReaderRichSegment.Paragraph)
        assertEquals("Таков вывод автора.", blocks.last().raw)
        assertEquals(text, blocks.joinToString(" ") { it.raw })
    }

    @Test fun namedHumanWithQuranReferenceMustNotBeMarkedAsVerse() {
        val text = "Ибн Аббас сказал: «Это моё объяснение слов аята» [«Аль-Фаджр», 89:14]."
        val block = parseReaderRichSegments(text).filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.COMPANION_SAYING, block.kind)
    }

    @Test fun numberingKeepOriginalPunctuationAfterRemovingBadge() {
        val original = "Первое: слова о прямом пути. Второе следует после первого объяснения."
        val result = parseReaderRichSegments(original)
        assertEquals(2, result.count { it is ReaderRichSegment.Numbered })
        assertEquals(original, result.joinToString(" ") { it.raw })
    }

    @Test fun alAsrCitationIncludesTranslationNoteInQuranCard() {
        val original = "Всевышний сказал: «Клянусь временем! Поистине, человек пребывает в убытке, кроме тех, кто уверовал, совершал праведные дела, заповедовал друг другу истину и заповедовал друг другу терпение» [аль-Аср, 103:1–3; перевод смысла]. Пречистый поклялся, что всякий человек в убытке."
        val blocks = parseReaderRichSegments(original)
        val verse = blocks.filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.VERSE, verse.kind)
        assertTrue(verse.body.endsWith("[аль-Аср, 103:1–3; перевод смысла]."))
        assertEquals("[аль-Аср, 103:1–3; перевод смысла]", verse.reference)
        assertTrue(!verse.body.contains("Пречистый поклялся"))
        assertEquals("Пречистый поклялся, что всякий человек в убытке.", blocks.last().raw)
        assertEquals(original, blocks.joinToString(" ") { it.raw })
    }

    @Test fun alMaidaCitationIncludesPartialVerseNoteInQuranCard() {
        val original = "Всевышний также сказал: «Скажи: “Сообщить ли вам о тех, чьё воздаяние у Аллаха ещё хуже этого? Это те, кого Аллах проклял, на кого разгневался и среди кого сделал обезьян и свиней”» [«Аль-Маида», 5:60; цитируется часть аята]. А не знающий истины в большей степени заслуживает названия заблудшего."
        val blocks = parseReaderRichSegments(original)
        val verse = blocks.filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.VERSE, verse.kind)
        assertTrue(verse.body.endsWith("[«Аль-Маида», 5:60; цитируется часть аята]."))
        assertEquals("[«Аль-Маида», 5:60; цитируется часть аята]", verse.reference)
        assertTrue(!verse.body.contains("А не знающий истины"))
        assertEquals("А не знающий истины в большей степени заслуживает названия заблудшего.", blocks.last().raw)
        assertEquals(original, blocks.joinToString(" ") { it.raw })
    }

    @Test fun adjacentQuranReferencesWithEditorialNotesDoNotSplit() {
        val original = "Всевышний сказал: «Первый аят» [«Аль-Бакара», 2:90]. Всевышний также сказал: «Второй аят» [«Аль-Маида», 5:60; цитируется часть аята]. Затем автор пояснил."
        val blocks = parseReaderRichSegments(original)
        val verses = blocks.filterIsInstance<ReaderRichSegment.Quote>()
        assertEquals(2, verses.size)
        assertTrue(verses[0].body.contains("[«Аль-Бакара», 2:90]"))
        assertTrue(verses[1].body.contains("[«Аль-Маида», 5:60; цитируется часть аята]"))
        assertEquals("Затем автор пояснил.", blocks.last().raw)
        assertEquals(original, blocks.joinToString(" ") { it.raw })
    }

    @Test fun annotatedQuranSourceDoesNotTurnHumanAttributionIntoVerse() {
        val original = "Ибн Аббас сказал: «Это слова моего объяснения» [аль-Аср, 103:1–3; перевод смысла]."
        val blocks = parseReaderRichSegments(original)
        val quote = blocks.filterIsInstance<ReaderRichSegment.Quote>().single()
        assertEquals(ReaderCitationKind.COMPANION_SAYING, quote.kind)
        assertEquals(original, blocks.joinToString(" ") { it.raw })
    }

}
