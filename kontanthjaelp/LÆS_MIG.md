# Kontanthjælp – Idle-sagen (Android)

Dette er et færdigt Android-projekt. I Saldo-repoet ligger det i mappen `kontanthjaelp/`, og GitHub bygger det med flowet **Byg Kontanthjælp APK** (`.github/workflows/build-kontanthjaelp.yml`), hver gang noget i mappen ændres. Spillet ligger i `app/src/main/assets/index.html` og kører i en indbygget browser (WebView). Det gemmer automatisk på telefonen og virker uden internet. Uden internet bruges telefonens egen skrifttype.

## Vej A: Få APK-filen via GitHub (anbefalet, intet skal installeres)

1. Opret en gratis konto på github.com, hvis du ikke har en.
2. Tryk på **+** øverst til højre og vælg **New repository**. Kald det fx `kontanthjaelp`. Vælg **Private** og tryk **Create repository**.
3. Tryk på linket **uploading an existing file**.
4. Pak zip-filen ud på din computer. Træk **hele indholdet** af mappen `android` ind i browservinduet. Mappen `.github` skal med. Den er skjult på nogle computere, så slå "vis skjulte filer" til.
5. Tryk **Commit changes**.
6. Gå til fanen **Actions**. Byggeriet "Byg APK" starter af sig selv og tager ca. 3–5 minutter.
7. Når der er et grønt flueben, så tryk på kørslen og hent **Kontanthjaelp-APK** nederst under *Artifacts*. Du får en zip-fil med `app-debug.apk` indeni.

## Vej B: Android Studio

1. Installer Android Studio (gratis) fra developer.android.com/studio.
2. Vælg **Open** og åbn mappen `android`.
3. Vent, mens Android Studio henter det, den skal bruge. Det sker første gang.
4. Vælg **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
5. APK-filen ligger i `app/build/outputs/apk/debug/app-debug.apk`.

## Installer på telefonen

1. Send `app-debug.apk` til telefonen, fx via mail, Google Drive eller USB.
2. Åbn filen på telefonen. Android spørger, om du vil tillade installation fra den app, du åbner filen med. Sig ja.
3. Tryk **Installer**.

## Opdater spillet senere

Erstat `app/src/main/assets/index.html` med en ny version, og hæv `versionCode` med 1 i `app/build.gradle`. Upload til GitHub igen (Vej A), eller byg igen (Vej B). Den gemte fremgang bliver bevaret ved opdatering.

## Google Play

Den APK, der bygges her, er en *debug*-version. Den er god til at teste og dele direkte, men Google Play kræver en *signeret release* (en .aab-fil) og en udviklerkonto. Det er næste skridt, hvis spillet skal i Play Butik.

## Teknisk

- Pakkenavn: `dk.idlesagen.kontanthjaelp`
- Minimum Android 8.0 (API 26), mål API 35
- Tilladelser: Internet (kun til skrifttype) og vibration
- Tilbage-knappen lukker først indbakke, pop-ups og faner. På forsiden lægger den appen i baggrunden.
