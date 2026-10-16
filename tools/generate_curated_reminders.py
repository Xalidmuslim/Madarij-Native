#!/usr/bin/env python3
"""Generate 74 curated, source-linked excerpts from both bundled volumes.

The 26 existing checked excerpts remain in KnowledgeReminders.kt.  This
manifest preserves the chosen sentence within its source paragraph; edits
to the translation fail the build if a sentence no longer matches.
"""
from __future__ import annotations
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
SELECTION = [
    ("madarij-v1-p558-draft", "Условием покаяния лицемера Он сделал"),
    ("madarij-v1-p476-draft", "Здесь два греха: уже минувший"),
    ("madarij-v1-fatiha-010", "Поклонение Ему есть благодарность"),
    ("madarij-v1-p482-draft", "Покаяние раба — возвращение"),
    ("madarij-v1-p214-draft", "В этом и состоит польза притч"),
    ("madarij-v1-p423-draft", "Такое всеобъемлющее обращение"),
    ("madarij-v1-p335-draft", "Пречистый Аллах создал творения"),
    ("madarij-v1-p213-draft", "Их заботой было приобретение"),
    ("madarij-v1-p484-draft", "Значение: кто покаялся перед"),
    ("madarij-v1-p474-draft", "Большинство людей не знает достоинства"),
    ("madarij-v1-p585-draft", "Исправление после утраты условия"),
    ("madarij-v1-p125-draft", "Того, кого Он почитает"),
    ("madarij-v1-p026-draft", "Всевышний Аллах подтвердил это"),
    ("madarij-v1-p131-draft", "Каждому велено только поклоняться"),
    ("madarij-v1-p166-draft", "Последнее — нечто сверх искренности"),
    ("madarij-v1-p442-draft", "Если его сожаление о грехе"),
    ("madarij-v1-p325-draft", "Мудрость основана на устранении"),
    ("madarij-v1-p276-draft", "Первое — невежество, противоречащее"),
    ("madarij-v1-p419-draft", "Разве всё познание и поклонение"),
    ("madarij-v1-p520-draft", "Смысл в том, что все ослушания"),
    ("madarij-v2-p252-draft", "Оно означает обращение сердца к Аллаху"),
    ("madarij-v2-p528-draft", "Терпение, упование, довольство, смиренное"),
    ("madarij-v2-p594-draft", "Полное состояние включает признание"),
    ("madarij-v2-p345-draft", "Предать лицо Аллаху означает"),
    ("madarij-v2-p064-draft", "Сколько в душах скрыто недостатков"),
    ("madarij-v2-p188-draft", "Любовь — средство передвижения"),
    ("madarij-v2-p562-draft", "Сравни человека, спящего с искренней"),
    ("madarij-v2-p072-draft", "Польза наставления заключается в том"),
    ("madarij-v2-p250-draft", "Высшее достоинство человеческой души"),
    ("madarij-v2-p597-draft", "Какую стоянку можно считать выше"),
    ("madarij-v2-p292-draft", "Правильное духовное исчезновение заключается"),
    ("madarij-v2-p217-draft", "Между рабом и счастьем и успехом"),
    ("madarij-v2-p367-draft", "Аллах велик в прощении"),
    ("madarij-v2-p500-draft", "Полная любовь и искренность"),
    ("madarij-v2-p247-draft", "Вера в предопределение рождает"),
    ("madarij-v2-p514-draft", "Ещё один великий вид поклонения"),
    ("madarij-v2-p194-draft", "Знающие Аллаха единодушны"),
    ("madarij-v2-p057-draft", "Поскольку кающийся уже вернулся"),
    ("madarij-v2-p044-draft", "В его сердце укрепляются свидетельства"),
    ("madarij-v2-p602-draft", "К полноте Его милости"),
    ("madarij-v2-p653-draft", "Познание — душа, сердцевина"),
    ("madarij-v2-p389-draft", "Отказ от причин, которые Аллах"),
    ("madarij-v2-p518-draft", "Сотворённое может быть злом и порчей"),
    ("madarij-v2-p657-draft", "Она не отменяет стремления и намерения"),
    ("madarij-v2-p117-draft", "Если раб хорошо думает об Аллахе"),
    ("madarij-v2-p529-draft", "Скрытая порча, обман и злоба"),
    ("madarij-v2-p582-draft", "Даже если просит о земном деле"),
    ("madarij-v2-p405-draft", "Обращение же раба к Господу с доверием"),
    ("madarij-v2-p643-draft", "Правдивое намерение — это полная"),
    ("madarij-v2-p622-draft", "Такое обращение означает нужду"),
    ("madarij-v2-p365-draft", "Пусть сердце добровольно устремляется"),
    ("madarij-v2-p556-draft", "Зу-н-Нун назвал три признака"),
    ("madarij-v2-p379-draft", "У человека есть возможность достигать"),
    ("madarij-v2-p290-draft", "Устремление же опирается на осознание"),
    ("madarij-v2-p412-draft", "Разумному стремящемуся не подобает"),
    ("madarij-v2-p121-draft", "Различить всё это возможно лишь"),
    ("madarij-v2-p120-draft", "Совершенство поклонения состоит в том"),
    ("madarij-v2-p512-draft", "Существование человеческой слабости"),
    ("madarij-v2-p034-draft", "Его снисходительность проявляется"),
    ("madarij-v2-p442-draft", "Смысл в другом: намерение направляет"),
    ("madarij-v2-p493-draft", "Поклонение включает совершенную любовь"),
    ("madarij-v2-p586-draft", "Как уже говорилось, благодарность"),
    ("madarij-v2-p035-draft", "Каждому имени соответствует особый"),
    ("madarij-v2-p071-draft", "Знай, что у человека может быть"),
    ("madarij-v2-p394-draft", "Причины относятся к мудрости"),
    ("madarij-v2-p465-draft", "Даже если любовь и довольство"),
    ("madarij-v2-p022-draft", "Любовь к Нему занимает в сердце"),
    ("madarij-v2-p451-draft", "Терпение состоит в удержании души"),
    ("madarij-v2-p509-draft", "Служение проявляется в том"),
    ("madarij-v2-p363-draft", "Знай: подлинное познание"),
    ("madarij-v2-p395-draft", "В той мере, в какой человек"),
    ("madarij-v2-p470-draft", "Когда сердце доверяет милости Аллаха"),
    ("madarij-v2-p078-draft", "Живость разума — это правильность"),
    ("madarij-v2-p267-draft", "Вместе с любовью и страхом надежда"),
]

