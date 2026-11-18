# Temporary host proof for PR #256 — Progressive VoiceInput A001

[VXG RealForever]

Source owner: PR #256.

Exact source candidate:

```text
head a363b216486f8f857dda4913176a9f9692135e3b
base 0c17d4e445bb87439bcad6200ae6e622b3284ebe
```

## Human outcome under proof

```text
mic starts
-> provisional words appear before Stop
-> short thinking pause refines but does not automatically finalize an unfinished thought
-> later context may revise the provisional tail
-> a stable sentence-like pause can settle text
-> settled audio is released
-> Stop performs the final reconciliation
-> Composer remains editable
-> Send is not invoked
```

A001 uses the existing qualified whisper.cpp Base English provider. Progressive passes are an
experience pressure test over the proven process provider; they are not yet a claim that
CLI-per-partial is the final efficiency topology.

The Product service now records only content-free pass identity:

```text
dictationPass=partial
dictationPass=final
```

No microphone audio or transcript text is written to VexLogger or returned by the carrier.

## One human action

Run exactly:

```bash
/bin/bash <(gh api --method GET -H 'Accept: application/vnd.github.raw+json' -f ref=aaa0043809399668cf4b19cbba3028e47e6977a3 'repos/VexGPT/VexLife-Prod/contents/relay/voiceinput-progressive/A001/RUN-MAC.command')
```

The terminal will open VexLife and give the lived speaking sequence. It waits for both an exact-source
partial pass and final pass.

Expected result:

```text
~/Downloads/RETURN-VEXLIFE-VOICEINPUT-PROGRESSIVE-A001.zip
```

This PR is proof carriage only; close unmerged after its evidence is consumed.

<!-- [VXG RealForever][PR256][PROGRESSIVE-DICTATION][TEMP-PROOF][A001] -->
