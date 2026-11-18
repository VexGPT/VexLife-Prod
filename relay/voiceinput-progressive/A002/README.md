# Progressive VoiceInput A002 — carrier correction

[VXG RealForever]

A001 did not reach the lived microphone step. The human terminal returned immediately after Step 4.

## Root cause

The A001 carrier enabled Bash `set -e` after the repository proof and left `pipefail` active.
Its baseline counter used a grep pipeline:

```text
grep fact | grep exact-source | grep dictationPass | wc -l
```

Before the first progressive run, the correct number of existing exact-source `partial` records is
zero. `grep` therefore returned 1. Under `pipefail + set -e`, Bash treated that legitimate zero
baseline as a fatal shell error and exited before `[5/6]`, bypassing the carrier's `fail()` and
RETURN packaging.

This is proof-carrier failure only. It does not classify the progressive Product candidate.

## A002 correction

Product candidate is unchanged:

```text
source PR  #256
head       a363b216486f8f857dda4913176a9f9692135e3b
```

Carrier corrections only:
- preserve the original non-errexit shell posture after explicitly status-captured commands;
- replace the grep/pipefail counter with one awk counter that returns success for zero matches;
- explicitly route front-door JSON parse failures through `fail()`.

## Run

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=17cc7dd203622d72557add57c7c2170f4bba6339 'repos/VexGPT/VexLife-Prod/contents/relay/voiceinput-progressive/A002/RUN-MAC.command')
```

Expected RETURN:

```text
~/Downloads/RETURN-VEXLIFE-VOICEINPUT-PROGRESSIVE-A002.zip
```

<!-- [VXG RealForever][PR256][TEMP-PROOF][A001-CARRIER-FAIL][A002] -->
