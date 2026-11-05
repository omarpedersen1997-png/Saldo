# Ryddig Start

App, der sætter orden på din nuværende startskærm med kategori-bokse, og som kan rydde unødvendige filer op.

## Funktioner

**Kategorier på din nuværende startskærm**
- Appen erstatter ikke din startskærm. Den lægger kategori-bokse (widgets) på den startskærm, du allerede bruger (Samsung, Pixel osv.).
- Apps sorteres automatisk i kategorier: Kommunikation, Sociale medier, Internet, Foto & video, Musik & lyd, Spil, Økonomi, Shopping & mad, Kort & rejser, Nyheder & læsning, Produktivitet, Sundhed & træning, Værktøjer, System og Andet.
- Tryk "＋ Startskærm" ved en kategori i appen, så lægges en boks med kategoriens apps på startskærmen. Tryk på en app i boksen for at åbne den.
- Hold fingeren på en app i Ryddig Start for at flytte den til en anden kategori. Boksen på startskærmen opdateres automatisk.
- Boksene kan også tilføjes via startskærmens egen widget-liste. Så spørger appen, hvilken kategori boksen skal vise.
- Android tillader ikke, at en app flytter ikoner eller laver mapper på en anden apps startskærm. Derfor bruges widgets.

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
3. Åbn Ryddig Start, og tryk på **＋ Startskærm** ved de kategorier, du vil have. Bekræft hver gang telefonen spørger.
   Fjern derefter de gamle løse ikoner fra startskærmen (hold fingeren på dem → Fjern). Selve apps bliver ikke slettet.
4. Gå til fanen **Ryd op**, og tryk på **Giv adgang**. Slå "Tillad adgang til at administrere alle filer" til.

Kræver Android 8.0 eller nyere.

## Byg selv

```
gradle -p ryddig-launcher assembleRelease
```

APK'en signeres med den medfølgende nøgle `app/ryddig-debug.keystore`, så nye versioner kan installeres oven på de gamle.
