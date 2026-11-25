#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SOURCE="$ROOT/packaging/macos-partner-host/VexLifePartnerHost.swift"
BUILD="$ROOT/build/macos-partner-host-proof"
HELPER_A="$BUILD/rebuild-a/vexlife-partner-host"
HELPER_B="$BUILD/rebuild-b/vexlife-partner-host"
ACTIVE_HELPER=""
RECEIPT="$BUILD/macos-partner-host-proof.json"

if [[ "$(uname -s)" != "Darwin" || "$(uname -m)" != "arm64" ]]; then
  echo "macOS Partner host proof requires Darwin arm64" >&2
  exit 2
fi

if [[ ! -f "$SOURCE" ]]; then
  echo "macOS Partner helper source is missing" >&2
  exit 2
fi

mkdir -p "$(dirname "$HELPER_A")" "$(dirname "$HELPER_B")"
rm -f "$HELPER_A" "$HELPER_B" "$RECEIPT"

compile_sign_helper() {
  local target="$1"
  /usr/bin/xcrun --sdk macosx swiftc -O "$SOURCE" -o "$target"
  chmod 700 "$target"
  /usr/bin/codesign --force --sign - --timestamp=none "$target" >/dev/null
  /usr/bin/codesign --verify --strict --verbose=2 "$target" >/dev/null
}

invoke_helper_with() {
  local helper="$1"
  local header="$2"
  local payload="$3"
  {
    printf '%s\n' "$header"
    if [[ -n "$payload" ]]; then
      printf '%s' "$payload"
    fi
  } | "$helper"
}

invoke_helper() {
  invoke_helper_with "$ACTIVE_HELPER" "$1" "$2"
}

compile_sign_helper "$HELPER_A"
ACTIVE_HELPER="$HELPER_A"
HELPER_A_SHA="$(/usr/bin/shasum -a 256 "$HELPER_A" | /usr/bin/awk '{print $1}')"

SCHEMA="vexlife-prod.partner-native-host/v1"
PROVIDER_REF="provider.openai.api"
CREDENTIAL_REF="credential.vexlife.partner.proof-$(uuidgen | tr '[:upper:]' '[:lower:]' | tr -d '-')"
SYNTHETIC_SECRET="vexlife-partner-proof-$(uuidgen | tr -d '-')"

delete_proof_credential() {
  local header
  header="{\"schemaVersion\":\"$SCHEMA\",\"operation\":\"DELETE\",\"providerRef\":\"$PROVIDER_REF\",\"credentialRef\":\"$CREDENTIAL_REF\"}"
  if [[ -n "$ACTIVE_HELPER" && -x "$ACTIVE_HELPER" ]]; then
    invoke_helper_with "$ACTIVE_HELPER" "$header" "" >/dev/null 2>&1 || true
  fi
  if [[ "$ACTIVE_HELPER" != "$HELPER_A" && -x "$HELPER_A" ]]; then
    invoke_helper_with "$HELPER_A" "$header" "" >/dev/null 2>&1 || true
  fi
}

cleanup() {
  set +e
  delete_proof_credential
  SYNTHETIC_SECRET=""
}
trap cleanup EXIT

SECRET_BYTES="$(LC_ALL=C printf '%s' "$SYNTHETIC_SECRET" | wc -c | tr -d ' ')"
STORE_HEADER="{\"schemaVersion\":\"$SCHEMA\",\"operation\":\"STORE\",\"providerRef\":\"$PROVIDER_REF\",\"credentialRef\":\"$CREDENTIAL_REF\",\"secretBytes\":$SECRET_BYTES}"
STORE_OUTPUT="$(invoke_helper "$STORE_HEADER" "$SYNTHETIC_SECRET")"
printf '%s\n' "$STORE_OUTPUT" | grep -q '"type":"STORED"'
printf '%s\n' "$STORE_OUTPUT" | grep -q '"present":true'

