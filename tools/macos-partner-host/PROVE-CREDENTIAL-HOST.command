#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SOURCE="$ROOT/packaging/macos-partner-host/VexLifePartnerHost.swift"
BUILD="$ROOT/build/macos-partner-host-proof"
HELPER="$BUILD/vexlife-partner-host"
RECEIPT="$BUILD/macos-partner-host-proof.json"

if [[ "$(uname -s)" != "Darwin" || "$(uname -m)" != "arm64" ]]; then
  echo "macOS Partner host proof requires Darwin arm64" >&2
  exit 2
fi

if [[ ! -f "$SOURCE" ]]; then
  echo "macOS Partner helper source is missing" >&2
  exit 2
fi

mkdir -p "$BUILD"
rm -f "$HELPER" "$RECEIPT"

xcrun --sdk macosx swiftc   -O   "$SOURCE"   -o "$HELPER"

chmod 700 "$HELPER"

SCHEMA="vexlife-prod.partner-native-host/v1"
PROVIDER_REF="provider.openai.api"
CREDENTIAL_REF="credential.vexlife.partner.proof-$(uuidgen | tr '[:upper:]' '[:lower:]' | tr -d '-')"
SYNTHETIC_SECRET="vexlife-partner-proof-$(uuidgen | tr -d '-')"

invoke_helper() {
  local header="$1"
  local payload="$2"
  {
    printf '%s\n' "$header"
    if [[ -n "$payload" ]]; then
      printf '%s' "$payload"
    fi
  } | "$HELPER"
}

delete_proof_credential() {
  local header
  header="{\"schemaVersion\":\"$SCHEMA\",\"operation\":\"DELETE\",\"providerRef\":\"$PROVIDER_REF\",\"credentialRef\":\"$CREDENTIAL_REF\"}"
  invoke_helper "$header" "" >/dev/null
}

cleanup() {
  set +e
  delete_proof_credential >/dev/null 2>&1
  SYNTHETIC_SECRET=""
}
trap cleanup EXIT

SECRET_BYTES="$(LC_ALL=C printf '%s' "$SYNTHETIC_SECRET" | wc -c | tr -d ' ')"
STORE_HEADER="{\"schemaVersion\":\"$SCHEMA\",\"operation\":\"STORE\",\"providerRef\":\"$PROVIDER_REF\",\"credentialRef\":\"$CREDENTIAL_REF\",\"secretBytes\":$SECRET_BYTES}"
STORE_OUTPUT="$(invoke_helper "$STORE_HEADER" "$SYNTHETIC_SECRET")"
printf '%s\n' "$STORE_OUTPUT" | grep -q '"type":"STORED"'
printf '%s\n' "$STORE_OUTPUT" | grep -q '"present":true'

OBSERVE_HEADER="{\"schemaVersion\":\"$SCHEMA\",\"operation\":\"OBSERVE\",\"providerRef\":\"$PROVIDER_REF\",\"credentialRef\":\"$CREDENTIAL_REF\"}"
OBSERVE_PRESENT="$(invoke_helper "$OBSERVE_HEADER" "")"
printf '%s\n' "$OBSERVE_PRESENT" | grep -q '"type":"OBSERVED"'
printf '%s\n' "$OBSERVE_PRESENT" | grep -q '"present":true'

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
  "schemaVersion": "vexlife-prod.partner-macos-host-proof/v1",
  "truthClass": "MACOS_PROTECTED_HOST_DETERMINISTIC_PROOF",
  "providerRef": "$PROVIDER_REF",
  "credentialRef": "$CREDENTIAL_REF",
  "swiftCompile": "PASS",
  "store": "PASS",
  "observePresent": "PASS",
  "delete": "PASS",
  "observeAbsent": "PASS",
  "executeMissingCredentialBeforeNetwork": "PASS",
  "realProviderCredentialUsed": false,
  "providerNetworkEffectExpected": false,
  "secretPersistedAfterProof": false
}
EOF

echo "MACOS_PARTNER_HOST_PROOF_PASS $RECEIPT"

# [VXG RealForever]
