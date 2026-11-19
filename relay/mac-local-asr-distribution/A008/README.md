# A008 — realization dispatch correction proof

[VXG RealForever]

A007 proved that the bundled Mac whisper.cpp provider bytes themselves were valid. The final #338
lived Mac run then exposed a different relationship failure: the shared whisper provider still
called the generic Home materialization observer directly, bypassing the selected realization.

That meant Mac's new private-service-image-relative provider path was accidentally interpreted
relative to Vex Home.

The correction does **not** add Mac/Windows conditionals:

```text
shared WhisperCpp provider
  -> realization.observeProvider(productHome)

Mac realization
  -> bundled private-service-image provider

Windows realization
  -> inherited Home provider observation
```

A008 proves the corrected exact #384 head with focused compile/tests and SourceOwner review. It does
not launch Product, use the microphone, mutate Home, or retest Windows lived behavior.

On PASS, resume the held final Mac lived regression on the corrected exact #338 candidate.

<!-- [VXG RealForever][PR384][PR386][A008][REALIZATION-DISPATCH] -->
