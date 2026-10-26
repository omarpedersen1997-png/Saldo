# PengeOverblik — release-checkliste

## Før test
- [ ] Bekræft at appnavnet kan bruges, og vælg endeligt application ID.
- [ ] Byg og kør appen i Android Studio med JDK 17 og Android SDK 36.
- [ ] Kontrollér at Gradle Sync og release-build gennemføres uden fejl.

## Funktionstest
- [ ] Første start viser tomt budget uden personlige eksempeldata.
- [ ] Tilføj indtægt; totalsum og råderum ændres korrekt.
- [ ] Tilføj udgift; totalsum og råderum ændres korrekt.
- [ ] Beløb med decimaler og dansk tastatur fungerer.
- [ ] Kategorier og fast-post-markering gemmes korrekt.
- [ ] Slet post; totalsummer opdateres.
- [ ] Luk og genstart appen; poster bevares.
- [ ] Test tomme felter, nul, negative værdier, meget store beløb og lange navne.
- [ ] Test lille skærm, stor skrift, mørk/lys systemindstilling og skærmlæser.
- [ ] Test på mindst én fysisk Android-enhed og en emulator.

## Google Play
- [ ] Byg signeret Android App Bundle (.aab) med egen upload-/signeringsnøgle.
- [ ] Gem signeringsnøglen sikkert; del den ikke.
- [ ] Kontrollér target API 36 eller nyere i forhold til gældende Play-krav.
- [ ] Udfyld butikstekst og upload et rigtigt appikon og faktiske skærmbilleder.
- [ ] Host privatlivspolitikken på en offentlig, stabil HTTPS-adresse.
- [ ] Udfyld Data safety ud fra den endelige build; kontroller alle tredjepartsbiblioteker.
- [ ] Udfyld målgruppe, indholdsbedømmelse, appadgang og øvrige Play Console-erklæringer.
- [ ] Test via intern/lukket test og gennemgå Play Console-feedback.
- [ ] Udgiv først efter bestået test og gennemgang.

## Vigtigt
Google Play-godkendelse kan ikke garanteres. En AAB, der kan bygges, er ikke det samme som en godkendt eller offentliggjort app.
