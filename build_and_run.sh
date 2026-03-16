#!/bin/bash
set -e

# Default URLs (can be overridden via environment variables)
OBP_API_URL="${OBP_API_URL:-http://localhost:8080}"
OBP_OIDC_URL="${OBP_OIDC_URL:-http://localhost:9000}"

MAX_RETRIES=30
RETRY_DELAY=5

wait_for_service() {
    local name="$1"
    local url="$2"
    local attempt=1

    echo "Waiting for $name at $url ..."
    while [ $attempt -le $MAX_RETRIES ]; do
        if curl -sf --max-time 5 "$url" > /dev/null 2>&1; then
            echo "$name is ready."
            return 0
        fi
        echo "  Attempt $attempt/$MAX_RETRIES - $name not available, retrying in ${RETRY_DELAY}s..."
        sleep $RETRY_DELAY
        attempt=$((attempt + 1))
    done

    echo "ERROR: $name at $url did not become available after $((MAX_RETRIES * RETRY_DELAY))s."
    exit 1
}

# Check dependencies before building
wait_for_service "OBP-API" "${OBP_API_URL}/obp/v5.1.0/root"
wait_for_service "OBP-OIDC" "${OBP_OIDC_URL}/obp-oidc/.well-known/openid-configuration"

echo "Building OBP-Hola..."
mvn clean package -DskipTests

echo "Running OBP-Hola..."
java -jar target/obp-hola-app-0.0.29-SNAPSHOT.jar "$@"
