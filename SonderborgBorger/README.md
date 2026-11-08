# Sønderborg Borger

En Android-app til borgerne i Sønderborg Kommune: nyheder, selvbetjening og kontaktoplysninger samlet ét sted.

> Uofficiel app. Den er ikke udgivet af Sønderborg Kommune. Kommunens egen hjemmeside er altid den gældende kilde.

## Funktioner
- **Nyheder**: henter nyheder fra Sønderborg Kommune, SønderborgNyt og DR Syd (RSS/Atom). Du kan søge, filtrere på kilde, gemme og dele nyheder. Træk ned for at opdatere.
- **Kun lokale nyheder**: regionale kilder som DR Syd filtreres, så du kun ser nyheder om Sønderborg, Als, Nordborg, Gråsten, Broager og de andre byer i kommunen.
- **Notifikationer**: appen tjekker kilderne cirka hver 3. time og giver besked om nye nyheder.
- **Borger**: 14 emner (Borgerservice, pas og kørekort, affald, skole, sundhed, ældre, job, bolig, trafik, kultur, klima, politik, ny i Sønderborg) med gode råd, selvbetjeningslinks og søgning på kommunens hjemmeside.
- **Kontakt**: Borgerservice (ring, skriv, kort), byrådets dagsordener og vigtige telefonnumre (112, 114, Lægevagten 70 11 07 07 m.fl.).
- **Egne kilder**: tilføj en hvilken som helst RSS-adresse eller hjemmeside. Har siden et feed-link, finder appen selv feedet.
- Lyst og mørkt tema. Alle data gemmes kun på telefonen.

## Byg APK
1. Push til `main` (eller kør workflowet **Build Sønderborg Borger** manuelt under fanen **Actions**).
2. Når kørslen er grøn, så download **Sonderborg-Borger-Android-APK** under Artifacts.
3. Installér APK-filen på din Android-telefon.

Lokalt: `cd SonderborgBorger && gradle testDebugUnitTest assembleDebug` (kræver Android SDK 36 og Java 17).

## Teknik
Kotlin, Jetpack Compose, Material 3, WorkManager. minSdk 26, targetSdk 36. Ingen tredjeparts netværksbiblioteker.

| Fil | Indhold |
|---|---|
| `Feed.kt` | RSS/Atom-parser, feed-opdagelse, lokalfilter |
| `Repository.kt` | Hentning af nyheder og lokal lagring |
| `NewsWorker.kt` | Baggrundstjek og notifikationer |
| `Content.kt` | Nyhedskilder, emner, links og telefonnumre |
| `MainActivity.kt` | Skærmene |

## Bør tjekkes før udgivelse
- Feed-adresserne er ikke testet fra byggemiljøet. Status for hver kilde vises under **Indstillinger**, og kilder kan rettes, slås fra eller tilføjes i appen.
- Borgerservices adresse og telefonnummer står i `Content.kt`. Bekræft dem på kommunens hjemmeside.
