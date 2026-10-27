#!/bin/sh
# --------------------------------------------------------------------
# Copyright (c) 2026, WSO2 LLC. (http://wso2.com) All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# -----------------------------------------------------------------------
#
# Build-time trust store hardening. Run as root during the image build, before
# dropping to the unprivileged runtime user.
#
# Why the enforcer is in scope: ConfigHolder.loadTrustStore() copies every
# accepted issuer out of the JDK default trust store into the enforcer's own
# KeyStore whenever TRUST_DEFAULT_CERTS is true (the default, see
# EnvVarConfig.DEFAULT_TRUST_DEFAULT_CERTS). That KeyStore backs the SSLContext
# used by ExtendedJWKSResourceRetriever for outbound JWKS / OAuth calls made
# during JWT validation, so the JDK trust store contents directly determine
# which issuers are accepted when fetching identity-provider signing keys.
#
# Three controls, applied in order:
#   1. Remove the pinned-out roots from the JDK cacerts. This is the control
#      that holds: with the anchor gone, re-encoded variants of a subordinate
#      certificate fail to chain as well.
#   2. Append the digests in the companion file to blocked.certs, which the JVM
#      consults on every path validation regardless of trust store. This also
#      covers operator-supplied trust stores mounted at TRUSTED_CA_CERTS_PATH.
#   3. Remove the same roots from the OS CA bundle (defence in depth - the Java
#      process does not read it, but anything else in the container would).
#
# The script fails the build if any pinned-out root survives, so the image can
# never ship silently un-remediated.
# -----------------------------------------------------------------------

set -eu

# Alias / subject substrings identifying the roots to remove. Matched case
# insensitively against both keytool aliases and X.509 subjects, so this works
# regardless of whether cacerts uses DN-derived or short-form aliases.
REMOVED_ROOT_PATTERN='emsign|aaa.?(certificate|services)'

BLOCKED_CERTS_SRC="${1:-/tmp/blocked-certs-additions.txt}"
JAVA_SECURITY_DIR="${JAVA_HOME}/lib/security"
BLOCKED_CERTS="${JAVA_SECURITY_DIR}/blocked.certs"
OS_CA_BUNDLE="/etc/ssl/certs/ca-certificates.crt"
CA_CERT_DIR="/usr/share/ca-certificates"
CA_CONF="/etc/ca-certificates.conf"
STOREPASS="changeit"

fail() {
    echo "[harden-truststore] FAILED: $1" >&2
    exit 1
}

# --- 1. Remove the pinned-out roots from the JDK trust store -----------------
echo "[harden-truststore] Removing pinned-out roots from ${JAVA_SECURITY_DIR}/cacerts"
keytool -list -cacerts -storepass "${STOREPASS}" 2>/dev/null \
    | sed -n 's/^\(.*\), [A-Za-z][a-z][a-z] [0-9]\{1,2\}, [0-9]\{4\}, trustedCertEntry,[[:space:]]*$/\1/p' \
    | grep -iE "${REMOVED_ROOT_PATTERN}" \
    | while IFS= read -r alias; do
        echo "[harden-truststore]   deleting cacerts alias: ${alias}"
        keytool -delete -alias "${alias}" -cacerts -storepass "${STOREPASS}"
    done

# --- 2. Block the listed certificate digests ---------------------------------
[ -f "${BLOCKED_CERTS_SRC}" ] || fail "fingerprint list not found at ${BLOCKED_CERTS_SRC}"

if [ ! -f "${BLOCKED_CERTS}" ]; then
    # Every supported JDK ships this file; recreate the header if it is absent
    # so the parser still recognises the digest algorithm.
    echo "Algorithm=SHA-256" > "${BLOCKED_CERTS}"
fi
echo "[harden-truststore] Appending $(grep -c '^[0-9A-Fa-f]\{64\}$' "${BLOCKED_CERTS_SRC}") digests to ${BLOCKED_CERTS}"
cat "${BLOCKED_CERTS_SRC}" >> "${BLOCKED_CERTS}"

