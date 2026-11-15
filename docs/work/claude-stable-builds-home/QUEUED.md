# Queued: VexLife-Prod GitHub Pages home with selectable stable Mac builds

`[VXG RealForever]`

Status: **QUEUED** (recorded 2026-11-14 at Victor's request; not started).

## Human intention

A person who is not a developer opens one VexLife-Prod home page, sees the stable versions that are
published, picks one, and runs one script for that version on their Mac. Every instance that
continues the work can see what is published, what is on `main`, and what is accepted, so continuous
work and stable builds stay distinguishable.

## Shape to form (when started)

```text
GitHub Pages site (source-owned, generated from release metadata)
  -> list of stable versions: version, source SHA, date, what changed, model catalog it ships
  -> per version: one copyable Mac command / downloadable .command that runs that version's
     exact bootstrap (packaging/bootstrap/vexlife-prod-bootstrap.command.template) at that SHA
  -> clearly separated: stable (accepted) vs main (continuous) vs drafts
```

## Constraints already known

- PROOF_ROUTE != ORDINARY_PRODUCT_ROUTE: the page publishes the ordinary windowed Setup route only.
- The bootstrap stays source-bound to one exact SHA per version (startup dependency law).
- Mac first; nothing else is promised by the page until it exists.
- A version is "stable" only when Victor accepts it; the page reads that acceptance, it does not
  decide it.
