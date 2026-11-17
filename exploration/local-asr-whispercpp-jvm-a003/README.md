# Local ASR whisper.cpp JVM/JNA seam A003

`[VXG RealForever]`

## Purpose

Pressure the shortest Product-shaped whisper.cpp realization after A002 proved the native runtime and
Whisper Base English model on the current M4 Mac.

Question:

> Can the existing Kotlin/JVM VexLife service call the pinned whisper.cpp native library directly
> through whisper.cpp's own Java/JNA binding over float PCM, without introducing a sibling ASR
> process merely to cross language/runtime boundaries?

This is still exploration. It does not change Product source.

## Why this is the next edge

A002 proved:

~~~text
whisper.cpp native runtime        PASS
Base English exact fixture text   PASS
Metal acceleration                PASS
faster-than-realtime cold run     PASS
~~~

The remaining topology question is:

~~~text
VexLife JVM
-> native whisper.cpp
~~~

versus:

~~~text
VexLife JVM
-> sibling ASR process
-> native whisper.cpp
~~~

The smaller topology wins only if the direct JVM/native binding actually works and remains
controllable.

## Upstream binding evidence

Pinned whisper.cpp source contains Java/JNA bindings that expose:

~~~text
WhisperCpp.initContext(modelPath)
WhisperCpp.fullTranscribe(params, float[])
WhisperCppJnaLibrary.whisper_full(...)
whisper_full_with_state(...)
native abort/progress/new-segment callback fields
~~~

The Java binding uses JNA. Upstream's README does not currently claim modern Apple-silicon Darwin
testing, so this seam must be proved rather than assumed.

## A003 experiment

A003:

1. pins the same whisper.cpp v1.9.5 release commit/tree used in A002;
2. builds the native shared libraries with Metal;
3. pins the same Base English model and same 6.625-second fixture;
4. compiles whisper.cpp's exact pinned Java binding plus one small probe through an isolated temporary
   Gradle/JNA environment;
5. points JNA at the locally-built native libraries;
6. reads the WAV into normalized float PCM in JVM;
7. transcribes through `WhisperCpp.fullTranscribe(...)`;
8. records timing/text/runtime/dependency identity;
9. removes the entire temporary environment.

No browser/microphone is used yet.

## Architecture boundary

A003 does **not** introduce:

~~~text
LocalAsrProvider production interface
VexPartner
generic VexService
VexExecution
Product Settings/configuration
ASR model lifecycle
Conversation integration
~~~

A PASS earns direct JVM/native realization as a serious Product topology candidate.

A failure is equally useful: it may show that the current upstream Java binding is stale or unsafe on
modern Apple Silicon, which would favor a bounded process/native boundary or another provider.

<!-- [VXG RealForever][VEXLIFE-PROD][LOCAL-ASR][WHISPERCPP][JVM][A003] -->
