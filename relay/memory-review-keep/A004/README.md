# Memory Review / Keep SourceOwner A004

[VXG RealForever]

A001-A003 remain immutable attempt history.

A004 targets the Score-probe-corrected Product candidate:

```text
PR #276
base 428bb31c8609474ecd6528a044ee47a9a9e89628
head b373170d8d8e88531e4ca9058512302d2ad735bb
tree ecb776c29713bbde82ee6b5e38fc90adc79485c6
```

A003 proved proposal-vs-decision candidate re-formation reaches semantic acceptance, then exposed the
review owner's invalid attempt to resolve a Score statement binding before any Score head existed.

A004 keeps Score's existing contract intact:

```text
read current Score coordinates
-> statementRef current?
   yes -> resolve/verify exact binding
   no  -> append once with observed scoreHeadSha256
```

Stable formation:

```text
VexLife-Memory-Review-Keep-RelayFormation-A004--sha256-1c647fce5464d08097bb19ae80ca363cb93954cca6d04b8a6dae384d7919df43.zip
sha256 1c647fce5464d08097bb19ae80ca363cb93954cca6d04b8a6dae384d7919df43
bytes 7728
```

A004 is not PASS until the canonical RETURN is consumed.

<!-- [VXG RealForever][PR276][PR278][A004] -->