# --- 3. Remove the pinned-out roots from the OS CA bundle --------------------
# Prefer the distribution's own tooling: delete the shipped root files, prune
# the manifest, then let update-ca-certificates regenerate the concatenated
# bundle and the hash symlinks. Rewriting the concatenated file directly would
# be silently undone the next time anything triggered a regeneration. The
# direct rewrite is kept only as a fallback for images that ship a pre-built
# bundle without the tooling.
if [ -d "${CA_CERT_DIR}" ] && command -v update-ca-certificates >/dev/null 2>&1; then
    echo "[harden-truststore] Removing pinned-out roots via update-ca-certificates"
    find "${CA_CERT_DIR}" -type f -name '*.crt' | while IFS= read -r cert_file; do
        if basename "${cert_file}" | grep -qiE "${REMOVED_ROOT_PATTERN}"; then
            echo "[harden-truststore]   deleting ${cert_file}"
            rm -f "${cert_file}"
        fi
    done
    if [ -f "${CA_CONF}" ]; then
        grep -ivE "${REMOVED_ROOT_PATTERN}" "${CA_CONF}" > "${CA_CONF}.new" || true
        mv "${CA_CONF}.new" "${CA_CONF}"
    fi
    update-ca-certificates --fresh
elif [ -f "${OS_CA_BUNDLE}" ]; then
    echo "[harden-truststore] Filtering ${OS_CA_BUNDLE} (no ca-certificates tooling present)"
    split_dir="$(mktemp -d)"
    awk -v d="${split_dir}" 'BEGIN { n = 0 }
         /BEGIN CERTIFICATE/ { n++ }
         n > 0 { print > sprintf("%s/cert-%04d.pem", d, n) }' "${OS_CA_BUNDLE}"

    : > "${OS_CA_BUNDLE}.new"
    for pem in "${split_dir}"/cert-*.pem; do
        [ -f "${pem}" ] || continue
        subject="$(openssl x509 -in "${pem}" -noout -subject 2>/dev/null)" || continue
        if echo "${subject}" | grep -qiE "${REMOVED_ROOT_PATTERN}"; then
            echo "[harden-truststore]   dropping OS bundle cert: ${subject}"
            continue
        fi
        cat "${pem}" >> "${OS_CA_BUNDLE}.new"
    done
    mv "${OS_CA_BUNDLE}.new" "${OS_CA_BUNDLE}"
    rm -rf "${split_dir}"
fi

# --- Verification: fail the build rather than ship a vulnerable image --------
echo "[harden-truststore] Verifying"

if keytool -list -cacerts -storepass "${STOREPASS}" 2>/dev/null | grep -qiE "${REMOVED_ROOT_PATTERN}"; then
    fail "pinned-out root(s) still present in cacerts"
fi

for digest in $(grep '^[0-9A-Fa-f]\{64\}$' "${BLOCKED_CERTS_SRC}"); do
    grep -qi "^${digest}$" "${BLOCKED_CERTS}" || fail "digest ${digest} missing from ${BLOCKED_CERTS}"
done

if [ -f "${OS_CA_BUNDLE}" ]; then
    if openssl storeutl -noout -text "${OS_CA_BUNDLE}" 2>/dev/null \
        | grep -i '^[[:space:]]*subject:' | grep -qiE "${REMOVED_ROOT_PATTERN}"; then
        fail "pinned-out root(s) still present in ${OS_CA_BUNDLE}"
    fi
fi

if [ -d "${CA_CERT_DIR}" ] && find "${CA_CERT_DIR}" -type f -name '*.crt' 2>/dev/null \
    | xargs -r basename -a 2>/dev/null | grep -qiE "${REMOVED_ROOT_PATTERN}"; then
    fail "pinned-out root file(s) still present under ${CA_CERT_DIR}"
fi

echo "[harden-truststore] OK - trust store contents verified"
