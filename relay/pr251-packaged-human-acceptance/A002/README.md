# Packaged human acceptance A002

[VXG RealForever]

## Status

A002 is the current Mac handoff for PR #364.

It is bound to exact VexLife-Prod current main:

```text
head a58777106b08bd58c167608fa0115729185ae3dd
tree 3baf658da68adfdd03eca19fc1877f656ec0a56e
```

It uses current Vex Relay / VexLocalBridge source:

```text
SDK head 8543c7d755faba04d4c4019c02e84f992667918a
SDK tree 6376f68279f0750ee6d3868b8472368199763064
contract  source.vexrelay.current-contract/v1
```

## What it does

On Victor's Mac:

1. verifies/acquires exact current SDK source;
2. forms a fresh source-managed digest-bound VexLocalBridge task;
3. acquires exact VexLife-Prod `main` and refuses to continue if remote main moved;
4. runs the Product-owned `archiveVexLifeProdMacApplication` route;
5. verifies the app seal, DMG SHA-256, DMG structure and source receipt;
6. copies the exact verified DMG + receipt + human checklist into a dedicated Downloads directory;
7. forms/copies one canonical RETURN ZIP.

Expected human artifact directory:

```text
~/Downloads/VexLife-PR251-Packaged-Acceptance-a58777106b08-A002/
```

The DMG inside that directory is the thing Victor must install/open and test.

## Acceptance boundary

```text
DMG_ARTIFACT_PROOF=PASS
!=
FEATURE_ACCEPTANCE=PASS
```

Even if the task returns PASS, feature acceptance remains:

`PENDING_VICTOR_INSTALLED_APP_TEST`

Only Victor's observation of the ordinary installed Product can accept or reject the feature relationship.

## A001 history

A001 was formed but deliberately never handed to Victor or executed after two carrier defects were found before handoff:

- macOS system Bash 3.2 does not provide `mapfile`;
- the packaged checklist source path was brittle after the payload changed cwd.

A001 bytes are preserved beside A002. A002 rotates task/attempt/package identity and fixes only those carriage defects.

<!-- [VXG RealForever][PR364][A002][PACKAGED-HUMAN-ACCEPTANCE][CURRENT-HANDOFF] -->
