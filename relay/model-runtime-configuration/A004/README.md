[VXG RealForever]

# PR #238 model/runtime configuration host proof — A004

## Product candidate

```text
base 47decbf10bdb53a33afc21affc1080bcf2b125fd
head 3d3900ea2c58564252d43507751344d27f2d08dc
tree ef51ab2fa58bffc4d024f09e52f7717b45d7321b
```

## A003 evidence carried forward

Victor Mac RETURN:

```text
outer sha256
79c6dbf21ed4a4d63b7adeab073b28934c4aba466c51a5bfab46f81297dd1ade

retained canonical inner sha256
ef00f2eb51cb5c795d7e26ad6e723ac57459300fd93fcefa806f8d238110bfeb
```

A003 proved the full JVM suite on the exact unchanged candidate:

```text
./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx2g jvmTest
BUILD SUCCESSFUL
```

The second proof command never spawned. Relay returned `INVALID_TIMEOUT_MS`.

Current SDK mismatch observed:

```text
manifest.mjs: timeoutMs <= 3,600,000
process.mjs:  timeoutMs <= 1,800,000
```

A003 used 3,600,000 for proof command #2. Classification: harness/execution-contract,
not Product source.

## A004

A004 preserves the same Product head/tree and does not replay the already-current JVM proof.
It runs only:

```text
./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-Xmx2g \
  -PvexlifeAppVersion=0.0.0 \
  compileKotlinJs compileTestKotlinJs checkVexLifeStrings generateVexLifeWebP1 \
  archiveVexLifeProdMacApplication
```

with `timeoutMs=1,800,000`, followed by the exact bounded REVIEW_SNAPSHOT.

Stable formation bundle:

```text
VexLife-Model-Runtime-Configuration-RelayFormation-A004
sha256 d98064aa67c1eb5ffbbd955bfa009d2c96f10ad520c70a93a62a73c11f7174ae
```

The current Relay review cap remains `maxFiles=10`; the Product candidate binding and full
changed-path membrane remain exact.

This companion path is proof carriage/history only; Product source remains PR #238.

<!-- [VXG RealForever][VEXLIFE-PROD][PR238][HOST-PROOF][A004] -->
