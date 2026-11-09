from pathlib import Path
p=Path('tools/check_volume4_quran.py');s=p.read_text().replace("        for m in re.finditer(","        unit_record_start = len(records)\n        previous_quote_end = 0\n        for m in re.finditer(",1)
needle="            records.append(record)"
replacement='''                # A shared range can refer to two quotations separated by the
                # author's explicit "up to his saying", rather than just the last.
                if (pos >= 0 and len(records) > unit_record_start
                        and re.fullmatch(r'\\s*إلى أن قال:\\s*', u['ar'][previous_quote_end:m.start()])):
                    previous = records[-1]
                    if (previous['status'] != 'unmatched'
                            and 'printed_reference' not in previous
                            and previous['actual_span'][0] == [sur, first]
                            and previous['actual_span'][1][0] == sur
                            and previous['actual_span'][1][1] < sequence[begin][1]
                            and list(sequence[begin]) == [sur, last]
                            and list(sequence[end]) == [sur, last]):
                        record['reference_status'] = 'grouped_span'
                        record['group_actual_span'] = [previous['actual_span'][0], list(sequence[end])]
            records.append(record)
            previous_quote_end = m.end()'''
assert needle in s;s=s.replace(needle,replacement);p.write_text(s)
