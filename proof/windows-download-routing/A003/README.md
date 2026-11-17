[VXG RealForever]

# Windows download-routing host proof A003

Proof carriage only for the Windows host-observation edge of Product experiment #215.

```text
source PR            VexGPT/VexLife-Prod #215
source candidate     1430e2749646a7be4daf873ed758b0fe1180506c
input ZIP sha256     8c34622df71bd9a8013033429d7b05c140433c5fe07fc4afa9a43be32e1633a0
RETURN ZIP sha256    c1a27432e6d077a81000bc5a6ec385f0a6f7aaac38605aa065ff9c8e946b5de5
disposition          PASS
mutationPerformed    false
```

Observed host:

```text
osPlatform                  WINDOWS
osArchitectureRaw           X64
processArchitectureRaw      X64
PROCESSOR_ARCHITECTURE      AMD64
PROCESSOR_ARCHITEW6432      absent
targetPlatform              WINDOWS
targetArchitecture          X86_64
processArchitecture         X86_64
routeRef                    route.vexlife-prod.windows.x86-64
```

Meaning:

```text
REAL WINDOWS HOST TARGET = WINDOWS / X86_64

HOST TARGET OBSERVED
!= WINDOWS PRODUCT REALIZATION QUALIFIED
!= WINDOWS RELEASE ARTIFACT EXISTS
!= PUBLIC DOWNLOAD ENABLED
```

The returned archive is retained byte-identically on this proof branch. Product source remains in #215.

<!-- [VXG RealForever][VEXLIFE-PROD][WINDOWS-X64][HOST-PROOF-A003] -->
