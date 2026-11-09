from pathlib import Path
p=Path('tools/test_check_volume4_quran.py');s=p.read_text().replace("            (36, 22):", "            (34, 17): checker.normalized('ذلك جزيناهم بما كفروا وهل نجازي إلا الكفور'),\n            (36, 22):",1)
pos=s.index('    def test_new_attested_printed_readings_match_without_mutating_source')
s=s[:pos]+'''    def test_saba_passive_reading_keeps_uthmani_spelling(self):
        quote = 'وهل يُجَٰزَى إلا الكفور'
        result = self.compare('v4-p521-02-ru', quote, 'سبأ: 17')
        self.assertEqual(result['status'], 'printed_reading_with_digital_scholarly_attestation')
        self.assertEqual(result['actual_span'], [[34, 17], [34, 17]])
        self.assertEqual(result['reference_status'], 'full_span')
        self.assertEqual(result['quote'], quote)

    def test_saba_exception_keeps_other_letter_and_reference_failures(self):
        quote = 'وهل يُجَٰزَى إلا الكفور'
        self.assertEqual(self.compare('other', quote, 'سبأ: 17')['status'], 'unmatched')
        self.assertEqual(self.compare('v4-p521-02-ru', quote+' زائد', 'سبأ: 17')['status'], 'unmatched')
        self.assertEqual(self.compare('v4-p521-02-ru', quote, 'سبأ: 18')['reference_status'], 'unmatched')

'''+s[pos:];p.write_text(s)
