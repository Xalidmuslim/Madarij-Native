#!/usr/bin/env python3
"""Conservative Q1 token screening; never edits corpus Arabic or old registry fields."""
import argparse, collections, difflib, hashlib, json, re, unicodedata
from pathlib import Path

WORD = re.compile('[ء-غف-ي]+')
ALIF = str.maketrans('أإآٱ','اااا')

def base(text):
    text = ''.join(c for c in text if unicodedata.category(c) != 'Mn' and c not in 'ـۥۦ')
    text = text.translate(ALIF)
    return [re.sub(r'^ءا','ا',w) for w in WORD.findall(text)]

def hamza_base(text):
    text=re.sub(r'[اوي]([\u064b-\u0652\u0656-\u065f\u0670\u06d6-\u06ed]*)[ٕٔ]', r'ء\1', text)
    text=text.replace('آ','ءا').translate(str.maketrans({'أ':'ء','إ':'ء','ؤ':'ء','ئ':'ء','ٱ':'ا','ٔ':'ء','ٕ':'ء'}))
    return base(text)

# Explicit lexical spellings only; never delete final alif from arbitrary verbs.
SILENT_ALIF = {'اولوا':'اولو','واولوا':'واولو','يرجوا':'يرجو','يدعوا':'يدعو',
               'ويعفوا':'ويعفو','اشكوا':'اشكو','يتلوا':'يتلو',
               'الرباا':'الربا','العلماءا':'العلماء','ءشكوا':'ءشكو','فاسءل':'فسءل','ويرجوا':'ويرجو','اليل':'الليل','فباءو':'فباءوا','جزاءا':'جزاء','برءاءا':'برءاء'}

def forms(token):
    # Only attested script devices: dagger alif, superscript marks, alif seats.
    token=re.sub('ۥ(?=[ء-غف-ي])','و',token).replace('ۧ','ي')
    variants = [token, token.replace('ٰ','ا')]
    imlai = token.replace('وٰ','ا')
    imlai = re.sub('ىٰ(?=[ء-غف-ي])','ا',imlai).replace('ىٰ','ى').replace('ٰ','ا')
    variants.extend([imlai,token.replace('ىٰ','ا').replace('وٰ','ا').replace('ٰ','ا')])
    values={''.join(normalizer(v)) for v in variants for normalizer in (base,hamza_base) if normalizer(v)}
    values.update(SILENT_ALIF[v] for v in tuple(values) if v in SILENT_ALIF)
    return frozenset(values)

def words(text):
    # Marks stay attached until token variants are generated; punctuation/digits delimit.
    return [w for w in re.split(r'[^\u0621-\u063a\u0641-\u065f\u0670-\u0671\u06d6-\u06edـ]+', text) if base(w)]

def ref_of(surah, verses):
    return f'{surah}:{min(verses)}' + (f'-{max(verses)}' if max(verses) != min(verses) else '')

def parse_ref(ref):
    m=re.fullmatch(r'(\d+):(\d+)(?:-(\d+))?',ref or '')
    return tuple(map(int,(m[1],m[2],m[3] or m[2]))) if m else None

def alignment(quote, source):
    source_forms=[forms(t) for t in source]
    source_base=[''.join(base(t)) for t in source]
    q=[]
    for token in quote:
        fs=forms(token)
        compatible={s for s,ss in zip(source_base,source_forms) if fs & ss}
        q.append(next(iter(compatible)) if len(compatible)==1 else ''.join(base(token)))
    matcher=difflib.SequenceMatcher(None,q,source_base,autojunk=False)
    changes=[]
    for op,a,b,c,d in matcher.get_opcodes():
        if op=='equal': continue
        edge=op=='insert' and a in (0,len(q))
        changes.append({'operation':op,'quote_tokens':quote[a:b],'Q1_tokens':source[c:d],
                        'reference_edge_not_quoted':edge})
    internal=[d for d in changes if not d['reference_edge_not_quoted']]
    if internal and all(d['operation']=='insert' for d in internal):
        status='ordered_quote_with_internal_Q1_tokens_not_quoted'
    elif internal:
        status='token_difference_requires_review'
    else:
        status='alignment_inconclusive_requires_review'
    return status,changes

