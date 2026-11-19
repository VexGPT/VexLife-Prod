# PR #396 WebToApp built-application lived proof companion

[VXG RealForever]

This is a single **DO-NOT-MERGE** source-controlled VexRelay/VexLocalBridge companion for the final
desktop application-lived gate.

It is stacked on the exact WebToApp application-shell candidate but owns no Product semantics:

```text
VexLife-Prod #396
base fdb9f77bbd9895d0ab6932f669091b46ea813e9b
head d13e8e4a774aea60362491420ffeee2c07f91d1c
tree 98ba3219e49f722272c976171cb736ed19ae9ac9
```

## Current state

```text
HELD_PENDING_PREREQUISITES
```

Do not execute either platform attempt until all exact prerequisites are consumed PASS/current:

```text
#397 A005
  -> exact #396 SourceOwner proof consumed PASS

SDK #1960
  -> exact #1959 application-lived host-effect SourceOwner proof consumed PASS

#1959 candidate
  head 926a2dacc06bd2e307b9adffc6dc406dfe52babc
  tree 7174e05fc62faaf063c8f244f26202c0946754d6

#1956 lifecycle membrane remains the predecessor relationship
```

No merge of #391/#396/#397/#1959/#1960 or this companion is permitted merely to satisfy this proof.
Victor's stronger gate remains authoritative.

## One application meaning

Both platforms prove the same human/Product relationship:

```text
build exact task-owned VexLife application
-> launch that exact application

human opens Product presentation
-> presentation arrives
-> human performs one ordinary Vex turn
-> proof observes SEND_ACCEPTED
                  PROVIDER_INVOCATION_REQUESTED
                  TURN_COMPLETED
   from VexLogger without reading Conversation content

human closes Product presentation
-> Product service/runtime remain current
-> Close != Quit

launch/open Product again
-> human explicitly chooses Quit VexLife
-> Product service/runtime release through their rightful owner
-> same Home remains

launch VexLife again
-> same Home is current
-> fresh Product/runtime can become current
-> next ordinary Vex turn can complete
```

The proof payload never issues a Send, clicks UI, drives Finder/Explorer, or automates system UI.
Human Product Controls remain human/Product authority. The payload observes only non-content Product
currentness, exact process/lifetime custody, and VexLogger chronology facts.

## Platform realization

Mac:

```text
task-owned archiveVexLifeProdMacApplication
-> exact formed VexLife.app
-> read formed Info.plist CFBundleExecutable
-> launch that executable directly under the VLB task process group
-> nested VexLifeProduct.app remains a descendant
-> launchd/receipt ownership remains with Product lifetime owners
```

Windows:

```text
task-owned FORM-WINDOWS-PREVIEW.ps1
-> exact formed app image
-> receipt binds entrypoint VexLife.exe
-> launch that exact executable directly inside the VLB Windows Job Object
-> WebView2 helper remains a descendant
-> Product/runtime custody remains with the Windows Product owner
```

```text
DIRECT TASK-OWNED EXECUTABLE LAUNCH
!= Finder / LaunchServices automation
!= Explorer automation
!= installation
```

## Effect boundary

Every lived task requires all exact grants:

```text
effect.vexlocalbridge.vexlife-prod.product-lifecycle-proof/v1
effect.vexlocalbridge.vexlife-prod.application-lived-proof/v1
effect.vexlocalbridge.vexlife-prod.build-dependency-fetch/v1
```

The build-fetch effect is separate and only covers attempt-local build dependency retrieval.

Still held:

```text
/Applications or Program Files installation
system UI automation
screen capture
broad process kill
arbitrary PID termination
proof-harness Conversation mutation
proof-harness Memory mutation
Home recreate/delete
model acquisition
model switch merely for proof
publication
READY
merge
```

Existing default Home only remains the first qualified lived boundary. Custom Home is HELD rather
than generalized into filesystem authority.

## Real-turn evidence

The harness records a chronology baseline and then observes only VexLogger records formed after it.

A completed human turn is:

```text
SEND_ACCEPTED
-> PROVIDER_INVOCATION_REQUESTED
-> TURN_COMPLETED
```

No message text, prompt, response, reasoning, or Conversation record is copied into proof evidence.

## Merge gate

```text
Mac built VexLife application lived run PASS
AND
Windows built VexLife application lived run PASS
= prerequisite for merge consideration

one-platform PASS != merge ready
source PASS != app-lived PASS
carrier PASS != app-lived PASS
```

<!-- [VXG RealForever][PR396][APPLICATION-LIVED][DO-NOT-MERGE] -->
