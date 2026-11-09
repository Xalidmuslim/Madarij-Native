from pathlib import Path
import json,subprocess,hashlib
R=Path('/workspace/Madarij-Native');D=R/'research/volume4';BASE='3d1e443b2dba9339255bbe875d1600301c609022'
def read(n):return json.loads((D/n).read_text())
def save(n,x):(D/n).write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n')
v=json.loads(Path('/workspace/recovery/support/recovered-validation.json').read_text());q=json.loads(Path('/workspace/recovery/support/recovered-quran.json').read_text());assert v['format_integrity_ok'] and len(v['draft_pages_complete'])==554 and not v['untranslated_captured_fragments'];assert len(q['records'])==578 and not any(r['status']=='unmatched' or r.get('reference_status') in ['unmatched','range_needs_review'] for r in q['records']);save('validation-report.json',v);save('quran-comparison.json',q)
t=read('translation.json');old=json.loads(subprocess.check_output(['git','-C',str(R),'show',BASE+':research/volume4/translation.json']));assert t['paragraphs'][:len(old['paragraphs'])]==old['paragraphs']
e=json.loads(subprocess.check_output(['git','-C',str(R),'show',BASE+':research/volume4/editorial-issues.json']));p=read('progress.json');newissues=[('V4-WORDING-355','v4-p355-04','Printed المشهود retained; historical review records unresolved wording in the argument distinguishing seeing from the Seen.'),('V4-SYNTAX-396',None,'Printed وعدم الغفلات retained; historical Russian interpretation uses a separate predication. Syntactic question remains open.'),('V4-TERM-551','v4-p551-04','شهود الأزلية والحكم: historical Russian terminology remains provisional.'),('V4-LEX-554','v4-p554-02','غير مكفي: historically chosen interpretation retained; alternative traditional analyses require editorial resolution.')]
for id,sid,note in newissues:
 u=next(u for u in t['paragraphs'] if sid in u['source_ids']) if sid else next(u for u in t['paragraphs'] if 'وعدم الغفلات' in u['ar']);e['issues'].append({'id':id,'source_ids':u['source_ids'],'status':'open_historical_editorial_question','finding':note,'recovered_from':'prior task record; no new semantic review asserted'})
 if id not in p['open_issue_ids']:p['open_issue_ids'].append(id)
save('editorial-issues.json',e);p['pages_without_open_context_questions']=v['self_reviewed_pages_without_open_context_questions'];p['review_provenance']='Historical Codex passes retained; current recovery reran mechanical integrity and canonical comparisons only.';p['github_branch']='codex/volume4-2019-translation';p['original_local_git_commit_unavailable']='38a3437';p['last_confirmed_remote_content_commit']=BASE;p['unsent_current_content']='Recovered full canonical files awaiting final GitHub push confirmation.';save('progress.json',p)
f=D/'opening.ru.md';s=f.read_text().replace('Неполный перевод с контрольного PDF.','Восстановленный полный черновой перевод авторского текста3–556 с контрольного PDF.');
for id,sid,note in newissues:
 u=next(u for u in t['paragraphs'] if sid in u['source_ids']) if sid else next(u for u in t['paragraphs'] if 'وعدم الغفلات' in u['ar'])
 if '> '+id+':' not in s:s=s.replace(u['ru'],u['ru']+'\n\n> '+id+': '+note)
f.write_text('\n'.join(l.rstrip() for l in s.splitlines()).rstrip()+'\n')
report=read('RECOVERY_PROVENANCE.json');report.update(verified_counts={'author_pages':554,'source_fragments':2487,'translation_units':2137,'quran_quotations':578},mechanical_integrity_passed=True,canonical_comparison_passed=True,checker_tests_passed=24,old_translation_units_unchanged=True,protected_earlier_volumes_unchanged=True,application_changed=False,apk_built=False,publication_readiness=False)
for b in report['blocks']:b['repository_backup_path']='research/volume4/recovery/restored-history/'+str(Path(b['file']).relative_to('/workspace/recovery'))
save('RECOVERY_PROVENANCE.json',report)
oldjournal=subprocess.check_output(['git','-C',str(R),'show',BASE+':TRANSLATION_PROGRESS_VOLUME4.md']).decode();prefix='''# Актуальное состояние восстановленного перевода

Полный исторический черновик авторского текста тома4 **3–556 (554 страницы)** восстановлен из сохранённых исходных команд. Прежние локальные Git-коммиты, включая38a3437, в возобновлённой среде недоступны; новые коммиты восстановления не выдаются за прежние Git-объекты. Новый перевод не выполнялся. Побайтовую идентичность утраченным файлам без прежних blob-хешей доказать нельзя; происхождение, извлечённые команды и ограничения каждого блока сохранены.

Канонические файлы: `research/volume4/translation.json`, `source-pages.jsonl`, читаемый `opening.ru.md`. Проверено:554 страницы,2487 исходных фрагментов,2137 переводных единиц,578 выделенных коранических цитат. Пропущенных записанных фрагментов нет. Хеши/пары/порядок/защищённые файлы прежних томов прошли проверку;24 теста checker проходят. Старые переводные единицы до начала продолжения не изменены. Исправления331/404/468/510 повторены по документированным родительским командам;305 восстановлен уже с корректным باب واحد.

Исторические статусы самопроверки сохранены как исторические: новой человеческой/богословской проверки в ходе восстановления не было. Открытые редакторские вопросы остаются; это черновик, не объявление готовности к публикации.557 и далее — указатели, не входящие в авторский перевод. Приложение не менялось и APK не собирался.

Целевая ветка GitHub: `codex/volume4-2019-translation`. Коммиты резервного сохранения используют `[skip ci]`, чтобы не запускать сборку. Итоговая удалённая фиксация подтверждается отдельно в `research/volume4/GITHUB_SAVE_RECEIPT.json`. Исходные пакеты и команды сохранены в `research/volume4/recovery/restored-history/`, манифест целого восстановления — `research/volume4/RECOVERY_PROVENANCE.json`.

## Исторический журнал удалённой базы до восстановления

Записи ниже описывают прежнее состояние до страницы302, а не действующий объём канонических файлов.

''';(R/'TRANSLATION_PROGRESS_VOLUME4.md').write_text(prefix+oldjournal)
print('Final files prepared; old prefix and protected files preserved.')
