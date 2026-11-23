# Support External API — durable Partner connection settings

`[VXG RealForever]`

## Human outcome

After restart, VexLife should remember which protected credential coordinate belongs to each connected Partner provider without ever storing the provider secret in the Vex Home.

## Durable contract

Path: runtime/vexlife-prod/settings/partner-connections.json

Stored meaning only:
- providerRef;
- opaque credentialRef;
- admitted non-secret account scope, currently Anthropic workspaceId when needed.

Never stored:
- API key/access-token value;
- Authorization or x-api-key value;
- protected-store blob;
- provider response body;
- account billing state;
- model availability;
- live qualification.

## Existing repository owner reused

The JVM store uses the existing VexDurableContractFile boundary, matching current VexLife settings practice: owned-root confinement, no symlink file, strict serialization, private permissions where supported, and atomic replacement.

Absent file means no configured Partner connections.

## Canonical shape

VexLifePartnerConnectionSettings.Value keeps at most one current entry per built-in provider and requires canonical provider order.

Each Entry validates the provider, bounded opaque credentialRef, and same-provider account scope. Provider-specific scope rules remain with VexLifePartnerAccountScope.

## Secret rejection

The schema contains no secret field. VexContractJson.Strict rejects unknown fields, so a durable file containing an injected apiKey/secret member is not accepted as Partner connection settings.

## Ownership boundary

This store does not create/read/delete OS secrets. It does not perform provider network effects or decide credential/account/model standing. The protected host remains the secret owner.

## Remaining convergence problem

Secret custody and durable coordinate persistence are two independently-owned effects. A later connection-configuration/convergence owner must coordinate them so a newly stored protected secret is retired if durable settings cannot be committed; replacement does not leave two current refs; removal does not silently leave an orphaned protected credential; and a durable ref whose host secret is missing projects truthful MISSING/held state rather than being repaired by invention.

That convergence work is not hidden inside this store.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][A2][CONNECTION-SETTINGS] -->