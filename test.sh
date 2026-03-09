#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$SCRIPT_DIR"
TMP_DIR="$(mktemp -d)"
COMPOSE_FILE="$TMP_DIR/docker-compose.smoke.yaml"
API_PROJECT_DIR="$TMP_DIR/petstore"
MOUNTED_APIS_DIR="$TMP_DIR/mounted-apis"
ARTIFACTS_DIR="${ARTIFACTS_DIR:-$REPO_ROOT/test-artifacts/${COMPOSE_PROJECT_NAME:-ccsmoke$$}}"
PROJECT_VERSION="$(grep -m1 '<version>' "$REPO_ROOT/pom.xml" | sed -E 's/.*<version>([^<]+)<\/version>.*/\1/')"
COMPOSE_PROJECT_NAME="${COMPOSE_PROJECT_NAME:-ccsmoke$$}"
MVN_BIN="${MVN_BIN:-mvn}"
SKIP_BUILD="${SKIP_BUILD:-0}"
KEEP_RUNNING="${KEEP_RUNNING:-0}"
FAILURE=1

if docker compose version >/dev/null 2>&1; then
    DOCKER_COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
    DOCKER_COMPOSE=(docker-compose)
else
    echo "error: docker compose is required" >&2
    exit 1
fi

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "error: required command not found: $1" >&2
        exit 1
    fi
}

run_compose() {
    "${DOCKER_COMPOSE[@]}" -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" "$@"
}

wait_for_http_code() {
    local url="$1"
    local expected="$2"
    local max_attempts="${3:-60}"
    local extra_args=("${@:4}")
    local attempt code

    for ((attempt = 1; attempt <= max_attempts; attempt++)); do
        code="$(curl -ksS -o /dev/null -w '%{http_code}' "${extra_args[@]}" "$url" || true)"
        if [[ "$code" == "$expected" ]]; then
            return 0
        fi
        sleep 2
    done

    echo "error: timed out waiting for $url to return HTTP $expected" >&2
    return 1
}

wait_for_nonzero_http_code() {
    local url="$1"
    local max_attempts="${2:-60}"
    local attempt code

    for ((attempt = 1; attempt <= max_attempts; attempt++)); do
        code="$(curl -ksS -o /dev/null -w '%{http_code}' "$url" || true)"
        if [[ "$code" != "000" ]]; then
            return 0
        fi
        sleep 2
    done

    echo "error: timed out waiting for $url to accept HTTP connections" >&2
    return 1
}

cleanup() {
    set +e

    if [[ "$FAILURE" -ne 0 ]]; then
        mkdir -p "$ARTIFACTS_DIR"
        echo
        echo "Smoke test failed. Recent container status:"
        run_compose ps | tee "$ARTIFACTS_DIR/compose-ps.log" || true
        echo
        echo "Recent logs:"
        run_compose logs --tail=100 | tee "$ARTIFACTS_DIR/compose-logs.log" || true
        if [[ -f "$COMPOSE_FILE" ]]; then
            cp "$COMPOSE_FILE" "$ARTIFACTS_DIR/docker-compose.smoke.yaml"
        fi
    fi

    if [[ "$KEEP_RUNNING" != "1" ]]; then
        run_compose down -v --remove-orphans >/dev/null 2>&1 || true
        rm -rf "$TMP_DIR"
    fi
}

trap cleanup EXIT

require_command docker
require_command curl
require_command grep
require_command sed
if [[ "$SKIP_BUILD" != "1" ]]; then
    require_command "$MVN_BIN"
fi

docker info >/dev/null

mkdir -p "$MOUNTED_APIS_DIR"
cp -R "$REPO_ROOT/integration/test-integration/src/test/resources/git-artifacts/directory/petstore" "$API_PROJECT_DIR"
cp -R "$API_PROJECT_DIR" "$MOUNTED_APIS_DIR/"

