# Releases: main builds, stable versions and the VexLife home

`[VXG RealForever]`

This is the process a person or an AI instance follows to publish VexLife. It is short on purpose:
every step is carried out by a workflow, and every page that lists builds reads what the workflows
published. Nothing here decides that a build is good; a stable version records that it was
accepted.

## Channels

| Channel | What it is | Made by | Tag |
|---|---|---|---|
| Main build | Every commit that lands on `main`, built for the Mac. Newest work, least lived. | `release` workflow, on every push to `main` | `private-service-image-<sha>` (prerelease) |
| Stable | A feature threshold on `main` that was lived and accepted. | `release` workflow, run by hand with a version | `v<major>.<minor>.<patch>` (marked latest) |
| Drafts | Open PRs. Never published as builds. | — | — |

A main build release carries the Product's own formed artifacts at that exact commit
(`archiveVexLifeProdServiceImage`): the private service image archive, its receipt, and the
source-bound bootstrap `vexlife-prod-bootstrap-darwin-arm64-<sha>.command` (plus the current-name
copy and digests). The bootstrap downloads that image from that release and verifies its SHA-256,
so a main build release is never deleted while a stable version points at it.

A stable version release carries the bootstrap of its commit, notes with `source-sha:`,
`image-release:` and `sha256 <file> <digest>` lines, and is marked latest.

## Cutting a stable version

When Victor says a threshold on `main` is good (for example "this is good, tag it as 0.7.0"):

1. Confirm `main` is the commit that was lived: the merged PRs since the previous version, their
   lived checks, and `main` green.
2. Run **Actions → release → Run workflow** on `main` with `version` = `0.7.0`
   (or `gh workflow run release --ref main -f version=0.7.0`).
3. The workflow tests, forms the exact Mac image and bootstrap, publishes the main build for that
   commit (if it is not there yet) and creates `v0.7.0` at that commit. A version that exists is
   never rebuilt or moved; a wrong version gets a new, higher version.
4. The `pages` workflow redeploys the home page when `release` completes; the version appears in
   the Mac download list.

Versions follow semver; while VexLife is before 1.0 a minor version marks a feature threshold and a
patch marks a fix to an accepted version.

## The VexLife home (GitHub Pages)

Source: `site/`. Deployed by `.github/workflows/pages.yml` on changes to the site, after every
`release` run, and by hand. At deploy time:

- `data/models.json` is formed by `./gradlew generateVexLifeSiteData` from `HereticModelCatalog` and
  the Mac host profile — the Requirements table is never restated by hand;
- `data/releases.json` is formed by `.github/release/releases-snapshot.py` from GitHub Releases
  (stable = `v*` not prerelease; main = `private-service-image-*`), with digests from the notes.

The page falls back to the public GitHub API when the snapshot is missing. Windows is shown as in
preparation until a Windows realization exists and has run on a real PC.

Pages source: **GitHub Actions** (set by the owner on 2026-11-15, replacing the Jekyll README
site). The first `pages` run after this lands on `main` replaces the README page with `site/`.

Walkthrough screenshots: `site/walkthrough/<step>.png` for the steps in `site/data/walkthrough.json`
(language, home, model, setup, ready), captured from a real first run on an Apple-silicon Mac. A
step without its screenshot says so.

## Not yet (Official Application trajectory)

The Mac download is the source-bound bootstrap script. The signed, notarized `VexLife.app` in a
versioned `.dmg` is the Official Application work (VexWork `project.vexlife.official-application`,
PR #139): bundle identity, Developer ID signing, notarization and the contained workspace. When a
release carries a `*-macos*.dmg` (or `*-darwin*.dmg`), the home page offers it in place of the
script with no page change. No `.exe` is built until a Windows model runtime exists.
