# PR #251 feature-preservation SourceOwner A017

[VXG RealForever]

Same semantic SourceOwner task as A016. Product source and proof semantics are unchanged.

A016 canonical inner RETURN:
- disposition SAFE_FAILURE
- first failure `WORKSPACE_PROJECT_MISSING:workspace`
- classification harness
- no repository binding, Product command, candidate review, source mutation, or remote mutation was reached.

A017 changes only the fresh RUN_ROOT workspace binding:
`workspace.createProjectPath: false -> true`

Exact Product candidate:
- base `98a22ede8d37b3b8868eb42f4e7a9a1d612b97d0`
- head `7477e1a32a4c27289a97c33aee8e1e67c63a64c1`
- tree `f2ab95582455fc1b7a22720cf285cf8c767584a2`
- 27 changed paths

Current Relay:
- SDK `8543c7d755faba04d4c4019c02e84f992667918a`
- contract `source.vexrelay.current-contract/v1`
- recipe `ExactHeadSourceOwnerProof`

Formation doorway:
- `VexLife-Context-Voice-Preservation-SourceOwner-RelayFormation-A017--sha256-c62d18193109aae614c556920547bcf3105f37f5356fc2696d58446c8e36fe90.zip`
- sha256 `c62d18193109aae614c556920547bcf3105f37f5356fc2696d58446c8e36fe90`
- bytes 8446

Held: approval, READY, merge, Product mutation, Home/Memory mutation, publication.
