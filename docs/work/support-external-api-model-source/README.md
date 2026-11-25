# Support External API — Models Local / Partner projection

`[VXG RealForever]`

## Human outcome

Let Settings → Models present one simple source filter:

```text
Local
Partner
```

without creating a second persisted model-selection truth.

## Source rule

```text
exact targeted model row
-> targeted source is derived

exact serving model row
-> serving source is derived

UI Local / Partner filter
-> presentation only
-> not saved as an independent setting
```

Permanent:

```text
SOURCE FILTER != MODEL CHOICE
SELECTED MODEL -> IMPLIES SOURCE
DO NOT PERSIST SOURCE + MODEL AS TWO WRITABLE TRUTHS
```

## Projection

`VexLifeModelSourceProjection` accepts two already-owned inventories:

```text
local offerings
  handle
  modelRef
  targeted
  serving

Partner offerings
  handle
  providerRef
  modelRef
  targeted
  serving
  exact connection projection
  exact support qualification
```

and emits one row list with:

```text
sourceKind
providerRef when Partner
targeted / serving
Partner connection standing
Partner support standing
live-validation disclosure requirement
```

The later renderer may filter those rows by source kind.

## What this does not own

No:
- Select / Use Control;
- model activation;
- model download/remove;
- local runtime configuration;
- provider connection mutation;
- credential action;
- provider model discovery;
- provider support authority;
- human-visible localized words;
- persisted source preference.

Current `VexLifeModelsFamily` remains the human model-control owner for its admitted model rows.
Future Partner model-control integration must consume the rightful Partner connection/qualification
owners rather than moving those meanings into this projection.

## Exact relationship checks

Partner rows are rejected unless:
- connection providerRef/modelRef exactly match the row;
- support providerRef/modelRef exactly match the row.

The projection also rejects:
- duplicate handles across Local/Partner rows;
- multiple targeted rows;
- multiple serving rows.

This prevents the source filter from papering over an inconsistent model-selection state.

## Support disclosure

The Partner support projection passes through:

```text
CONTRACT_PROVEN_LIVE_UNQUALIFIED
  -> liveValidationDisclosureRequired=true

LIVE_QUALIFIED
  -> liveValidationDisclosureRequired=false
```

The projection does not author the eventual information-icon copy.

## Dependency posture

This candidate is formed on the current zero-cost integration candidate #559.

It does not touch Warm Boot #518 source.

Future integration with the existing JVM `VexLifeModels.projection()` should:
1. adapt current Heretic rows into `LocalOffering`;
2. adapt source-supported Partner model rows into `PartnerOffering`;
3. preserve the existing exact target/serving owner;
4. let presentation filter the combined projection.

Do not modify `VexLifeModelsFamily` merely to make the source filter exist.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][A1][MODEL-SOURCE-PROJECTION] -->
