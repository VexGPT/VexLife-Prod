# PR #396 Mac candidate build delivery

[VXG RealForever]

Single **DO-NOT-MERGE** VexRelay/VexLocalBridge companion for producing one human-testable macOS
candidate build from exact WebToApp source.

## Attempt history

```text
A001-A004  terminal source/build failures resolved in sequence
A005       Product build PASS; carrier retained-evidence finalization failed
A006       terminal PASS by recovery of A005 exact build
A007       terminal PASS; first build containing AppKit first-run constant repair
           lived inspection reached Product but Gatekeeper blocked embedded whisper-server
A008       CURRENT
           Product head 4da7548b798532c65156e0dc64ee65bc3d349a10
           Product tree c246ea6ce353f14558a0cc448b112330888483d5
```

A001-A007 are historical evidence. Do not replay them.

A008 rebuilds the same Product/container meaning with one new Mac trust-boundary repair:
- discover Mach-O items under the embedded jlink Java runtime + Local-ASR provider;
- ad-hoc sign and strict-verify each item explicitly, deepest path first;
- do not use `codesign --deep`;
- compute Local-ASR/service-image fingerprints only after those exact signed bytes exist;
- preserve the existing truthful release state:
  `AD_HOC_SIGNATURE_NO_DEVELOPER_ID_NO_NOTARIZATION`;
- then seal the nested VexLifeProduct.app and outer VexLife.app as before.

This does not claim Developer ID identity or notarization. It is intended to test whether one
explicit unsigned-app approval can coherently encompass the app's carried native helpers instead
of Gatekeeper treating whisper-server / Java as unrelated unsigned attachments.

Expected PASS delivery:

```text
~/Downloads/VexLife-WebToApp-Mac-4da7548b/
  VexLife-macOS-AppleSilicon.dmg
  VexLife-macOS-AppleSilicon.dmg.sha256
  VexLife-macOS-AppleSilicon.receipt.json
  BUILD-DELIVERY.json
```

<!-- [VXG RealForever][PR396][MAC-BUILD-DELIVERY][A008-CURRENT][NESTED-NATIVE-CODE-SEAL][DO-NOT-MERGE] -->
