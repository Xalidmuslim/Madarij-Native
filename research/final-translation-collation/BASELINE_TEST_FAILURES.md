# Исходные неудачные проверки тома II

Команда: `python3 -m unittest discover -s tools -p "test_volume2*.py"`. На объединённой базе bac689a до правок тома I: 203 теста, семь failure-записей. Три относятся к подслучаям одного метода. Ошибки не вызваны правками первого тома.

- `FAIL: test_516_angel_and_examples_in_authorial_sequence (test_volume2_highrisk_printed_pages.EarlierPages.test_516_angel_and_examples_in_authorial_sequence)` — ожидаемая строка 'народом Самуд'.
- `FAIL: test_page_601_and_602_authorial_phrases (test_volume2_p601_602_fidelity.Volume2Pages601602.test_page_601_and_602_authorial_phrases)` — ожидаемая строка 'взаимному обмену и взаимопомощи'.
- `FAIL: test_source_point_and_safety_flags (test_volume2_pages081_100_deep.VolumeTwoPrinted81To100.test_source_point_and_safety_flags)` — ожидаемая строка 'посредством'.
- `FAIL: test_no_truncated_logic_and_author_interpositions (test_volume2_pages301_350.Vol2Pages301350.test_no_truncated_logic_and_author_interpositions) (page=313, term='Вам переданное знание')` — ожидаемая строка 'Вам переданное знание'.
- `FAIL: test_no_truncated_logic_and_author_interpositions (test_volume2_pages301_350.Vol2Pages301350.test_no_truncated_logic_and_author_interpositions) (page=329, term='Следует сказать так:')` — ожидаемая строка 'Следует сказать так:'.
- `FAIL: test_no_truncated_logic_and_author_interpositions (test_volume2_pages301_350.Vol2Pages301350.test_no_truncated_logic_and_author_interpositions) (page=330, term='любое проявление Его довольства')` — ожидаемая строка 'любое проявление Его довольства'.
- `FAIL: test_full_reasoning_restore (test_volume2_pages551_600.VolumeTwo551600.test_full_reasoning_restore)` — ожидаемая строка 'собственную честь и достоинство'.

Предварительная диагностика: проверки требуют буквальных словосочетаний; некоторые различаются только пунктуацией («Вам — переданное знание») или формулировкой («обмену и взаимной помощи»). Это ещё не доказывает корректности всего проверяемого фрагмента. Полноценный разбор и второй том пока впереди. Существующие тесты не изменены и не отключены.

Повтор после блоков тома I 3–22 и 23–42: те же 203 теста и те же семь failure-записей, новых неудач не обнаружено. Отдельная структурная проверка второго тома проходит.
