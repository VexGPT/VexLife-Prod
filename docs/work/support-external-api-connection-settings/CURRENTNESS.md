# Support External API — configured Partner currentness

`[VXG RealForever]`

## Purpose

After restart, join the Home's non-secret Partner connection coordinates with protected-host credential presence without reading or repairing the secret.

## Currentness states

NOT_CONFIGURED:
- no durable settings entry;
- credential standing MISSING.

PRESENT:
- durable providerRef/credentialRef exists;
- protected host says the exact coordinate exists;
- credential standing PRESENT_UNVERIFIED.

DANGLING_REF:
- durable providerRef/credentialRef exists;
- protected host says the secret is absent;
- credential standing MISSING;
- credentialRef remains visible as recovery/diagnostic evidence.

DANGLING_REF is not automatically deleted or re-created.

## Restart law

Protected secret presence does not prove provider authentication, account usability, service readiness, model access, or invocation qualification.

The initial per-model connection projection therefore starts account/service/model access as UNKNOWN and invocation as NEVER_QUALIFIED. A present credential produces CHECKING; missing/dangling produces NEEDS_CREDENTIAL.

Historical successful provider observations are not replayed as current truth after restart.

## Effect boundary

The observer uses only ProtectedHostStore.contains(). It never calls store() or delete(), never reads secret bytes, and never performs a provider network request.

## Downstream

This currentness source can supply truthful initial connection rows to the catalog-bound Partner inventory. Provider verification/model discovery can later replace UNKNOWN dimensions with exact observations.

Transactional connection add/replace/remove convergence remains a separate effect owner.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][A2][CONFIGURED-CURRENTNESS] -->