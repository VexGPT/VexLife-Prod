#!/usr/bin/env python3
# [VXG RealForever]
from __future__ import annotations
import hashlib
from pathlib import Path
import stat
import zipfile

ROOT = Path(__file__).resolve().parent
MEMBERS = [
    "FORMATION-A001.json",
    "FORM-AND-RUN-A001.mjs",
    "RUN-VEXRELAY-COMPANION-ENGAGEMENT-MAC-A001.command",
]
manifest_lines = []
for name in MEMBERS:
    data = (ROOT / name).read_bytes()
    manifest_lines.append(f"{hashlib.sha256(data).hexdigest()}  {name}")
manifest = ("\n".join(manifest_lines) + "\n").encode()
all_members = [(name, (ROOT / name).read_bytes()) for name in MEMBERS] + [("PACKET-MANIFEST.sha256", manifest)]
tmp = ROOT / "VEXLIFE-COMPANION-ENGAGEMENT-MAC-A001.zip"
with zipfile.ZipFile(tmp, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
    for name, data in sorted(all_members):
        zi = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
        zi.create_system = 3
        mode = 0o755 if name.endswith((".command", ".mjs")) else 0o644
        zi.external_attr = (stat.S_IFREG | mode) << 16
        zi.compress_type = zipfile.ZIP_DEFLATED
        zf.writestr(zi, data)
digest = hashlib.sha256(tmp.read_bytes()).hexdigest()
final = ROOT / f"VEXLIFE-COMPANION-ENGAGEMENT-MAC-A001--sha256-{digest}.zip"
tmp.replace(final)
print(final)
print(digest)
print(
    "ZIP=\"$HOME/Downloads/" + final.name + "\"; "
    "EXPECTED=\"" + digest + "\"; "
    "OBSERVED=\"$(/usr/bin/shasum -a 256 \"$ZIP\" | /usr/bin/awk '{print $1}')\"; "
    "[ \"$OBSERVED\" = \"$EXPECTED\" ] || { echo \"ZIP SHA-256 mismatch\" >&2; exit 4; }; "
    "ROOT=\"$(/usr/bin/mktemp -d \"$HOME/Downloads/.vexlife-companion-engagement-a001.XXXXXX\")\"; "
    "/usr/bin/ditto -x -k \"$ZIP\" \"$ROOT\"; "
    "(cd \"$ROOT\" && /usr/bin/shasum -a 256 -c PACKET-MANIFEST.sha256 && /bin/bash RUN-VEXRELAY-COMPANION-ENGAGEMENT-MAC-A001.command); "
    "STATUS=$?; /bin/rm -rf \"$ROOT\"; exit $STATUS"
)
