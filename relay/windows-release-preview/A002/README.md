# Temporary Relay carriage for PR #286 — A002

[VXG RealForever]

## Recovery from A001

A001 never entered VexRelay/Product execution. It formed the exact package but placed that package
beneath the bootstrap TEMP root. The qualified Windows VexLocalBridge profile admits input packages
only from `~/Downloads`.

The accepted bridge also requires the execution capsule to be the exact digest-derived sibling of
the package. The already-successful #282 carrier uses exactly that geometry.

A002 therefore changes **only carriage placement**:

~~~text
A001
  outer package -> %TEMP%/.../o                 REJECTED before execution
  capsule       -> %TEMP%/...                   NOT REACHED

A002
  outer package -> ~/Downloads                  qualified input root
  capsule       -> exact digest package sibling accepted bridge shape
~~~

## Product candidate unchanged

~~~text
base 470c02c9ac268118b0a5f5cf8b34261f096c9899
head 058514545ee55e18f5665d05effa70e293faa399
tree 92f75c2a48016cc1efcedc9c494bf10217384e6f
changed paths 5
~~~

The Product source, proof commands, JDK, SDK/Relay contract, authority, and preview-builder source are
unchanged from A001.

A002 runs:
1. `checkVexLifeStrings`;
2. focused Windows preview / RuntimeSession / service-process JVM tests;
3. `compileKotlinJs`;
4. exact five-path review.

After canonical SourceOwner PASS, the same one-command outer bootstrap executes the source-owned
`FORM-WINDOWS-PREVIEW.ps1` from the exact proven checkout, forms a digest-bound preview ZIP in
Downloads, extracts it, and launches the exact `VexLife.exe` for Victor's lived test.

No GitHub Actions. This temporary PR is proof carriage only and must remain unmerged.

<!-- [VXG RealForever][PR286][A001-CARRIER-FAILURE][A002-CURRENT] -->
