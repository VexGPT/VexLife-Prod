# Releases: intentional previews, stable versions and the VexLife home

`[VXG RealForever]`

This is the process a person or an AI instance follows to publish VexLife. It is short on purpose:
every mechanical step is carried by source-owned build/workflow paths, and every page that lists
builds reads what those workflows actually published. Nothing here decides that a build is good; a
stable version records that Victor accepted a lived feature threshold.

## Channels

| Channel | What it is | Made by | Tag |
|---|---|---|---|
| Pull-request proof | Exact candidate app/DMG proof. Never a public release. | `mac-app-proof` workflow on packaging/release branches and PRs | workflow artifact only |
| Preview | A proof-gated immutable downloadable snapshot of an admitted `main` commit. | automatic `release` workflow on every push to `main`; manual Preview remains available | `private-service-image-<sha>` prerelease |
| Stable | A feature threshold on `main` that was lived and accepted. | `release` workflow, run by hand with channel = `stable` and a version | `v<major>.<minor>.<patch>`, marked latest |

## [VexLife Prod versioning]

Release identity is projected all the way into the human Mac distribution without creating a second
installed Product identity.

~~~text
candidate
  release identity   = exact source SHA
  DMG                = VexLife-Candidate-<sha7>-macOS-AppleSilicon.dmg
  mounted volume     = VexLife Candidate <sha7>

preview
  release identity   = exact source SHA
  DMG                = VexLife-Preview-<sha7>-macOS-AppleSilicon.dmg
  mounted volume     = VexLife Preview <sha7>

stable
  release identity   = semantic version + exact source SHA
  DMG                = VexLife-<version>-macOS-AppleSilicon.dmg
  mounted volume     = VexLife <version>

all channels
  installed app      = VexLife.app
  bundle identifier  = com.vexlife.prod
~~~

The distribution name tells the person and macOS which exact delivery artifact is mounted. The
installed application remains one canonical Product so update/currentness, Home, service lifetime,
Conversation and Memory do not fork merely because two release artifacts exist.

Permanent:

~~~text
RELEASE IDENTITY
-> DISTRIBUTION IDENTITY
-> VERSIONED DMG + VERSIONED MOUNTED VOLUME

VERSIONED DISTRIBUTION != SECOND PRODUCT
VERSIONED DISTRIBUTION != SECOND HOME
VERSIONED DISTRIBUTION != SECOND SERVICE TOPOLOGY

VexLife.app + com.vexlife.prod
= canonical installed Product identity
~~~

For an update, the ordinary human route is therefore:

~~~text
download exact newer distribution
-> open its distinctly named DMG
-> drag VexLife.app to Applications
-> Replace the older VexLife.app when Finder asks
-> launch the Applications copy
-> Product Entry observes installed/running currentness
-> Update & Restart / Repair & Restart only when the observed Product state admits it
~~~

Launching a second copy directly from a mounted DMG is not the update mechanism. Distribution
versioning exists so several downloaded/mounted release artifacts remain intelligible while the
installed Product remains singular.

Migration is forward-only. Releases published before this contract remain immutable with their
historical asset/volume names; they are not renamed or rebuilt. The Pages catalog accepts both legacy
and release-identified DMG names because it discovers the Mac artifact by platform/file type rather
than one fixed basename. The first release formed after this v2 contract begins the versioned
distribution line.

The realized app also embeds `VexLifeProdDistributionChannel` and
`VexLifeProdDistributionIdentity` beside its exact source SHA. Those fields are diagnostic/currentness
projection only; they do not create another Product identity.

The historical preview tag name remains `private-service-image-<sha>` for compatibility with the current
release/page catalog, but a published preview or stable release carries the Product artifacts below:

- **preferred human download:** one release-identified DMG
  (`VexLife-Preview-<sha7>-macOS-AppleSilicon.dmg` or
  `VexLife-<version>-macOS-AppleSilicon.dmg`), containing the canonical `VexLife.app` plus
  an Applications shortcut;
- **recovery/predecessor carriage:** the exact private service image archive/receipt and
  source-bound bootstrap `.command`.

`VexLife.app` embeds the same exact source-bound private service image beneath its Resources and
uses the existing native Setup Journey / Product Entry owners. Packaging does not create a second
setup decision tree.

## Current Mac trust boundary

The public Mac build is intentionally honest about what exists today:

~~~text
GitHub repository + exact release + source SHA + SHA-256
+ ad-hoc macOS bundle signature
= open-source provenance / exact release identity

Developer ID
+ Apple notarization
= NOT PRESENT
~~~

