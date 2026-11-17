# Cache-Prefix Current-State Host Proof — Relay Formation A001

[VXG RealForever]

This is the companion host-proof carriage for Product candidate PR #230.

```text
Product source / semantics = PR #230
this branch                = Relay formation / host-proof carriage only
```

## Exact Product proof candidate

```text
repository  VexGPT/VexLife-Prod
base        d0cba51decfe9057ac9afc007fdeb37da213cc97
head        cb1280d61e38b9d9a1e64cb80e4f28b59f8124e5
tree        7cda340ab4f725efde3a3ae9b289c8abcb0ab68e
branch      VXG-111726-cache-prefix-current-state-a001
PR          #230
```

The carriage commit(s) on this companion branch are **not** part of that Product candidate.

```text
PRODUCT PROOF CANDIDATE
!=
RELAY CARRIAGE COMMIT
```

## Why this bundle exists

The current Relay contract source-manages the executable task's bounded `formedAt -> expiresAt`
window. Therefore the durable artifact is a stable digest-bound **formation bundle**, following the
proven pattern from PR #209. On Victor's Mac it:

1. verifies the exact formation ZIP bytes;
2. acquires exact Vextreme-SDK source;
3. verifies `source.vexrelay.current-contract/v1`;
4. uses the accepted `ExactHeadSourceOwnerProof` recipe and native archive carrier;
5. forms the fresh time-bound VexRelay/VexLocalBridge task;
6. executes the exact Product candidate proof;
7. reveals the canonical RETURN ZIP.

## Exact formation bundle

```text
VexLife-Cache-Prefix-Current-State-RelayFormation-A001--sha256-7e115d37cc03437e80877c77ba41699ac8de8254525bca3e6f2e180d44846220.zip

sha256
7e115d37cc03437e80877c77ba41699ac8de8254525bca3e6f2e180d44846220
```

Members:

```text
RUN.command
formation.mjs
README.txt
```

## Relay source binding

```text
repository  vgong24/Vextreme-SDK
head        8543c7d755faba04d4c4019c02e84f992667918a
tree        6376f68279f0750ee6d3868b8472368199763064
contract    source.vexrelay.current-contract/v1
recipe      ExactHeadSourceOwnerProof
carrier     adapter.vexlocalbridge.native-vexrelay-task-archive/v1
host        macos-victor
```

## Proof boundary

The task runs:

```text
./gradlew --no-daemon jvmTest

./gradlew --no-daemon \
  compileKotlinJs \
  compileTestKotlinJs \
  checkVexLifeStrings \
  generateVexLifeWebP1
```

and the recipe forms the exact local review snapshot against the fixed base.

No source mutation, branch/PR publication, approval, READY, merge, Home mutation, model-runtime
mutation, cache-policy mutation or release effect is authorized.

## Architecture boundary

PR #230 now derives prompt component wire identity from a typed parent/child topology rather than a
flat component-ID registry. Receipt evidence carries the derived component ref plus parent ref.

<!-- [VXG RealForever][VEXLIFE-PROD][CACHE-PREFIX-CURRENT-STATE][HOST-PROOF][A001] -->
