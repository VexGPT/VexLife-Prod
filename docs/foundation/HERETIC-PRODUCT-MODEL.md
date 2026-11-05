# Heretic product model package

`[VXG RealForever]`

## Human question

Can the fresh VexLife product answer **what local Heretic model it is built to use** from one
compiler-visible package, without making setup scripts, runtime processes, or presentation code
alternate owners of that identity?

This foundation ports the already-earned Heretic product identity from current VexLife into:

~~~text
vexlife.model.heretic
  HereticProductModel
    CurrentIdentity
    CertifiedArtifactMembers
    HomeIdentity
    Configuration
    ConfigurationState
~~~

## Porting rule

This is a faithful relationship port, not an implementation transplant.

Current VexLife spreads the relationship across:

~~~text
blueprint/model-bundle-registry.json
blueprint/artifact-registry.json
src/core/heretic-runtime-link.mjs
src/core/heretic-runtime-establishment.mjs
src/core/heretic-persistent-runtime-owner.mjs
scripts/establish-heretic-runtime.mjs
~~~

Those sources currently answer several different questions. The fresh package sorts the **stable
product facts** into one semantic/catalog owner while keeping runtime effects outside it.

~~~text
MODEL IDENTITY / CERTIFIED ARTIFACT COMPOSITION
  -> vexlife.model.heretic

ARTIFACT ACQUISITION
PYTHON / MLX ENVIRONMENT CREATION
PROCESS START / STOP
LAUNCHD / PLATFORM OWNERSHIP
HEALTH + MODEL + CHAT QUALIFICATION
  -> separate platform/runtime realization
~~~

The package is deliberately effect-inert.

## Exact current product identity ported

~~~text
model bundle
  model-bundle.vexlife.heretic.qwen3.8-27b.mlx4.329658ee8f27.001

model
  model.devex.heretic.qwen3.8-27b.mlx4.329658ee8f27

profile
  model-profile.devex.qwen38-27b-heretic-mlx4.certified.20260928A

artifact set
  artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2

content-set SHA-256
  329658ee8f270056bc29d8a6cfd6e9f1b7d9d14c659452c9e4dd46fc7c78d780

certified artifact members
  11

certified bytes
  15,153,242,506

runtime adapter
  adapter.runtime.mlx.macos-victor.001

runtime class
  MLX_LM_DIRECT_RUNTIME

transport
  MLX_LM_OPENAI_NUMERIC_LOOPBACK

current VexLife product host profile
  profile.repository.vexlife.macos-v1

Python
  3.14.7

mlx-lm
  0.32.0

endpoint
  http://127.0.0.1:8765

request model
  default_model
~~~

The historical runtime-link source also recognizes an older SDK host-profile identity for legacy
receipt compatibility. The fresh package does **not** collapse that historical identity into the
current product identity.

## Configuration relationship

Current VexLife's product configuration is preserved exactly in meaning:

~~~text
Heretic establishment pending
  HERETIC_ESTABLISHMENT_PENDING

        ↓ exact qualified runtime receipt

bound Heretic product model
  BOUND_HERETIC_PRODUCT_MODEL
~~~

Both states remain scoped to:

~~~text
Vex Home identity
+ current Companion lineage
+ exact Heretic product identity
~~~

And neither configuration state grants these effects:

~~~text
automaticFallback   = false
automaticDownload   = false
automaticActivation = false
~~~

Selecting the model is therefore not permission to download it, start it, replace another model, or
activate a runtime.

## Certified artifact catalog

The 11-member bundle is source-placed as typed data with:

~~~text
role ref
artifact ref
filename
media type
SHA-256
expected byte count
provider source ref
license ref
~~~

The catalog asserts unique refs/names, exact member count, and the exact aggregate byte count.

The artifact bytes themselves are **not** stored in this repository.

## VexArchitecture boundary

This package is not a Family merely because it is important.

It currently answers an identity/configuration question, not a human Control against live State.

~~~text
HERETIC PRODUCT MODEL CONTRACT
!= LIVE MODEL PROCESS
!= DOWNLOAD OWNER
!= EXECUTION CUSTODY
!= COMPANION
!= PRESENTATION
~~~

When the fresh build ports runtime realization, that work must identify the next rightful owners
rather than growing effectful methods inside `HereticProductModel`.

## Provenance and currentness

The VexLife sources above are formation provenance for the faithful port.

After acceptance, this package owns the fresh product's Heretic identity. Runtime code should consume
this package rather than repeatedly reading historical VexLife files or copying their constants.

A later intentional model/profile/artifact change must update this package as one source change and
re-prove every true dependent. It must not be inferred from whichever runtime script changed most
recently.

<!-- [VXG RealForever][VEXLIFE-PROD][HERETIC-PRODUCT-MODEL] -->
