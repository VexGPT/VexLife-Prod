# PR #251 feature-preservation SourceOwner A016

[VXG RealForever]

Temporary proof carriage only. This child is not Product source and must never merge into #251.

Exact Product candidate:
- base `98a22ede8d37b3b8868eb42f4e7a9a1d612b97d0`
- head `7477e1a32a4c27289a97c33aee8e1e67c63a64c1`
- tree `f2ab95582455fc1b7a22720cf285cf8c767584a2`
- 27 changed paths
- branch `VXG-111726-vex-context-profiles-a001`

Current Relay binding:
- SDK head `8543c7d755faba04d4c4019c02e84f992667918a`
- SDK tree `6376f68279f0750ee6d3868b8472368199763064`
- contract `source.vexrelay.current-contract/v1`
- contract blob `b53cbac01c2deeb45fad5c32ce98e0796c04126f`
- recipe `ExactHeadSourceOwnerProof`
- carrier `adapter.vexlocalbridge.native-vexrelay-task-archive/v1`
- host `macos-victor`

Formation doorway:
- `VexLife-Context-Voice-Preservation-SourceOwner-RelayFormation-A016--sha256-ae52a1b217e9677e74a5e04f96791ed022c9982dd90e7269c2b1b9fb88b30766.zip`
- sha256 `ae52a1b217e9677e74a5e04f96791ed022c9982dd90e7269c2b1b9fb88b30766`
- bytes 8126
- members: `RUN.command`, `formation.mjs`, `relay-context.json`, `README.txt`

The doorway calls the exact SDK-owned `formVexRelayTaskArchiveCarrier(...)`. It does not reimplement Relay ZIP mechanics.

SourceOwner gate:
1. `checkVexLifeStrings`
2. full `jvmTest`
3. `compileKotlinJs compileTestKotlinJs`
4. exact 27-path candidate review snapshot

Approval, READY, merge, Product mutation, Home/Memory mutation and publication remain held.

On PASS: fresh semantic Assurance, then one combined lived Product check for Voice-language + Vex Context.