def main(args):
    registry=Path(args.registry);qpath=Path(args.quran)
    original=[json.loads(s) for s in registry.read_text().splitlines() if s.strip()]
    quran=json.loads(qpath.read_text());surahs={};verse_tokens={};index=collections.defaultdict(set)
    for surah in quran:
        sid=surah['id'];tokens=[];vids=[]
        for verse in surah['verses']:
            ts=words(verse['text']);verse_tokens[sid,verse['id']]=ts
            tokens.extend(ts);vids.extend([verse['id']]*len(ts))
        surahs[sid]=(tokens,vids,[forms(t) for t in tokens])
        for pos,fs in enumerate(surahs[sid][2]):
            for f in fs:index[f].add((sid,pos))
    results=[];counts=collections.Counter();old_mismatch=collections.Counter();unresolved=collections.Counter()
    for row in original:
        quote=words(row['arabic_quote']);qforms=[forms(t) for t in quote];matches=[]
        starts=set().union(*(index[f] for f in qforms[0])) if qforms else set()
        for sid,pos in sorted(starts):
            ts,vs,fs=surahs[sid]
            if pos+len(quote)>len(ts):continue
            if all(qf & sf for qf,sf in zip(qforms,fs[pos:pos+len(quote)])):
                selected=ts[pos:pos+len(quote)];strict=all(base(a)==base(b) for a,b in zip(quote,selected))
                matches.append({'reference':ref_of(sid,vs[pos:pos+len(quote)]),'surah':sid,
                                'start_token_in_surah':pos,'end_token_exclusive':pos+len(quote),
                                'normalization_level':'diacritics_recitation_marks_alif_seats' if strict else 'uthmani_imlai_script_compatible',
                                'Q1_matched_tokens':selected})
        printed=parse_ref(row.get('resolved_reference'));expected=[]
        if printed:
            sid,start,end=printed
            for vid in range(start,end+1):expected.extend(verse_tokens.get((sid,vid),[]))
        at_printed=[m for m in matches if printed and m['surah']==printed[0] and parse_ref(m['reference'])[1]>=printed[1] and parse_ref(m['reference'])[2]<=printed[2]]
        out=dict(row)
        detail={'source':'Q1','screen_only':True,'expert_verified':False,'visual_original_comparison':False,
                'human_expert_verified':False,'inference_does_not_verify_author_intent_or_reading':True,
                'Q1_exact_normalized_occurrence_count':len(matches),'token_count':len(quote),
                'inferred_reference':None,'reference_basis':'existing_registry_reference' if printed else 'unresolved',
                'normalization_policy':'q1-conservative-token-v1'}
        if not quote:
            status='empty_arabic_quote_requires_review'
        elif at_printed:
            chosen=at_printed[0]
            status=('contiguous_match_at_printed_reference' if chosen['normalization_level'].startswith('diacritics') else 'orthography_compatible_contiguous_match_at_printed_reference')
            detail['match']=chosen
            detail['unquoted_reference_token_count']=len(expected)-len(quote)
            detail['excerpt_status']='entire_reference_token_span' if len(expected)==len(quote) else 'proper_contiguous_excerpt_not_proof_of_source_completeness'
        elif not printed and len(matches)==1:
            chosen=matches[0];detail['inferred_reference']=chosen['reference'];detail['reference_basis']='unique_exact_normalized_phrase_in_Q1';detail['match']=chosen
            detail['short_quote_warning']=len(quote)<3
            status='reference_inferred_from_unique_normalized_phrase'
        elif not printed:
            status='reference_ambiguous_multiple_exact_matches' if matches else 'reference_unresolved_no_exact_normalized_match'
        elif matches:
            status='quote_matches_outside_existing_reference_requires_review'
            detail['alternative_references']=sorted({m['reference'] for m in matches})
            if len(matches)==1:detail['alternative_unique_match']=matches[0]
        else:
            status,diffs=alignment(quote,expected)
            detail['token_alignment_differences']=diffs
            detail['alignment_is_heuristic_not_textual_verification']=True
        detail['status']=status
        if len(matches)>1:detail['matching_references']=sorted({m['reference'] for m in matches})
        out['Q1_enhanced_screen']=detail;results.append(out);counts[status]+=1
        if row['electronic_screen']=='requires_text_or_orthography_review':old_mismatch[status]+=1
        if not row.get('resolved_reference'):unresolved[status]+=1
    assert len(results)==len(original)==1083
    assert [r['id'] for r in results]==[r['id'] for r in original]
    assert all(all(r[k]==s[k] for k in s) for r,s in zip(results,original))
    target=Path(args.output);target.write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in results))
    summary={'records':len(results),'Q1_sha256':hashlib.sha256(qpath.read_bytes()).hexdigest(),
        'input_registry_sha256':hashlib.sha256(registry.read_bytes()).hexdigest(),
        'output_sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'script_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
        'statuses':dict(counts),'original_478_mismatches':dict(old_mismatch),'original_240_unresolved':dict(unresolved),
        'original_fields_preserved':True,'reader_ar_ru_changed':False,'expert_verified':False,'visual_original_comparison':False,
        'normalization':['strip vocalization/recitation marks and tatweel','normalize alif seats أإآٱ to ا; initial ءا to ا','tokenwise dagger-alif omission/expansion; Uthmani وٰ/medial ىٰ to imlai alif where compatible; hamza carriers aligned using consonantal spelling variants; explicit lexical silent-alif list','no general hamza deletion, no ة/ه merger, no negation/pronoun deletion, no fuzzy inference; expand small high yeh and word-internal small waw; finite spelling list is explicit in script'],
        'limits':['Unique normalized phrase identifies only an electronic Q1 occurrence; it does not prove author intent, printed edition content, recitation, or translation correctness.','A proper excerpt may be intentional or extraction-truncated; matching does not certify source completeness.','Token edit alignment is heuristic; missing/extra/replacement labels require manual reading.','Existing printed-reference parsing remains unmodified, including possibly incorrect parsed references.']}
    Path(args.summary).write_text(json.dumps(summary,ensure_ascii=False,indent=2));print(json.dumps(summary,ensure_ascii=False,indent=2))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--registry',default='/workspace/Madarij-Native/research/final-translation-collation/QURAN_CANDIDATES.jsonl');p.add_argument('--quran',default='/tmp/collation-quran-pinned.json');p.add_argument('--output',default='/tmp/quran-registry-enhanced.jsonl');p.add_argument('--summary',default='/tmp/quran-registry-enhanced-summary.json');args=p.parse_args()
    assert base('لَا')==['لا'] and base('نَعْبُدُ')!=base('نَسْتَعِينُ')
    assert forms('ٱلۡعَٰلَمِينَ') & forms('الْعَالَمِينَ')
    assert forms('ٱلصَّلَوٰةَ') & forms('الصلاة')
    assert forms('يُسۡـَٔلُ') & forms('يُسْأَلُ')
    assert forms('شَيۡـٔٗا') & forms('شَيْئًا')
    assert forms('يَرۡجُواْ') & forms('يرجو')
    assert forms('ٱلۡعُلَمَـٰٓؤُاْ') & forms('العلماء')
    assert forms('النَّبِيِّينَ') & forms('ٱلنَّبِيِّـۧنَ')
    assert forms('وَرَاءِ') & forms('وَرَآيِٕ')
    assert not forms('سؤال') & forms('سال')
    assert not forms('قَالُوا') & forms('قال')
    assert not forms('رَحْمَتُهُ') & forms('رحمتهما')
    main(args)
