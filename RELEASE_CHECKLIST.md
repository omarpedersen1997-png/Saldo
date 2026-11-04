# Release-tjekliste — Saldo 4.4.0 (versionCode 33)

## 1. Build (GitHub Actions)
- [x] `Saldo-4.4.0-Udgivelsesklar.zip` og `.github/workflows/build-saldo-4-4-0.yml` ligger i repoet.
- [x] Kildekontrol (`scripts/validate-saldo-source.py`) og enhedstests (CSV, faste poster, gæld, budgetforslag, radar, prognose, lommeregner, lønrytme) kører i workflowet.
- [x] Release-build med R8 bygges i workflowet.
- [ ] Merge ændringerne til `main`, så workflowet `build-saldo-4-4-0.yml` kan køres derfra.
- [ ] GitHub Secrets: `SUPABASE_URL`, `SUPABASE_ANON_KEY`.

## 2. Signering (upload-nøgle) – se `SIGNERING.md`
- [ ] Opret upload-keystore (gem den og adgangskoderne sikkert – mistes de, kan appen ikke opdateres uden nulstilling via Google).
- [ ] GitHub Secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- [ ] Kør workflowet og hent `Saldo-4.4.0-release.aab` fra artefaktet.

## 3. Test på rigtig telefon (intern test i Play Console)
- [ ] Velkomstguide (inkl. valg af lønrytme), login/logout, PIN, fingeraftryk og automatisk lås.
- [ ] "Fortsæt uden konto" på en ny installation, og senere "Log ind eller opret konto" under Indstillinger (de lokale data skal følge med til en ny konto).
- [ ] Spørg Saldo: egne tal (rådighed, løn, regninger, forbrug pr. måned) og vidensemner (fx ÅOP, aktiesparekonto).
- [ ] Lønrytme: månedlig (også løn d. 31 i korte måneder), 14 dage og ugentlig – kontrollér "Næste løn", pr. dag og prognose.
- [ ] "Del til Saldo" fra netbank/Filer, CSV-import fra mindst to banker.
- [ ] Poster/regninger: opret, rediger, slet + Fortryd, kvittering, note, faste poster over månedsskifte.
- [ ] Budget (forslag, tempo), opsparing (indbetal), gæld (plan), rapporter, kalender, prognose.
- [ ] Skjul beløb, tema (Lys/Mørk/System), widgets (4 stk.), genveje, notifikationer.
- [ ] Onlinebackup, gendannelse, kontosletning.
- [ ] Lille skærm, stor skriftstørrelse og TalkBack.

## 4. Play Console – se `PLAY_CONSOLE_GUIDE.md` for alle svar trin for trin
- [ ] Privatlivspolitik: udfyld navn og support-e-mail i `docs/privacy/index.html`, deploy GitHub Pages.
- [ ] Butikstekst fra `STORE_LISTING_DA.md` (udfyld support-e-mail).
- [ ] Datasikkerhed fra `DATA_SAFETY_4.4.0.md`.
- [ ] Appadgang: appen kan bruges uden login ("Fortsæt uden konto"). Angiv det i Play Console; giv evt. en testkonto, så Google også kan afprøve onlinebackup.
- [ ] Indholdsbedømmelse, målgruppe (ikke rettet mod børn), annoncer: nej.
- [x] Nye skærmbilleder: 8 stk. i store-assets/screenshots (og play-skaermbilleder/ i repoets rod), 1080×1920, nyt design med eksempeldata.
- [ ] Upload AAB til intern test → lukket test → produktion.
