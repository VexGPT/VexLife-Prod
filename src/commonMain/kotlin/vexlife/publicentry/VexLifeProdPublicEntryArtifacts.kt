package vexlife.publicentry

import vexlife.contract.VexContractJson

/**
 * Deterministic browser-native realization of [VexLifeProdPublicEntry.Projection].
 *
 * The projection remains the public-entry truth. These artifacts are presentation only and carry no
 * Product execution authority. In particular, the page never observes localhost, Home, model,
 * Conversation, Memory, or process state.
 */
object VexLifeProdPublicEntryArtifacts {
    data class Bundle(
        val html: String,
        val css: String,
        val javascript: String,
        val manifest: String,
    ) {
        val paths: List<String> =
            listOf(
                "index.html",
                "vexlife.css",
                "vexlife.js",
                "public-entry.json",
            )

        fun content(
            path: String,
        ): String =
            when (
                path
            ) {
                "index.html" -> html
                "vexlife.css" -> css
                "vexlife.js" -> javascript
                "public-entry.json" -> manifest
                else ->
                    error(
                        "unknown VexLife public-entry artifact: " +
                            path,
                    )
            }
    }

    fun generate(
        projection: VexLifeProdPublicEntry.Projection,
    ): Bundle =
        Bundle(
            html =
                renderHtml(
                    projection,
                ),
            css = Css,
            javascript = Javascript,
            manifest =
                VexContractJson.encode(
                    projection,
                ),
        )

