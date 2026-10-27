"""Focused checks for repeated verses and a documented printed reading."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import check_volume4_quran as checker


class QuranComparisonTests(unittest.TestCase):
    repeated = 'قل يا قوم اعملوا على مكانتكم إني عامل فسوف تعلمون'
    with_ya = 'يا عبادي لا خوف عليكم اليوم'

    def compare(self, unit, quote, reference, ru=''):
        verses = {
            (6, 135): checker.normalized(self.repeated),
            (10, 45): checker.normalized('ويوم يحشرهم كأن لم يلبثوا إلا ساعة من النهار يتعارفون بينهم'),
            (39, 39): checker.normalized(self.repeated),
            (43, 68): checker.normalized('يا عباد لا خوف عليكم اليوم'),
            (83, 15): checker.normalized('كلا إنهم عن ربهم يومئذ لمحجوبون'),
            (83, 16): checker.normalized('ثم إنهم لصالو الجحيم'),
            (89, 21): checker.normalized('كلا إذا دكت الأرض دكا دكا'),
            (89, 22): checker.normalized('وجاء ربك والملك صفا صفا'),
            (89, 23): checker.normalized('وجيء يومئذ بجهنم يومئذ يتذكر الإنسان وأنى له الذكرى'),
            (89, 24): checker.normalized('يقول يا ليتني قدمت لحياتي'),
        }
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / 'research/volume4/translation.json'
            target.parent.mkdir(parents=True)
            data = {'paragraphs': [{'id': unit, 'ar': '{' + quote + '} [' + reference + ']', 'ru': ru}]}
            original = json.dumps(data, ensure_ascii=False)
            target.write_text(original)
            with patch.object(checker, 'ROOT', root), patch.object(checker, 'load_canonical', return_value=verses):
                result = checker.compare(Path('unused'))['records'][0]
            self.assertEqual(target.read_text(), original)
            return result

    def test_identical_quote_uses_valid_printed_reference(self):
        result = self.compare('test', self.repeated, 'الزمر: 39')
        self.assertEqual(result['actual_span'], [[39, 39], [39, 39]])
        self.assertEqual(result['reference_status'], 'full_span')

    def test_matching_letters_do_not_accept_wrong_reference(self):
        result = self.compare('test', self.repeated, 'ص: 39')
        self.assertEqual(result['reference_status'], 'unmatched')

    def test_attested_reading_preserved_without_source_mutation(self):
        result = self.compare('v4-p089-01-ru', self.with_ya, 'الزخرف: 68')
        self.assertEqual(result['status'], 'printed_reading_with_digital_scholarly_attestation')
        self.assertEqual(result['reference_status'], 'full_span')

    def test_reading_exception_does_not_hide_other_letter_errors(self):
        result = self.compare('v4-p089-01-ru', self.with_ya + ' زائد', 'الزخرف: 68')
        self.assertEqual(result['status'], 'unmatched')
        result = self.compare('other', self.with_ya, 'الزخرف: 68')
        self.assertEqual(result['status'], 'unmatched')

    def test_printed_silent_alif_keeps_full_multi_verse_reference(self):
        for unit in ('v4-p106-03-ru', 'v4-p198-04-ru'):
            result = self.compare(unit, 'كلا إنهم عن ربهم يومئذ لمحجوبون ثم إنهم لصالوا الجحيم', 'المطففين: 15–16')
            self.assertEqual(result['status'], 'printed_orthography_with_control_image')
            self.assertEqual(result['actual_span'], [[83, 15], [83, 16]])
            self.assertEqual(result['reference_status'], 'full_span')

    def test_attested_nun_reading_of_gathering_is_preserved(self):
        quote = 'ويوم نحشرهم كأن لم يلبثوا إلا ساعة من النهار يتعارفون بينهم'
        result = self.compare('v4-p186-02-ru', quote, 'يونس: 45')
        self.assertEqual(result['status'], 'printed_reading_with_digital_scholarly_attestation')
        self.assertEqual(result['actual_span'], [[10, 45], [10, 45]])
        self.assertEqual(result['reference_status'], 'full_span')

    def test_nun_reading_does_not_hide_unrelated_errors(self):
        quote = 'ويوم نحشرهم كأن لم يلبثوا إلا ساعة من النهار يتعارفون بينهم'
        self.assertEqual(self.compare('other', quote, 'يونس: 45')['status'], 'unmatched')
        self.assertEqual(self.compare('v4-p186-02-ru', quote + ' زائد', 'يونس: 45')['status'], 'unmatched')
        self.assertEqual(self.compare('v4-p186-02-ru', quote, 'يونس: 46')['reference_status'], 'unmatched')

    fajar = ('إذا دكت الأرض دكا دكا وجاء ربك والملك صفا صفا '
             'وجيء يومئذ بجهنم يومئذ يتذكر الإنسان وأنى له الذكرى يقول يا ليتني قدمت لحياتي')

    def test_control_reference_error_stays_visible_with_actual_span(self):
        result = self.compare('v4-p195-04-ru', self.fajar, 'الفجر: 21–26', '(89:21–24)')
        self.assertEqual(result['reference_status'], 'printed_range_error_documented')
        self.assertEqual(result['printed_reference'], [89, 21, 26])
        self.assertEqual(result['actual_span'], [[89, 21], [89, 24]])
        self.assertEqual(result['corrected_russian_reference'], [89, 21, 24])

    def test_documented_reference_does_not_accept_other_wrong_ranges(self):
        for unit, reference in [('other', 'الفجر: 21–26'), ('v4-p195-04-ru', 'الفجر: 21–27')]:
            self.assertEqual(self.compare(unit, self.fajar, reference)['reference_status'], 'range_needs_review')
        self.assertEqual(self.compare('v4-p195-04-ru', self.fajar + ' زائد', 'الفجر: 21–26')['status'], 'unmatched')
        self.assertEqual(self.compare('v4-p195-04-ru', self.fajar, 'الفجر: 21–26', '(89:21–26)')['reference_status'], 'range_needs_review')


if __name__ == '__main__':
    unittest.main()
