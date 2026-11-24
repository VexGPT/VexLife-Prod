# PR #602 Mac DMG hygiene convergence — SourceOwner A001

[VXG RealForever]

DO-NOT-MERGE proof companion for Product PR #602.

## Exact Product candidate

```text
base   225b8c75bda33ef25b85c1da77956b9e9ad6a56b
head   789f1fe78bdfdaaf85bb244723c0d580a428bc33
tree   78bf9cae8a404b1b70e680b1dec425844b3cef8a
branch VXG-112426-chatgpt-mac-dmg-hygiene-convergence-a001
paths  3
```

## Human outcome

Finish the already-assured Response Readiness trajectory by clearing the separate Mac release-artifact hygiene hold without weakening the minimal DMG contract.

SourceOwner proves on the real Mac:
- all repository/JVM/localization predicates remain green;
- the app and DMG form from the exact source head;
- checksum, hdiutil verification, codesign and source identity pass;
- the bundled ASR provider remains exact;
- `.fseventsd`, `.Spotlight-V100`, `.Trashes`, and the old Privacy webloc are absent from the final read-only DMG;
- `.VolumeIcon.icns` and FinderInfo remain present;
- named proof diagnostics identify any remaining exact failure.

The builder keeps `.fseventsd/no_log` only during the writable mount, removes all bookkeeping after the final intentional write, and detaches immediately. The suppression marker itself must not survive into the sealed DMG.

## Canonical proof

```text
bash tools/release/prove-mac-candidate.sh 0.0.0
```

On PASS: fresh Independent Assurance, then integrated lifecycle/currentness convergence. No proof predicate, Gatekeeper policy, READY state, approval, or merge is changed by this companion.

<!-- [VXG RealForever][PR602][MAC-RELEASE][DMG-HYGIENE][SOURCEOWNER-A001][DO-NOT-MERGE] -->
