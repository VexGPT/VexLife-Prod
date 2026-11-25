# Support External API — Partner Models current state

`[VXG RealForever]`

## Purpose

Compose the source-owned Partner inventory with durable connection settings and protected-host credential presence so Settings can have truthful Partner model rows immediately after restart without a provider API call.

## Inputs

- durable non-secret Partner connection settings;
- protected-host contains() observation;
- current support qualification projections supplied by their rightful owner;
- optional exact Partner targeted/serving coordinates.

## Output

- configured provider currentness including NOT_CONFIGURED / PRESENT / DANGLING_REF;
- one initial connection projection per source-owned profile;
- catalog-bound Partner model offering rows.

## Restart meaning

No configured credential -> NEEDS_CREDENTIAL.
Configured + protected secret present -> PRESENT_UNVERIFIED -> CHECKING.
Configured ref + missing protected secret -> DANGLING_REF -> NEEDS_CREDENTIAL.

Account, service and model access remain UNKNOWN until provider observation refreshes them. Historical success is not replayed.

## Proof/support meaning

This projection does not invent support proof. The caller supplies the current VexLifePartnerSupportQualification projections. HELD support therefore remains HELD even when a credential is present.

## Effect boundary

The projection never stores/deletes credentials, never performs provider network calls, never selects/uses a model, and never persists target/serving state.

## Downstream

This is the first source that can lawfully supply Partner rows to the real Models service without fake connection defaults. Integration into VexLifeModels.kt still requires a current-writer check and a decision about where the runtime obtains the protected-host observer/support projections.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][A1][PARTNER-MODELS-CURRENT-STATE] -->