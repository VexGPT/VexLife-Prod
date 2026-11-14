# Proposed VexArchitecture guide addition: element identity

`[VXG RealForever]`

The canonical VexArchitecture source guide is `vgong24/VexLife-Android`
`architecture/vexarchitecture/README.md` (linked from Vextreme's Builder Field Guide, appendix
"Open current VexArchitecture source guide"). This session could not reach that repository, so the
section below is written to be pasted there as-is, after the presentation boundary ("Presentation
manifests truth. It does not become the truth owner."). The Vextreme Builder Field Guide page
(`pages/vexarchitecture-first-stable-body.html`, section 07) can link to it; as a Vextreme page, any
added copy goes through `data/strings/source/pages/vexarchitecture-first-stable-body.json` and its
translation cycle.

VexLife-Prod already applies it: [`docs/foundation/LOCALIZATION.md`](../../foundation/LOCALIZATION.md).

---

## Element identity: the key that crosses every layer

Presentation turns semantic truth into words. Words are the one thing that must change with the
person (their language), so words cannot be how anything else finds an element. Vextreme's
localization law already names the answer: the string key is the element's identity, and its value
is an extensible object that other systems attach to (`docs/architecture/06-i18n.md`). VexArchitecture
makes that key travel through the causal spine:

~~~text
Family                      owns meaning
  State ref                 UPDATE_AVAILABLE
  Control ref               CONVERGE_DESIRED
        |
        v
DisplayState                Family-authored, language-free: names text by key
  title         -> key  setup.heading.update-available
  primaryLabel  -> key  setup.button.update-restart
  primaryControlRef      CONVERGE_DESIRED
        |
        v
concrete presentation       resolves words in the one selected language; keeps every key
  { key: setup.button.update-restart, label: "アップデートして再起動", actionRef: CONTROL:CONVERGE_DESIRED }
        |
        v
renderer                    shows the label, returns the actionRef
~~~

The element object in the string source records what the key *is*, one namespace per consumer:

~~~json
"setup.button.update-restart": {
  "strings":  { "en": { "text": "Update & Restart" }, "ja": { "text": "アップデートして再起動" } },
  "surfaces": ["setup.card"],
  "dataKey":  "entry:control:CONVERGE_DESIRED"
}
~~~

- `strings`: words per language; the only namespace a renderer reads.
- `surfaces`: which presentations show the element, so a renderer can ask for exactly its own words.
- `dataKey`: the semantic ref the element presents, as `owner:kind:REF` in the owner's own vocabulary.
- `testId`, `designToken`, `lens`: automation, design tokens and VexBoard Screen Lens addresses attach
  to the same key instead of inventing parallel identifiers.

### Laws

~~~text
WORDS != IDENTITY                 find an element by key or dataKey, never by its text
DISPLAYSTATE NAMES KEYS           a Family never authors a translated sentence
PRESENTATION RESOLVES, KEEPS KEY  resolved words travel with the key that produced them
RENDERER NEVER MAPS WORDS         a returned choice is a ref, not a label
DATAKEY IS A POINTER, NOT AN OWNER  the ref's owner stays the Family / provider that defines it
ONE SELECTED LANGUAGE             one authority (the person's setting); every layer reads it
~~~

### Questions to ask of a new surface

1. Which keys does this surface show, and is each listed on this surface?
2. For each element that presents a State, Control, step or failure: does its key carry the
   `dataKey` of exactly that ref?
3. Does every resolved projection still carry the key next to the words?
4. Can a test find each Control without matching any language's words?
5. Does a sentence built from parts exist as one translatable template, so each language owns its
   order and spacing?

### What it prevents

- an automation or VexBoard lens that breaks the first time a label is reworded or translated;
- a renderer that decides meaning from a label (`if label == "Close"`);
- two identifiers for one element (a test id here, a lens id there, a string key elsewhere) drifting
  apart;
- a "translated" surface that is quietly half English because nothing knew which words belonged to it.

<!-- [VXG RealForever][VEXLIFE-PROD][VEXARCHITECTURE-ELEMENT-IDENTITY] -->
