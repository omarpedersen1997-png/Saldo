# Saldo – Android-startpakke

## Byg APK fra GitHub (telefon eller computer)
1. Upload hele indholdet af denne ZIP til roden af dit GitHub-repository. Upload/udpak filerne, så `.github`, `app`, `build.gradle.kts` og `settings.gradle.kts` ligger direkte i roden. Upload ikke ZIP-filen som en enkelt fil.
2. Commit ændringerne til branch `main`.
3. Åbn fanen **Actions**.
4. Vælg **Build Saldo from source** og tryk **Run workflow**, hvis den ikke startede automatisk.
5. Når kørslen er grøn, åbn den og download **Saldo-Android-debug-APK** under Artifacts.
6. Installér APK-filen på Android. Android kan bede dig tillade installation fra den app, du downloader med.

## Vigtigt
Denne startpakke indeholder Android-kildekode og GitHub Actions-buildworkflow. Workflowet er ikke tidligere testet i dette miljø, så en grøn build skal bekræftes i GitHub Actions. Kildekoden er et udgangspunkt baseret på den tidligere app; den komplette visuelle Saldo-redesignskitse er ikke fuldt implementeret i Kotlin-koden endnu. OCR-import af Betalingsservice skal også testes og eventuelt færdigimplementeres før udgivelse.

## Teknologi
- Kotlin og Jetpack Compose
- Android SDK med Java 17
- Gradle 8.14.3 i GitHub Actions
- Lokalt gemte budgetdata
- Material 3
