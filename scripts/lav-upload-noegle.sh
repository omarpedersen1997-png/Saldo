#!/usr/bin/env bash
# Opretter Saldos upload-nøgle til Google Play og laver teksten til GitHub Secrets.
# Kræver Java (keytool), fx fra Android Studio. Kør:  bash scripts/lav-upload-noegle.sh
set -euo pipefail

KEYSTORE="saldo-upload.jks"
ALIAS="saldo-upload"

if [ -e "$KEYSTORE" ]; then
  echo "$KEYSTORE findes allerede – den overskrives ikke. Flyt eller slet den først." >&2
  exit 1
fi
command -v keytool >/dev/null || { echo "keytool mangler. Installér Java eller Android Studio." >&2; exit 1; }

read -r -s -p "Vælg en lang adgangskode (mindst 12 tegn): " PASS; echo
read -r -s -p "Gentag adgangskoden: " PASS2; echo
[ "$PASS" = "$PASS2" ] || { echo "Adgangskoderne er ikke ens." >&2; exit 1; }
[ "${#PASS}" -ge 12 ] || { echo "Adgangskoden er for kort." >&2; exit 1; }

keytool -genkeypair -keystore "$KEYSTORE" -storetype PKCS12 -keyalg RSA -keysize 4096 -validity 10000 \
  -alias "$ALIAS" -storepass "$PASS" -keypass "$PASS" -dname "CN=Saldo, C=DK" >/dev/null

base64 < "$KEYSTORE" | tr -d '\n' > saldo-upload.b64

cat <<TXT

Færdig. Tilføj fire secrets i GitHub (Settings → Secrets and variables → Actions → New repository secret):
  ANDROID_KEYSTORE_BASE64   = hele indholdet af saldo-upload.b64
  ANDROID_KEYSTORE_PASSWORD = adgangskoden du lige valgte
  ANDROID_KEY_ALIAS         = $ALIAS
  ANDROID_KEY_PASSWORD      = samme adgangskode

VIGTIGT: Gem $KEYSTORE og adgangskoden et sikkert sted (fx en password-manager).
Upload aldrig filerne til GitHub-repoet. Slet saldo-upload.b64, når den er sat ind.
TXT
