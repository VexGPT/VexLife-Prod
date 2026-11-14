# Localization and element identity

`[VXG RealForever]`

## Human question

Can a person use VexLife in their own language from the first screen of setup onward, with Vex
replying in it, while every human-visible element keeps one identity that any other system (tests,
VexBoard, design tokens, automation) can find regardless of the language it is shown in?

## Law (ported from Vextreme `docs/architecture/06-i18n.md`)

~~~text
NO DISPLAY STRING IS HARDCODED
  a string a human will read is a keyed entry in localization/strings/source/,
  referenced by key, never by its words

THE KEY IS THE ELEMENT'S IDENTITY
  key = {scope}.{element-type}.{semantic-name}
  value = an extensible element object; each system reads only its own namespace

ONE SELECTED-LANGUAGE AUTHORITY
  the person's language in their Vex Home (runtime/vexlife-prod/settings/person.json)
  Vex replies in it, Speech reads in it, screens are shown in it

A LANGUAGE IS OFFERED ONLY WHEN COMPLETE
  every key of the scope has a current translation; partial translation is not a finished mode
  a missing or stale translation shows English; it is a safety net, not a reason to offer it
~~~

## Element object

~~~json
"setup.button.update-restart": {
  "strings":  { "en": { "text": "Update & Restart" }, "ja": { "text": "アップデートして再起動" } },
  "surfaces": ["setup.card"],
  "dataKey":  "entry:control:CONVERGE_DESIRED"
}
~~~

| Namespace | Required | Meaning | Reader |
|---|---|---|---|
| `strings` | yes | words per language: `text`, optional `aria-label`, `_stale` when its English changed | `VexLifeStringCatalog.resolve` |
| `surfaces` | yes | every presentation that shows the element, `{scope}.{surface}` | `keysOn(surface)`: the setup window asks for exactly `setup.window` |
| `dataKey` | when the element presents a semantic ref | `owner:kind:REF` in the owner's own vocabulary | `keysFor(dataKey)`, mapping tests |
| `testId`, `designToken`, `lens` | reserved | automation id, design token, VexBoard Screen Lens address | attach when that system does |

Any other namespace is refused (BLOCK) so a typo cannot become a silent new namespace; adding one
is a change to `VexLifeStringCatalog.Namespaces` and to this table.

## How element identity maps through VexArchitecture

The string key is the element's anchor at every layer. Words change with language; the key and
its `dataKey` do not.

~~~text
Family (owns meaning)
  State ref      UPDATE_AVAILABLE
  Control ref    CONVERGE_DESIRED
      |
      v
DisplayState (Family-authored, language-free)
  title          VexLifeText(setup.heading.update-available)     dataKey entry:state:UPDATE_AVAILABLE
  primaryLabel   VexLifeText(setup.button.update-restart)         dataKey entry:control:CONVERGE_DESIRED
  primaryControlRef  CONVERGE_DESIRED
      |
      v
concrete presentation (resolves words in the selected language, keeps the key)
  Card.titleKey  setup.heading.update-available     Card.title  "VexLife の新しいバージョンを使えます。"
  Button.key     setup.button.update-restart        Button.label "アップデートして再起動"
  Button.actionRef CONTROL:CONVERGE_DESIRED
      |
      v
renderer (AppKit window / browser)
  shows label, returns actionRef; never authors words, never maps words to meaning
~~~

Tests hold the mapping true rather than trusting it:

- a displayed Control's label key carries `entry:control:<that Control>`; a State's title and
  detail carry `entry:state:<that State>`; a journey step's label carries `entry:step:<that step>`;
- every card button's `key` maps through `dataKey` to the exact `actionRef` it returns;
- every `dataKey` names a ref its owner really has (`VexLifeProdSetupElements.DataKeys` reads the
  owners' own vocabularies; it owns none of them);
- every key used in source exists, and every key in source is used by something.

`dataKey` is how VexBoard, a test or an automation finds "the button that converges the Product"
without matching English (or Japanese) words.

## Composite text

A sentence assembled from parts is itself a translatable template, so each language decides its
own order and spacing (Japanese joins sentences without spaces; English with them):

~~~text
setup.description.host-held   "{reason} {requirement} {observed} {outcome}"   (en)
                              "{reason}{requirement}{observed}{outcome}"      (ja)
~~~

`VexLifeText` fills each `{placeholder}` from exactly one of `args` (literal: a path, a chip name),
`gib` (byte counts, shown in the language's decimal style: `14.1` / `14,1`) or `parts` (nested
keyed text, resolved in the same language). A text must fill exactly the placeholders its English
names.

## Integrity and the translation cycle

| Level | Condition | Effect |
|---|---|---|
| BLOCK | key without English, outside its scope or convention, unknown language or namespace, missing `surfaces`, placeholders differing from English | the source is refused at load and by `checkVexLifeStrings` |
| STALE | English changed since the last compile | `compileVexLifeStrings` marks that key's translations `_stale`; they stop showing and the language stops being complete |
| CURRENT | `localization/strings/manifest.json` records each key's 12-character English hash (Vextreme `enHash`) and current languages | `checkVexLifeStrings` and `VexLifeStringsTest` fail when source and manifest disagree |

Changing English: edit the source, run `./gradlew compileVexLifeStrings`, translate every key it
reports stale (remove `_stale`), compile again. A language returns to choosers only when the
scope is complete again.

## Current scopes

| Scope | Surfaces | Languages complete | Source |
|---|---|---|---|
| `setup` | `setup.card`, `setup.window`, `setup.progress`, `setup.failure`, `setup.notice` | en ja zh ko fr de es pt ar hi | `localization/strings/source/setup.json` |

The `setup` translations were written by Claude and have not had native-speaker review yet.

## Language in the Setup Journey

Choosing a language is the first step of setup (see [SETUP-JOURNEY.md](SETUP-JOURNEY.md)):

~~~text
--setup-language --home <p> [--language <code>] [--system-language <code>]
  no effect; language = explicit -> saved in Home -> Mac's preferred -> en
  returns languageChosen, the complete languages, and the setup.window words in that language

--choose-language <code> --home <p>
  saves into the Home only when it is already a canonical Vex Home; never creates one

--inspect-front-door ... --language <code>
--activate-front-door-control <ref> ... --language <code>
  every card, failure, progress line and the ready notice in that language;
  an explicitly chosen language is saved once the Home exists (after FORM_HOME)
~~~

The setup window holds no copy of its own except the two messages shown when the setup program
itself cannot run (no catalog is reachable then).

## Not yet migrated

| Surface | Today | Next |
|---|---|---|
| Workspace chrome (header, settings, composer) | English strings in the generated P1 runtime | `workspace` scope; the service serves the resolved bundle for the person's language |
| Vision / Memory / Private mode DisplayStates | English `String` fields | `VexLifeText`, same as the Entry Family |
| Technical detail and observer diagnostics | English, by design | stays English: it is evidence, quoted as observed |

<!-- [VXG RealForever][VEXLIFE-PROD][LOCALIZATION] -->