There are no paid Apple credentials in the repository or workflow. The DMG can therefore be built and
published without an Apple Developer Program membership, but macOS may require the person to
explicitly allow the app before first launch. The Apple-owned Gatekeeper alert cannot be customized
by VexLife. After a blocked launch, the download page provides a direct Privacy & Security deep-link
plus the ordinary Apple menu -> System Settings -> Privacy & Security fallback. The person still makes
the protected `Open Anyway` decision in System Settings. VexLife never clears quarantine or changes
Gatekeeper policy. The DMG carries no second executable/helper just to navigate Settings.

If a future legitimate Developer ID/notarization capability becomes available, it should extend this
same application artifact boundary; absence of that credential must not block ordinary app/DMG
formation.

## Forming the Mac artifact

The Product-owned Gradle route targets **Apple silicon on macOS 14.0 or later** and is:

~~~text
archiveVexLifeProdMacApplication
  -> archiveVexLifeProdServiceImage
  -> exact source-bound private image
  -> osacompile existing native Setup Journey as VexLife.app
  -> render the Vextreme mark through a source-owned 1024x1024 app-icon canvas
  -> replace the applet-native applet.icns with that full-canvas artwork
  -> remove osacompile's generated Assets.car / CFBundleIconName precedence
  -> embed exact private image in Contents/Resources
  -> bind bundle/source/trust metadata
  -> restore an ad-hoc bundle seal
  -> reuse the branded applet.icns as the mounted DMG volume icon
  -> project candidate/preview/stable release identity into the DMG filename + mounted volume name
  -> form + verify the exact release-identified DMG
  -> SHA-256 + release receipt
~~~

The current Vision runtime contract remains
`LOCAL_QUALIFIED_NOT_DISTRIBUTABLE / LOCAL_ONLY_NO_RELEASE`. A public app build therefore refuses
to embed that local-only Vision runtime. Vision distribution remains separately held rather than
laundered into the DMG.

For stable runs, the workflow supplies the requested semantic version as
`-PvexlifeAppVersion=<major.minor.patch>` and distribution channel `stable`. A preview uses
`0.0.0` internally, distribution channel `preview`, and remains release-identified by its exact
source SHA/release tag. Candidate proof defaults to distribution channel `candidate` and likewise
uses the exact source SHA. The bundle embeds the exact expected source SHA in every channel.
Every new commit admitted to `main` now automatically enters the source-identified Preview release
cycle. Its Preview version is the exact Git source SHA, not an automatically invented semantic
version. Publication still requires the full release proof to pass. Stable
`v<major>.<minor>.<patch>` remains an intentional lived/accepted threshold chosen explicitly.

## One Mac candidate-proof process

When Victor or a continuing instance asks for an exact PR/test DMG, the source-owned command is:

~~~text
bash tools/release/prove-mac-candidate.sh
~~~

Release callers pass both the semantic version and the distribution channel explicitly:

~~~text
bash tools/release/prove-mac-candidate.sh 0.1.0 stable
~~~

Ordinary PR/candidate proof takes no arguments and therefore forms the exact source-identified
`candidate` distribution.

The candidate proof requires a clean exact checkout on Apple-silicon macOS and owns the whole
non-public artifact-proof sequence:

~~~text
exact HEAD
-> candidate/preview/stable distribution identity
-> jvmTest + checkVexLifeStrings
-> archiveVexLifeProdMacApplication
-> exact canonical VexLife.app + source binding
-> release-identified DMG filename + volume-name verification
-> DMG recorded SHA verification
-> hdiutil verify
-> strict ad-hoc codesign verification
-> read-only mount
-> mounted app/source/icon/private-service-image/provider checks
-> distribution-trust contract check
-> exact DMG + Product receipt
~~~

It **does not publish** a GitHub Release, move a tag, update Pages, install the app, reset Home, choose
a model, or claim lived Product acceptance.

The existing executors now consume this one owner:

~~~text
mac-app-proof workflow
  -> prove-mac-candidate.sh

publish-mac-release.sh
  -> exact-main / channel / version admission
  -> prove-mac-candidate.sh
  -> publication only after proof
~~~

A VexLocalBridge/VexRelay host package may clone or check out one exact candidate and invoke this
script, then return its output/receipt. The package owns transport only. It must not re-author the test
ordering, Gradle formation route, DMG verification, codesign verification, or mounted-content checks.

Permanent:

~~~text
CANDIDATE PROOF != RELEASE PUBLICATION
CANDIDATE PROOF != LIVED PRODUCT ACCEPTANCE
HOST CARRIAGE != BUILD RECIPE AUTHORITY
EXACT SOURCE CHANGE != PERMISSION TO CHANGE PACKAGING TOPOLOGY
~~~

## One release process; executor chosen by current constraints

There is no required calendar schedule. Preview publication follows source integration; Stable
publication remains an intentional accepted threshold. The release meaning does not change based on
who executes it.

### Automatic Product cycle

