#!/usr/bin/env python3
"""Deterministic, source-verifiable additions to the Study section.

These are RECOGNITION questions, not theological judgments. Correct choices
are verbatim sentences in a specific printed chapter. Distractors are
verbatim sentences from different chapters in the SAME volume. No LLM or
network calls, paraphrases, fabricated attributions, or edits to the books.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
SENTENCE_SPLIT = re.compile(r'(?<=[.!?…])\s+(?=[А-ЯЁ«])')
SPACE = re.compile(r'\s+')
SKIP_STARTS = ('раздел ', 'глава ', 'том ', 'сура ', '…', '...', '—',
               'итак,', 'что касается', 'другими словами')

def excerpt(text: str) -> str | None:
    text = SPACE.sub(' ', text.replace('**', ' ').replace('\r', ' ')).strip()
    candidates = []
    for sentence in SENTENCE_SPLIT.split(text):
        sentence = sentence.strip()
        if not (86 <= len(sentence) <= 205):
            continue
        if sentence.casefold().startswith(SKIP_STARTS):
            continue
        if any(x in sentence for x in ('[', ']', 'https://', '«', '»', '…')):
            continue
        if sentence.count(':') > 1 or sentence.count(';') > 2:
            continue
        if not sentence[-1] in '.!?':
            continue
        # Preserve every word: returned sentence must be a verbatim substring.
        if sentence not in text:
            continue
        candidates.append(sentence)
    if not candidates:
        return None
    return sorted(candidates, key=lambda s: (abs(len(s) - 134), s))[0]

def select(chapter, max_count: int):
    unique = {}
    for p in chapter.get('paragraphs', []):
        if p.get('role') != 'author':
            continue
        q = excerpt(p.get('ru', ''))
        if q is None:
            continue
        if q.casefold() in unique:
            continue
        unique[q.casefold()] = {'paragraph': p, 'sentence': q}
    values = list(unique.values())
    # Even distribution across the printed chapter, not just the first pages.
    if len(values) <= max_count:
        return values
    return [values[(i * (len(values) - 1)) // (max_count - 1)] for i in range(max_count)]

def make():
    old = json.loads((ASSETS / 'study.json').read_text(encoding='utf-8'))
    existing_ids = {x['id'] for x in old['questions']}
    books = [
        (1, json.loads((ASSETS / 'corpus.json').read_text(encoding='utf-8')), 3),
        (2, json.loads((ASSETS / 'volume2-preview.json').read_text(encoding='utf-8')), 14)
    ]
    questions, exercises = [], []
    original_question_ids = {1: [], 2: []}
    report = {}
    all_options_by_volume = {}
    for volume, book, per_chapter in books:
        chapters = book['chapters']
        # Retain all old 14-per-chapter volume-2 questions verbatim, with
        # stable IDs and answer alternatives. New additional candidates come
        # AFTER the original selection, never replacing progress records.
        base_pool = {c['id']: select(c, per_chapter) for c in chapters}
        extra_pool = {
            c['id']: ([item for item in select(c, 38)
                       if item['sentence'] not in {v['sentence'] for v in base_pool[c['id']]}]
                      if volume == 2 else [])
            for c in chapters
        }
        pool = {c['id']: base_pool[c['id']] + extra_pool[c['id']] for c in chapters}
        base_options = [(c['id'], candidate['sentence']) for c in chapters
                        for candidate in base_pool[c['id']]]
        all_options = [(c['id'], candidate['sentence']) for c in chapters
                       for candidate in pool[c['id']]]
        chapter_corpus = {c['id']: SPACE.sub(' ', ' '.join(
            p.get('ru', '') for p in c.get('paragraphs', [])
        )) for c in chapters}
        if len(all_options) < 20:
            raise ValueError(f'Insufficient useful verified sentences in volume {volume}')
        all_options_by_volume[volume] = all_options
        used = set()
        count = 0
        for index, chapter in enumerate(chapters):
            chapter_id = chapter['id']
            selected = pool[chapter_id]
            for ci, item in enumerate(selected):
                p, sentence = item['paragraph'], item['sentence']
                if sentence.casefold() in used:
                    continue
                used.add(sentence.casefold())
                digest = hashlib.sha256((p['id'] + '\n' + sentence).encode('utf-8')).hexdigest()[:12]
                question_id = f'book-recognition-v{volume}-{digest}'
                if question_id in existing_ids:
                    raise ValueError(f'Duplicate study question {question_id}')
                if ci < len(base_pool[chapter_id]):
                    original_question_ids[volume].append(question_id)
                # Nearby chapters are more challenging and keep topic coherence.
                option_pool = base_options if ci < len(base_pool[chapter_id]) else all_options
                distractors = [
                    (other, other_sentence) for other, other_sentence in option_pool
                    if other != chapter_id and other_sentence != sentence
                    and other_sentence not in chapter_corpus[chapter_id]
                ]
                distractors.sort(key=lambda v: (
                    abs(next(i for i, c in enumerate(chapters) if c['id'] == v[0]) - index),
                    abs(len(v[1]) - len(sentence)),
                    hashlib.sha256((question_id + v[1]).encode('utf-8')).hexdigest()
                ))
                chosen = distractors[:3]
                if len(chosen) < 3:
                    raise ValueError(f'Not enough alternatives for {question_id}')
                options = [
                    {'text': sentence, 'correct': True,
                     'explanation': 'Эта формулировка действительно есть в выбранном разделе. Откройте исходный абзац для полного контекста.'}
                ] + [
                    {'text': other_sentence, 'correct': False,
                     'explanation': 'Фраза взята из другого раздела этого же тома. Сопоставьте её с текстом выбранной главы.'}
                    for _, other_sentence in chosen
                ]
                # Avoid a predictable first-option answer even before UI shuffling.
                shift = int(digest[:2], 16) % 4
                options = options[shift:] + options[:shift]
                prompts = (
                    'Какая из этих формулировок действительно встречается в выбранном разделе?',
                    'Найдите точную мысль автора из этой части книги.',
                    'Какое из четырёх утверждений принадлежит этому разделу?',
                    'Какую формулировку можно найти в тексте выбранной главы?'
                )
                questions.append({
                    'id': question_id, 'chapterId': chapter_id,
                    'level': 'basic', 'kind': 'recognition',
                    'prompt': prompts[ci % len(prompts)],
                    'options': options, 'sourceParagraphId': p['id'],
                    'sourceExcerpt': sentence
                })
                count += 1
                if volume == 2 and ci % 3 == 0:
                    exercises.append({
                        'id': f'book-reflection-v2-{digest}',
                        'chapterId': chapter_id,
                        'sourceParagraphId': p['id'],
                        'prompt': f'Прочитайте мысль автора и объясните её своими словами: «{sentence}». Как она связана с темой раздела?',
                        'hint': 'Для самопроверки перечитайте исходный абзац в книге. Не считайте редакторский ориентир готовым правильным ответом.'
                    })
        report[f'volume_{volume}'] = count
        if volume == 2:
            report['volume_2_extra'] = count - sum(len(base_pool[c['id']]) for c in chapters)
    if report['volume_1'] < 140 or report['volume_2'] < 200:
        raise ValueError(f'Insufficient coverage: {report}')
    # Regression signatures from the previously shipped 484 APK. Existing
    # attempt keys must remain valid; new v2 questions are append-only.
    expected_legacy_hashes = {
        1: '67fe50de213674ea8aa44c55ad1d5d53f9250b6117a13c4383c23e200510dd0e',
        2: 'f03a7827e7c8b62e3c4254a607b2b2525b1b854ee44e2daab63cb09be4027533',
    }
    for volume, expected in expected_legacy_hashes.items():
        actual = hashlib.sha256('\n'.join(original_question_ids[volume]).encode('utf-8')).hexdigest()
        if actual != expected:
            raise ValueError(f'Legacy volume {volume} progress IDs changed: {actual}')
    new_ids = [x['id'] for x in questions]
    if len(new_ids) != len(set(new_ids)):
        raise ValueError('Generated question IDs not unique')
    sources = {
        p['id']: (vol, c['id'], SPACE.sub(' ', p.get('ru', '').replace('**', ' ')))
        for vol, book, _ in books
        for c in book['chapters']
        for p in c['paragraphs']
    }
    for q in questions:
        options = q['options']
        assert len(options) == 4 and sum(x['correct'] for x in options) == 1
        assert len({x['text'] for x in options}) == 4
        vol, ch, original = sources[q['sourceParagraphId']]
        assert ch == q['chapterId'], 'Wrong source chapter'
        assert q['sourceExcerpt'] in original, 'Correct answer not in book'
        assert next(x for x in options if x['correct'])['text'] == q['sourceExcerpt']
        # Recognition distractors are real sentences from other chapters,
        # not invented religious claims.
        from_other_chapters = {
            sentence for candidate_ch, sentence in all_options_by_volume[vol]
            if candidate_ch != ch
        }
        assert all(x['text'] in from_other_chapters
                   for x in options if not x['correct'])
    out = {'schema': 1, 'generation': 'deterministic-verbatim-chapter-recognition',
           'questions': questions, 'exercises': exercises}
    (ASSETS / 'study_supplement.json').write_text(
        json.dumps(out, ensure_ascii=False, indent=2) + '\n', encoding='utf-8'
    )
    print('STUDY SUPPLEMENT:', report, 'new questions,', len(exercises), 'reflective tasks')

if __name__ == '__main__':
    make()
