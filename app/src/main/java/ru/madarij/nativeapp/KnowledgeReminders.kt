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
        KnowledgeReminder("Все болезни сердца восходят к двум основам: испорченности знания и испорченности намерения.", "madarij-v1-p083-draft-section", "madarij-v1-p084-draft", 84),
        KnowledgeReminder("Кроме того, сердце поражают две тяжёлые болезни, и если вовремя не заняться ими, они неизбежно доведут его до гибели: рия (показуха) и высокомерие.", "madarij-v1-p083-draft-section", "madarij-v1-p087-draft", 87),
        KnowledgeReminder("Поклонение соединяет две основы: предельную любовь и предельное смирение и покорность.", "madarij-v1-p115-draft-section", "madarij-v1-p115-draft", 115),
        KnowledgeReminder("Во время чтения Корана лучше всего собрать сердце и устремление на размышлении над ним и понимании его, словно Всевышний Аллах обращается к тебе посредством Своей Книги.", "madarij-v1-p132-draft-section", "madarij-v1-p137-draft", 137),
        KnowledgeReminder("Дела телесных органов без дел сердца либо вовсе лишены пользы, либо приносят её лишь в малой степени.", "madarij-v1-p148-draft-section", "madarij-v1-p154-draft", 154),
        KnowledgeReminder("У избранных из них даже дозволенные дела благодаря намерению превращаются в поклонение и средство приближения к Аллаху.", "madarij-v1-p159-draft-section", "madarij-v1-p165-draft", 165),
        KnowledgeReminder("Духовное зрение — свет, который Аллах бросает в сердце; им человек видит действительность сообщённого посланниками словно очевидец, видящий глазами.", "madarij-v1-p188-draft-section", "madarij-v1-p190-draft", 190),
        KnowledgeReminder("Инаба (обращение к Аллаху) соединяет любовь и благоговейный страх; раб не бывает обращающимся без соединения обеих.", "madarij-v1-p204-draft-section", "madarij-v1-p209-draft", 209),
        KnowledgeReminder("Шукр (благодарность) объединяет все стоянки веры, поэтому она самая возвышенная и высокая.", "madarij-v1-p204-draft-section", "madarij-v1-p210-draft", 210),
        KnowledgeReminder("Знай: пока до раба не дошел призыв, он спит сном беспечности; сердце его спит, хотя глаза бодрствуют.", "madarij-v1-p212-draft-section", "madarij-v1-p215-draft", 215),
        KnowledgeReminder("Кто видит милость Аллаха лишь в еде, одежде, здоровье тела и своем положении среди людей, вовсе не обладает долей этого света.", "madarij-v1-p220-draft-section", "madarij-v1-p220-draft", 220),
        KnowledgeReminder("Пусть тот, кто желает своей душе добра, счастья и преуспеяния, внимательно рассмотрит это в себе и других.", "madarij-v1-p252-draft-section", "madarij-v1-p252-draft", 252),
        KnowledgeReminder("Довольство собственной покорностью — одно из проявлений легкомыслия и глупости души.", "madarij-v1-p267-draft-section", "madarij-v1-p268-draft", 268),
        KnowledgeReminder("Чем выше искомое в твоём сердце, тем меньше и ничтожнее кажется тебе цена, которую ты отдаёшь ради него.", "madarij-v1-p267-draft-section", "madarij-v1-p270-draft", 270),
        KnowledgeReminder("Кто не находит этого в своём сердце, пусть подозревает свою таубу и возвращается к её исправлению.", "madarij-v1-p283-draft-section", "madarij-v1-p289-draft", 289),
        KnowledgeReminder("Аллах велел ему благодарить — не потому, что Сам нуждается в благодарности, а чтобы посредством неё человек получил ещё больше Его милости.", "madarij-v1-p299-draft-section", "madarij-v1-p301-draft", 301),
        KnowledgeReminder("Как же после этого проницательный и правдивый сможет любоваться каким-либо своим благим делом, созерцая изъяны своей души и дела и милость Аллаха к нему?", "madarij-v1-p342-draft-section", "madarij-v1-p347-draft", 347),
        KnowledgeReminder("Такое всеобъемлющее обращение нужно для того, чтобы покаяние охватило и грехи, известные рабу, и те, которых он не знает.", "madarij-v1-p417-draft-section", "madarij-v1-p423-draft", 423),
        KnowledgeReminder("Искренность в покаянии, поклонении и совете — очищение от всякого обмана, недостатка и порчи и осуществление совершеннейшим образом.", "madarij-v1-p472-draft-section", "madarij-v1-p477-draft", 477),
        KnowledgeReminder("Сколько раз Аллах напоминал им о Своих милостях, а они отворачивались от Его поминания и забывали Его!", "madarij-v1-p543-draft-section", "madarij-v1-p543-draft", 543),
        KnowledgeReminder("Все Его деяния исполнены блага и мудрости, милости, справедливости и добра.", "madarij-v1-p028-draft-section", "madarij-v1-p030-draft", 30),
        KnowledgeReminder("У речи есть слова и смысл, и она обращена одновременно к уху и к сердцу.", "madarij-v1-p061-draft-section", "madarij-v1-p068-draft", 68),
        KnowledgeReminder("Решившись и собрав намерение, он переходит к ступени самоотчета — различению того, что ему причитается, и того, что он должен.", "madarij-v1-p259-draft-section", "madarij-v1-p259-draft", 259),
        KnowledgeReminder("Этот смысл слишком высок, чтобы исчерпываться одним словом «бедность»: это сердцевина и тайна убудийи.", "madarij-v1-p318-draft-section", "madarij-v1-p325-draft", 325)
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
