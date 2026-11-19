# PR #396 WebToApp application-shell SourceOwner A004

[VXG RealForever]

A001, A002 and A003 are immutable superseded-before-execution history. Do not execute or replay them.

A004 binds the settled application-shell candidate after the full static WebToApp + ordinary-entry audit.

## Exact candidate

```text
base   fdb9f77bbd9895d0ab6932f669091b46ea813e9b
head   d13e8e4a774aea60362491420ffeee2c07f91d1c
tree   98ba3219e49f722272c976171cb736ed19ae9ac9
branch VXG-111926-chatgpt-webtoapp-application-shell-a001
paths  22
```

## Meaning bound by A004

```text
OPEN_CURRENT -> OPEN_PRESENTATION
QUIT_CURRENT -> QUIT_PRODUCT

Close presentation != Quit Product

desired application source
!= exact currently-owned Product source

endpoint/readiness unavailable
!= Product ownership unavailable

durable VexLife.app
-> NATIVE_EMBEDDED
-> exact native presenter required
-> presenter failure is fail-closed

bootstrap temporary Setup.app
-> EXTERNAL_BROWSER_RECOVERY
-> historical browser handoff only
```

The candidate also preserves Product-owned web features at the native-engine boundary:
- JavaScript alert/confirm/text-input dialogs remain presentation behavior;
- exact Product file import may use an ordinary human file chooser;
- exact Product downloads use an ordinary human save chooser;
- external/new-window/custom-scheme authority remains held;
- exact Product microphone may prompt; remote Browser content does not inherit device/native authority;
- durable Mac app carries the responsible TCC microphone privacy copy.

## Route

```text
source.vexrelay.current-contract/v1
ExactHeadSourceOwnerProof
adapter.vexlocalbridge.native-vexrelay-task-archive/v1
macos-victor

GitHub Actions NOT USED
Local Workspace NOT USED
Victor ZIP carriage NOT USED
Product/application launch HELD
real Home/model effects HELD
merge HELD
```

The native carrier consumes the inner canonical RETURN and the A004 consumer validates/consumes the
outer RETURN locally.

## Merge boundary

A004 SourceOwner PASS cannot create merge authority.

```text
built Mac VexLife application lived run PASS
+ built Windows VexLife application lived run PASS
= required before merge consideration
```

<!-- [VXG RealForever][PR396][PR397][SOURCEOWNER][A004] -->
