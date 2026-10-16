# «Степени идущих» — Google Play readiness

**Current release status: Preparation only. NOT cleared for publication.**

## Text provenance and permissions

- Author: Ibn al-Qayyim (d. 1350). The historical author text is public domain by age.
- Russian: according to the developer, produced specifically for the app using AI from Arabic without consulting existing Russian translations. This statement describes the workflow, not a definitive legal assignment of copyright or scholarly certification.
- Arabic source edition: 2019, edited by Nabil ibn Nassar al-Sindi. The editor's modern additions, critical apparatus, footnotes, layout and image scans may have separately protected rights. Source inventory `research/volume2-source/source_audit.json` records **636 pages with editorial notes separated**; this is **not a complete manual legal/source audit**.
- Volume 1 and 2 translations and Arabic/Quran/hadith citations require human scholarly, linguistic and source collation before describing the edition as fully verified.
- Do not include print scan images or modern editor notes in the publicly distributed corpus unless separately cleared. Keep evidence of provenance and removal decisions. The app itself must not claim critical-edition certification.

All outstanding checks and supporting links are maintained in `docs/play-clearance.json`. Production build uses a fail-closed check until evidence is recorded. No synthetic or invented certificates.

## Technical bundle variants

```bash
# CI candidate: optimized R8 Android App Bundle for QA, with
# isolated package ID ru.madarij.nativeapp.playqa<run_number>.
# Development signed. Cannot be uploaded as the final Play application.
./gradlew :app:bundleRelease -PplayCandidate=true

# True Play upload AAB: fixed package ru.madarij.nativeapp,
# production title «Степени идущих», R8 enabled.
# Fails without documentary clearance and the owner's persistent signing key.
export MADARIJ_KEYSTORE=/path/to/private-upload-key.jks
export MADARIJ_STORE_PASSWORD='...'
export MADARIJ_KEY_ALIAS='...'
export MADARIJ_KEY_PASSWORD='...'
./gradlew :app:bundleRelease -PplayProduction=true
```

Do **not** put keystore or passwords in Git or include them in chat. Configure as GitHub Actions encrypted secrets if automation is later required. Keep secure backups of the original upload key. Google Play App Signing generally handles distribution keys separately from the developer's upload key.

Prior to opening a permanent Play Console listing:
- Decide the **final package ID** `ru.madarij.nativeapp` and confirm it is unused or assigned to you. This cannot be changed for that Play app after publication.
- Confirm versionCode is higher than any existing Play release, and that `targetSdk=36`.
- Review `AndroidManifest.xml`, data handling and any generated content; provide an accessible privacy policy where required; complete Data safety declarations accurately.
- Prepare icon, feature graphic, screenshots (captured from actual app), short and full descriptions, appropriate content rating and support contact.
- Conduct internal/closed testing and verify install/update using Play App Signing and owner key before a production roll-out.
- Publish only after documentary and scholarly review. Google Play review/approval is separate from a successful Gradle build.

**Do not treat a successfully generated AAB as a proof that Google Play will approve its content or that permissions are in order.**
