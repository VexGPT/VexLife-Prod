# Support External API — Partner connection configuration convergence

`[VXG RealForever]`

## Problem

Protected OS credential storage and durable Home connection settings are two independently-owned effects. A crash between them must not force VexLife to guess whether a secret exists, silently orphan an old credential, or replay a secret the Product no longer has.

## One durable transition surface

partner-connections.json now carries transition intent in the same settings contract. No second journal is added.

Transition kinds:
- STAGE_CONNECT;
- STAGE_REPLACE;
- CLEANUP_REPLACED;
- REMOVE.

## Connect

1. Allocate an opaque credentialRef without effect.
2. Persist STAGE_CONNECT with provider/ref/account scope.
3. Store the protected secret at that exact ref.
4. Commit the current Entry and clear transition.

If the process dies after step 2, recovery observes whether the protected secret exists. Absent -> roll back to unconfigured. Present -> finalize the durable entry without needing the secret again.

## Replace

1. Keep old Entry current.
2. Persist STAGE_REPLACE(newRef).
3. Store new protected secret.
4. Commit new Entry + CLEANUP_REPLACED(oldRef).
5. Retire old protected secret.
6. Clear transition.

A crash never loses both coordinates. Cleanup debt remains durable until old-secret retirement is proven.

## Remove

1. Keep current Entry.
2. Persist REMOVE(currentRef).
3. Retire protected secret.
4. Remove Entry + transition.

If deletion fails, REMOVE remains durable and recovery retries exact retirement.

## Recovery

Recovery reads only durable transition state plus protected-host presence. It never asks the person to re-enter a secret when the staged protected secret already exists.

STAGE_CONNECT absent -> roll back.
STAGE_CONNECT present -> finalize connection.
STAGE_REPLACE absent -> keep old current and roll back.
STAGE_REPLACE present -> switch current and clean old.
CLEANUP_REPLACED -> retire old refs and clear.
REMOVE -> retire current ref and remove entry.

## Secret lifetime

connect()/replace() zero the caller-provided CharArray in a finally block. Custody still copies into the protected-host call and zeroes its temporary copy.

## Ownership boundary

This coordinator owns convergence only. It does not authenticate with providers, observe billing/model access, select/use a model, or perform inference.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][A2][CONNECTION-CONVERGENCE] -->