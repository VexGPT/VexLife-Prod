# Continuity cache baseline Relay — A003

[VXG RealForever]

This directory retains the stable formation bundle for the source-proof gate of PR #210.

Product candidate proved by the bundle:

```text
base  03aee62cdbe3c1e269d9e94e05b1bc06901e3a25
head  d370079fbaaa863412f859f233f37cc706e25f2d
tree  c2698fba02810a8278152d142338c27d78a40c6d
```

Bundle:

```text
VexLife-Continuity-Cache-Baseline-Relay-A003--sha256-d292b0ee85dde151d7238956ade5e0d4669c3e4fff1d5df5697af5c1d8930460.zip
```

The formed VexRelay task is time-bounded and is therefore created fresh on the Mac. The committed
bundle is the durable recipe/carriage artifact.

A003 is source proof only: full JVM tests, Kotlin/JS compilation, localization currentness and Web P1
generation. It does not install/replace the Product service, mutate Home, or start/replace the model
runtime. Lived cache measurement follows only after A003 PASS.
