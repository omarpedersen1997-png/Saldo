# Signering af Saldo til Google Play

Google Play kræver en signeret AAB. Workflowet bygger den automatisk, når fire GitHub Secrets er sat.

## 1. Opret en upload-nøgle (én gang)
Kræver Java (fx Android Studio). Kør i en terminal:

```
keytool -genkeypair -v -keystore saldo-upload.jks -keyalg RSA -keysize 4096 -validity 10000 -alias saldo-upload
```

Vælg en stærk adgangskode og gem filen `saldo-upload.jks` + adgangskoden et sikkert sted (fx en password-manager). **Upload aldrig filen til repoet.**

## 2. Lav filen om til tekst
- macOS/Linux: `base64 -i saldo-upload.jks | tr -d '\n' > saldo-upload.b64`
- Windows (PowerShell): `[Convert]::ToBase64String([IO.File]::ReadAllBytes("saldo-upload.jks")) > saldo-upload.b64`

## 3. Tilføj GitHub Secrets
Repo → Settings → Secrets and variables → Actions → New repository secret:
- `ANDROID_KEYSTORE_BASE64` = indholdet af `saldo-upload.b64`
- `ANDROID_KEYSTORE_PASSWORD` = keystore-adgangskoden
- `ANDROID_KEY_ALIAS` = `saldo-upload`
- `ANDROID_KEY_PASSWORD` = nøgle-adgangskoden (ofte den samme)

## 4. Byg
Kør workflowet. Artefaktet indeholder nu også `Saldo-4.4.0-release.aab`, som uploades i Play Console. Slå "Play App Signing" til (standard), så Google opbevarer appens endelige signeringsnøgle.
