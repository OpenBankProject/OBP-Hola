#!/bin/bash
################################################################################
# OBP-Hola development certificate set
#
# Hola is a TPP: it calls OBP-API over mutual TLS, so it needs
#
#   hola-client   a client certificate OBP-API's truststore accepts, and
#   hola-truststore  the CA that signed OBP-API's server certificate.
#
# Both come from the same root, OBP-API's development CA. This script generates
# Hola's own private key and has that CA sign it -- the arrangement being
# modelled, where the TPP holds its key and the bank's CA issues the certificate.
# Nothing here is ever handed a key it did not generate.
#
# It therefore needs an OBP-API checkout. Point OBP_API_HOME at one, or let the
# usual sibling locations be searched:
#
#   ./scripts/generate_dev_certs.sh
#   OBP_API_HOME=~/src/OBP-API ./scripts/generate_dev_certs.sh
#
# Everything it writes is DEVELOPMENT-ONLY: the keys are committed to a public
# repository and the password is in this file.
################################################################################

set -euo pipefail

HOLA_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$HOLA_ROOT/src/main/resources/cert"
PASSWORD=123456
DAYS=3650

# Hola is a different organisation from the bank, and says so. A TPP sharing
# TESOBE's O would make a Consumer or regulated-entity lookup that matches on the
# subject look better tested than it is.
HOLA_DN="/C=DE/O=Hola Ltd/OU=TPP/CN=obp-hola"

# --- locate OBP-API ----------------------------------------------------------
find_obp_api() {
    local candidate
    for candidate in \
        "${OBP_API_HOME:-}" \
        "$HOLA_ROOT/../OBP-API" \
        "$HOLA_ROOT/../constantine2nd/OBP-API" \
        "$HOME/Tesobe/GitHub/OBP-API" \
        "$HOME/Tesobe/GitHub/constantine2nd/OBP-API"; do
        [[ -n "$candidate" ]] || continue
        if [[ -f "$candidate/obp-api/src/test/resources/cert/dev-ca.key" ]]; then
            (cd "$candidate" && pwd)
            return 0
        fi
    done
    return 1
}

if ! OBP_API="$(find_obp_api)"; then
    cat >&2 <<MSG
Cannot find an OBP-API checkout containing the development CA.

Looked for obp-api/src/test/resources/cert/dev-ca.key under:
    \$OBP_API_HOME (${OBP_API_HOME:-unset})
    $HOLA_ROOT/../OBP-API
    $HOLA_ROOT/../constantine2nd/OBP-API
    \$HOME/Tesobe/GitHub/OBP-API
    \$HOME/Tesobe/GitHub/constantine2nd/OBP-API

Hola's client certificate has to be signed by the CA that OBP-API's truststore
trusts, so a checkout is required. Set OBP_API_HOME to one, and make sure its
certificate set has been generated:

    ./scripts/generate_dev_certs.sh        (in the OBP-API repo)
MSG
    exit 1
fi

CA_DIR="$OBP_API/obp-api/src/test/resources/cert"
echo ">>> Signing against the OBP-API development CA at $CA_DIR"
openssl x509 -in "$CA_DIR/dev-ca.crt" -noout -subject -nameopt RFC2253 | sed 's/^/      /'

mkdir -p "$OUT"
cd "$OUT"

# --- Hola's client identity --------------------------------------------------
# Generated here, signed there: the private key never leaves this repository.
openssl req -newkey rsa:2048 -nodes -keyout hola-client.key -out hola-client.csr \
    -subj "$HOLA_DN" 2>/dev/null

cat > hola-client.ext <<'EXT'
basicConstraints=CA:FALSE
keyUsage=critical,digitalSignature
extendedKeyUsage=clientAuth
EXT

openssl x509 -req -in hola-client.csr \
    -CA "$CA_DIR/dev-ca.crt" -CAkey "$CA_DIR/dev-ca.key" -CAcreateserial \
    -days "$DAYS" -extfile hola-client.ext -out hola-client.crt 2>/dev/null
rm -f hola-client.csr hola-client.ext "$CA_DIR/dev-ca.srl"

openssl pkcs12 -export -inkey hola-client.key -in hola-client.crt \
    -certfile "$CA_DIR/dev-ca.crt" -name hola-client \
    -out hola-client.p12 -passout "pass:$PASSWORD"

# --- what Hola trusts --------------------------------------------------------
# The CA only. OBP-API's server certificate is issued by it, so pinning the leaf
# would simply break the next time OBP-API regenerates its set.
rm -f hola-truststore.p12
keytool -importcert -noprompt -alias obp-dev-ca -file "$CA_DIR/dev-ca.crt" \
    -keystore hola-truststore.p12 -storetype PKCS12 -storepass "$PASSWORD" 2>/dev/null

echo
echo ">>> Done. Password for every store: $PASSWORD"
echo "      $OUT/hola-client.p12   $(openssl x509 -in hola-client.crt -noout -subject -nameopt RFC2253 | sed 's/subject=//')"
echo "      $OUT/hola-truststore.p12   trusts $(openssl x509 -in "$CA_DIR/dev-ca.crt" -noout -subject -nameopt RFC2253 | sed 's/subject=//')"
echo
echo "    The 'mtls' profile already points at both. Start OBP-API with --mtls and run:"
echo "      ./build_and_run.sh --spring.profiles.active=keycloak,mtls,local"
