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
        for m in re.finditer(r'\{([^{}]+)\}(?:\s*\[([^:\]]+):\s*(\d+)(?:\s*[–—\-ـ]+\s*(\d+))?\])?', u['ar']):
            quote = m[1]
            raw = normalized(quote)
            candidate = raw
            variant = u['id'] == 'v4-p015-03-ru' and 'توقد' in raw
            if variant:
                candidate = raw.replace('توقد', 'يوقد', 1)
            evidence = ('Control leaf15 reads تَوَقَّدَ; Ibn Khalawayh Hujja, sura24 discussion, PageV01P262: past active verb with ta and tashdid. Digital witness headword defective, explanatory paragraph explicit; printed author quote retained unchanged. No named reader attribution asserted.' if variant else None)
            if u['id'] == 'v4-p089-01-ru' and raw.startswith('ياعباديلاخوف'):
                candidate = raw.replace('ياعباديلاخوف', 'ياعبادلاخوف', 1)
                variant = True
                evidence = 'Control leaf89 enlarged q43-page89.png retains ya in يا عبادي. Ibn Khalawayh Hujja, sura43 discussion, PageV01P322–323 explicitly attests both retention and omission of ya. Comparison-only omission; printed author source and Russian quotation unchanged. No named reader attribution asserted.'
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
                record.update(status='printed_reading_with_digital_scholarly_attestation' if variant else 'normalised_letters_match', actual_span=[list(sequence[begin]),list(sequence[end])])
                if variant:
                    record['evidence']=evidence
            if m[2]:
                record['printed_reference']=[sur,first,last]
                record['reference_status']='unmatched'
                if pos >= 0 and sur == sequence[begin][0] and first == sequence[begin][1]:
                    record['reference_status']='full_span' if last == sequence[end][1] else 'printed_start_only' if last == first else 'range_needs_review'
            records.append(record)
    return {'method':'Read-only normalized Arabic letter and reference comparison. Printed variants retained. Does not certify Russian meaning or independent human review.', 'canonical_sha256':CANONICAL_SHA256,'canonical_verses_loaded':len(verses),'records':records}

if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('canonical_path',type=Path)
    report=compare(parser.parse_args().canonical_path)
    print(json.dumps(report,ensure_ascii=False,indent=2))
    raise SystemExit(int(any(x['status']=='unmatched' or x.get('reference_status') in ['unmatched','range_needs_review'] for x in report['records'])))
