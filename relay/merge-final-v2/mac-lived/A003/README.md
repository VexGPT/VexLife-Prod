# Final #338 Mac VoiceInput closure — A003

[VXG RealForever]

A003 is deliberately **one end-to-end run**, not another symptom-by-symptom human loop.

The A002 debug export proved the A008 interface correction advanced Mac from a disabled microphone to
a truthful `READY -> REQUESTING_MICROPHONE -> permission GRANTED` path. It also proved the circuit
stopped before `VOICE_INPUT_AUDIO_CAPTURE_STARTED`: the browser was waiting on
`POST /api/v1/local-asr/prepare`.

History then exposed a second cross-platform leak. The last lived-qualified Mac persistent
whisper-server inherited the Product service working directory and supplied only
`DYLD_LIBRARY_PATH`. Windows commit `6929c3b...` later introduced executable-parent cwd for its
native DLL image; #382 copied that behavior into the Mac realization even though Mac never earned it.

The corrected relationship is implementation-owned:

```text
shared whisper runtime
  -> realization.configureProcess(...)

Mac
  -> inherit Product-service cwd
  -> DYLD_LIBRARY_PATH = bundled provider/bin

Windows
  -> cwd = released provider/Release
  -> DLL layout unchanged
```

No platform conditional is added to shared code.

A003 refuses to ask Victor for microphone interaction until the machine has already proved:
1. focused Kotlin compile/tests on the exact candidate;
2. exact DMG build/verification;
3. exact installed Product source/current Home;
4. selected Local-ASR is READY;
5. `POST /api/v1/local-asr/prepare` returns HTTP 204.

If step 5 fails or times out, A003 automatically packages bounded service logs, chronology, endpoint
state and whisper-server process evidence. Victor does not need to manually diagnose another
half-started microphone state.

Only after machine preparation succeeds does the runner ask for one lived mic -> editable Composer ->
human Send -> Vex reply pass, followed by normal quit/reopen and a second typed turn.

A001 and A002 remain historical evidence and are not rewritten.

<!-- [VXG RealForever][PR338][PR384][PR390][MAC-LIVED-A003][MACHINE-GATED] -->