def main() -> None:
    assert len(SELECTION) == 74 and len({p for p, _ in SELECTION}) == 74
    source = {}
    for vol, filename in ((1, "corpus.json"), (2, "volume2-preview.json")):
        book = json.loads((ASSETS / filename).read_text(encoding="utf-8"))
        for ch in book["chapters"]:
            for paragraph in ch["paragraphs"]:
                if paragraph.get("role") != "author":
                    continue
                source[paragraph["id"]] = (vol, ch["id"], paragraph)

    result = []
    for pid, start in SELECTION:
        assert pid in source, f"Lost source paragraph: {pid}"
        volume, chapter_id, paragraph = source[pid]
        ru = paragraph["ru"]
        # Split exactly as during excerpt selection, retaining original
        # punctuation; never write paraphrases into the reminder deck.
        simplified = ru.replace("\\r", "").replace("Раздел\\n", "").replace("Глава\\n", "").replace("**", "")
        parts = re.split(r"(?<=[.!?…])\\s+(?=[А-ЯЁ«“])", simplified)
        matches = [s.strip().strip('"«»') for s in parts if s.strip().startswith(start)]
        assert len(matches) == 1, f"Ambiguous source excerpt: {pid} / {start!r} -> {len(matches)}"
        quote = matches[0]
        assert quote in ru, f"Not a verbatim substring: {pid}"
        assert 55 <= len(quote) <= 200, f"Unexpected excerpt size: {pid} ({len(quote)})"
        m = re.search(r"-p(\\d{3})", pid)
        if volume == 1:
            pages = paragraph.get("source_pages", [])
            page = pages[0] if pages else (int(m.group(1)) if m else 3)
        else:
            assert m, f"Missing printed page: {pid}"
            page = int(m.group(1))
        result.append({"text": quote, "volume": volume, "chapterId": chapter_id,
                       "paragraphId": pid, "page": int(page)})
    assert len(result) == 74
    assert sum(x["volume"] == 1 for x in result) == 20
    assert sum(x["volume"] == 2 for x in result) == 54
    out = ASSETS / "curated_knowledge_reminders.json"
    out.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\\n", encoding="utf-8")
    print(f"PASS: {len(result)} verbatim curated excerpts; V1=20 V2=54; {out.name}")

if __name__ == "__main__":
    main()
