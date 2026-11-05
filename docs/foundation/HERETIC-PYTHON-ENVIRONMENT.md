# Heretic Python + MLX environment

`[VXG RealForever]`

## Human question

Once the exact Heretic model bytes exist, can the fresh product prove or create the exact runtime
software environment without letting model selection, process startup, or generic setup scripts own
Python/MLX identity?

## Relationship

~~~text
certified Heretic product identity
  Python 3.14.7
  mlx-lm 0.32.0
        ↓
HereticPythonEnvironment

existing environment?
  yes
    -> prove real non-symlink directory
    -> run exact environment probe
    -> Python version exact
    -> mlx-lm version exact
    -> sys.prefix == selected environment root
    -> sys.executable == selected bin/python
    -> retained pip install report exists
    -> REUSED_VERIFIED

  no / invalid real directory
    -> creation authorized?
         no  -> HOLD
         yes -> exact bootstrap Python probe
              -> python -m venv
              -> binary-only mlx-lm==0.32.0
              -> retain pip report
              -> exact runtime probe
              -> CREATED_VERIFIED
~~~

## Faithfully ported current VexLife laws

- bootstrap Python must be exactly 3.14.7;
- runtime Python must be exactly 3.14.7;
- `mlx-lm` must be exactly 0.32.0;
- environment identity includes both `sys.prefix` and `sys.executable`;
- existing environment reuse requires the retained pip installation report;
- installation is binary-only and noninteractive;
- missing/invalid environment does not grant creation authority;
- an existing symlink or non-directory is rejected **before** repair cleanup;
- a failed authorized creation cleans only the environment it just owns;
- environment completion does not start or activate the model runtime.

The JVM probe representation is intentionally simplified from VexLife's JSON probe to a private
line protocol. The facts and validation are the same; no public/product contract depends on the
probe's serialization.

## Placement

~~~text
<Vex Home>/
  runtime/
    heretic/
      python-3.14.7-mlx-lm-0.32.0/
        bin/python
        vexlife-mlx-lm-install-report.json
        ...
~~~

## VexArchitecture boundary

~~~text
MODEL IDENTITY
  HereticProductIdentity

MODEL BYTES
  HereticArtifactMaterialization

RUNTIME SOFTWARE ENVIRONMENT
  HereticPythonEnvironment

PROCESS CONSTRUCTION / LIFETIME
  MacUserLaunchdProcess + Heretic runtime owner (later)

RUNTIME QUALIFICATION MEANING
  HereticRuntimeQualification

COMPANION / CONVERSATION
  separate owners
~~~

Environment creation is an effect, but it is not runtime-process execution.

## Formation proof

Disposable Kotlin/JVM 1.9 with an injected command runner:

~~~text
production source compile                  PASS
authorized venv creation                   PASS
binary-only mlx-lm install path            PASS
pip report retained                        PASS
exact Python/mlx-lm/root/executable probe  PASS
CREATED_VERIFIED                           PASS
subsequent REUSED_VERIFIED                 PASS
~~~

Repository tests additionally cover held missing state, wrong-version reuse, failed-creation cleanup,
and symlink deletion-authority refusal.

Still held before acceptance:

~~~text
PR #21 accepted product identity/runtime semantics
repository Kotlin 2.4.20 JVM + jvmTest
real macOS Python 3.14.7 / mlx-lm 0.32.0 host proof
~~~

<!-- [VXG RealForever][VEXLIFE-PROD][HERETIC-PYTHON-ENVIRONMENT] -->