Every new `main` commit automatically enters the Preview cycle; its exact SHA is the Preview version:

~~~text
merge / admitted push to main
-> exact push SHA
-> release workflow on macOS
-> publish-mac-release.sh preview "" <exact-push-sha>
-> prove exact source / tests / app / DMG
-> Preview tag private-service-image-<sha>
-> VexLife-Preview-<sha7>-macOS-AppleSilicon.dmg
-> refresh Pages from newest current main

later main commits do not erase an older queued Preview:
exact push SHA remains publishable only while it is an ancestor of current main
~~~

The exact SHA is the automatic Preview version. This avoids fabricating SemVer meaning for every
merge. A downloadable distribution exists only after the repository-owned release proof succeeds; a
failed proof leaves that main SHA unreleased rather than blessing broken Product bytes.
A rewrite that removes the push SHA from current `main` ancestry fails closed rather than publishing
a detached history.

Stable remains separate:

~~~text
Preview  = automatic source version for every main commit
Stable   = explicit SemVer after lived acceptance
~~~

The source-owned Mac publication process is:

~~~text
tools/release/publish-mac-release.sh preview
tools/release/publish-mac-release.sh stable 0.1.0
~~~

Manual Preview and Stable publication require a clean exact current `origin/main`. Automatic
Preview publication instead binds the exact GitHub push SHA and admits it only while that SHA remains
an ancestor of current `origin/main`; this lets queued main commits retain their own immutable
Preview identity. Every route runs the Product tests and forms/verifies the DMG before publication.
Automatic Preview defers the Pages projection to the workflow so it can refresh from newest current
main after an older queued release lands. For a stable version, `gh release create` creates the
`vX.Y.Z` tag at that exact source automatically. Victor never manually creates/moves a tag, edits a
version file, builds a DMG or uploads assets.

If the GitHub Release succeeds but the following Pages publication fails, the release already exists.
Do **not** rebuild, move, or replay the release merely to repair its public catalog projection. Resume
with the read-only/site-owned recovery route:

~~~text
bash tools/site/publish-gh-pages.sh
~~~

Two executors may lawfully invoke that same source-owned process:

- **VexLocalBridge direct release host package on Victor's Mac — billing-aware primary route.**
  The human carriage is intentionally the same simple shape used by Relay-hosted work: one bounded
  package, one exact Mac command, one canonical RETURN. The inner lifecycle effect is **not** a
  `vex.relay.task/v2` RELEASE operation. VexRelay's current action catalog does not own release
  lifecycle effects; release publication stays with this Product release owner and is carried through
  a qualified direct VexLocalBridge host package. The package runs
  `tools/release/publish-mac-release.sh` rather than reproducing release semantics.
- **VexRelay / VexLocalBridge proof — pre-publication evidence when needed.** Relay may prove the
  exact source/build/artifact candidate through its admitted repository/host proof contracts without
  claiming that proof itself published the release.
- **GitHub Actions — automatic Preview + manual release route.** Every push to `main` invokes the
  same source-owned script as an exact-SHA Preview. **Actions → release → Run workflow** remains the
  manual surface for an explicit Preview or Stable `0.x.y` cutoff.

The similar human carriage is deliberate:

~~~text
one host invocation + one canonical return
!= one shared inner effect schema

VEXRELAY_PROOF != RELEASE_LIFECYCLE_EFFECT
VEXLOCALBRIDGE_RELEASE_CARRIAGE != VEXRELAY_RELEASE_TASK
~~~

The first real Preview publication and later clean Preview publication both used the direct
VexLocalBridge host-package pattern. A canonical RETURN allowed the continuing AI instance to
observe partial publication, diagnose the initial Pages caller-working-directory failure, repair
only that held projection, and continue without making Victor interpret logs or manually reconstruct
the release state.

Every admitted `main` commit automatically schedules its Preview through the hosted release
executor. The direct VexLocalBridge route remains the bounded recovery/manual publication surface.
Stable publication still requires an explicit semantic version and is never inferred from a merge.

Versions follow semver while VexLife is before 1.0:

- `0.1.0 → 0.2.0`: a new accepted feature threshold;
- `0.1.0 → 0.1.1`: a fix to that accepted line.

A version that already exists is never rebuilt or moved; a correction gets a new version.

## The VexLife home (GitHub Pages)

`site/` remains the authored site source. Formation is source-owned by:

~~~text
tools/site/form-pages-site.sh
~~~

That process copies the authored site, forms `data/models.json` from the Product's model/host owners,
forms `data/releases.json` from actual GitHub Releases, and adds `.nojekyll`. The resulting directory
is the complete static site; neither GitHub Actions nor VexRelay owns alternate page meaning.

### Recommended billing-aware publication profile: gh-pages branch

The direct publication process is:

