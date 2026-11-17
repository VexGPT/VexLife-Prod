# Local ASR sherpa-onnx spike A001

`[VXG RealForever]`

## Purpose

Pressure one exact local-ASR realization without changing Product source semantics.

This exploration answers only:

> Can the released sherpa-onnx JVM + macOS ARM64 runtime decode a pinned small English streaming
> model locally on Victor's Mac, with inspectable latency, memory, partial/final transcript and
> artifact identity?

It does not prove Product integration, browser PCM ingress, provider selection UI, model distribution
or final dictation quality.

## Architecture boundary

~~~text
VexLifeVoiceInputFamily     UNCHANGED
VexLife Product service     UNCHANGED
Vex Home                    UNCHANGED
Heretic runtime / venv      UNCHANGED
browser SpeechRecognition   NOT USED

disposable host workspace
-> released sherpa JVM jar
-> released macOS ARM64 native jar
-> released small English streaming model
-> upstream deterministic WAV
-> streaming JVM recognizer
-> bounded evidence RETURN
~~~

No `VexPartner`, `VexService`, `VexExecution` or provider framework is created by this
experiment.

## Exact provider inputs

~~~text
sherpa-onnx source witness
aa273e8029af4e0aabac7d7455a177c5b493aa34

runtime release
v2.0.1

JVM API jar
sherpa-onnx-jvm-2.0.1.jar
sha256 8320e2457b6e38dd624fb619d3552e11ef78ffeb394622056126b16f2fd99d12

macOS ARM64 native jar
sherpa-onnx-native-lib-osx-aarch64-2.0.1.jar
sha256 718a4ea4799341146ef9a0cc15daa70e6622ae4a16911952604035f396703499

streaming English model
sherpa-onnx-streaming-zipformer-en-20M-2023-02-17.tar.bz2
expected sha256 9c559283e8498d3fe95913c79ca1cb454bb26281ac2b102b41306c7d752765d9
~~~

The model's upstream lineage is the Apache-2.0
`desh2608/icefall-asr-librispeech-pruned-transducer-stateless7-streaming-small` checkpoint.
The sherpa conversion archive is used only for this qualification run.

## What the probe exercises

~~~text
WaveReader(upstream test_wavs/0.wav)
-> 100 ms PCM chunks
-> OnlineStream.acceptWaveform(...)
-> OnlineRecognizer.isReady/decode
-> changing partial results
-> endpoint observation/reset
-> inputFinished
-> final transcript
~~~

The exact same `acceptWaveform(..., 16000)` relationship is the seam a later Product spike may feed
from browser/Web Audio PCM.

## Human effect

Run one command from the PR/carrier. The probe downloads about 140 MB of temporary provider/model
artifacts into a temporary directory beneath Downloads, verifies pinned digests, executes inference,
forms:

~~~text
~/Downloads/RETURN-VEXLIFE-LOCAL-ASR-SHERPA-A001.zip
~~~

and removes the temporary downloaded artifacts afterward.

The RETURN contains technical evidence only. It does not contain human microphone audio.

<!-- [VXG RealForever][VEXLIFE-PROD][LOCAL-ASR][SHERPA][A001] -->
