# Ryddig Start

Android-startskærm (launcher), der sorterer dine apps i kategorier, og som kan rydde unødvendige filer op.

## Funktioner

**Startskærm**
- Apps sorteres automatisk i kategorier: Kommunikation, Sociale medier, Internet, Foto & video, Musik & lyd, Spil, Økonomi, Shopping & mad, Kort & rejser, Nyheder & læsning, Produktivitet, Sundhed & træning, Værktøjer, System og Andet.
- Hold fingeren på en app for at flytte den til en anden kategori, skjule den, åbne App-info eller afinstallere den.
- Tryk på en kategori for at folde den sammen eller ud.
- Søgefelt, ur og dato øverst. Telefonens baggrundsbillede vises bagved.
- Listen opdateres af sig selv, når du installerer eller fjerner apps.

**Ryd op**
- Viser hvor meget lagerplads der er brugt og fri.
- Scanner telefonen for: installationsfiler (APK), midlertidige filer og logs, miniaturebilleder, tomme mapper, papirkurv, gamle downloads (over 90 dage) og store filer (over 100 MB).
- De sikre kategorier er valgt på forhånd. Gamle downloads, papirkurv og store filer skal du selv vælge.
- Intet slettes, før du har set listen og bekræftet.
- Mappen `Android/` (apps' egne data) bliver aldrig rørt.
- Andre apps' cache kan Android ikke lade en app slette. Til det er der en knap, der åbner telefonens egen oprydning.

## Installation

1. Åbn den nyeste udgivelse "Ryddig Start (build N)" under **Releases** på GitHub, og hent `RyddigStart.apk` på telefonen.
2. Åbn filen, og tillad installation fra ukendte kilder, hvis telefonen spørger.
3. Åbn Ryddig Start, og tryk på **Vælg som startskærm**.
4. Gå til fanen **Ryd op**, og tryk på **Giv adgang**. Slå "Tillad adgang til at administrere alle filer" til.

Kræver Android 8.0 eller nyere.

## Byg selv

```
gradle -p ryddig-launcher assembleRelease
```

APK'en signeres med den medfølgende nøgle `app/ryddig-debug.keystore`, så nye versioner kan installeres oven på de gamle.
