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

    def compare(self, unit, quote, reference):
        verses = {
            (6, 135): checker.normalized(self.repeated),
            (39, 39): checker.normalized(self.repeated),
            (43, 68): checker.normalized('يا عباد لا خوف عليكم اليوم'),
            (83, 15): checker.normalized('كلا إنهم عن ربهم يومئذ لمحجوبون'),
            (83, 16): checker.normalized('ثم إنهم لصالو الجحيم'),
        }
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            target = root / 'research/volume4/translation.json'
            target.parent.mkdir(parents=True)
            data = {'paragraphs': [{'id': unit, 'ar': '{' + quote + '} [' + reference + ']'}]}
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
        result = self.compare('v4-p106-03-ru', 'كلا إنهم عن ربهم يومئذ لمحجوبون ثم إنهم لصالوا الجحيم', 'المطففين: 15–16')
        self.assertEqual(result['status'], 'printed_orthography_with_control_image')
        self.assertEqual(result['actual_span'], [[83, 15], [83, 16]])
        self.assertEqual(result['reference_status'], 'full_span')


if __name__ == '__main__':
    unittest.main()
