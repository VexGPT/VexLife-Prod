# Support External API — initial Partner model/profile catalog

`[VXG RealForever]`

## Purpose

Give VexLife a small source-owned set of Partner model representations it knows how to integrate, without treating a provider model-list response as Product support authority.

Initial baseline:
- OpenAI: gpt-6.2-sol
- Anthropic: claude-sonnet-5-5
- Kimi Open Platform: kimi-k3

These are representation candidates only. Account access, billing, current provider visibility, deterministic Product qualification, and live qualification remain separately owned.

## Current public source basis

OpenAI's current API model docs identify gpt-6.2-sol as a current flagship/API model, with current latest-model vision support, function tools, and configurable reasoning effort.

Anthropic's current model overview/API primer identify claude-sonnet-5-5 and state current models support vision and tool use; the primer documents adaptive thinking/effort for Sonnet 5.5.

Kimi's current API model-selection/troubleshooting docs identify kimi-k3, native visual understanding, reasoning_effort control, and the standard tool_calls / tool_call_id client-tool loop.

These public references must be re-grounded before changing the catalog because provider model lineups are external/current truth.

## Permanent distinctions

PROVIDER MODEL LIST != VEXLIFE SUPPORT CATALOG
PUBLIC MODEL SUPPORT != THIS ACCOUNT CAN ACCESS
REPRESENTATION SUPPORT != PRODUCT QUALIFICATION
PRODUCT QUALIFICATION != LIVE PROVIDER QUALIFICATION

## Narrow initial scope

The first catalog intentionally carries one baseline model per provider. It is enough to prove the full Vex relationship before broadening model choice.

Provider-hosted tools are not admitted here. clientToolsOnly=true means tools continue through the Vex Capability Atlas/Bridge.

## Relationship to A4

This catalog supplies representation facts only. A4 #566 joins those facts with independent Product qualification refs and current Partner connection truth to derive AVAILABLE / HELD / NOT_APPLICABLE.

No capability qualification ref is minted by this catalog.

## Relationship to A1

A future Partner model inventory adapter may create a PartnerOffering only when:
- the model exists in this source-owned catalog;
- the exact account connection says model access is available;
- current support/qualification standing permits the Product posture.

An arbitrary model string returned by GET /v1/models must not dynamically manufacture a VexLife profile.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][MODEL-PROFILE-CATALOG] -->