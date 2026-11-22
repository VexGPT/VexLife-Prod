#!/usr/bin/env python3
from __future__ import annotations
import hashlib, stat, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
members=['BOOTSTRAP-A003.command']
manifest=''.join(f"{hashlib.sha256((ROOT/n).read_bytes()).hexdigest()}  {n}\n" for n in members).encode()
items=[(n,(ROOT/n).read_bytes()) for n in members]+[('PACKET-MANIFEST.sha256',manifest)]
tmp=ROOT/'VEXLIFE-PR516-SOURCE-OWNER-A003.zip'
with zipfile.ZipFile(tmp,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    for n,d in sorted(items):
        zi=zipfile.ZipInfo(n,date_time=(1980,1,1,0,0,0)); zi.create_system=3; zi.external_attr=(stat.S_IFREG|(0o755 if n.endswith('.command') else 0o644))<<16; zi.compress_type=zipfile.ZIP_DEFLATED; z.writestr(zi,d)
digest=hashlib.sha256(tmp.read_bytes()).hexdigest(); final=ROOT/f'VEXLIFE-PR516-SOURCE-OWNER-A003--sha256-{digest}.zip'; tmp.replace(final); print(final); print(digest)
