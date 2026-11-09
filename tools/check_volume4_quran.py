#!/usr/bin/env python3
"""Compare quotations with unchanged Tanzil; keep attested printed readings."""
import argparse
import bisect
import hashlib
import json
from pathlib import Path
import re
from check_volume3_quran import load_canonical, normalized, SURAS, CANONICAL_SHA256
ROOT = Path(__file__).resolve().parents[1]
HUJJA_WITNESS = ('Ibn Khalawayh Hujja, OpenITI/RELEASE '
                 'cfc4157a3cf2054c0888f133970a4eaa3e22e58c, Shamela0026140-ara1; '
                 'SHA256480cfe92bde0c1674d08d4020c57be808ea3a5d93a338b06c8d412b6813d3172. ')

PRINTED_RANGE_ERRORS = {
    'v4-p500-06-ru': (
        (39, 64, 65), ((39, 64), (39, 66)),
        r'(?:\[(?:«аз-Зумар»,\s*)?39:64[–—-]66\]|\(39:64[–—-]66\))',
        'Control leaves500–501 print الزمر:64–65, but the complete quotation ends with39:66 بل الله فاعبد وكن من الشاكرين.'),
    'v4-p502-02-ru': (
        (36, 23, 23), ((36, 22), (36, 23)),
        r'(?:\[(?:«Йа син»,\s*)?36:22[–—-]23\]|\(36:22[–—-]23\))',
        'Control leaf502 prints يس:23, but the complete quotation begins with36:22 وما لي لا أعبد الذي فطرني وإليه ترجعون.'),
}

