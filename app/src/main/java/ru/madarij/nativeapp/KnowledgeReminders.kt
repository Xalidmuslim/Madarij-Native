package ru.madarij.nativeapp

import android.content.Context
import kotlin.random.Random

/** Verified sentences from the app's existing Russian translation of Madarij vol. 1.
 * Provenance is an author-role paragraph from corpus.json, with its source page.
 * The Russian text is the app's draft translation (not an independent scholarly edition).
 */
internal data class KnowledgeReminder(val text: String, val chapterId: String, val paragraphId: String, val page: Int)

internal object KnowledgeReminders {
    val items: List<KnowledgeReminder> = listOf(
        KnowledgeReminder("Чем глубже внутренний взор размышляет над ним, тем больше оно прибавляет ему руководства и прозрения.", "madarij-v1-opening-001-section", "madarij-v1-opening-005", 3),
        KnowledgeReminder("Каких сокровищ лишились те, кто отвернулся от текстов Откровения и не стал черпать знание из их светильника!", "madarij-v1-opening-001-section", "madarij-v1-opening-011", 6),
        KnowledgeReminder("В зависимости от знания истины и следования ей люди делятся на эти три группы.", "madarij-v1-fatiha-004-section", "madarij-v1-fatiha-020", 16),
        KnowledgeReminder("Облагодетельствование включает дар наставления — полезное знание и праведное деяние, то есть верное руководство и религию истины, — а также завершённость благодеяния прекрасной наградой и воздаянием.", "madarij-v1-fatiha-029-section", "madarij-v1-fatiha-032", 20),
        KnowledgeReminder("Знание — совершенство, мудрость — совершенство, и сочетание знания с мудростью — тоже совершенство.", "madarij-v1-p051-draft-section", "madarij-v1-p054-draft", 54),
        KnowledgeReminder("Аллах упомянул этих двух благородных пророков и похвалил обоих за мудрость и знание, однако в этом конкретном деле особо выделил Сулеймана тем, что даровал ему понимание.", "madarij-v1-p061-draft-section", "madarij-v1-p064-draft", 64),
        KnowledgeReminder("Все болезни сердца восходят к двум основам: испорченности знания и испорченности намерения.", "madarij-v1-p083-draft-section", "madarij-v1-p084-draft", 84),
        KnowledgeReminder("Для сердец, которые восприняли Речь Всевышнего Аллаха и удостоились особого понимания её смыслов, нет ничего целительнее смыслов этой суры.", "madarij-v1-p083-draft-section", "madarij-v1-p088-draft", 88),
        KnowledgeReminder("Познающие, обладающие внутренним зрением, исходят от знания об Аллахе к пониманию Его действий и творений, тогда как большинство людей приходит к знанию о Нём через Его творения и действия.", "madarij-v1-p091-draft-section", "madarij-v1-p095-draft", 95),
        KnowledgeReminder("Сподвижники Посланника Аллаха, да благословит его Аллах и приветствует, завоевали земли неверия и превратили их в земли ислама, а сердца открывали Кораном, знанием и наставлением.", "madarij-v1-p107-draft-section", "madarij-v1-p112-draft", 112),
        KnowledgeReminder("Искренность рождается из знания Аллаха, а отсутствие ожиданий от людей — из знания творений.", "madarij-v1-p123-draft-section", "madarij-v1-p127-draft", 127),
        KnowledgeReminder("Во время чтения Корана лучше всего собрать сердце и устремление на размышлении над ним и понимании его, словно Всевышний Аллах обращается к тебе посредством Своей Книги.", "madarij-v1-p132-draft-section", "madarij-v1-p137-draft", 137),
        KnowledgeReminder("Его степени в знании — две: первая — знание Аллаха, вторая — знание Его религии.", "madarij-v1-p159-draft-section", "madarij-v1-p164-draft", 164),
        KnowledgeReminder("Различие людей в этом духовном зрении соответствует различию в знании и понимании пророческих текстов и в знании порочности сомнений, противоречащих их истинам.", "madarij-v1-p188-draft-section", "madarij-v1-p192-draft", 192),
        KnowledgeReminder("Он подразумевает, что человек подчиняется знанию, позволяя ему воспитывать и исправлять себя, и стремится отвечать на призыв религиозного повелительного предписания всякий раз, когда оно призывает его.", "madarij-v1-p196-draft-section", "madarij-v1-p203-draft", 203),
        KnowledgeReminder("Хашья (благоговейный страх) соединяет знание Аллаха со знанием Его права на поклонение.", "madarij-v1-p204-draft-section", "madarij-v1-p209-draft", 209),
        KnowledgeReminder("Хая (стыдливость перед Аллахом) соединяет познание с муракабой — осознанием Его наблюдения.", "madarij-v1-p204-draft-section", "madarij-v1-p210-draft", 210),
        KnowledgeReminder("Муракаба (осознание постоянного наблюдения Аллаха) соединяет познание и благоговейный страх; соответственно им утверждается эта ступень.", "madarij-v1-p204-draft-section", "madarij-v1-p211-draft", 211),
        KnowledgeReminder("Он сказал: «Познание милости проясняется тремя вещами: светом разума, вглядыванием в проблеск благодеяния и размышлением о людях, подвергшихся бедствию».", "madarij-v1-p220-draft-section", "madarij-v1-p220-draft", 220),
        KnowledgeReminder("Я говорю: размышление бывает двух видов — связанное со знанием и познанием и связанное со стремлением и волей.", "madarij-v1-p224-draft-section", "madarij-v1-p224-draft", 224),
        KnowledgeReminder("Как может воплощать истинную убудийю тот, кто говорит «Тебе одному мы поклоняемся», совершенно не сознавая своего рабства и действительности этих слов в знании, познании, намерении, воле и деле?", "madarij-v1-p224-draft-section", "madarij-v1-p231-draft", 231),
        KnowledgeReminder("Пусть раб внимательно размышляет над этим чрезвычайно опасным вопросом и различает, где перед ним благодеяние, а где — довод против него.", "madarij-v1-p259-draft-section", "madarij-v1-p265-draft", 265),
        KnowledgeReminder("Кто отдаёт аль-Фатихе её должное — в знании, созерцании, состоянии и познании, — тот понимает: невозможно читать её, осуществляя истинную убудийю, без искренней таубы.", "madarij-v1-p275-draft-section", "madarij-v1-p276-draft", 276),
        KnowledgeReminder("Поразмысли над этим разделом и охвати его знанием: он относится к основам духовного пути и познания.", "madarij-v1-p390-draft-section", "madarij-v1-p390-draft", 390),
        KnowledgeReminder("Незнание не избавляет его от ответственности за них, если он мог приобрести знание: он грешит тем, что не приобретает доступное ему знание и не действует согласно ему, и его ослушание тяжелее.", "madarij-v1-p417-draft-section", "madarij-v1-p422-draft", 422),
        KnowledgeReminder("Поэтому в День воскресения свет появляется справа и впереди них в этой мере, соответственно свету этих слов в их сердцах — знанию, делу, познанию и состоянию.", "madarij-v1-p503-draft-section", "madarij-v1-p508-draft", 508)
    )

