# Local ASR whisper.cpp comparator A002

`[VXG RealForever]`

## Purpose

Compare a second local-ASR realization against sherpa A002 on the **same 6.625-second deterministic
LibriSpeech WAV**, before any Product provider interface is accepted.

This comparator asks:

> Does a pinned whisper.cpp release with the English Base model materially improve transcript
> quality while retaining acceptable local latency, memory and packaging characteristics?

It does not modify Product behavior.

## Exact source/runtime

~~~text
whisper.cpp release
v1.9.5

release commit
d1be6fde11ac6e0407606b4e42fe72d34add8037

release tree
6fe2755682242a5c01aba65a22b3c0305d747841

runtime license
MIT

model
ggml-base.en.bin

model bytes
147,964,211

model sha256
a03779c86df3323075f5e796cb2ce5029f00ec8869eee3fdfb897afe36c6d002
~~~

The English Base Whisper model is used as a practical quality comparator, not selected Product
configuration.

## Same fixture as sherpa A002

The probe obtains the exact WAV from the already-pinned sherpa model archive:

~~~text
archive
sherpa-onnx-streaming-zipformer-en-20M-2023-02-17.tar.bz2

sha256
9c559283e8498d3fe95913c79ca1cb454bb26281ac2b102b41306c7d752765d9

fixture member sha256 from sherpa A002
6bc58a4efdf20daac252b6b1502632601a71efe0308f6757dc1eda34891a7e4f
~~~

Canonical reference:

~~~text
AFTER EARLY NIGHTFALL THE YELLOW LAMPS WOULD LIGHT UP HERE AND THERE
THE SQUALID QUARTER OF THE BROTHELS
~~~

Sherpa A002 20M result:

~~~text
THE YELLOW LAMPS WOULD LIGHT UP HERE AND THERE
THE SQUALID QUARTER OF THE BRAFFLELS
~~~

The comparator does not claim corpus WER from one sentence.

## Build boundary

If CMake is already installed, the probe uses it.

Otherwise it downloads a temporary pinned Kitware CMake 4.5.0 macOS universal archive:

~~~text
sha256
a645d69fd21e521759cc0c64e7c60462d43faf0c7214ac05f75c80f7c5037dde
~~~

Nothing is installed globally.

The pinned whisper.cpp source is cloned into a temporary Downloads directory, built for the
`whisper-cli` target, run, and removed afterward.

## Evidence

Expected RETURN:

~~~text
~/Downloads/RETURN-VEXLIFE-LOCAL-ASR-WHISPERCPP-A002.zip
~~~

Evidence includes:
- host/tool identity;
- exact source commit/tree;
- CMake/build output;
- model + fixture hashes;
- whisper.cpp stdout/stderr/timings;
- macOS `time -l` memory evidence;
- normalized final transcript.

No microphone is captured and no Product/Home/Heretic state is modified.

<!-- [VXG RealForever][VEXLIFE-PROD][LOCAL-ASR][WHISPERCPP][A002] -->

## A002 carrier correction

A001 never reached whisper.cpp because the temporary CMake tarball was assumed to extract directly to `$CMAKE_ROOT/CMake.app`. A002 resolves the exact extracted `*/CMake.app/Contents/bin/cmake` executable from the temporary root instead. No provider/model/probe logic changes.
