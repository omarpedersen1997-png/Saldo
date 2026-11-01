# Release-tjekliste — Saldo 4.4.0 (versionCode 33)

## 1. Build (GitHub Actions)
- [x] `Saldo-4.4.0-Udgivelsesklar.zip` og `.github/workflows/build-saldo-4-4-0.yml` ligger i repoet.
- [x] Kildekontrol (`scripts/validate-saldo-source.py`) og enhedstests kører i workflowet.
- [ ] Merge ændringerne til `main`, så workflowet `build-saldo-4-4-0.yml` kan køres derfra.
- [ ] GitHub Secrets: `SUPABASE_URL`, `SUPABASE_ANON_KEY`.

## 2. Signering (upload-nøgle) – se `SIGNERING.md`
- [ ] Opret upload-keystore (gem den og adgangskoderne sikkert – mistes de, kan appen ikke opdateres uden nulstilling via Google).
- [ ] GitHub Secrets: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- [ ] Kør workflowet og hent `Saldo-4.4.0-release.aab` fra artefaktet.

## 3. Test på rigtig telefon (intern test i Play Console)
- [ ] Velkomstguide, login/logout, PIN, fingeraftryk og automatisk lås.
- [ ] "Del til Saldo" fra netbank/Filer, CSV-import fra mindst to banker.
- [ ] Poster/regninger: opret, rediger, slet + Fortryd, kvittering, note, faste poster over månedsskifte.
- [ ] Budget (forslag, tempo), opsparing (indbetal), gæld (plan), rapporter, kalender, prognose.
- [ ] Skjul beløb, tema (Lys/Mørk/System), widgets (4 stk.), genveje, notifikationer.
- [ ] Onlinebackup, gendannelse, kontosletning.
- [ ] Lille skærm, stor skriftstørrelse og TalkBack.

## 4. Play Console
- [ ] Privatlivspolitik: udfyld navn og support-e-mail i `docs/privacy/index.html`, deploy GitHub Pages.
- [ ] Butikstekst fra `STORE_LISTING_DA.md` (udfyld support-e-mail).
- [ ] Datasikkerhed fra `DATA_SAFETY_4.4.0.md`.
- [ ] Appadgang: testkonto (e-mail + adgangskode) til Googles gennemgang, da login er påkrævet.
- [ ] Indholdsbedømmelse, målgruppe (ikke rettet mod børn), annoncer: nej.
- [ ] Nye skærmbilleder (min. 2, gerne 4–8) taget på telefonen af 4.4.0 – de nuværende viser det gamle design.
- [ ] Upload AAB til intern test → lukket test → produktion.
