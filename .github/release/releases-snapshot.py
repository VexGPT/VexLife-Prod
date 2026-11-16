#!/usr/bin/env python3
"""Snapshot of published VexLife releases for the home page (site/data/releases.json).

Reads GitHub Releases through `gh api` and keeps only what the page shows: stable versions
(vX.Y.Z, not prerelease) and main builds (private-service-image-<sha>), their source commit, and
their assets with the SHA-256 published in the release notes (`sha256 <file> <digest>` lines).
Decides nothing: what is stable is whatever was cut as a stable version.
"""
import argparse
import datetime
import json
import re
import subprocess
import sys

STABLE = re.compile(r"^v\d+\.\d+\.\d+$")
MAIN = re.compile(r"^private-service-image-([0-9a-f]{40})$")
SOURCE = re.compile(r"source-sha:\s*([0-9a-f]{40})")
DIGEST = re.compile(r"^sha256\s+(\S+)\s+([0-9a-f]{64})\s*$", re.M)


def releases(repository):
    out = subprocess.run(
        ["gh", "api", "--paginate", f"repos/{repository}/releases?per_page=100"],
        check=True, capture_output=True, text=True,
    ).stdout
    # --paginate concatenates JSON arrays.
    items = []
    for chunk in re.split(r"(?<=\])\s*(?=\[)", out.strip()):
        if chunk:
            items.extend(json.loads(chunk))
    return items


def snapshot(items, repository, main_sha):
    kept = []
    for release in items:
        if release.get("draft"):
            continue
        tag = release.get("tag_name", "")
        main = MAIN.match(tag)
        stable = STABLE.match(tag) and not release.get("prerelease")
        if not (main or stable):
            continue
        body = release.get("body") or ""
        digests = dict(DIGEST.findall(body))
        source = SOURCE.search(body)
        kept.append({
            "tag": tag,
            "name": release.get("name") or tag,
            "channel": "stable" if stable else "main",
            "sourceSha": main.group(1) if main else (source.group(1) if source else None),
            "publishedAt": release.get("published_at"),
            "url": release.get("html_url"),
            "assets": [
                {
                    "name": asset["name"],
                    "size": asset.get("size"),
                    "url": asset.get("browser_download_url"),
                    "sha256": digests.get(asset["name"]),
                }
                for asset in release.get("assets", [])
            ],
        })
    return {
        "schemaVersion": "vexlife-prod.site-releases/v1",
        "repository": repository,
        "mainSha": main_sha,
        "formedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds"),
        "releases": kept,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--repository", required=True)
    parser.add_argument("--main-sha", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--from-file", help="read the releases API JSON from a file (tests)")
    args = parser.parse_args()
    items = json.load(open(args.from_file)) if args.from_file else releases(args.repository)
    data = snapshot(items, args.repository, args.main_sha)
    with open(args.out, "w") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")
    print(f"releases: {sum(r['channel'] == 'stable' for r in data['releases'])} stable, "
          f"{sum(r['channel'] == 'main' for r in data['releases'])} main", file=sys.stderr)


if __name__ == "__main__":
    main()
