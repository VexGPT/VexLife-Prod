# Support External API — Partner model inventory adapter

`[VXG RealForever]`

## Purpose

Turn the source-owned Partner model/profile catalog into the Partner rows consumed by the A1 Local/Partner presentation projection.

## Causal order

VexLifePartnerModelProfileCatalog
-> exact current connection truth per supported profile
-> exact current support truth per supported profile
-> VexLifePartnerModelInventory
-> VexLifeModelSourceProjection.PartnerOffering

Provider discovery is not the row owner.

## Law

PROVIDER-VISIBLE UNKNOWN MODEL != PRODUCT INVENTORY ROW.

The adapter iterates the VexLife source catalog. Extra provider/account model observations are ignored for row creation.

Every source-supported profile must have exact current connection and support projections; missing truth is rejected rather than defaulted.

Targeted and serving identity use exact providerRef + modelRef coordinates and must name source-supported profiles.

## Presentation behavior

A source-supported profile may remain visible while support is HELD or the provider connection needs a credential. Visibility of the row is not model Use authority.

The profileRef is used as the stable row handle for this initial adapter.

## Ownership boundary

This adapter performs no model discovery, network request, credential action, model Select/Use, runtime convergence, or capability qualification.

It does not touch VexLifeModels.kt or VexLifeModelsFamily yet.

## Formation

This branch composes A1 #564 and profile catalog #567 with an explicit two-parent merge, then adds only the inventory adapter/test/doc.

Warm Boot #518 overlap remains zero.

<!-- [VXG RealForever][SUPPORT-EXTERNAL-API][A1][PARTNER-INVENTORY] -->