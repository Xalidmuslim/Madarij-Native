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
        KnowledgeReminder("Самое большее, чего достигают дела, если раб вложил в них всю искренность и старание и совершил их наилучшим образом, — это благодарность Аллаху за часть оказанных ему милостей.", "madarij-v1-p140-draft-section", "madarij-v1-p144-draft", 144),
        KnowledgeReminder("Понимание — это милость Всевышнего Аллаха к Своему рабу и свет, который Он вселяет в его сердце.", "madarij-v1-p061-draft-section", "madarij-v1-p064-draft", 64),
        KnowledgeReminder("Аллах обладает могуществом, говорит и ни в ком не нуждается; в Своих словах и деяниях Он — на прямом пути.", "madarij-v1-p028-draft-section", "madarij-v1-p028-draft", 28),
        KnowledgeReminder("Кроме того, сердце поражают две тяжёлые болезни, и если вовремя не заняться ими, они неизбежно доведут его до гибели: рия (показуха) и высокомерие.", "madarij-v1-p083-draft-section", "madarij-v1-p087-draft", 87),
        KnowledgeReminder("Поклонение соединяет две основы: предельную любовь и предельное смирение и покорность.", "madarij-v1-p115-draft-section", "madarij-v1-p115-draft", 115),
        KnowledgeReminder("Всякое состояние с Аллахом и всякая стоянка, которые сопровождаются дальнейшим движением к Аллаху и предпочтением Его воли собственной воле раба, — благодеяние от Аллаха.", "madarij-v1-p259-draft-section", "madarij-v1-p265-draft", 265),
        KnowledgeReminder("Дела телесных органов без дел сердца либо вовсе лишены пользы, либо приносят её лишь в малой степени.", "madarij-v1-p148-draft-section", "madarij-v1-p154-draft", 154),
        KnowledgeReminder("Духовное зрение — свет, который Аллах бросает в сердце; им человек видит действительность сообщённого посланниками словно очевидец, видящий глазами.", "madarij-v1-p188-draft-section", "madarij-v1-p190-draft", 190),
        KnowledgeReminder("Инаба (обращение к Аллаху) соединяет любовь и благоговейный страх; раб не бывает обращающимся без соединения обеих.", "madarij-v1-p204-draft-section", "madarij-v1-p209-draft", 209),
        KnowledgeReminder("Шукр (благодарность) объединяет все стоянки веры, поэтому она самая возвышенная и высокая.", "madarij-v1-p204-draft-section", "madarij-v1-p210-draft", 210),
        KnowledgeReminder("Знай: пока до раба не дошел призыв, он спит сном беспечности; сердце его спит, хотя глаза бодрствуют.", "madarij-v1-p212-draft-section", "madarij-v1-p215-draft", 215),
        KnowledgeReminder("Когда человек узнал, что в его пользу и что на нём, он приступает к исполнению лежащего на нём и освобождению от него — это покаяние.", "madarij-v1-p204-draft-section", "madarij-v1-p204-draft", 204),
        KnowledgeReminder("Покаяние — завершение пути каждого идущего и каждого приближённого Аллаха; к нему устремляются те, кто знает Аллаха, знает убудийю перед Ним и знает то, что подобает Ему.", "madarij-v1-p204-draft-section", "madarij-v1-p206-draft", 206),
        KnowledgeReminder("Раб постоянно находится либо перед благодеянием Аллаха к нему, либо перед доводом Аллаха против него, и не выходит за пределы этих двух состояний.", "madarij-v1-p259-draft-section", "madarij-v1-p263-draft", 263),
        KnowledgeReminder("Всякое знание, за которым следует дело, угодное Аллаху, — благодеяние; иначе оно становится доводом.", "madarij-v1-p259-draft-section", "madarij-v1-p264-draft", 264),
        KnowledgeReminder("Покаяние же включает множество отдельных актов поклонения по числу грехов; у каждого греха особое покаяние.", "madarij-v1-p432-draft-section", "madarij-v1-p437-draft", 437),
        KnowledgeReminder("Верующий никогда не наслаждается ослушанием полностью и не испытывает от него совершенной радости.", "madarij-v1-p275-draft-section", "madarij-v1-p278-draft", 278),
        KnowledgeReminder("Довольство собственной покорностью — одно из проявлений легкомыслия и глупости души.", "madarij-v1-p267-draft-section", "madarij-v1-p268-draft", 268),
        KnowledgeReminder("Чем выше искомое в твоём сердце, тем меньше и ничтожнее кажется тебе цена, которую ты отдаёшь ради него.", "madarij-v1-p267-draft-section", "madarij-v1-p270-draft", 270),
        KnowledgeReminder("Человек повинуется Аллаху, следуя свету от Аллаха, надеясь на награду Аллаха, и оставляет ослушание Аллаха, следуя свету от Всевышнего, боясь наказания Аллаха.", "madarij-v1-p314-draft-section", "madarij-v1-p314-draft", 314),
        KnowledgeReminder("Искренность в покаянии, поклонении и совете — очищение от всякого обмана, недостатка и порчи и осуществление совершеннейшим образом.", "madarij-v1-p472-draft-section", "madarij-v1-p477-draft", 477),
        KnowledgeReminder("Кто вернулся к Аллаху в этой обители покаянием, вернётся при воскресении с наградой.", "madarij-v1-p479-draft-section", "madarij-v1-p483-draft", 483),
        KnowledgeReminder("Все Его деяния исполнены блага и мудрости, милости, справедливости и добра.", "madarij-v1-p028-draft-section", "madarij-v1-p030-draft", 30),
        KnowledgeReminder("Искреннее покаяние разрушает предшествующее ему; поэтому Он возместит одному причинённую ему несправедливость и не накажет другого благодаря полноте его покаяния.", "madarij-v1-p609-draft-section", "madarij-v1-p610-draft", 610),
        KnowledgeReminder("Вся религия — умножение послушания, и самое любимое Аллаху творение — наиболее умножающее его.", "madarij-v1-p407-draft-section", "madarij-v1-p408-draft", 408),
        KnowledgeReminder("Человек не становится кающимся одним прекращением, намерением и сожалением, пока у него не появится решительное намерение совершить предписанное и его исполнение.", "madarij-v1-p472-draft-section", "madarij-v1-p472-draft", 472)
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
        val prefs = context.getSharedPreferences("madarij_knowledge_reminders_v2", Context.MODE_PRIVATE)
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
