# Temporary Relay carriage for currentized PR #243 — A006

[VXG RealForever]

A005 proved the pre-currentization candidate exactly. While that proof was running, PR #246 merged
the Models/configuration integration into main. #223 and #243 were then forward-currentized without
rewriting their owned implementation blobs; #243 also received one documentation-only currentness
correction describing #246 as merged main rather than an open neighbor.

A006 exists because the immutable candidate head/tree changed.

Exact candidate:

~~~text
base 46b16f241bb73c77a7081aa8a8832705fb3e49ec
head 0ebc8a768b2bc356c1912b87d10fae70b4a3c9e6
tree 76ec2d4dd534aa886da372a07b679e93cc1f62a1
~~~

Proof shape is unchanged from successful A005:

1. checkVexLifeStrings
2. focused JVM platform/runtime realization tests
3. compileKotlinJs
4. exact review snapshot of the seven #243 paths

It reuses the proven compact Windows path policy, exact Temurin 21.0.13+9 dependency, and one
top-level digest-bound Downloads RETURN.

No GitHub Actions. No Product mutation. No approval/READY/merge effect.

<!-- [VXG RealForever][PR243][TEMP-RELAY][A006][CURRENTIZED] -->
