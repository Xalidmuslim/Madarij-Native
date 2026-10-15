# Full alternative Arabic alignment — machine triage only

This inventory aligns all embedded author Arabic units currently present in the reader: 644 units in volume I (printed pages 3–610) and 676 units in volume II (printed pages 3–659). Editor-note paragraphs are excluded. Every unit is queried against each complete local OpenITI witness, JK000081 (1973 edition) and Shamela0008370 (1996 edition). These are different editions from the reader's selected 2019 source. The report does not establish fidelity to that edition, semantic accuracy, religious-source authenticity, or visual collation. No Arabic or Russian text was replaced and the producer changes only its requested research output files.

## Artifacts and rerun

- `full_alternative_alignment.py`: portable Python 3 standard-library producer.
- `FULL_ALTERNATIVE_ALIGNMENT.jsonl`: one method header, 2,640 paragraph/witness records, and 2,530 related-page summaries.
- `FULL_ALTERNATIVE_ALIGNMENT_SUMMARY.json`: pinned input hashes, counts, flag lists and validation results.
- `validate_full_alternative_alignment.py`: local independent-format/anchor proof used for this run. It reads artifacts beside the script; the producer accepts explicit paths.

Run the producer with:

```sh
python3 research/final-translation-collation/full_alternative_alignment.py \
  --repo /workspace/Madarij-Native \
  --source-dir /tmp \
  --output research/final-translation-collation/FULL_ALTERNATIVE_ALIGNMENT.jsonl \
  --summary research/final-translation-collation/FULL_ALTERNATIVE_ALIGNMENT_SUMMARY.json
```

The two input names are `0751IbnQayyimJawziyya.MadarijSalikin.JK000081-ara1` and `0751IbnQayyimJawziyya.MadarijSalikin.Shamela0008370-ara1`. SHA-256 values in the summary identify the exact local files used; remote provenance is not independently re-fetched or certified.

## Method

1. Read author units, preserving stable reader IDs. Volume I uses `source_pages`; volume II's page spans come from its explicit source tags. Reader Arabic hashes are recorded separately from normalized tokens.
2. Remove parenthetical and square-bracketed material, OpenITI metadata, page markers, annotation tags, and OpenITI heading lines from the witness stream. Apply NFKD, strip combining marks and tatweel, normalize alif maqsura to ya, and retain Arabic-letter tokens. NFKD also erases hamza distinctions. This deliberately loses information and must not be used to rewrite the source.
3. Build exact seven-token indexes over each whole witness, retaining only seven-grams occurring at most three times. There is no global `SequenceMatcher`, global edit-distance computation or full-text comparison.
4. Query all seven-grams from a reader unit. Cluster candidate start offsets in 96-token bins; evaluate the six most populated bins and their immediate neighbors. Select monotone anchor chains inside these local offset windows. Rank candidates by union coverage of query tokens covered by seven-token anchors, then chain length.
5. Extend each chosen anchor locally left/right while normalized words remain exactly equal. `anchor_coverage` measures the union of rare seven-token anchors. `extended_exact_token_coverage` measures their contiguous exact extensions. Neither metric measures semantic correctness. Unanchored spans and bounded short excerpts support later human triage.
6. Units shorter than seven tokens use exact short-string search, with unique, ambiguous and missing results separated. They receive no seven-gram proof. At most twelve short candidates and eight seven-token anchor examples are emitted per unit. No full alternative text is emitted.

## Locator precision

Each anchor has a token index and *both* surrounding `PageV..P..` markers, `marker_before` and `marker_after`. Start/end brackets are also supplied. Markers occur at varying line positions, sometimes within sentences; this run does not assert a visually verified page-start/page-end convention. Use the bracket pair to locate the passage in the alternative file. Never treat its volume/page numbers as 2019 pagination.

Page summaries aggregate complete associated reader units. If a reader unit crosses a page boundary, its coverage contributes to both related-page summaries and is explicitly listed under `crosspage_reader_units`. This is a locator and unit-coverage summary, not an invented exact segmentation of that unit into printed pages. Split opening units of volume II are preserved individually.

## Triage flags and limitations

- `NO_RARE_7GRAM_ANCHOR`: no sufficiently rare exact seven-word anchor. It does **not** prove the passage is absent; frequent Quran/hadith text, short quotations, spelling, editorial variation, or corruption can all cause this.
- `LOW_RARE_ANCHOR_COVERAGE`: fewer than 70% of normalized query tokens covered by selected rare anchors.
- `LOW_EXTENDED_EXACT_COVERAGE`: less than 80% covered after contiguous exact extensions.
- `LONG_UNANCHORED_SPAN`: at least twenty consecutive query tokens remain unanchored.
- `SINGLE_ANCHOR_ONLY`: weak localization, even if an exact extension is long.
- Short-unit flags distinguish limited proof and ambiguity.

All flags are **questions for review**, not findings authorizing correction. The algorithm can miss shifted blocks beyond its local offset window, align a repeated quotation to a nearby occurrence, or score common quotations poorly because their seven-grams were deliberately discarded. Footnotes/headings are not perfectly distinguished from author text in every older witness. Older editions can have genuine differences. No missing words, citations, grades or original readings were reconstructed.

The independent check validates all 2,640 unique paragraph/witness pairs, all 2,530 page summaries, numerical bounds, and every emitted sample anchor's exact normalized equality and surrounding-marker lookup. This proves mechanical output consistency only.
