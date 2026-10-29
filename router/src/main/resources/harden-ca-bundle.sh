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
# Pins the OS CA bundle contents of the router image. Run as root during the
# image build.
#
# The shipped root files are deleted and their entries dropped from
# /etc/ca-certificates.conf before update-ca-certificates --fresh regenerates
# /etc/ssl/certs/ca-certificates.crt and the hash symlinks. Rewriting the
# concatenated bundle directly is avoided because it would be undone the next
# time anything triggered update-ca-certificates.
#
# The bundle is re-read afterwards and the build fails if the expected state is
# not reached.
# -----------------------------------------------------------------------

set -eu

# Matched case insensitively against both the shipped .crt filenames and the
# decoded X.509 subjects.
REMOVED_ROOT_PATTERN='emsign|aaa.?(certificate|services)'

CA_CERT_DIR="/usr/share/ca-certificates"
CA_CONF="/etc/ca-certificates.conf"
CA_BUNDLE="/etc/ssl/certs/ca-certificates.crt"

fail() {
    echo "[harden-ca-bundle] FAILED: $1" >&2
    exit 1
}

# The rebuilt bundle is read back with openssl, and the pruning itself goes
# through update-ca-certificates. Neither is installed by the Dockerfiles - both
# come from the base image - and a missing openssl would make the subject check
# produce no output and pass vacuously, so the build would succeed on an
# unverified bundle. Require both up front instead.
command -v update-ca-certificates >/dev/null 2>&1 \
    || fail "update-ca-certificates not found - the base image does not ship the ca-certificates tooling this script relies on"
command -v openssl >/dev/null 2>&1 \
    || fail "openssl not found - ${CA_BUNDLE} cannot be read, so the result cannot be verified"

# --- 1. Delete the shipped root files ----------------------------------------
echo "[harden-ca-bundle] Removing pinned-out roots from ${CA_CERT_DIR}"
find "${CA_CERT_DIR}" -type f -name '*.crt' | while IFS= read -r cert_file; do
    if basename "${cert_file}" | grep -qiE "${REMOVED_ROOT_PATTERN}"; then
        echo "[harden-ca-bundle]   deleting ${cert_file}"
        rm -f "${cert_file}"
    fi
done

# --- 2. Drop their entries from the bundle manifest --------------------------
if [ -f "${CA_CONF}" ]; then
    echo "[harden-ca-bundle] Pruning ${CA_CONF}"
    grep -ivE "${REMOVED_ROOT_PATTERN}" "${CA_CONF}" > "${CA_CONF}.new" || true
    mv "${CA_CONF}.new" "${CA_CONF}"
fi

# --- 3. Rebuild the concatenated bundle and the hash symlinks ----------------
echo "[harden-ca-bundle] Rebuilding ${CA_BUNDLE}"
update-ca-certificates --fresh

# --- Verification: fail the build if the expected state is not reached -------
echo "[harden-ca-bundle] Verifying"

[ -s "${CA_BUNDLE}" ] || fail "${CA_BUNDLE} is missing or empty after rebuild"

bundle_subjects="$(openssl storeutl -noout -text "${CA_BUNDLE}")" \
    || fail "could not read ${CA_BUNDLE}"
if echo "${bundle_subjects}" | grep -i '^[[:space:]]*subject:' \
    | grep -qiE "${REMOVED_ROOT_PATTERN}"; then
    fail "pinned-out root(s) still present in ${CA_BUNDLE}"
fi

if find /etc/ssl/certs -name '*.pem' -o -name '*.crt' 2>/dev/null \
    | xargs -r basename -a 2>/dev/null | grep -qiE "${REMOVED_ROOT_PATTERN}"; then
    fail "pinned-out root(s) still linked under /etc/ssl/certs"
fi

echo "[harden-ca-bundle] OK - CA bundle verified ($(grep -c 'BEGIN CERT' "${CA_BUNDLE}") certificates)"
