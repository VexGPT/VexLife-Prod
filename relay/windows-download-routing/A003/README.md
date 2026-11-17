[VXG RealForever]

# Windows download-routing host probe — A003

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

This probe performs no network, Product, Home, model, service, registry, or release mutation.

Package offered to Victor:

```text
VEXLIFE-WINDOWS-HOST-PROBE-A003--sha256-8c34622df71bd9a8013033429d7b05c140433c5fe07fc4afa9a43be32e1633a0.zip
sha256=8c34622df71bd9a8013033429d7b05c140433c5fe07fc4afa9a43be32e1633a0
bytes=4059
```

Contained Product probe SHA-256:

```text
tools/windows/vexlife-windows-host-probe.ps1
5fe13b36751b4d6a2de7973ec0fe27f2d1f2a44a2c334290ce82ec040cc9cfe6
```

Carriage runner SHA-256:

```text
RUN-WINDOWS-HOST-PROBE.ps1
1ee51bdd0399010dc542ac494feebe880dbf4c1875a982cd5be78fca7a162fad
```

The human boundary is one ZIP + one exact PowerShell command + one returned ZIP. This A003 package
uses the VexRelay/VexLocalBridge human-boundary shape for a pre-admission read-only host observation,
but its returned archive is explicitly not relabeled as a canonical vex.relay.result/v2 RETURN.

A001/A002 are superseded formation history; A003 is the offered package.

<!-- [VXG RealForever][VEXLIFE-PROD][WINDOWS-HOST-PROBE][A003] -->