    /** Do not repeat any sentence until the entire shuffled round is exhausted.
     * On a new round the first entry is different from the previous round's last.
     */
    fun newOrder(last: Int, random: Random = Random.Default): List<Int> {
        val indices = items.indices.toMutableList()
        indices.shuffle(random)
        if (indices.size > 1 && indices.first() == last) {
            val swap = 1 + random.nextInt(indices.size - 1)
            val temp = indices[0]
            indices[0] = indices[swap]
            indices[swap] = temp
        }
        return indices
    }

    /** Called once per new Activity session; never during in-app navigation. */
    fun nextForLaunch(context: Context): Int {
        val prefs = context.getSharedPreferences("madarij_knowledge_reminders_v1", Context.MODE_PRIVATE)
        val order = prefs.getString("order", null)?.split(',')?.mapNotNull { it.toIntOrNull() }
        val cursor = prefs.getInt("cursor", 0)
        val valid = order != null && order.size == items.size &&
            order.toSet() == items.indices.toSet() && cursor in 0 until items.size
        val deck = if (valid) order!! else newOrder(prefs.getInt("last", -1))
        val pos = if (valid) cursor else 0
        val index = deck[pos]
        prefs.edit().putString("order", deck.joinToString(","))
            .putInt("cursor", pos + 1).putInt("last", index).apply()
        return index
    }
}