OBSERVE_HEADER="{\"schemaVersion\":\"$SCHEMA\",\"operation\":\"OBSERVE\",\"providerRef\":\"$PROVIDER_REF\",\"credentialRef\":\"$CREDENTIAL_REF\"}"
OBSERVE_PRESENT_A="$(invoke_helper "$OBSERVE_HEADER" "")"
printf '%s\n' "$OBSERVE_PRESENT_A" | grep -q '"type":"OBSERVED"'
printf '%s\n' "$OBSERVE_PRESENT_A" | grep -q '"present":true'

compile_sign_helper "$HELPER_B"
HELPER_B_SHA="$(/usr/bin/shasum -a 256 "$HELPER_B" | /usr/bin/awk '{print $1}')"
ACTIVE_HELPER="$HELPER_B"

OBSERVE_PRESENT_B="$(invoke_helper "$OBSERVE_HEADER" "")"
printf '%s\n' "$OBSERVE_PRESENT_B" | grep -q '"type":"OBSERVED"'
printf '%s\n' "$OBSERVE_PRESENT_B" | grep -q '"present":true'

DELETE_HEADER="{\"schemaVersion\":\"$SCHEMA\",\"operation\":\"DELETE\",\"providerRef\":\"$PROVIDER_REF\",\"credentialRef\":\"$CREDENTIAL_REF\"}"
DELETE_OUTPUT="$(invoke_helper "$DELETE_HEADER" "")"
printf '%s\n' "$DELETE_OUTPUT" | grep -q '"type":"DELETED"'
printf '%s\n' "$DELETE_OUTPUT" | grep -q '"present":false'

OBSERVE_ABSENT="$(invoke_helper "$OBSERVE_HEADER" "")"
printf '%s\n' "$OBSERVE_ABSENT" | grep -q '"type":"OBSERVED"'
printf '%s\n' "$OBSERVE_ABSENT" | grep -q '"present":false'

BODY='{"model":"proof-only-no-network","stream":true}'
BODY_BYTES="$(LC_ALL=C printf '%s' "$BODY" | wc -c | tr -d ' ')"
BODY_SHA="$(LC_ALL=C printf '%s' "$BODY" | shasum -a 256 | awk '{print $1}')"
EXECUTE_HEADER="{\"schemaVersion\":\"$SCHEMA\",\"operation\":\"EXECUTE\",\"providerRef\":\"$PROVIDER_REF\",\"credentialRef\":\"$CREDENTIAL_REF\",\"expectedRequestUrl\":\"https://api.openai.com/v1/responses\",\"dialect\":\"OPENAI_RESPONSES\",\"publicProtocolHeaders\":{\"content-type\":\"application/json\",\"accept\":\"text/event-stream, application/json\"},\"requestBodySha256\":\"$BODY_SHA\",\"requestBodyBytes\":$BODY_BYTES,\"redirectPolicy\":\"DENY\"}"
MISSING_EXECUTE="$(invoke_helper "$EXECUTE_HEADER" "$BODY")"
printf '%s\n' "$MISSING_EXECUTE" | grep -q '"type":"TRANSPORT_FAILURE"'
printf '%s\n' "$MISSING_EXECUTE" | grep -q '"failureCode":"CREDENTIAL_NOT_PRESENT"'

cat > "$RECEIPT" <<EOF
{
  "schemaVersion": "vexlife-prod.partner-macos-host-proof/v2",
  "truthClass": "MACOS_PROTECTED_HOST_REBUILD_CONTINUITY_PROOF",
  "providerRef": "$PROVIDER_REF",
  "credentialRef": "$CREDENTIAL_REF",
  "helperASha256": "$HELPER_A_SHA",
  "helperBSha256": "$HELPER_B_SHA",
  "swiftCompileA": "PASS",
  "codesignA": "PASS",
  "storeA": "PASS",
  "observePresentA": "PASS",
  "swiftCompileB": "PASS",
  "codesignB": "PASS",
  "observePresentAfterRebuildB": "PASS",
  "deleteB": "PASS",
  "observeAbsentB": "PASS",
  "executeMissingCredentialBeforeNetworkB": "PASS",
  "realProviderCredentialUsed": false,
  "providerNetworkEffectExpected": false,
  "secretPersistedAfterProof": false
}
EOF

echo "MACOS_PARTNER_HOST_REBUILD_CONTINUITY_PROOF_PASS $RECEIPT"

# [VXG RealForever]
