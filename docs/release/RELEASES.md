# Releases: main builds, stable versions and the VexLife home

`[VXG RealForever]`

This is the process a person or an AI instance follows to publish VexLife. It is short on purpose:
every mechanical step is carried by source-owned build/workflow paths, and every page that lists
builds reads what those workflows actually published. Nothing here decides that a build is good; a
stable version records that Victor accepted a lived feature threshold.

## Channels

| Channel | What it is | Made by | Tag |
|---|---|---|---|
| Pull-request proof | Exact candidate app/DMG proof. Never a public release. | `mac-app-proof` workflow on packaging/release branches and PRs | workflow artifact only |
| Main build | Every commit that lands on `main`, built for Mac Apple silicon. Newest work, least lived. | `release` workflow on every push to `main` | `private-service-image-<sha>` prerelease |
| Stable | A feature threshold on `main` that was lived and accepted. | `release` workflow, run by hand with a version | `v<major>.<minor>.<patch>`, marked latest |

The historical main tag name remains `private-service-image-<sha>` for compatibility with the current
release/page catalog, but the release now carries two related Product artifacts:

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
explicitly allow the app before first launch. The download page says this directly.

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
  -> embed exact private image in Contents/Resources
  -> bind bundle/source/trust metadata
  -> restore an ad-hoc bundle seal
  -> form + verify VexLife-macOS-AppleSilicon.dmg
  -> SHA-256 + release receipt
~~~

The current Vision runtime contract remains
`LOCAL_QUALIFIED_NOT_DISTRIBUTABLE / LOCAL_ONLY_NO_RELEASE`. A public app build therefore refuses
to embed that local-only Vision runtime. Vision distribution remains separately held rather than
laundered into the DMG.

For stable runs, the workflow supplies the requested semantic version as
`-PvexlifeAppVersion=<major.minor.patch>`. Continuous main builds use `0.0.0` as the internal app
version and remain identified externally by exact source SHA/release tag. Cutting a stable version
does not replace the continuous DMG for that SHA; the stable DMG is a separately version-stamped
artifact bound to the same source.

## Cutting a stable version

When Victor says a threshold on `main` is good (for example "this is good, tag it as 0.7.0"):

1. Confirm `main` is the commit that was lived: the merged PRs since the previous version, their
   lived checks, and the required current proof.
2. Run **Actions → release → Run workflow** on `main` with `version` = `0.7.0`
   (or `gh workflow run release --ref main -f version=0.7.0`).
3. The workflow first requires the exact SHA's already-published main build. It then tests and forms
   a version-stamped stable DMG from that same source **without rewriting the main build**, and
   creates `v0.7.0` at that commit. A version that exists is never rebuilt or moved; a wrong
   version gets a new, higher version.
4. The `pages` workflow redeploys after `release` completes; the version appears in the Mac
   download list.

Versions follow semver; while VexLife is before 1.0 a minor version marks a feature threshold and a
patch marks a fix to an accepted version.

## The VexLife home (GitHub Pages)

Source: `site/`. Deployed by `.github/workflows/pages.yml` on changes to the site, after every
`release` run, and by hand. At deploy time:

- `data/models.json` is formed by `./gradlew generateVexLifeSiteData` from
  `HereticModelCatalog` and the Mac host profile;
- `data/releases.json` is formed by `.github/release/releases-snapshot.py` from GitHub Releases,
  with SHA-256 values read from the release notes.

The page prefers a Mac `.dmg` when a release contains one and falls back to the predecessor
bootstrap otherwise. Windows remains visibly in preparation until a Windows realization actually
exists and has real-machine proof.

**Repository setting:** GitHub Pages → Build and deployment → Source is configured to
**GitHub Actions** as of 2026-11-15.

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
-> launch
-> existing Setup Journey
-> Product CURRENT
-> real Vex turn
~~~

The repository's Mac startup integrity rule still applies: packaging PASS is not sustained-lifetime
or Conversation PASS.

<!-- [VXG RealForever][VEXLIFE-PROD][RELEASES] -->
