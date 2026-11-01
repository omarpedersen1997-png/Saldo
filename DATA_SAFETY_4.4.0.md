# Google Play – Datasikkerhed (udkast til Saldo 4.4.0)

Svarene bygger på koden i 4.4.0. Kontrollér dem mod jeres endelige Supabase-opsætning, før de indsendes.

## Indsamler eller deler appen brugerdata?
- **Indsamles:** Ja – kun når brugeren opretter konto og/eller vælger onlinebackup (Supabase).
- **Deles med tredjeparter:** Nej. Supabase er databehandler for tjenesten (tæller ikke som "deling").
- **Kryptering under overførsel:** Ja (HTTPS).
- **Brugeren kan anmode om sletning:** Ja – i appen og via https://omarpedersen1997-png.github.io/Saldo/account-deletion/

## Datatyper
| Datatype (Play-kategori) | Indsamles | Formål | Valgfri |
|---|---|---|---|
| Personlige oplysninger → E-mailadresse | Ja | Kontoadministration | Nej (login kræves) |
| Økonomiske oplysninger → Andre økonomiske oplysninger (poster, budgetter, regninger, gæld, opsparing, noter) | Ja, ved onlinebackup | Appfunktionalitet (backup/gendannelse) | Ja |
| Fotos (kvitteringer) | Nej – kun lokalt på enheden | – | – |
| Filer og dokumenter (CSV-import) | Nej – læses lokalt | – | – |
| App-aktivitet, placering, kontakter, enheds-id | Nej | – | – |

## Behandles kun på enheden (indsamles ikke)
- Tekstgenkendelse af regningsbilleder (Google ML Kit på enheden).
- Kvitteringsbilleder, CSV-filer, PIN (saltet hash) og biometri (håndteres af Android).

## Tilladelser i manifestet
INTERNET, POST_NOTIFICATIONS, USE_BIOMETRIC. Ingen placering, kamera, kontakter eller lager-tilladelser (filer vælges via Androids filvælger).
