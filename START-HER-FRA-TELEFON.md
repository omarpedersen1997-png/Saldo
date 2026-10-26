# Start her fra telefonen – PengeOverblik 3.0.0

## Hvis du bygger via ZIP i GitHub
1. Upload `PengeOverblik-3.0.0-superapp.zip` til repositoryets øverste mappe, samme niveau som `.github`.
2. Sæt indholdet af `.github/workflows/android-build.yml` fra ZIP-projektet ind i repositoryets `.github/workflows/android-build.yml`.
3. Tryk **Commit changes**.
4. Åbn **Actions**, vælg **Build PengeOverblik 3.0.0**, og start `Run workflow` hvis den ikke starter automatisk.
5. Når kørslen er grøn, åbn den og download `PengeOverblik-3.0.0-APK` under Artifacts.

## Nye funktioner i 3.0.0
- Statistikside med samlet indtægt, udgift, resultat og kategori-fordeling.
- CSV-eksport af budgetposter.
- JSON-backup af budgetposter og regninger.
- Fem faner: Overblik, Poster, Regninger, Statistik og Mål.

Appen gemmer data lokalt. Eksport starter kun, når brugeren selv trykker på eksportknappen.
