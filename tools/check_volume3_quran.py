#!/usr/bin/env python3
"""Compare detected quotations/references with a pinned Tanzil digital witness.

Read-only comparison: never edits the canonical text, author text or translation.
This checks Arabic letters and references after stated normalization. It cannot
certify Russian meaning, identify every implicit citation or grade hadiths.
"""
import argparse
import bisect
from collections import Counter
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
CANONICAL_SHA256 = "374d13c056285552edbde9fc1def53c7993a3e8434ec88cb5734fca87644e954"
SURAS = (
    "الفاتحة البقرة آل_عمران النساء المائدة الأنعام الأعراف الأنفال التوبة يونس هود يوسف الرعد إبراهيم الحجر النحل الإسراء الكهف مريم طه "
    "الأنبياء الحج المؤمنون النور الفرقان الشعراء النمل القصص العنكبوت الروم لقمان السجدة الأحزاب سبأ فاطر يس الصافات ص الزمر غافر "
    "فصلت الشورى الزخرف الدخان الجاثية الأحقاف محمد الفتح الحجرات ق الذاريات الطور النجم القمر الرحمن الواقعة الحديد المجادلة الحشر الممتحنة "
    "الصف الجمعة المنافقون التغابن الطلاق التحريم الملك القلم الحاقة المعارج نوح الجن المزمل المدثر القيامة الإنسان المرسلات النبأ النازعات عبس "
    "التكوير الانفطار المطففين الانشقاق البروج الطارق الأعلى الغاشية الفجر البلد الشمس الليل الضحى الشرح التين العلق القدر البينة الزلزلة العاديات "
    "القارعة التكاثر العصر الهمزة الفيل قريش الماعون الكوثر الكافرون النصر المسد الإخلاص الفلق الناس"
).split()


def strip_marks(text):
    return re.sub("[\u064b-\u065f\u0670\u0640\u06d6-\u06ed]", "", text)


def normalized(text):
    text = re.sub("[أإآٱ]", "ا", strip_marks(text)).replace("ى", "ي")
    return "".join(re.findall("[ء-ي]+", text))


def load_canonical(path):
    raw = path.read_bytes()
    if hashlib.sha256(raw).hexdigest() != CANONICAL_SHA256:
        raise ValueError("Unexpected canonical file: use the documented, unchanged OpenITI Tanzil witness")
    text = raw.decode("utf-8-sig").replace("\n~~", " ")
    text = re.sub(r"\bms[0-9]+\b", "", text)  # OpenITI navigation markers only.
    verses = {}
    for line in text.splitlines():
        m = re.match(r"#\s*(\d+)\s*\|\s*(\d+)\s*\|\s*(.*)", line)
        if m:
            key = (int(m[1]), int(m[2]))
            if key in verses:
                raise ValueError(f"Duplicate canonical verse: {key}")
            verses[key] = normalized(m[3])
    if len(verses) != 6236 or {s for s, _ in verses} != set(range(1, 115)):
        raise ValueError("Incomplete canonical text")
    return verses


def readings(key, quote):
    result = [normalized(quote)]
    # Attested in Ibn Khalawayh; these are comparison alternatives only.
    # Entire resulting quotations must still match the canonical text.
    if key == "v3-p021-04" and result[0].startswith("ومايشاءون"):
        result.append(result[0].replace("ومايشاءون", "وماتشاءون", 1))
    if key == "v3-p086-06+v3-p087-01" and "لفتيته" in result[0]:
        result.append(result[0].replace("لفتيته", "لفتيانه", 1))
    return result


