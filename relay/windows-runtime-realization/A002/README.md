# Temporary Relay carriage for PR #243 — A002

[VXG RealForever]

This branch is proof carriage only. It is stacked directly on PR #243 and must not become Product architecture.

It exists because A001 stopped before Relay formation on a missing host-only VEX_HOME binding. A002 preserves the same semantic task and rotates attempt/request/correlation/package identities. The Windows runner acquires the exact current accepted Vextreme-SDK source through GitHub CLI, verifies the SDK head/tree/current-contract blob, then lets the source-managed VexRelay recipe compiler and native VexLocalBridge task-archive carrier form and execute the proof.

Product candidate:
- PR #243
- head 2c54c81f3d318163ee5b09a6d35363af75c33c9d
- tree 524bb1e8112c45f0455af8f2f6b81375cea67c93
- fixed evidence base 5f0d95f49a5a20d021d2bc25e80ee4a1cb1460b0

Relay:
- contract source.vexrelay.current-contract/v1
- SDK 8543c7d755faba04d4c4019c02e84f992667918a
- recipe ExactHeadSourceOwnerProof
- host windows-victor
- expected canonical inner RETURN RETURN-VEXLIFE-WINDOWS-RUNTIME-REALIZATION-SOURCEOWNER-A002.zip

No GitHub Actions. No Product-source mutation. No merge/ready/approval effect.
