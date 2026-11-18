# Temporary Relay carriage for PR #243 — A003

[VXG RealForever]

## Why A003 exists

A002 formed and executed the native VexRelay carrier, but the inner Relay stopped before repository
grounding:

~~~text
inner disposition  SAFE_FAILURE
first failure      spawn C:\Program Files\GitHub CLI\gh.EXE ENOENT
Product proof      NOT REACHED
tests executed     false
~~~

The outer carrier retained the canonical inner RETURN unchanged at SHA-256:

~~~text
fe1f2c47e62b52a1e719d5124e11c5fb2d6a9330fef92f3e08d2c27bc4136021
~~~

The uploaded outer RETURN itself was:

~~~text
a81e5ccd2676d61513c558219227e4376b67e09f9693778b2f4491b018ac5cc2
~~~

The observed outer work cwd was 264 characters, beyond the classic Windows MAX_PATH envelope. The
same outer RETURN recorded cleanup PowerShell spawn ENOENT. A003 therefore treats this as one
Windows harness/path-envelope defect rather than a Product-source failure.

## A003 path policy

Semantic refs remain descriptive. Machine-only path carriers become compact:

~~~text
taskStorageKey     t243rt
attemptStorageKey  a003
workspace path     p
repository path    r
outer package stem first 12 chars of canonical stem
~~~

The Product candidate remains exactly PR #243:

~~~text
head 2c54c81f3d318163ee5b09a6d35363af75c33c9d
tree 524bb1e8112c45f0455af8f2f6b81375cea67c93
base 5f0d95f49a5a20d021d2bc25e80ee4a1cb1460b0
~~~

Because VexLife-Prod is public, the inner Relay uses ordinary exact Git clone rather than GitHub CLI
clone. GitHub CLI remains only the outer bootstrap for acquiring the private accepted Vextreme-SDK
source.

## Human return surface

The VexLocalBridge outer RETURN is the single human return surface because it preserves both carrier
cleanup/effect evidence and the retained canonical inner VexRelay RETURN. After execution, the A003
PowerShell entrypoint:

1. consumes the source-managed VexLocalBridge result pointer;
2. verifies the outer RETURN ZIP SHA-256;
3. copies those exact bytes unchanged to the user's Downloads directory;
4. preserves the source-formed RETURN filename and full --sha256-<digest>.zip suffix;
5. truncates only the semantic filename prefix if the complete filename would exceed 255 characters;
6. verifies the copied SHA-256 and opens Explorer selecting that top-level Downloads ZIP.

The nested capsule remains machine/recovery custody. Victor only needs the one complete top-level
Downloads ZIP. The copy is human transport convenience; the byte digest remains evidence identity.

No GitHub Actions. No Product-source mutation. No merge/ready/approval effect.

<!-- [VXG RealForever][VEXLIFE-PROD][PR243][TEMP-RELAY][A003][WINDOWS-PATH-ENVELOPE] -->
