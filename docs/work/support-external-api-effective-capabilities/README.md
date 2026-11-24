# Support External API — Partner effective model capabilities

`[VXG RealForever]`

## Human question

For one exact Partner provider/model relationship, which already-owned Vex abilities are actually available now?

## No provider/model claims are created here

This candidate intentionally does not say that any named OpenAI, Anthropic, or Kimi model supports Vex reach, provider-native tools, reasoning-effort control, or visual input.

Those facts require a separate source-owned Partner model/profile relationship. This projection only joins supplied profile evidence with current connection truth.

Permanent:

MODEL NAME != CAPABILITY
PROVIDER MARKETING != PRODUCT QUALIFICATION
REPRESENTED != QUALIFIED
CONNECTION READY != CAPABILITY QUALIFIED
CAPABILITY PROJECTION != CAPABILITY AUTHORITY

## Input

One exact ProfileEvidence carries providerRef, modelRef, profileRef, and for each capability: represented + optional qualificationRef.

A capability that is not represented may not carry a qualification reference. The qualification reference is intentionally opaque here; its rightful proof owner decides what evidence establishes the Product relationship.

## Current connection join

Capability availability requires all current Partner connection dimensions: credential ACCEPTED, account USABLE, service READY, modelAccess AVAILABLE.

Then:
- not represented -> NOT_APPLICABLE;
- represented but no Product qualification -> HELD;
- represented + qualified but connection not currently ready -> HELD;
- represented + qualified + connection ready -> AVAILABLE.

Invocation history is not used to overwrite the independent current dimensions. Therefore LAST_INVOCATION_FAILED can coexist with an AVAILABLE represented/qualified capability when credential/account/service/model currentness is still usable; the invocation failure remains visible through the connection standing.

## Contract-proven before live-provider proof

This projection deliberately does not depend on final Support Qualification standing.

That avoids a circular proof dependency:

A4/P2 relationship proof -> may produce vexRelationshipConformanceProofRef -> then Support Qualification can become CONTRACT_PROVEN_LIVE_UNQUALIFIED.

A final support standing therefore does not need to exist before A4 can prove the relationship that helps form it.

## Visual input

Visual input keeps the same law already present on the local capability owner:

VISUAL MODEL CANDIDATE != QUALIFIED VISUAL INPUT

A Partner profile may say visual input is represented while the Product qualification ref remains absent. The effective standing is then HELD.

## Dependency posture

This candidate is formed on #559 and does not touch Warm Boot #518.

It reuses the existing VexLifeEffectiveModelCapabilities.Standing vocabulary so Local and Partner rows do not invent two meanings for AVAILABLE / HELD / NOT_APPLICABLE.

A later source-owned Partner model-profile catalog is still required before any concrete provider/model capability claim may appear in Product UI.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][A4][EFFECTIVE-CAPABILITIES] -->