from pathlib import Path
p=Path('tools/test_check_volume4_quran.py');s=p.read_text().replace("def compare(self, unit, quote, reference, ru=''):","def compare(self, unit, quote, reference, ru='', all_records=False):").replace("verses = {","verses = {\n            (4, 163): checker.normalized('إنا أوحينا إليك كما أوحينا إلى نوح والنبيين من بعده'),\n            (4, 164): checker.normalized('وكلم الله موسى تكليما'),",1).replace("result = checker.compare(Path('unused'))['records'][0]","records = checker.compare(Path('unused'))['records']\n                result = records if all_records else records[0]")
insert='''    grouped = 'إنا أوحينا إليك كما أوحينا إلى نوح والنبيين من بعده} إلى أن قال: {وكلم الله موسى تكليما'

    def test_common_reference_covers_explicitly_separated_quotations(self):
        records = self.compare('test', self.grouped, 'النساء: 163–164', all_records=True)
        self.assertEqual(records[1]['reference_status'], 'grouped_span')
        self.assertEqual(records[1]['group_actual_span'], [[4, 163], [4, 164]])

    def test_grouped_reference_rejects_wrong_start_end_or_sura(self):
        for reference in ('النساء: 162–164', 'النساء: 163–165', 'الأنعام: 163–164'):
            with self.subTest(reference=reference):
                records = self.compare('test', self.grouped, reference, all_records=True)
                self.assertIn(records[1]['reference_status'], ('unmatched', 'range_needs_review'))

    def test_grouped_reference_requires_explicit_ellipsis_marker(self):
        records = self.compare('test', self.grouped.replace('إلى أن قال:', 'وكذلك:'), 'النساء: 163–164', all_records=True)
        self.assertEqual(records[1]['reference_status'], 'unmatched')

    def test_grouped_reference_does_not_hide_corrupt_first_quote(self):
        records = self.compare('test', self.grouped.replace('نوح', 'نوح زائد'), 'النساء: 163–164', all_records=True)
        self.assertEqual(records[0]['status'], 'unmatched')
        self.assertEqual(records[1]['reference_status'], 'unmatched')

'''
s=s.replace('    def test_identical_quote_uses_valid_printed_reference(self):',insert+'    def test_identical_quote_uses_valid_printed_reference(self):');p.write_text(s)
