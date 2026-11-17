[VXG RealForever]

# Windows download-routing host probe — A001

Proof carriage only for Product experiment PR #215.

Exact Product candidate:

```text
repository  VexGPT/VexLife-Prod
PR          #215
head        1430e2749646a7be4daf873ed758b0fe1180506c
parent      #139 platform-realization foundation
Claude base #138 selectable-model stack
```

Human outcome under test:

```text
real Windows PC
-> observe native OS architecture
-> preserve process architecture separately
-> resolve WINDOWS/X86_64 or WINDOWS/ARM64
-> return machine-readable evidence
```

This probe does not install VexLife, download a model, modify Home, change services, publish a release,
or claim that a Windows Product realization already exists.

Package offered to Victor:

```text
VEXLIFE-WINDOWS-HOST-PROBE-A001--sha256-a272fcb5bc5c4987b25cb32d8d248f904e3cddba968f4e6095c718d68399de9f.zip
sha256=a272fcb5bc5c4987b25cb32d8d248f904e3cddba968f4e6095c718d68399de9f
bytes=3983
```

Contained Product probe source SHA-256:

```text
tools/windows/vexlife-windows-host-probe.ps1
5fe13b36751b4d6a2de7973ec0fe27f2d1f2a44a2c334290ce82ec040cc9cfe6
```

Carriage runner SHA-256:

```text
RUN-WINDOWS-HOST-PROBE.ps1
28e9406e24a7076a88b02df57300b8586d0d0095976d4f9b63b5c1cb6c294901
```

The human boundary is one ZIP + one exact PowerShell command + one returned ZIP. This A001 package
uses the VexRelay/VexLocalBridge human-boundary shape for a pre-admission read-only host observation,
but its returned archive is explicitly **not** relabeled as a canonical `vex.relay.result/v2`
RETURN. The actual source-owner Relay proof can be formed after the observed Windows target is known.

<!-- [VXG RealForever][VEXLIFE-PROD][WINDOWS-HOST-PROBE][A001] -->
