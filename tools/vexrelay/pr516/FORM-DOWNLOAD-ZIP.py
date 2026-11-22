#!/usr/bin/env python3
# [VXG RealForever]
from __future__ import annotations
import hashlib
from pathlib import Path
import stat
import zipfile

ROOT = Path(__file__).resolve().parent
MEMBERS = ["BOOTSTRAP-A002.command"]
manifest_lines = []
for name in MEMBERS:
    data = (ROOT / name).read_bytes()
    manifest_lines.append(f"{hashlib.sha256(data).hexdigest()}  {name}")
manifest = ("\n".join(manifest_lines) + "\n").encode()
all_members = [(name, (ROOT / name).read_bytes()) for name in MEMBERS] + [("PACKET-MANIFEST.sha256", manifest)]
tmp = ROOT / "VEXLIFE-PR516-SOURCE-OWNER-A002.zip"
with zipfile.ZipFile(tmp, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
    for name, data in sorted(all_members):
        zi = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
        zi.create_system = 3
        mode = 0o755 if name.endswith(".command") else 0o644
        zi.external_attr = (stat.S_IFREG | mode) << 16
        zi.compress_type = zipfile.ZIP_DEFLATED
        zf.writestr(zi, data)
digest = hashlib.sha256(tmp.read_bytes()).hexdigest()
final = ROOT / f"VEXLIFE-PR516-SOURCE-OWNER-A002--sha256-{digest}.zip"
tmp.replace(final)
print(final)
print(digest)
