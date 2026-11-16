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
| Preview | An intentional downloadable snapshot of current `main` for sharing/testing. Ordinary merges do not publish one. | `release` workflow, run by hand with channel = `preview` | `private-service-image-<sha>` prerelease |
| Stable | A feature threshold on `main` that was lived and accepted. | `release` workflow, run by hand with channel = `stable` and a version | `v<major>.<minor>.<patch>`, marked latest |

The historical preview tag name remains `private-service-image-<sha>` for compatibility with the current
release/page catalog, but a published preview or stable release carries the Product artifacts below:

- **preferred human download:** `VexLife-macOS-AppleSilicon.dmg`, containing `VexLife.app` plus
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
  -> form + verify VexLife-macOS-AppleSilicon.dmg
  -> SHA-256 + release receipt
~~~

The current Vision runtime contract remains
`LOCAL_QUALIFIED_NOT_DISTRIBUTABLE / LOCAL_ONLY_NO_RELEASE`. A public app build therefore refuses
to embed that local-only Vision runtime. Vision distribution remains separately held rather than
laundered into the DMG.

For stable runs, the workflow supplies the requested semantic version as
`-PvexlifeAppVersion=<major.minor.patch>`. A preview uses `0.0.0` internally and remains identified
externally by its exact source SHA/release tag. Ordinary merges to `main` do not publish anything.
Victor or a continuing instance chooses when a source threshold is worth publishing.

## One release process; executor chosen by current constraints

There is no required calendar schedule. A release is an intentional cutoff, and the release meaning
does not change based on who executes it.

The source-owned Mac publication process is:

~~~text
tools/release/publish-mac-release.sh preview
tools/release/publish-mac-release.sh stable 0.1.0
~~~

It requires a clean exact current `origin/main`, runs the Product tests, forms/verifies the DMG and
publishes the GitHub Release. For a stable version, `gh release create` creates the `vX.Y.Z` tag at
that exact source automatically. Victor never manually creates/moves a tag, edits a version file,
builds a DMG or uploads assets.

Two executors may lawfully invoke that same process:

- **VexRelay / Victor's Mac — billing-aware primary route.** New Relay formation consumes current
  contract `source.vexrelay.current-contract/v1`; the current contract admits host profile
  `macos-victor` and routes repository operations through the current repository adapter. The Relay
  task runs the source-owned script rather than reproducing its release logic in the task.
- **GitHub Actions — optional hosted route.** **Actions → release → Run workflow** is a thin
  `workflow_dispatch` wrapper around the same script. Choose `preview`, or choose `stable` and
  enter a version such as `0.1.0`.

Ordinary merges to `main` publish nothing. A shareable preview or stable version exists only when
one of these executors intentionally runs the release process.

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
bootstrap otherwise. It labels the unversioned channel as **Preview**, not as an always-current main
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
download exact PR/release DMG
-> open DMG
-> drag VexLife.app to Applications
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