cat > "$COMPOSE_FILE" <<EOF
services:
  router:
    image: wso2/choreo-connect-router:${PROJECT_VERSION}
    environment:
      ROUTER_ADMIN_HOST: 0.0.0.0
      ROUTER_ADMIN_PORT: 9000
      ROUTER_CLUSTER: default_cluster
      ROUTER_LABEL: Default
      ROUTER_PRIVATE_KEY_PATH: /home/wso2/security/keystore/mg.key
      ROUTER_PUBLIC_CERT_PATH: /home/wso2/security/keystore/mg.pem
      ADAPTER_HOST: adapter
      ADAPTER_PORT: 18000
      ADAPTER_CA_CERT_PATH: /home/wso2/security/truststore/mg.pem
      ENFORCER_HOST: enforcer
      ENFORCER_PORT: 8081
      ENFORCER_ANALYTICS_RECEIVER_PORT: 18090
      ENFORCER_CA_CERT_PATH: /home/wso2/security/truststore/mg.pem
      CONCURRENCY: 2
    volumes:
      - ${REPO_ROOT}/resources/router/security:/home/wso2/security
    ports:
      - "9000:9000"
      - "9090:9090"
      - "9095:9095"
    depends_on:
      - adapter
      - enforcer

  adapter:
    image: wso2/choreo-connect-adapter:${PROJECT_VERSION}
    environment:
      ADAPTER_PRIVATE_KEY_PATH: /home/wso2/security/keystore/mg.key
      ADAPTER_PUBLIC_CERT_PATH: /home/wso2/security/keystore/mg.pem
      cp_admin_pwd: admin
      adapter_admin_pwd: admin
    volumes:
      - ${REPO_ROOT}/resources/adapter/security:/home/wso2/security
      - ${REPO_ROOT}/resources/conf/log_config.toml:/home/wso2/conf/log_config.toml
      - ${REPO_ROOT}/resources/conf/config.toml:/home/wso2/conf/config.toml
      - ${MOUNTED_APIS_DIR}:/home/wso2/artifacts/apis
    ports:
      - "9843:9843"
      - "18000:18000"

  enforcer:
    image: wso2/choreo-connect-enforcer:${PROJECT_VERSION}
    environment:
      ENFORCER_PRIVATE_KEY_PATH: /home/wso2/security/keystore/mg.key
      ENFORCER_PUBLIC_CERT_PATH: /home/wso2/security/keystore/mg.pem
      TRUSTED_CA_CERTS_PATH: /home/wso2/security/truststore
      ADAPTER_HOST_NAME: adapter
      ADAPTER_HOST: adapter
      ADAPTER_XDS_PORT: 18000
      ENFORCER_LABEL: Default
      ENFORCER_REGION: UNKNOWN
      XDS_MAX_MSG_SIZE: 4194304
      XDS_MAX_RETRIES: 3
      JAVA_OPTS: -Dhttpclient.hostnameVerifier=AllowAll
      enforcer_admin_pwd: admin
      tm_admin_pwd: admin
      analytics_authURL: https://localhost:8080
      analytics_authToken: ""
    volumes:
      - ${REPO_ROOT}/resources/enforcer/security:/home/wso2/security
      - ${REPO_ROOT}/resources/conf/log4j2.properties:/home/wso2/conf/log4j2.properties
      - ${REPO_ROOT}/resources/enforcer/dropins:/home/wso2/lib/dropins
    ports:
      - "8081:8081"
      - "9001:9001"

  mockBackend:
    image: wso2/choreo-connect-mock-backend:${PROJECT_VERSION}
    ports:
      - "2383:2383"
EOF

echo "Using project version: $PROJECT_VERSION"
echo "Compose project name: $COMPOSE_PROJECT_NAME"

if [[ "$SKIP_BUILD" != "1" ]]; then
    echo
    echo "==> Building router, adapter, enforcer, and mock backend images"
    "$MVN_BIN" -PRelease -pl router,adapter,enforcer-parent,integration/mock-backend-server -am clean package -DskipTests
fi

echo
echo "==> Starting Choreo Connect and local mock backend"
run_compose up -d

echo
echo "==> Waiting for local services"
wait_for_http_code "https://localhost:9095/health" "200" 90
wait_for_http_code "https://localhost:9095/ready" "200" 90
wait_for_nonzero_http_code "https://localhost:9843/" 90
wait_for_http_code "http://localhost:2383/v2/pet/findByStatus?status=available" "200" 60

echo
echo "==> Requesting test key"
TOKEN="$(curl -fksS -X POST "https://localhost:9095/testkey" -d "scope=read:pets" -H "Authorization: Basic YWRtaW46YWRtaW4=")"
TOKEN="$(printf '%s' "$TOKEN" | tr -d '\r\n')"
if [[ -z "$TOKEN" ]]; then
    echo "error: empty token received from /testkey" >&2
    exit 1
fi

echo
echo "==> Waiting for mounted API to be available"
wait_for_http_code "https://localhost:9095/v2/pet/findByStatus?status=available" "200" 90 -H "Authorization: Bearer $TOKEN" -H "accept: application/json"

echo
echo "==> Verifying unauthorized invoke is rejected"
UNAUTHORIZED_CODE="$(curl -ksS -o /dev/null -w '%{http_code}' "https://localhost:9095/v2/pet/findByStatus?status=available")"
if [[ "$UNAUTHORIZED_CODE" != "401" ]]; then
    echo "error: unauthorized invoke returned HTTP $UNAUTHORIZED_CODE, expected 401" >&2
    exit 1
fi
echo "Unauthorized invoke returned HTTP $UNAUTHORIZED_CODE"

echo
echo "==> Invoking mounted API"
API_RESPONSE="$(curl -fksS "https://localhost:9095/v2/pet/findByStatus?status=available" -H "Authorization: Bearer $TOKEN" -H "accept: application/json")"
echo "$API_RESPONSE"

if ! grep -q 'doggieUpdated' <<<"$API_RESPONSE"; then
    echo "error: API response did not contain expected mock backend payload" >&2
    exit 1
fi

echo
echo "==> Verifying router notice and license files exist in the built image"
docker run --rm --entrypoint sh "wso2/choreo-connect-router:${PROJECT_VERSION}" -lc 'test -f /LICENSE.txt && test -f /NOTICE.txt'

FAILURE=0

echo
echo "Smoke test completed successfully."
if [[ "$KEEP_RUNNING" == "1" ]]; then
    echo "Containers are still running under compose project: $COMPOSE_PROJECT_NAME"
else
    echo "Containers will be cleaned up now."
fi
