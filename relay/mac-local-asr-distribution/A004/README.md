# A004 — SourceOwner retry after v4 Vision-schema identity correction

[VXG RealForever]

A003 outer RETURN sha256:
1373727998c6ec72caab731bba034b09e4960482f1133fa0b517342be7faeb65

A003 retained inner RETURN sha256:
d0bd58591254580ed2d0e7a2d4534bf1f033f8dd4c99e5dea723367fb7883399

A003 proved the heap repair: production/test Kotlin compilation completed and 834 JVM tests ran.
Exactly one test failed:
`VexLifeProdPrivateServiceImageTest.localQualifiedVisionRuntimeIsExplicitlyObserved`
at the `observe()` call.

Root cause was in #384: v4 `VisionSchema` was accepted by the schema/runtime relationship check,
but omitted from the earlier manifest-identity schema set. #384 now includes v4 in that initial gate.

Corrected #384:
```text
head cb47d5c92db709048ac668cfa3c0eadd34b02152
tree c500a68873499ec754ea50a43d85f427debccb99
```

A004 preserves the same SourceOwner task, rotates attempt identity, and re-runs the same source/package
threshold with the already-proven 2 GB / one-worker proof envelope. Local Workspace is not used.

Formation ZIP:
```text
sha256 c161340ee14b4f65cdf9b7d37fea3f99397b9d741e9c1e508413b23ee086da69
bytes 9231
```

No Product launch, real Home mutation, microphone, approval, READY, merge or release publication.

<!-- [VXG RealForever][PR384][PR386][A004][DO-NOT-MERGE] -->