    private fun renderHtml(
        projection: VexLifeProdPublicEntry.Projection,
    ): String =
        buildString {
            appendLine("<!doctype html>")
            appendLine("<html lang=\"en\" data-vexlife-page=\"" + html(projection.pageRef) + "\">")
            appendLine("<head>")
            appendLine("  <meta charset=\"utf-8\">")
            appendLine("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            appendLine("  <meta name=\"color-scheme\" content=\"light dark\">")
            appendLine("  <meta name=\"theme-color\" content=\"#111318\">")
            appendLine(
                "  <meta name=\"description\" content=\"" +
                    html(
                        "Meet VexLife, understand what stays continuous, and enter the current local Product through one stable source-bound Mac path.",
                    ) +
                    "\">",
            )
            appendLine(
                "  <link rel=\"canonical\" href=\"" +
                    html(
                        projection.canonicalProductPage,
                    ) +
                    "\">",
            )
            appendLine("  <title>VexLife — Continue with your local companion</title>")
            appendLine("  <link rel=\"stylesheet\" href=\"./vexlife.css\">")
            appendLine("  <script type=\"module\" src=\"./vexlife.js\"></script>")
            appendLine("</head>")
            appendLine(
                "<body data-product-effect-class=\"" +
                    html(
                        projection.pageProductEffectClass,
                    ) +
                    "\" data-execution-authority=\"" +
                    html(
                        projection.executionAuthority,
                    ) +
                    "\">",
            )
            appendLine("  <a class=\"skip-link\" href=\"#main\">Skip to VexLife</a>")
            appendLine("  <header class=\"site-header\">")
            appendLine("    <div class=\"shell header-inner\">")
            appendLine("      <div class=\"family-mark\" aria-hidden=\"true\">")
            appendLine("        <svg viewBox=\"0 0 120 120\"><path d=\"M51.89 109.32A50 50 0 1 1 68.11 109.32L39.73 35.68 60 65.41 80.27 35.68Z\"/></svg>")
            appendLine("      </div>")
            appendLine("      <nav class=\"family-nav\" aria-label=\"Vextreme, VexLife, and VexWork\">")
            projection.familyWorlds.forEachIndexed {
                index,
                world,
                ->
                if (
                    index >
                    0
                ) {
                    appendLine("        <span class=\"family-separator\" aria-hidden=\"true\">/</span>")
                }
                append(
                    "        <a href=\"" +
                        html(
                            world.href,
                        ) +
                        "\"",
                )
                if (
                    world.current
                ) {
                    append(" class=\"family-current\" aria-current=\"page\"")
                }
                appendLine(
                    ">" +
                        html(
                            world.label,
                        ) +
                        "</a>",
                )
            }
            appendLine("      </nav>")
            appendLine("      <a class=\"header-action\" href=\"#continue\">Continue with VexLife</a>")
            appendLine("    </div>")
            appendLine("  </header>")

            appendLine("  <main id=\"main\">")
            appendLine("    <section class=\"hero shell\">")
            appendLine("      <div class=\"hero-copy\">")
            appendLine("        <p class=\"eyebrow\">Local-first companion · public entry</p>")
            appendLine(
                "        <h1>" +
                    html(
                        projection.title,
                    ) +
                    "</h1>",
            )
            appendLine(
                "        <p class=\"hero-tagline\">" +
                    html(
                        projection.tagline,
                    ) +
                    "</p>",
            )
            appendLine(
                "        <p class=\"hero-body\">" +
                    html(
                        "Come through the same doorway whether VexLife is already running, needs an update or repair, or is meeting this Mac for the first time. The local Product observes first and decides which choices are lawful.",
                    ) +
                    "</p>",
            )
            appendLine("        <div class=\"hero-actions\">")
            appendLine("          <a class=\"button button-primary\" href=\"#continue\">Continue with VexLife</a>")
            appendLine("          <a class=\"button button-secondary\" href=\"#continuity\">What stays with you</a>")
            appendLine("        </div>")
            appendLine("        <p class=\"effect-note\"><span aria-hidden=\"true\">○</span>This public page performs no VexLife Product effect. Running the Terminal entry is a separate human action.</p>")
            appendLine("      </div>")
            appendLine("      <div class=\"experience-card\" aria-label=\"Illustration of the local VexLife workspace\">")
            appendLine("        <div class=\"experience-topbar\">")
            appendLine("          <span class=\"experience-dot\"></span><span class=\"experience-dot\"></span><span class=\"experience-dot\"></span>")
            appendLine("          <span class=\"experience-address\">Local VexLife</span>")
            appendLine("        </div>")
            appendLine("        <div class=\"experience-body\">")
            appendLine("          <div class=\"browser-preview\">")
            appendLine("            <div class=\"preview-kicker\">Browser</div>")
            appendLine("            <div class=\"browser-card\"><span></span><span></span><span></span></div>")
            appendLine("            <div class=\"browser-card browser-card-short\"><span></span><span></span></div>")
            appendLine("          </div>")
            appendLine("          <div class=\"companion-preview\">")
            appendLine("            <div class=\"preview-head\"><strong>Vex</strong><span>Conversation · Settings</span></div>")
            appendLine("            <div class=\"turn turn-vex\">Here with you. What are we continuing?</div>")
            appendLine("            <div class=\"turn turn-human\">Continue from where we left off.</div>")
            appendLine("            <div class=\"composer-preview\">Message Vex… <span>↑</span></div>")
            appendLine("          </div>")
            appendLine("        </div>")
            appendLine("        <p class=\"experience-caption\">A representation of the local Product. This hosted page is not the connected VexLife runtime.</p>")
            appendLine("      </div>")
            appendLine("    </section>")

            appendLine("    <section class=\"section shell\" id=\"states\">")
            appendLine("      <div class=\"section-heading\">")
            appendLine("        <p class=\"eyebrow\">One doorway · current state decides</p>")
            appendLine("        <h2>You do not have to guess whether to install, update, repair, or open.</h2>")
            appendLine("        <p>The accepted Product-entry Family owns those choices. The public page only explains them.</p>")
            appendLine("      </div>")
            appendLine("      <div class=\"state-grid\">")
            projection.entryOptions.forEach {
                option ->
                appendLine("        <article class=\"state-card\" data-state-ref=\"" + html(option.stateRef) + "\">")
                appendLine("          <p class=\"state-ref\">" + html(humanState(option.stateRef)) + "</p>")
                appendLine("          <h3>" + html(option.title) + "</h3>")
                appendLine("          <p>" + html(option.detail) + "</p>")
                appendLine("          <div class=\"state-actions\">")
                option.primaryLabel
                    ?.let {
                        appendLine("            <span class=\"state-primary\">" + html(it) + "</span>")
                    }
                option.secondaryLabel
                    ?.let {
                        appendLine("            <span class=\"state-secondary\">" + html(it) + "</span>")
                    }
                if (
                    option.primaryLabel ==
                    null &&
                    option.secondaryLabel ==
                    null
                ) {
                    appendLine("            <span class=\"state-secondary\">" + html(option.cancelLabel) + "</span>")
                }
                appendLine("          </div>")
                appendLine("        </article>")
            }
            appendLine("      </div>")
            appendLine("    </section>")

            appendLine("    <section class=\"section continuity-section\" id=\"continuity\">")
            appendLine("      <div class=\"shell\">")
            appendLine("        <div class=\"section-heading section-heading-wide\">")
            appendLine("          <p class=\"eyebrow\">Continuity first</p>")
            appendLine("          <h2>An update is not a request to start your relationship over.</h2>")
            appendLine("          <p>Product entry observes what already exists before it chooses an action. Update and repair are convergence controls, not reset controls.</p>")
            appendLine("        </div>")
            appendLine("        <div class=\"continuity-grid\">")
            appendLine("          <article><span>01</span><h3>Home</h3><p>The existing VexLife Home is observed before Product entry decides whether to open, converge, repair, or set up.</p></article>")
            appendLine("          <article><span>02</span><h3>Conversation + Memory</h3><p>This public doorway has no authority to erase or rewrite them. Product entry preserves the continuity boundary instead of inventing a fresh identity.</p></article>")
            appendLine("          <article><span>03</span><h3>Model relationship</h3><p>Update and repair do not grant acquisition authority. Only initial setup may explicitly authorize missing prerequisite acquisition.</p></article>")
            appendLine("        </div>")
            appendLine("      </div>")
            appendLine("    </section>")

            appendLine("    <section class=\"section shell route-section\">")
            appendLine("      <div class=\"route-card\">")
            appendLine("        <div>")
            appendLine("          <p class=\"eyebrow\">Current admitted Mac host profile</p>")
            appendLine("          <h2>" + html(projection.hostProfile.chipModel) + "</h2>")
            appendLine(
                "          <p>macOS " +
                    html(
                        projection.hostProfile.architecture,
                    ) +
                    " · at least " +
                    html(
                        gib(
                            projection.hostProfile.minimumMemoryBytes,
                        ),
                    ) +
                    " GiB physical memory. The local Product re-observes current prerequisites before effects.</p>",
            )
            appendLine("        </div>")
            appendLine("        <div class=\"route-boundary\">")
            appendLine("          <strong>Current route</strong>")
            appendLine("          <span>Mac Product entry</span>")
            appendLine("          <span>Windows realization remains a later platform adapter over the same Product-entry meaning.</span>")
            appendLine("        </div>")
            appendLine("      </div>")
            appendLine("    </section>")

            appendLine("    <section class=\"section shell continue-section\" id=\"continue\">")
            appendLine("      <div class=\"continue-copy\">")
            appendLine("        <p class=\"eyebrow\">One stable Mac entry</p>")
            appendLine("        <h2>Continue with VexLife.</h2>")
            appendLine("        <p>This command downloads the current source-bound VexLife-Prod bootstrap and its published SHA-256 from the current GitHub Release, verifies the bootstrap, then opens the native Product entry. The page itself does not run it.</p>")
            appendLine("      </div>")
            appendLine("      <div class=\"terminal-card\">")
            appendLine("        <div class=\"terminal-head\"><span>Mac Terminal</span><button type=\"button\" data-copy-command>Copy command</button></div>")
            appendLine("        <pre><code id=\"mac-command\">")
            append(
                html(
                    projection.bootstrapCommand
                        .joinToString(
                            "\n",
                        ),
                ),
            )
            appendLine("</code></pre>")
            appendLine("        <p class=\"terminal-note\">The stable command follows the Product release channel. You do not need to track a changing source SHA yourself.</p>")
            appendLine("      </div>")
            appendLine("    </section>")

            appendLine("    <section class=\"section shell truth-section\">")
            appendLine("      <div class=\"section-heading\">")
            appendLine("        <p class=\"eyebrow\">Technical truth without making it your job</p>")
            appendLine("        <h2>The identities remain inspectable underneath the simple doorway.</h2>")
            appendLine("      </div>")
            appendLine("      <dl class=\"truth-list\">")
            appendLine("        <div><dt>Current Product source</dt><dd><a href=\"https://github.com/VexGPT/VexLife-Prod\">VexGPT/VexLife-Prod</a></dd></div>")
            appendLine("        <div><dt>Release channel</dt><dd>" + html(projection.releaseCoordinate.currentChannel) + "</dd></div>")
            appendLine("        <div><dt>Stable bootstrap asset</dt><dd><code>" + html(projection.releaseCoordinate.currentBootstrapAssetName) + "</code></dd></div>")
            appendLine("        <div><dt>Stable human doorway</dt><dd><a href=\"" + html(projection.stableHumanDoorway) + "\">" + html(projection.stableHumanDoorway) + "</a></dd></div>")
            appendLine("        <div><dt>This public page</dt><dd>Product effects = NONE · execution authority = NONE</dd></div>")
            appendLine("      </dl>")
            appendLine("    </section>")
            appendLine("  </main>")

            appendLine("  <footer class=\"site-footer\">")
            appendLine("    <div class=\"shell footer-inner\">")
            appendLine("      <div><strong>VexLife</strong><span>Your local companion, continued through one stable doorway.</span></div>")
            appendLine("      <a href=\"https://github.com/VexGPT/VexLife-Prod\">Source &amp; technical details</a>")
            appendLine("    </div>")
            appendLine("  </footer>")
            appendLine("</body>")
            appendLine("</html>")
        }

