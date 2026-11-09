from pathlib import Path
p=Path('tools/check_volume4_quran.py');s=p.read_text();needle="            pos = whole.find(candidate)";assert needle in s;s=s.replace(needle,"""            if u['id'] == 'v4-p521-02-ru' and raw.startswith('وهليجزيالاالكفور'):
                candidate = raw.replace('وهليجزيالاالكفور', 'وهلنجازيالاالكفور', 1)
                variant = True
                evidence = HUJJA_WITNESS + ('Sura34:17, lines6318–6324, PageV01P293 explicitly attest passive ya/fatha and active nun/kasra. Actual enlarged control521 has يُجَٰزَى with dagger alif, retained as printed. Alignment to Tanzil active نجازي is comparison-only; no named reader attribution.')
"""+needle);p.write_text(s)
