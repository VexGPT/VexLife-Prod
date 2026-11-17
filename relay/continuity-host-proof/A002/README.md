# Continuity Host Proof Relay Formation A002

[VXG RealForever]

This directory retains the stable formation artifact for VexLife-Prod PR #209.

A fully formed VexRelay/VexLocalBridge task package is intentionally not committed here because the
current Relay contract source-manages a bounded `formedAt -> expiresAt` execution window. The stable
repository artifact is therefore this exact digest-bound **formation bundle**, which forms a fresh
canonical task on the Mac at execution time.

## Bound VexLife-Prod candidate

```text
PR #208 base/head
03aee62cdbe3c1e269d9e94e05b1bc06901e3a25

PR #209 head
f065b23e5db99245335c8955e5d51f365197662f

PR #209 tree
41435b82f5e7574ee3e501170e11d3d280dacf5d

PR #209 branch
VXG-111626-direct-continuity-host-proof-a001
```

## Formation source

The bundle acquires and verifies:

```text
vgong24/Vextreme-SDK
8543c7d755faba04d4c4019c02e84f992667918a
```

That source contains the current `source.vexrelay.current-contract/v1`, native VexRelay
task-archive formation owner, and accepted Mac VexLocalBridge runner.

## Bundle

```text
VexLife-Continuity-Host-Proof-RelayFormation-A002--sha256-b4959d2a902160c4c6acaeb496161c1e47007e85f31a47b864056ced30378219.zip

sha256
b4959d2a902160c4c6acaeb496161c1e47007e85f31a47b864056ced30378219
```

The ZIP contains `RUN.command`, `formation.mjs`, and `README.txt`. The formation source is retained
inside the exact digest-bound artifact so a future chat can recover the same bytes from repository
history without relying on a ChatGPT sandbox attachment.

## Effect boundary

This proof authorizes read/query/check/local-test/result formation only. It does not authorize source
mutation, push, publication, merge, Product Home mutation, or model-runtime effects.