    private fun humanState(
        stateRef: String,
    ): String =
        when (
            stateRef
        ) {
            "CURRENT" -> "Already current"
            "UPDATE_AVAILABLE" -> "Update available"
            "REPAIR_REQUIRED" -> "Repair needed"
            "UPDATE_REPAIR_REQUIRED" -> "Update + repair"
            "SETUP_REQUIRED" -> "First setup"
            "HOLD_UNKNOWN" -> "Ownership unclear"
            else -> stateRef
        }

    private fun gib(
        bytes: Long,
    ): String =
        (
            bytes /
                (
                    1024L *
                        1024L *
                        1024L
                )
        )
            .toString()

    private fun html(
        value: String,
    ): String =
        value
            .replace(
                "&",
                "&amp;",
            )
            .replace(
                "\"",
                "&quot;",
            )
            .replace(
                "<",
                "&lt;",
            )
            .replace(
                ">",
                "&gt;",
            )

    private val Css =
        """
        :root {
          color-scheme: light dark;
          --bg: #0e1015;
          --bg-elevated: #151821;
          --panel: rgba(255,255,255,.055);
          --panel-strong: rgba(255,255,255,.09);
          --ink: #f7f7f4;
          --muted: #a9adba;
          --line: rgba(255,255,255,.12);
          --accent: #d7ff75;
          --accent-ink: #11140b;
          --soft: #c7b7ff;
          --max: 1180px;
          --radius: 24px;
          font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
        }
        * { box-sizing: border-box; }
        html { scroll-behavior: smooth; }
        body {
          margin: 0;
          background:
            radial-gradient(circle at 76% 12%, rgba(199,183,255,.13), transparent 28rem),
            radial-gradient(circle at 8% 28%, rgba(215,255,117,.07), transparent 24rem),
            var(--bg);
          color: var(--ink);
          line-height: 1.55;
        }
        a { color: inherit; }
        button, code, pre { font: inherit; }
        .skip-link { position:absolute; left:-9999px; top:0; }
        .skip-link:focus { left:1rem; top:1rem; z-index:100; background:var(--ink); color:var(--bg); padding:.6rem 1rem; border-radius:999px; }
        .shell { width:min(calc(100% - 2rem), var(--max)); margin:0 auto; }
        .site-header {
          position: sticky; top:0; z-index:50;
          backdrop-filter: blur(18px);
          background: rgba(14,16,21,.78);
          border-bottom:1px solid var(--line);
        }
        .header-inner { min-height:72px; display:flex; align-items:center; gap:1rem; }
        .family-mark { width:34px; height:34px; flex:0 0 auto; }
        .family-mark svg { width:100%; height:100%; fill:var(--ink); }
        .family-nav { display:flex; align-items:center; gap:.55rem; font-size:.92rem; color:var(--muted); }
        .family-nav a { text-decoration:none; }
        .family-nav a:hover { color:var(--ink); }
        .family-current { color:var(--ink)!important; font-weight:700; }
        .family-separator { opacity:.35; }
        .header-action {
          margin-left:auto; text-decoration:none; font-size:.9rem; font-weight:700;
          padding:.65rem .95rem; border:1px solid var(--line); border-radius:999px;
          background:var(--panel);
        }
        .hero { min-height:720px; display:grid; grid-template-columns:1.02fr .98fr; gap:3.5rem; align-items:center; padding:6rem 0 5rem; }
        .eyebrow { margin:0 0 1rem; text-transform:uppercase; letter-spacing:.14em; font-size:.76rem; font-weight:800; color:var(--accent); }
        h1, h2, h3, p { margin-top:0; }
        h1 { font-size:clamp(4.5rem, 10vw, 8.5rem); line-height:.84; letter-spacing:-.07em; margin-bottom:1.4rem; }
        .hero-tagline { font-size:clamp(1.35rem, 2.2vw, 2rem); line-height:1.2; max-width:16ch; margin-bottom:1.2rem; }
        .hero-body { color:var(--muted); max-width:62ch; font-size:1.05rem; }
        .hero-actions { display:flex; flex-wrap:wrap; gap:.75rem; margin:2rem 0 1.4rem; }
        .button { display:inline-flex; min-height:48px; align-items:center; justify-content:center; padding:.75rem 1.1rem; border-radius:999px; text-decoration:none; font-weight:800; }
        .button-primary { background:var(--accent); color:var(--accent-ink); }
        .button-secondary { border:1px solid var(--line); background:var(--panel); }
        .effect-note { display:flex; gap:.55rem; color:var(--muted); font-size:.87rem; }
        .effect-note span { color:var(--accent); }
        .experience-card { border:1px solid var(--line); background:linear-gradient(145deg, rgba(255,255,255,.09), rgba(255,255,255,.035)); border-radius:30px; box-shadow:0 28px 80px rgba(0,0,0,.28); overflow:hidden; transform:rotate(1.1deg); }
        .experience-topbar { height:52px; display:flex; align-items:center; gap:.4rem; padding:0 1rem; border-bottom:1px solid var(--line); }
        .experience-dot { width:8px; height:8px; border-radius:50%; background:rgba(255,255,255,.35); }
        .experience-address { margin-left:.5rem; color:var(--muted); font-size:.78rem; }
        .experience-body { display:grid; grid-template-columns:1fr 1fr; min-height:410px; }
        .browser-preview, .companion-preview { padding:1.2rem; }
        .browser-preview { border-right:1px solid var(--line); background:rgba(255,255,255,.025); }
        .preview-kicker { font-size:.72rem; text-transform:uppercase; letter-spacing:.12em; color:var(--muted); margin-bottom:1rem; }
        .browser-card { min-height:145px; padding:1rem; border-radius:18px; background:rgba(255,255,255,.06); margin-bottom:.8rem; }
        .browser-card span { display:block; height:8px; border-radius:999px; background:rgba(255,255,255,.18); margin:.7rem 0; }
        .browser-card span:nth-child(2) { width:76%; }
        .browser-card span:nth-child(3) { width:54%; }
        .browser-card-short { min-height:95px; }
        .preview-head { display:flex; justify-content:space-between; align-items:center; gap:.5rem; margin-bottom:1.2rem; }
        .preview-head span { color:var(--muted); font-size:.72rem; }
        .turn { padding:.8rem .9rem; border-radius:16px; font-size:.86rem; margin:.75rem 0; max-width:92%; }
        .turn-vex { background:rgba(199,183,255,.12); border:1px solid rgba(199,183,255,.18); }
        .turn-human { margin-left:auto; background:rgba(215,255,117,.12); border:1px solid rgba(215,255,117,.18); }
        .composer-preview { margin-top:2rem; border:1px solid var(--line); border-radius:16px; padding:.85rem; color:var(--muted); display:flex; justify-content:space-between; }
        .composer-preview span { color:var(--ink); }
        .experience-caption { margin:0; padding:.8rem 1.2rem 1rem; color:var(--muted); font-size:.76rem; border-top:1px solid var(--line); }
        .section { padding:6rem 0; }
        .section-heading { max-width:720px; margin-bottom:2.5rem; }
        .section-heading-wide { max-width:850px; }
        .section-heading h2, .continue-copy h2, .route-card h2 { font-size:clamp(2.2rem, 4.5vw, 4rem); line-height:.98; letter-spacing:-.045em; margin-bottom:1rem; }
        .section-heading > p:last-child, .continue-copy > p:last-child { color:var(--muted); }
        .state-grid { display:grid; grid-template-columns:repeat(3, 1fr); gap:1rem; }
        .state-card { min-height:280px; display:flex; flex-direction:column; border:1px solid var(--line); border-radius:var(--radius); padding:1.4rem; background:var(--panel); }
        .state-ref { font-size:.72rem; text-transform:uppercase; letter-spacing:.1em; color:var(--accent); }
        .state-card h3 { font-size:1.35rem; line-height:1.15; }
        .state-card > p:not(.state-ref) { color:var(--muted); font-size:.93rem; }
        .state-actions { margin-top:auto; padding-top:1.2rem; display:flex; flex-wrap:wrap; gap:.5rem; }
        .state-primary, .state-secondary { display:inline-flex; padding:.45rem .68rem; border-radius:999px; font-size:.75rem; font-weight:800; }
        .state-primary { background:var(--accent); color:var(--accent-ink); }
        .state-secondary { border:1px solid var(--line); }
        .continuity-section { border-block:1px solid var(--line); background:rgba(255,255,255,.022); }
        .continuity-grid { display:grid; grid-template-columns:repeat(3, 1fr); gap:1rem; }
        .continuity-grid article { border-top:1px solid var(--line); padding-top:1.4rem; }
        .continuity-grid article > span { color:var(--accent); font-size:.76rem; font-weight:800; }
        .continuity-grid h3 { margin:.6rem 0; font-size:1.4rem; }
        .continuity-grid p { color:var(--muted); }
        .route-card { border:1px solid var(--line); border-radius:32px; padding:2rem; background:linear-gradient(120deg, rgba(199,183,255,.08), rgba(215,255,117,.05)); display:grid; grid-template-columns:1.25fr .75fr; gap:2rem; align-items:end; }
        .route-boundary { display:flex; flex-direction:column; gap:.45rem; color:var(--muted); font-size:.9rem; }
        .route-boundary strong { color:var(--ink); }
        .continue-section { display:grid; grid-template-columns:.82fr 1.18fr; gap:3rem; align-items:start; }
        .continue-copy { position:sticky; top:110px; }
        .terminal-card { border:1px solid var(--line); border-radius:24px; overflow:hidden; background:#090b0f; color:#e7e9ee; box-shadow:0 26px 70px rgba(0,0,0,.3); }
        .terminal-head { min-height:52px; padding:0 1rem; display:flex; align-items:center; justify-content:space-between; border-bottom:1px solid rgba(255,255,255,.1); color:#aeb3c0; font-size:.8rem; }
        .terminal-head button { border:1px solid rgba(255,255,255,.14); background:rgba(255,255,255,.07); color:#fff; padding:.45rem .7rem; border-radius:999px; cursor:pointer; }
        .terminal-card pre { margin:0; padding:1.2rem; overflow:auto; font-family:"SFMono-Regular", Consolas, monospace; font-size:.78rem; line-height:1.65; white-space:pre; }
        .terminal-card code { font-family:inherit; }
        .terminal-note { margin:0; padding:0 1.2rem 1.2rem; color:#8d93a2; font-size:.78rem; }
        .truth-list { margin:0; display:grid; grid-template-columns:1fr 1fr; border-top:1px solid var(--line); }
        .truth-list div { display:grid; grid-template-columns:170px 1fr; gap:1rem; padding:1rem 0; border-bottom:1px solid var(--line); }
        .truth-list div:nth-child(odd) { padding-right:1.5rem; }
        .truth-list div:nth-child(even) { padding-left:1.5rem; border-left:1px solid var(--line); }
        .truth-list dt { color:var(--muted); }
        .truth-list dd { margin:0; overflow-wrap:anywhere; }
        .site-footer { border-top:1px solid var(--line); padding:2rem 0 3rem; color:var(--muted); }
        .footer-inner { display:flex; justify-content:space-between; gap:2rem; align-items:flex-start; }
        .footer-inner div { display:flex; flex-direction:column; gap:.25rem; }
        .footer-inner strong { color:var(--ink); }
        @media (max-width: 900px) {
          .hero { grid-template-columns:1fr; min-height:auto; padding-top:4rem; }
          .experience-card { transform:none; }
          .state-grid, .continuity-grid { grid-template-columns:1fr 1fr; }
          .continue-section { grid-template-columns:1fr; }
          .continue-copy { position:static; }
          .route-card { grid-template-columns:1fr; }
          .truth-list { grid-template-columns:1fr; }
          .truth-list div:nth-child(odd), .truth-list div:nth-child(even) { padding-inline:0; border-left:0; }
        }
        @media (max-width: 620px) {
          .shell { width:min(calc(100% - 1.25rem), var(--max)); }
          .header-action { display:none; }
          .family-mark { width:28px; height:28px; }
          .family-nav { font-size:.8rem; gap:.35rem; }
          h1 { font-size:4rem; }
          .hero { padding-top:3.2rem; }
          .experience-body { grid-template-columns:1fr; }
          .browser-preview { border-right:0; border-bottom:1px solid var(--line); }
          .state-grid, .continuity-grid { grid-template-columns:1fr; }
          .section { padding:4.5rem 0; }
          .truth-list div { grid-template-columns:1fr; gap:.25rem; }
          .footer-inner { flex-direction:column; }
        }
        @media (prefers-color-scheme: light) {
          :root {
            --bg:#f6f5f1;
            --bg-elevated:#fff;
            --panel:rgba(20,20,24,.045);
            --panel-strong:rgba(20,20,24,.075);
            --ink:#17181b;
            --muted:#676a73;
            --line:rgba(20,20,24,.13);
            --accent:#365f08;
            --accent-ink:#fff;
            --soft:#5d49a8;
          }
          body { background:radial-gradient(circle at 76% 12%, rgba(93,73,168,.08), transparent 28rem), radial-gradient(circle at 8% 28%, rgba(54,95,8,.06), transparent 24rem), var(--bg); }
          .site-header { background:rgba(246,245,241,.82); }
          .terminal-card { color:#e7e9ee; }
        }
        """.trimIndent() +
            "\n"

    private val Javascript =
        """
        const button = document.querySelector('[data-copy-command]');
        const command = document.querySelector('#mac-command');

        button?.addEventListener('click', async () => {
          if (!command) return;
          const text = command.textContent ?? '';
          try {
            await navigator.clipboard.writeText(text);
            const previous = button.textContent;
            button.textContent = 'Copied';
            window.setTimeout(() => { button.textContent = previous; }, 1600);
          } catch {
            const selection = window.getSelection();
            const range = document.createRange();
            range.selectNodeContents(command);
            selection?.removeAllRanges();
            selection?.addRange(range);
            button.textContent = 'Select + copy';
          }
        });
        """.trimIndent() +
            "\n"
}

// [VXG RealForever]
