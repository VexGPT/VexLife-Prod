# Kotlin formatting recipe

`[VXG RealForever]`

The repository's Kotlin form is `ktfmt --kotlinlang-style` (see `docs/CULTURE.md`, Source form).

ktfmt keeps any argument list that already contains a line break broken, so files written with
one-token-per-line layout need a pre-pass that joins those lists; ktfmt then re-breaks only what
exceeds 100 columns.

~~~bash
# ktfmt 0.64 from Maven Central
curl -fsSLo /tmp/ktfmt.jar \
  https://repo1.maven.org/maven2/com/facebook/ktfmt/0.64/ktfmt-0.64-with-dependencies.jar

# 1. join multi-line (...) spans that hold no lambdas, comments or raw strings (strings are skipped exactly)
python3 tools/kotlin-format/join_multiline_args.py path/to/File.kt ...
# 2. format
java -jar /tmp/ktfmt.jar --kotlinlang-style path/to/File.kt ...
# ktfmt settles function-type parameter lists on a second pass; run it until --dry-run lists nothing
java -jar /tmp/ktfmt.jar --kotlinlang-style --dry-run --set-exit-if-changed path/to/File.kt ...
~~~

Then check that the change is formatting-only before committing it on its own: string literals
byte-identical, whitespace-insensitive tokens equal apart from import order and trailing commas, and
the tests that cover the files pass. Files skipped by the pre-pass (raw `"""` strings) are still
formatted by ktfmt; only their argument lists stay as written.

Migrate one slice at a time, when that slice is next changed, and skip files other open PRs are
editing.

<!-- [VXG RealForever][VEXLIFE-PROD][SOURCE-FORM] -->
