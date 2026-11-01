# AI-svar i "Spørg Saldo" – opsætning

Appen virker uden dette trin: almindelige spørgsmål besvares på telefonen. AI-svar kræver tre ting i dit Supabase-projekt.

## 1. API-nøgle fra Anthropic
1. Opret en konto på https://console.anthropic.com og tilføj betaling.
2. Opret en API-nøgle (Settings → API Keys). Del den aldrig og læg den aldrig i appen eller på GitHub.
3. Sæt gerne en månedlig udgiftsgrænse i konsollen (Settings → Limits).

## 2. Database-funktion til daglig grænse
Kør den nederste del af `SUPABASE-SCHEMA.sql` (afsnittet "Spørg Saldo (AI-svar)") i Supabase Dashboard → SQL Editor.

## 3. Installer edge-funktionen
I en terminal i projektmappen (med Supabase CLI):

```bash
supabase login
supabase link --project-ref <dit-projekt-id>
supabase secrets set ANTHROPIC_API_KEY=<din-nøgle>
supabase functions deploy saldo-assistant
```

Valgfrie hemmeligheder:
- `SALDO_ASSISTANT_DAILY_LIMIT` – spørgsmål pr. bruger pr. dag (standard 30).
- `SALDO_ASSISTANT_MODEL` – AI-model (standard `claude-opus-5-5`). `claude-haiku-5-5` er markant billigere, men svarer mindre grundigt.

## Pris (vejledende)
Et spørgsmål sender typisk ca. 2.000-4.000 tokens og får 200-600 tokens tilbage. Med standardmodellen (4 $/20 $ pr. million tokens ind/ud) koster det ca. 0,02-0,03 $ pr. spørgsmål; med `claude-haiku-5-5` under en tiendedel cent. Følg forbruget i Anthropic-konsollen.

## Før udgivelse
- Privatlivspolitikken (`docs/privacy/index.html`) og `DATA_SAFETY_4.4.0.md` beskriver nu AI-svar. Opdater Data safety i Play Console tilsvarende.
- Test: log ind i appen, åbn Spørg Saldo, tryk "AI: fra" → "Slå AI-svar til", og spørg fx "Hvad er ÅOP?".
