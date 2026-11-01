# Saldo 4.4.0 – udfyld Google Play Console trin for trin

Alle svar herunder passer til appen, som den er bygget nu. Felter markeret **[DIG]** kræver dine egne oplysninger.

---

## 0. Før du starter
- [ ] Signeret AAB: læg upload-nøglen ind i GitHub (se `SIGNERING.md` eller kør `scripts/lav-upload-noegle.sh`), kør workflowet, og hent `Saldo-4.4.0-release.aab` fra artefaktet **Saldo-4.4.0-Android**.
- [ ] Privatlivspolitikken skal være online: merge pull request #1 til `main`, så GitHub Pages udgiver `https://omarpedersen1997-png.github.io/Saldo/privacy/`. Sæt først navn og support-e-mail ind i `docs/privacy/index.html` **[DIG]**.

## 1. Opret appen
Play Console → **Opret app**
| Felt | Svar |
|---|---|
| Appnavn | `Saldo – Budget og overblik` |
| Standardsprog | Dansk – da-DK |
| App eller spil | App |
| Gratis eller betalt | Gratis |
| Erklæringer | Sæt flueben i begge (udviklerprogrampolitikker og amerikanske eksportlove) |

## 2. Butiksside (Vækst → Butiksside → Primær butiksside)
| Felt | Hvor det findes |
|---|---|
| Appnavn | `Saldo – Budget og overblik` |
| Kort beskrivelse | `STORE_LISTING_DA.md` → "Kort beskrivelse" (74 tegn) |
| Fuld beskrivelse | `STORE_LISTING_DA.md` → "Fuld beskrivelse" (kopiér hele afsnittet; **fed** skrift vises som almindelig tekst, det er fint) |
| Appikon (512×512) | `store-assets/app-icon-512.png` |
| Feature-grafik (1024×500) | `store-assets/feature-graphic-1024x500.jpg` |
| Telefonskærmbilleder | De 8 billeder i `play-skaermbilleder/` (i denne rækkefølge: 01 → 08) |
| Tablet-skærmbilleder | Kan springes over |

**Butiksindstillinger**
| Felt | Svar |
|---|---|
| Kategori | Finans |
| Tags | Budget, Personlig økonomi (vælg dem, der findes) |
| E-mail | Din support-e-mail **[DIG]** |
| Websted | Kan stå tomt, eller `https://omarpedersen1997-png.github.io/Saldo/` |

## 3. Appindhold (Politik → Appindhold)

**Privatlivspolitik:** `https://omarpedersen1997-png.github.io/Saldo/privacy/`

**Annoncer:** Nej, min app indeholder ikke annoncer.

**Appadgang:** *Alle funktioner i min app er tilgængelige uden særlig adgang.*
(Appen kan bruges med "Fortsæt uden konto". Vil du også have Google til at afprøve onlinebackup, kan du i stedet vælge "Alle eller nogle funktioner er begrænsede" og oplyse en testkonto **[DIG]**.)

**Indholdsklassificering (IARC-spørgeskema)**
- E-mail: din e-mail **[DIG]**
- Kategori: *Alle andre apptyper* (hjælpeprogram/produktivitet)
- Vold, seksuelt indhold, grimt sprog, stoffer, hasardspil: **Nej** til alt
- Kan brugere kommunikere eller dele indhold med hinanden? **Nej**
- Deler appen brugerens placering? **Nej**
- Kan brugere købe digitale varer? **Nej**
- Ubegrænset internetadgang (webbrowser)? **Nej**
- Forventet resultat: PEGI 3 / alle aldre

**Målgruppe og indhold**
- Aldersgrupper: **18 og derover** (vælg kun den)
- Appen er ikke rettet mod børn.

**Nyhedsapp:** Nej · **COVID-19-app:** Nej · **Offentlig myndighed:** Nej

**Økonomiske funktioner (Financial features):**
Vælg den mulighed, der dækker *budgettering / privatøkonomisk overblik* (fx "Personal finance management"). Saldo tilbyder **ikke** lån, betalinger, bankkonti, kryptovaluta, investering eller handel med værdipapirer – vælg ingen af de muligheder. Spørger Google, om appen forbinder til bankkonti: **Nej** (kun brugerens egen CSV-import).

**Sundhedsapp:** Nej

**Datasikkerhed:** Udfyld efter `DATA_SAFETY_4.4.0.md`. Kort fortalt:
- Indsamler eller deler appen data? **Ja** (kun ved valgfri konto/onlinebackup)
- Krypteres data under overførsel? **Ja**
- Kan brugere anmode om sletning? **Ja** – `https://omarpedersen1997-png.github.io/Saldo/account-deletion/`
- Datatyper: *Personlige oplysninger → E-mailadresse* (Kontoadministration, valgfri) og *Økonomiske oplysninger → Andre økonomiske oplysninger* (Appfunktionalitet, valgfri). Intet deles med tredjeparter.

**Sletning af konto:** URL `https://omarpedersen1997-png.github.io/Saldo/account-deletion/` – kontoen kan også slettes i appen under Mere → Indstillinger.

## 4. Test og udgivelse
1. **Intern test** (op til 100 testere, kører med det samme): Test og udgiv → Test → Intern test → Opret ny version → upload AAB → udgivelsesnoter fra `STORE_LISTING_DA.md` → "Nyheder i 4.4.0".
   - Slå **Play App Signing** til, når du bliver spurgt (standard).
2. **Lukket test:** Er din udviklerkonto en ny *personlig* konto, kræver Google typisk en lukket test med mindst 12 testere i 14 dage i træk, før du kan søge om produktion. Play Console viser kravet under "Udgivelsesoversigt", hvis det gælder dig. Testere kan være venner og familie med en Gmail-adresse.
3. **Produktion:** Når kravene er opfyldt: Produktion → Opret ny version → vælg samme AAB → Send til gennemgang. Første gennemgang tager typisk et par dage.

## 5. Tekst til testere
> Hej! Jeg har lavet Saldo, en budget-app der viser, hvad du har til rådighed frem til næste løn. Vil du hjælpe med at teste den i 14 dage? Tryk på linket, vælg "Bliv tester", og installér appen fra Google Play. Du kan bruge den uden konto. Skriv gerne til mig, hvis noget driller.

## 6. Senere opdateringer
Hæv `versionCode` (og `versionName`) i `app/build.gradle.kts` for hver ny version – Google afviser en AAB med samme versionCode som en tidligere.
