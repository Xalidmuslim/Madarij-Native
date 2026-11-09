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
    nahl43 = 'وما أرسلنا من قبلك إلا رجالا يوحى إليهم فاسألوا أهل الذكر إن كنتم لا تعلمون بالبينات والزبر'
    anbiya25 = 'وما أرسلنا من قبلك من رسول إلا يوحى إليه أنه لا إله إلا أنا فاعبدون'
    zumar29 = 'ضرب الله مثلا رجلا فيه شركاء متشاكسون ورجلا سالما لرجل هل يستويان مثلا الحمد لله بل أكثرهم لا يعلمون'
    zumar64 = ('قل أفغير الله تأمروني أعبد أيها الجاهلون ولقد أوحي إليك وإلى الذين من قبلك '
               'لئن أشركت ليحبطن عملك ولتكونن من الخاسرين بل الله فاعبد وكن من الشاكرين')
    yasin22 = ('وما لي لا أعبد الذي فطرني وإليه ترجعون أأتخذ من دونه آلهة إن يردن الرحمن '
               'بضر لا تغن عني شفاعتهم شيئا ولا ينقذون')

    def compare(self, unit, quote, reference, ru='', all_records=False):
        verses = {
            (4, 163): checker.normalized('إنا أوحينا إليك كما أوحينا إلى نوح والنبيين من بعده'),
            (4, 164): checker.normalized('وكلم الله موسى تكليما'),
            (6, 135): checker.normalized(self.repeated),
            (10, 30): checker.normalized('هنالك تبلو كل نفس ما أسلفت وردوا إلى الله مولاهم الحق وضل عنهم ما كانوا يفترون'),
            (10, 45): checker.normalized('ويوم يحشرهم كأن لم يلبثوا إلا ساعة من النهار يتعارفون بينهم'),
            (16, 43): checker.normalized(self.nahl43.replace('يوحى', 'نوحي').removesuffix(' بالبينات والزبر')),
            (16, 44): checker.normalized('بالبينات والزبر وأنزلنا إليك الذكر لتبين للناس ما نزل إليهم ولعلهم يتفكرون'),
            (21, 25): checker.normalized(self.anbiya25.replace('يوحى', 'نوحي')),
            (34, 17): checker.normalized('ذلك جزيناهم بما كفروا وهل نجازي إلا الكفور'),
            (36, 22): checker.normalized('وما لي لا أعبد الذي فطرني وإليه ترجعون'),
            (36, 23): checker.normalized('أأتخذ من دونه آلهة إن يردن الرحمن بضر لا تغن عني شفاعتهم شيئا ولا ينقذون'),
            (39, 29): checker.normalized(self.zumar29.replace('سالما', 'سلما')),
            (39, 39): checker.normalized(self.repeated),
            (39, 64): checker.normalized('قل أفغير الله تأمروني أعبد أيها الجاهلون'),
            (39, 65): checker.normalized('ولقد أوحي إليك وإلى الذين من قبلك لئن أشركت ليحبطن عملك ولتكونن من الخاسرين'),
            (39, 66): checker.normalized('بل الله فاعبد وكن من الشاكرين'),
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
                records = checker.compare(Path('unused'))['records']
                result = records if all_records else records[0]
            self.assertEqual(target.read_text(), original)
            return result

    grouped = 'إنا أوحينا إليك كما أوحينا إلى نوح والنبيين من بعده} إلى أن قال: {وكلم الله موسى تكليما'

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

    yunus30 = 'هنالك تبلوا كل نفس ما أسلفت وردوا إلى الله مولاهم الحق وضل عنهم ما كانوا يفترون'

    def test_yunus30_printed_silent_alif_is_compared_without_mutation(self):
        result = self.compare('v4-p246-03-ru', self.yunus30, 'يونس: 30')
        self.assertEqual(result['status'], 'printed_orthography_with_control_image')
        self.assertEqual(result['actual_span'], [[10, 30], [10, 30]])
        self.assertEqual(result['reference_status'], 'full_span')

    def test_yunus30_alignment_cannot_hide_other_errors(self):
        self.assertEqual(self.compare('other', self.yunus30, 'يونس: 30')['status'], 'unmatched')
        self.assertEqual(self.compare('v4-p246-03-ru', self.yunus30 + ' زائد', 'يونس: 30')['status'], 'unmatched')
        self.assertEqual(self.compare('v4-p246-03-ru', self.yunus30.replace('كل نفس', 'كل روح'), 'يونس: 30')['status'], 'unmatched')
        self.assertEqual(self.compare('v4-p246-03-ru', self.yunus30, 'يونس: 31')['reference_status'], 'unmatched')

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

    readings = (
        ('v4-p472-03-ru', nahl43, 'النحل: 43–44', [[16, 43], [16, 44]]),
        ('v4-p499-01-ru', anbiya25, 'الأنبياء: 25', [[21, 25], [21, 25]]),
        ('v4-p508-04-ru', zumar29, 'الزمر: 29', [[39, 29], [39, 29]]),
    )

    def test_saba_passive_reading_keeps_uthmani_spelling(self):
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

    def test_new_attested_printed_readings_match_without_mutating_source(self):
        for unit, quote, reference, span in self.readings:
            with self.subTest(unit=unit):
                result = self.compare(unit, quote, reference)
                self.assertEqual(result['status'], 'printed_reading_with_digital_scholarly_attestation')
                self.assertEqual(result['actual_span'], span)
                self.assertEqual(result['reference_status'], 'full_span')
                self.assertIn('Ibn Khalawayh', result['evidence'])
                self.assertIn('480cfe92', result['evidence'])

    def test_new_readings_cannot_hide_wrong_letters_elsewhere(self):
        for unit, quote, reference, _ in self.readings:
            with self.subTest(unit=unit):
                for corrupt in (quote + ' زائد', quote[0] + 'س' + quote[1:]):
                    self.assertEqual(self.compare(unit, corrupt, reference)['status'], 'unmatched')

    def test_new_readings_require_the_exact_unit(self):
        for _, quote, reference, _ in self.readings:
            self.assertEqual(self.compare('other', quote, reference)['status'], 'unmatched')
        self.assertEqual(self.compare('v4-p499-01-ru', self.nahl43, 'النحل: 43–44')['status'], 'unmatched')

    def test_new_readings_do_not_accept_wrong_references(self):
        for unit, quote, _, _ in self.readings:
            self.assertEqual(self.compare(unit, quote, 'الزمر: 30')['reference_status'], 'unmatched')

    range_errors = (
        ('v4-p500-06-ru', zumar64, 'الزمر: 64–65', [39, 64, 65], [[39, 64], [39, 66]], '[«аз-Зумар»,39:64–66]'),
        ('v4-p502-02-ru', yasin22, 'يس: 23', [36, 23, 23], [[36, 22], [36, 23]], '[«Йа син»,36:22–23]'),
    )

    def test_new_printed_range_errors_keep_printed_and_corrected_references(self):
        for unit, quote, reference, printed, span, ru in self.range_errors:
            with self.subTest(unit=unit):
                result = self.compare(unit, quote, reference, ru)
                self.assertEqual(result['reference_status'], 'printed_range_error_documented')
                self.assertEqual(result['printed_reference'], printed)
                self.assertEqual(result['actual_span'], span)
                self.assertEqual(result['corrected_russian_reference'], [span[0][0], span[0][1], span[1][1]])

    def test_new_range_exceptions_require_correct_russian_reference(self):
        for unit, quote, reference, _, _, _ in self.range_errors:
            for ru in ('', '[39:64–65]', '[36:23]', '[39:63–66]', '[36:22–24]'):
                with self.subTest(unit=unit, ru=ru):
                    self.assertNotEqual(self.compare(unit, quote, reference, ru)['reference_status'], 'printed_range_error_documented')

    def test_new_range_exceptions_require_unit_range_and_actual_span(self):
        for unit, quote, reference, _, _, ru in self.range_errors:
            with self.subTest(unit=unit):
                self.assertNotEqual(self.compare('other', quote, reference, ru)['reference_status'], 'printed_range_error_documented')
                self.assertNotEqual(self.compare(unit, quote, reference.replace('23', '24').replace('65', '67'), ru)['reference_status'], 'printed_range_error_documented')
                self.assertNotEqual(self.compare(unit, quote.rsplit(' ', 1)[0], reference, ru)['reference_status'], 'printed_range_error_documented')
                self.assertEqual(self.compare(unit, quote + ' زائد', reference, ru)['status'], 'unmatched')


if __name__ == '__main__':
    unittest.main()