def check(canonical_path):
    verses = load_canonical(canonical_path)
    sequence = list(verses)
    starts, offset = [], 0
    for v in verses.values():
        starts.append(offset)
        offset += len(v)
    whole = "".join(verses.values())
    assert len(SURAS) == 114
    names = {normalized(n.replace("_", " ")): i + 1 for i, n in enumerate(SURAS)}
    names.update({normalized("المؤمن"): 40, normalized("بني إسرائيل"): 17})
    file = ROOT / "research/volume3/translation.json"
    units = json.loads(file.read_text())["paragraphs"]
    quotes, printed_refs, russian_refs = [], [], []
    for u in units:
        key = "+".join(u["source_ids"])
        found = re.findall(r"\{([^{}]+)\}", u["ar"])
        for quote in found:
            record = {"source_ids": u["source_ids"], "quote": quote, "status": "unmatched"}
            for n, text in enumerate(readings(key, quote)):
                pos = whole.find(text)
                if pos >= 0:
                    record.update(status="normalized_match" if n == 0 else "attested_reading_variant",
                                  first_matching_verse=list(sequence[bisect.bisect_right(starts, pos) - 1]))
                    break
            quotes.append(record)
        pattern = r"\{([^{}]+)\}\s*\[([^:\]]+):\s*(\d+)(?:\s*[–—\-ـ]+\s*(\d+))?\]"
        for m in re.finditer(pattern, u["ar"]):
            name = m[2]
            if normalized(name) == normalized("آية"):
                context_names = re.findall(r"سورة\s+([ء-ي]+)", strip_marks(u["ar"][:m.start()]))
                name = context_names[-1] if context_names else name
            surah = names.get(normalized(name))
            a, b = int(m[3]), int(m[4] or m[3])
            record = {"source_ids": u["source_ids"], "printed_label": m[2] + ":" + m[3], "status": "unmatched"}
            if surah:
                for end in range(b, b + 4):
                    if not all((surah, v) in verses for v in range(a, end + 1)):
                        break
                    window = "".join(verses[surah, v] for v in range(a, end + 1))
                    if any(text in window for text in readings(key, m[1])):
                        record.update(status="matched_printed_range" if end == b else "abbreviated_printed_reference",
                                      actual_reference=[surah, a, end])
                        break
            printed_refs.append(record)
        for m in re.finditer(r"(?<!\d)(\d{1,3}):(\d{1,3})(?:\s*[–—\-ـ]+\s*(\d{1,3}))?", u["ru"]):
            surah, a, b = int(m[1]), int(m[2]), int(m[3] or m[2])
            record = {"source_ids": u["source_ids"], "reference": m[0], "status": "unmatched"}
            if all((surah, v) in verses for v in range(a, b + 1)):
                window = "".join(verses[surah, v] for v in range(a, b + 1))
                if any(text in window for quote in found for text in readings(key, quote)):
                    record["status"] = "matches_quoted_text"
                elif (key == "v3-p458-05" and (surah, a, b) == (33, 6, 6) and
                      "وهو أب لهم" in u["ar"] and any(n["kind"] == "reported_quran_reading" for n in u.get("editorial_notes", []))):
                    record["status"] = "documented_historical_reading_not_Hafs_text"
            russian_refs.append(record)
    unresolved = [r for group in [quotes, printed_refs, russian_refs] for r in group if r["status"] == "unmatched"]
    return {
        "method": "Mechanical Arabic-text/reference comparison plus explicitly documented same-agent contextual checks; not independent human review or Russian semantic certification.",
        "canonical_source": {"project": "Tanzil, Simple Clean 1.1", "url": "https://tanzil.net", "license": "CC BY 3.0; source file retained unchanged, not bundled with the app",
                             "repository": "OpenITI/RELEASE", "ref": "cfc4157a3cf2054c0888f133970a4eaa3e22e58c", "blob": "8d0a6c806598d6c6afe6b7f9b20940ab1baeddee", "sha256": CANONICAL_SHA256},
        "translation_sha256": hashlib.sha256(file.read_bytes()).hexdigest(),
        "canonical_verse_count": len(verses), "brace_delimited_quotations": len(quotes),
        "quote_status_counts": dict(Counter(r["status"] for r in quotes)),
        "printed_reference_count": len(printed_refs), "russian_numeric_reference_count": len(russian_refs),
        "unresolved": unresolved, "quotes": quotes, "printed_references": printed_refs,
        "russian_references": russian_refs, "independent_human_review": False,
    }


if __name__ == "__main__":
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("canonical_path", type=Path)
    args = ap.parse_args()
    report = check(args.canonical_path)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    raise SystemExit(1 if report["unresolved"] else 0)