~~~text
tools/site/publish-gh-pages.sh
~~~

It requires a clean exact current `origin/main`, forms the site through the same source-owned process,
then pushes only the formed static bytes to the root of a dedicated `gh-pages` branch.

For this profile the one repository setting is:

~~~text
Settings
-> Pages
-> Build and deployment
-> Source: Deploy from a branch
-> Branch: gh-pages
-> Folder: /(root)
~~~

VexRelay can run that publication script through the current repository-operations route. A connected
provider worker with exact repository authority can also realize the equivalent `gh-pages` update
directly. GitHub may still show its internal Pages deployment run after the branch changes, but no
custom VexLife site-build workflow is required to form the bytes.

### Optional GitHub Actions publication profile

`.github/workflows/pages.yml` is retained as a **manual-only** wrapper around
`tools/site/form-pages-site.sh`. It applies only while the repository Pages source is configured as
**GitHub Actions**. It does not run on ordinary pushes/releases.

The repository can use either final Pages profile, but Pages has one active source mode at a time.
Switching executor must not fork site formation logic.

The page prefers a Mac `.dmg` when a release contains one and falls back to the predecessor
bootstrap otherwise. It labels the source-versioned prerelease channel as **Preview**, not as an always-current main
build. The selector keeps at most the two newest Preview releases visible so an update can be compared
against its immediate predecessor before that older Preview is intentionally withdrawn. Windows remains
visibly in preparation until a Windows realization actually exists and has real-machine proof.

Current observed setting as of 2026-11-15: **Deploy from a branch → gh-pages → /(root)**.
Victor switched the repository to this profile and the public VexLife page loaded successfully from
the formed `gh-pages` branch. Future publication should therefore update `gh-pages` through the
source-owned branch-publish route unless the repository setting is intentionally changed again.

Walkthrough screenshots remain lived evidence: `site/walkthrough/<step>.png` for the steps in
`site/data/walkthrough.json` (language, home, model, setup, ready). A step without its screenshot
says so.

## Preview update-alert acceptance checkpoint

The first Preview that contains the lived/merged Mac service-lifetime correction was published from:

~~~text
Preview B
source 7345cc2e366e918a9298f0a03c96950c9bae4ac0
tag    private-service-image-7345cc2e366e918a9298f0a03c96950c9bae4ac0
published 2026-11-18T10:53:35Z
~~~

Its GitHub prerelease and the source-owned `gh-pages` release catalog independently read back that exact
source. The immediate release-alert acceptance sequence intentionally uses a later Preview cut from a
new exact `main` source so both sides of the comparison run the current Product lifetime/release logic:

~~~text
install/open published Preview B
-> Product identifies its installed source as one published Preview
-> publish later Preview C from exact current main
-> release observer sees Preview C as the newest intentionally published Preview
-> Release Inbox projects one unread Preview update
-> collapsed Spiral host shows the update dot
-> expanded Alerts child owns the dot instead
-> View update opens the public VexLife download surface
-> install/open Preview C
-> installed Preview identity becomes current
~~~

The later Preview does not need a fabricated feature merely to change identity. A truthful source-owned
documentation/currentization change may create the next exact source cutoff; the release script still
re-runs Product tests, rebuilds the exact source-bound DMG and publishes a distinct immutable Preview.

Permanent:

~~~text
UNPUBLISHED CANDIDATE SOURCE != INSTALLED PUBLISHED PREVIEW IDENTITY
SOURCE SHA CHANGE != FEATURE CLAIM
NEWER PUBLISHED PREVIEW != AUTOMATIC INSTALL
RELEASE ALERT != RELEASE PUBLICATION AUTHORITY
VIEW UPDATE != INSTALL EFFECT
~~~

## Proof boundary

A macOS runner can prove that the exact source:

- passes the repository JVM/string checks;
- compiles the existing native AppKit setup renderer;
- forms `VexLife.app` with the exact embedded private image;
- verifies the ad-hoc bundle seal;
- forms and verifies the DMG;
- emits exact hashes/receipt.

That is **artifact proof**, not lived product completion. The remaining human/host proof after an
exact PR DMG exists is the ordinary route:

~~~text
download exact release-identified PR/Preview/Stable DMG
-> open the distinctly named DMG
-> drag the canonical VexLife.app to Applications (Replace the older app when updating)
-> explicitly allow the unsigned app if macOS requires it
-> macOS may separately confirm the bundled `java` runtime; choose Open only for the trusted exact release
-> launch
-> existing Setup Journey
-> Product CURRENT
-> real Vex turn
~~~

The repository's Mac startup integrity rule still applies: packaging PASS is not sustained-lifetime
or Conversation PASS.

<!-- [VXG RealForever][VEXLIFE-PROD][RELEASES] -->
