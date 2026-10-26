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
            pos = whole.find(candidate)
            record = {'unit':u['id'], 'quote':quote, 'status':'unmatched'}
            if pos >= 0:
                begin = bisect.bisect_right(starts, pos)-1
                end = bisect.bisect_right(starts, pos+len(candidate)-1)-1
                record.update(status='printed_reading_with_digital_scholarly_attestation' if variant else 'normalised_letters_match', actual_span=[list(sequence[begin]),list(sequence[end])])
                if variant:
                    record['evidence']='Control leaf15 reads تَوَقَّدَ; Ibn Khalawayh Hujja, sura24 discussion, PageV01P262: past active verb with ta and tashdid. Digital witness headword defective, explanatory paragraph explicit; printed author quote retained unchanged. No named reader attribution asserted.'
            if m[2]:
                sur = names.get(normalized(m[2]))
                first,last = int(m[3]),int(m[4] or m[3])
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
