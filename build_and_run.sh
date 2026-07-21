#!/bin/bash
set -e

################################################################################
# Build and run OBP-Hola, after checking that the services it needs are up.
#
# Defaults match the tracked "mtls" and "keycloak" profiles and use the same environment
# variables, so overriding one place overrides both:
#
#   OBP_BASE_URL          where OBP-API is         (default https://localhost:8080)
#   KEYCLOAK_PUBLIC_URL   the OIDC provider realm  (default http://localhost:7787/realms/master)
#   KEYCLOAK_CLIENT_SECRET  secret for the OIDC client (no default -- see the dev profile)
#
# Per-machine settings belong in src/main/resources/application-local.properties
# (gitignored; copy it from application-local.properties.example) rather than in the
# tracked profiles. Note environment variables OUTRANK that file.
#
# Examples:
#   ./build_and_run.sh --spring.profiles.active=keycloak,mtls,local
#   ./build_and_run.sh --spring.profiles.active=keycloak,local          (no mTLS)
#   SKIP_HEALTH_CHECKS=true ./build_and_run.sh --spring.profiles.active=keycloak,mtls,local
################################################################################

# Captured up front: inside a function $* would be the FUNCTION's arguments, so the
# "try this instead" hints below would echo the wrong command line.
SCRIPT_ARGS="$*"

OBP_BASE_URL="${OBP_BASE_URL:-${OBP_API_URL:-https://localhost:8080}}"
KEYCLOAK_PUBLIC_URL="${KEYCLOAK_PUBLIC_URL:-http://localhost:7787/realms/master}"

MAX_RETRIES="${MAX_RETRIES:-30}"
RETRY_DELAY="${RETRY_DELAY:-5}"

# Wait for a TCP port to accept connections.
#
# Deliberately NOT an HTTP probe. OBP-API in its dev mTLS mode serves HTTPS with
# client_auth=need, so the TLS handshake is rejected outright unless the caller
# presents a client certificate -- any plain `curl` health check fails against a
# perfectly healthy server. A TCP check answers the only question worth asking
# here ("is it listening?") and works for http, https and mTLS alike.
wait_for_port() {
    local name="$1" url="$2"
    local hostport host port scheme
    scheme="${url%%://*}"
    hostport="${url#*://}"; hostport="${hostport%%/*}"
    host="${hostport%%:*}"
    port="${hostport##*:}"
    if [ "$port" = "$host" ]; then                  # no explicit port in the URL
        [ "$scheme" = "https" ] && port=443 || port=80
    fi

    echo "Waiting for $name at $host:$port ..."
    local attempt=1
    while [ $attempt -le "$MAX_RETRIES" ]; do
        if (exec 3<>"/dev/tcp/$host/$port") 2>/dev/null; then
            exec 3<&- 2>/dev/null || true
            echo "$name is ready."
            return 0
        fi
        echo "  Attempt $attempt/$MAX_RETRIES - $name not reachable, retrying in ${RETRY_DELAY}s..."
        sleep "$RETRY_DELAY"
        attempt=$((attempt + 1))
    done

    echo ""
    echo "ERROR: $name is not reachable at $host:$port after $((MAX_RETRIES * RETRY_DELAY))s."
    case "$name" in
      OBP-API) echo "  Start it in the OBP-API repo:  ./flushall_fast_build_and_run.sh --mtls"
               echo "  Running it without mTLS? Use:  OBP_BASE_URL=http://localhost:8080 $0 $SCRIPT_ARGS" ;;
      OIDC)    echo "  Start your OIDC provider, or point this run at a different one:"
               echo "    KEYCLOAK_PUBLIC_URL=http://localhost:7070/realms/master $0 $SCRIPT_ARGS" ;;
    esac
    echo "  To start anyway:  SKIP_HEALTH_CHECKS=true $0 $SCRIPT_ARGS"
    exit 1
}

# The OIDC discovery document is plain HTTP and is fetched eagerly at startup,
# so it is worth checking properly rather than just testing the port.
wait_for_oidc() {
    local url="$KEYCLOAK_PUBLIC_URL/.well-known/openid-configuration"
    wait_for_port "OIDC" "$KEYCLOAK_PUBLIC_URL"
    if ! curl -sf --max-time 10 "$url" > /dev/null 2>&1; then
        echo ""
        echo "ERROR: $KEYCLOAK_PUBLIC_URL is listening but served no discovery document."
        echo "  Tried: $url"
        echo "  Check the realm name in KEYCLOAK_PUBLIC_URL."
        exit 1
    fi
    echo "OIDC discovery document OK."
}

if [ "${SKIP_HEALTH_CHECKS:-false}" != "true" ]; then
    wait_for_port "OBP-API" "$OBP_BASE_URL"
    wait_for_oidc
else
    echo ">>> SKIP_HEALTH_CHECKS=true - not checking OBP-API or the OIDC provider"
fi

# The tracked keycloak profile deliberately leaves the client secret empty, so it has to
# come from the environment or from the (gitignored) local profile. Warn early rather than
# failing later in the OAuth flow with a bare "invalid_client".
LOCAL_PROFILE="src/main/resources/application-local.properties"
if [[ "$*" == *"keycloak"* ]] \
   && [ -z "${KEYCLOAK_CLIENT_SECRET:-}" ] \
   && ! grep -qE '^[[:space:]]*oauth2\.client_secret[[:space:]]*=[[:space:]]*[^[:space:]]' "$LOCAL_PROFILE" 2>/dev/null; then
    echo ""
    echo "WARNING: no OIDC client secret found - the token exchange will fail."
    echo "  Set oauth2.client_secret in $LOCAL_PROFILE"
    echo "  (copy it from ${LOCAL_PROFILE}.example), or export KEYCLOAK_CLIENT_SECRET."
    echo "  Keycloak admin console: Clients > open-bank-project > Credentials"
    echo ""
fi

echo "Building OBP-Hola..."
mvn clean package -DskipTests

# Glob rather than a hardcoded version, so a version bump in pom.xml does not
# silently break this script.
JAR=$(ls -1 target/obp-hola-app-*.jar 2>/dev/null | grep -v -- '-sources\|-javadoc' | head -1)
if [ -z "$JAR" ]; then
    echo "ERROR: no runnable jar found in target/ after the build."
    exit 1
fi

echo "Running OBP-Hola ($JAR)..."
java -jar "$JAR" "$@"