def compare(path):
    verses = load_canonical(path)
    sequence = list(verses)
    starts, offset = [], 0
    for value in verses.values():
        starts.append(offset)
        offset += len(value)
    whole = ''.join(verses.values())
    verse_indices = {key: i for i, key in enumerate(sequence)}
    names = {normalized(n.replace('_', ' ')): i+1 for i, n in enumerate(SURAS)}
    records = []
    for u in json.loads((ROOT/'research/volume4/translation.json').read_text())['paragraphs']:
        unit_record_start = len(records)
        previous_quote_end = 0
        for m in re.finditer(r'\{([^{}]+)\}(?:\s*\[([^:\]]+):\s*(\d+)(?:\s*[–—\-ـ]+\s*(\d+))?\])?', u['ar']):
            quote = m[1]
            raw = normalized(quote)
            candidate = raw
            orthography = False
            variant = u['id'] == 'v4-p015-03-ru' and 'توقد' in raw
            if variant:
                candidate = raw.replace('توقد', 'يوقد', 1)
            evidence = ('Control leaf15 reads تَوَقَّدَ; Ibn Khalawayh Hujja, sura24 discussion, PageV01P262: past active verb with ta and tashdid. Digital witness headword defective, explanatory paragraph explicit; printed author quote retained unchanged. No named reader attribution asserted.' if variant else None)
            if u['id'] == 'v4-p089-01-ru' and raw.startswith('ياعباديلاخوف'):
                candidate = raw.replace('ياعباديلاخوف', 'ياعبادلاخوف', 1)
                variant = True
                evidence = 'Control leaf89 enlarged q43-page89.png retains ya in يا عبادي. Ibn Khalawayh Hujja, sura43 discussion, PageV01P322–323 explicitly attests both retention and omission of ya. Comparison-only omission; printed author source and Russian quotation unchanged. No named reader attribution asserted.'
            if u['id'] in ('v4-p106-03-ru', 'v4-p198-04-ru') and 'لصالواالجحيم' in raw:
                candidate = raw.replace('لصالواالجحيم', 'لصالوالجحيم', 1)
                orthography = True
                evidence = 'Control leaves106/199, enlarged q83-word-page106.png and q83word199.png: لصالوا with a silent-alif zero mark in printed Quran orthography; unchanged Tanzil Simple Clean has لصالو. Comparison-only spelling alignment, not a different reading. Control Arabic retained.'
            if u['id'] == 'v4-p246-03-ru' and raw.startswith('هنالكتبلواكلنفس'):
                candidate = raw.replace('هنالكتبلواكلنفس', 'هنالكتبلوكلنفس', 1)
                orthography = True
                evidence = 'Control leaf246, enlarged q1030-246.png, prints تبلوا with a silent-alif zero mark in10:30; unchanged Tanzil Simple Clean has تبلو. Comparison-only spelling alignment, not a different reading. Control Arabic retained.'
            if u['id'] == 'v4-p186-02-ru' and raw.startswith('ويومنحشرهم'):
                candidate = raw.replace('ويومنحشرهم', 'ويوميحشرهم', 1)
                variant = True
                evidence = 'Control leaf186, enlarged q10word186.png, reads نَحْشُرُهُمْ in10:45. Ibn Khalawayh Hujja, suraYunus discussion, PageV01P184, explicitly attests ya and nun. Comparison-only alignment to unchanged Tanzil يحشرهم; control Arabic and Russian Мы retained. No named reader attribution asserted.'
            if u['id'] in ('v4-p472-03-ru', 'v4-p499-01-ru'):
                wording = ('الارجالايوحياليهم' if u['id'] == 'v4-p472-03-ru'
                           else 'منرسولالايوحياليه')
                if wording in raw:
                    candidate = raw.replace(wording, wording.replace('يوحي', 'نوحي'), 1)
                    variant = True
                    evidence = HUJJA_WITNESS + (
                        'Control leaf473 prints يوحى إليهم in16:43–44; personally read primary lines4487–4489, between OpenITI markers PageV01P211/P212: يقرأ بالياء وفتح الحاء وبالنون وكسر الحاء. '
                        if u['id'] == 'v4-p472-03-ru' else
                        'Control leaf499 prints يُوحَى إليه in21:25. Direct exact-verse attestation: Ibn Mujahid Sabca, OpenITI/RELEASE cfc4157a3cf2054c0888f133970a4eaa3e22e58c, Shamela0005530-ara1, lines6954–6956, heading quotes singular وما أرسلنا من قبلك من رسول إلا نوحي إليه and explicitly verse25; following lines attest active nun and passive ya. SHA2567cbe25b667f87bdf8a8bd4fa8dfa4a3ff9e4b85715c089b240aefc07f29ab45f, Gitblobb8325cca414dbe2c77da82442ce8c413099ca6b3. Hujja plural headword alone is not used as exact-verse evidence. '
                    ) + 'Comparison-only passive-ya to active-nun alignment to unchanged Tanzil; printed AR and passive Russian wording retained. No named reader attribution asserted.'
            if u['id'] == 'v4-p508-04-ru' and 'ورجلاسالمالرجل' in raw:
                candidate = raw.replace('ورجلاسالمالرجل', 'ورجلاسلمالرجل', 1)
                variant = True
                evidence = HUJJA_WITNESS + 'Control leaf508, enlarged image and crop, prints سَالِمًا in39:29. Personally read primary lines6665–6668, PageV01P308–309: يقرأ بإثبات الألف وكسر اللام وبحذفها وفتح اللام. Comparison-only alif omission to unchanged Tanzil سلما; printed AR and RU retained. No named reader attribution asserted.'
            if u['id'] == 'v4-p521-02-ru' and raw.startswith('وهليجزيالاالكفور'):
                candidate = raw.replace('وهليجزيالاالكفور', 'وهلنجازيالاالكفور', 1)
                variant = True
                evidence = HUJJA_WITNESS + ('Sura34:17, lines6318–6324, between OpenITI markers PageV01P293/P294 explicitly attest passive ya/fatha and active nun/kasra. Actual enlarged control521 has يُجَٰزَى with dagger alif, retained as printed. Alignment to Tanzil active نجازي is comparison-only; no named reader attribution.')
            pos = whole.find(candidate)
            if m[2]:
                sur = names.get(normalized(m[2]))
                first,last = int(m[3]),int(m[4] or m[3])
                index = verse_indices.get((sur, first))
                if index is not None:
                    # Identical wording can occur in several suras. Prefer an
                    # occurrence starting in the cited verse, without accepting
                    # a later verse or changing a genuinely wrong reference.
                    cited_pos = whole.find(candidate, starts[index])
                    verse_end = starts[index + 1] if index + 1 < len(starts) else len(whole)
                    if starts[index] <= cited_pos < verse_end:
                        pos = cited_pos
            record = {'unit':u['id'], 'quote':quote, 'status':'unmatched'}
            if pos >= 0:
                begin = bisect.bisect_right(starts, pos)-1
                end = bisect.bisect_right(starts, pos+len(candidate)-1)-1
                status = ('printed_reading_with_digital_scholarly_attestation' if variant else
                          'printed_orthography_with_control_image' if orthography else 'normalised_letters_match')
                record.update(status=status, actual_span=[list(sequence[begin]),list(sequence[end])])
                if variant or orthography:
                    record['evidence']=evidence
            if m[2]:
                record['printed_reference']=[sur,first,last]
                record['reference_status']='unmatched'
                if pos >= 0 and sur == sequence[begin][0] and first == sequence[begin][1]:
                    record['reference_status']='full_span' if last == sequence[end][1] else 'printed_start_only' if last == first else 'range_needs_review'
                    if (u['id'] == 'v4-p195-04-ru' and (sur, first, last) == (89, 21, 26)
                            and sequence[end] == (89, 24)
                            and re.search(r'\(89:21[–—-]24\)', u.get('ru', ''))):
                        record['reference_status'] = 'printed_range_error_documented'
                        record['corrected_russian_reference'] = [89, 21, 24]
                        record['reference_evidence'] = 'Actual control195 enlarged q89ref195.png prints21–26, but quotation ends with89:24 يقول يا ليتني قدمت لحياتي. Printed AR reference retained, RU corrected to21–24 and mechanically checked. Missing25–26 not inserted into author text; this status documents a source reference error, not a correct printed full span.'
                documented = PRINTED_RANGE_ERRORS.get(u['id'])
                if documented and pos >= 0:
                    printed, span, russian_reference, reference_evidence = documented
                    actual_first, actual_last = span
                    # These two control errors quote complete verses. Require
                    # their entire text, not merely matching start/end numbers.
                    complete_quote = ''.join(verses.get((actual_first[0], n), '')
                                             for n in range(actual_first[1], actual_last[1] + 1))
                    if ((sur, first, last) == printed
                            and (sequence[begin], sequence[end]) == span
                            and candidate == complete_quote
                            and re.search(russian_reference, u.get('ru', ''))):
                        record['reference_status'] = 'printed_range_error_documented'
                        record['corrected_russian_reference'] = [actual_first[0], actual_first[1], actual_last[1]]
                        record['reference_evidence'] = reference_evidence + ' Printed AR retained; corrected RU range mechanically checked. This documents a printed reference error, not a correct printed full span.'
                # A shared range can refer to two quotations separated by the
                # author's explicit "up to his saying", rather than just the last.
                if (pos >= 0 and len(records) > unit_record_start
                        and re.fullmatch(r'\s*إلى أن قال:\s*', u['ar'][previous_quote_end:m.start()])):
                    previous = records[-1]
                    if (previous['status'] != 'unmatched'
                            and 'printed_reference' not in previous
                            and previous['actual_span'][0] == [sur, first]
                            and previous['actual_span'][1][0] == sur
                            and previous['actual_span'][1][1] < sequence[begin][1]
                            and list(sequence[begin]) == [sur, last]
                            and list(sequence[end]) == [sur, last]):
                        record['reference_status'] = 'grouped_span'
                        record['group_actual_span'] = [previous['actual_span'][0], list(sequence[end])]
            records.append(record)
            previous_quote_end = m.end()
    return {'method':'Read-only normalized Arabic letter and reference comparison. Printed variants retained. Does not certify Russian meaning or independent human review.', 'canonical_sha256':CANONICAL_SHA256,'canonical_verses_loaded':len(verses),'records':records}

if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('canonical_path',type=Path)
    report=compare(parser.parse_args().canonical_path)
    print(json.dumps(report,ensure_ascii=False,indent=2))
    raise SystemExit(int(any(x['status']=='unmatched' or x.get('reference_status') in ['unmatched','range_needs_review'] for x in report['records'])))
