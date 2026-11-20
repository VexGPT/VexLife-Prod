# VexLife Local Artifacts — Mac build / lived file-reach proof companion

[VXG RealForever]

Product/source authority remains #414 + proof/lived correction child #433. This TEMP RELAY branch
carries Mac proof and candidate delivery only.

## Current lived finding

Victor attached `Victor_Gong_Resume_2026.docx`. Product staging and request binding worked; Vex
saw the file name and readable-byte metadata and reasoned that it should use the attachment capability,
but it diverted into Conversation Source Return instead of reading the current-request file.

Observed boundary:

```text
file staging                                  PASS
request attachment binding                   PASS
artifact manifest reaches provider           PASS
model notices DOCX                           PASS
model intends attachment read                PASS
actual attachment capability exchange        NOT OBSERVED
Conversation Source Return used instead      OBSERVED
```

## Source correction

```text
#433
head 3c8db58e2caee71997b6d8e1adeae89b5956eb56
tree 3366982303c6e2de1b805ace2f45860482345bc9
```

The correction:
- projects the exact request-local `resourceRef` in the artifact manifest;
- tells Vex to use the attachments capability's semantic `read` action, not one provider spelling;
- keeps file bytes out of the prompt until bounded read;
- states that Conversation Source Return cannot retrieve current-request artifact bytes;
- pins the real provider-native `attachments_read` schema in the provider-turn proof.

## A006

Fresh build against the corrected source:

```text
jvmTest + checkVexLifeStrings
-> Screen Lens currentness
-> archiveVexLifeProdMacApplication
-> source/app/DMG/receipt verification
-> canonical RETURN
-> obvious human DMG copied directly to:
   ~/Downloads/VexLife-Local-Artifacts-3c8db58e.dmg
```

Boundary:

```text
SOURCE/BUILD PASS != LIVED FILE-READ PASS
ATTACHMENT MANIFEST != FILE CONTENT IN PROMPT
CONVERSATION SOURCE RETURN != CURRENT REQUEST ARTIFACT READ
READY / MERGE HELD
```

<!-- [VXG RealForever][LOCAL-ARTIFACTS][A006][LIVED-FILE-REACH][DO-NOT-MERGE] -->
