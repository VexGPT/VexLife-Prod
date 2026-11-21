package vexlife.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifeWebP1ResponsiveLayoutTest {
    @Test
    fun conversationMeasureScalesWithTheCompanionPane() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts
                .generate()

        assertTrue(
            bundle.css.contains(
                "max-width: 96%; padding: 10px 14px 11px;",
            ),
            "Conversation bubbles should be allowed to grow with the available Companion width",
        )
        assertTrue(
            bundle.css.contains(
                "max-width: 96%; max-height: 220px;",
            ),
            "human-visible reasoning should share the wider conversation measure",
        )
    }

    @Test
    fun oneBrowserCompanionSeamAdaptsItsRendererOrientationOnNarrowScreens() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts
                .generate()
        val javascript =
            bundle.javascript
        val css =
            bundle.css

        assertTrue(
            javascript.contains(
                "const NARROW_BROWSER_DEFAULT_SHARE = 0.34;",
            ),
            "the existing narrow Browser height should remain the renderer default until the human resizes",
        )
        assertTrue(
            javascript.contains(
                "const currentSplitShare = (narrow = narrowWorkspace()) =>",
            ) &&
                javascript.contains(
                    "(narrow ? NARROW_BROWSER_DEFAULT_SHARE : browser.authoredBrowserShare);",
                ),
            "all seam interactions should read one effective allocation rather than duplicate responsive defaults",
        )
        assertTrue(
            javascript.contains(
                "const narrowWorkspace = () => window.matchMedia('(max-width: 860px)').matches;",
            ),
            "responsive orientation must be renderer-local rather than a second spatial owner",
        )
        assertTrue(
            javascript.contains(
                "ui.seam.setAttribute('aria-orientation', narrow ? 'horizontal' : 'vertical');",
            ),
        )
        assertTrue(
            javascript.contains(
                "const setSplitFromPointer = (clientX, clientY) =>",
            ) &&
                javascript.contains(
                    "const position = narrow ? (clientY - rect.top) : (clientX - rect.left);",
                ),
            "the same allocation relationship should resize on the axis that is actually visible",
        )
        assertTrue(
            javascript.contains(
                "const current = currentSplitShare(narrow);",
            ),
            "keyboard resizing must begin from the same responsive share currently shown to the human",
        )
        assertTrue(
            javascript.contains(
                "const decrease = narrow ? 'ArrowUp' : 'ArrowLeft';",
            ) &&
                javascript.contains(
                    "const increase = narrow ? 'ArrowDown' : 'ArrowRight';",
                ),
            "keyboard resizing should follow the manifested seam orientation",
        )
        assertTrue(
            javascript.contains(
                "window.addEventListener('resize', applySplit);",
            ),
            "crossing the responsive breakpoint must currentize the manifested seam orientation",
        )

        assertTrue(
            css.contains(
                "grid-template-rows: minmax(0, var(--before)) 12px minmax(0, 1fr);",
            ),
            "narrow presentation should stack Browser and Companion around the same allocation",
        )
        assertTrue(
            css.contains(
                ".frame[data-collapsed=\"before\"] { grid-template-rows: 0 12px minmax(0, 1fr); }",
            ),
            "Hide browser should remain meaningful when the relationship is stacked vertically",
        )
        assertTrue(
            css.contains(
                ".frame[data-collapsed=\"after\"] { grid-template-rows: minmax(0, 1fr) 12px 0; }",
            ),
            "Hide companion should remain meaningful when the relationship is stacked vertically",
        )
        assertTrue(
            css.contains(
                "cursor: row-resize;",
            ),
        )
        assertTrue(
            css.contains(
                ".seam-collapse { width: 26px; height: 18px; opacity: 1; }",
            ),
            "narrow/touch manifestation must keep the existing collapse affordances perceivable without hover",
        )
        assertFalse(
            css.contains(
                ".seam { display: none; }",
            ),
            "narrow presentation must not discard the existing seam/collapse controls",
        )
    }

    @Test
    fun companionHeaderConsolidatesEngagementAndKeepsRegionOwnedResponsiveLayout() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts
                .generate()

        assertTrue(
            bundle.html.contains(
                "<option value=\"PRIVATE\">Private</option>",
            ),
            "Private should be one engagement choice instead of a second visible header control",
        )
        assertTrue(
            bundle.html.contains(
                "id=\"private-mode\" hidden",
            ),
            "the existing Private semantic Control should remain addressable but not compete for header space",
        )
        assertTrue(
            bundle.html.contains(
                "id=\"vision-control\" hidden",
            ),
            "the existing Vision semantic Control should remain addressable while the Swirl is its visible affordance",
        )
        assertTrue(
            bundle.css.contains(
                "grid-template-areas: \"mode status settings\";",
            ),
            "wide Companion header should allocate only engagement, runtime truth, and navigation",
        )
        assertFalse(
            bundle.css.contains(
                "mode status persistence vision settings",
            ),
            "Private and Vision should no longer consume their own visible header tracks",
        )
        assertTrue(
            bundle.css.contains(
                "container-name: companion;",
            ) &&
                bundle.css.contains(
                    "container-type: inline-size;",
                ),
            "the user-resizable Companion Region must own the width observed by its header",
        )
        assertTrue(
            bundle.css.contains(
                "@container companion (max-width: 820px)",
            ) &&
                bundle.css.contains(
                    "@container companion (max-width: 620px)",
                ) &&
                bundle.css.contains(
                    "@container companion (max-width: 420px)",
                ),
            "Companion header reflow must follow Companion Region width across wide, narrow, and very narrow seam allocations",
        )
        assertTrue(
            bundle.css.contains(
                "\"mode settings\"",
            ) &&
                bundle.css.contains(
                    "\"status status\"",
                ) &&
                bundle.css.contains(
                    "\"settings\";",
                ),
            "narrow Companion layouts should reflow the three visible relationships without resurrecting duplicate controls",
        )
        val workspaceMedia =
            bundle.css
                .substringAfter(
                    "@media (max-width: 860px) {",
                )
                .substringBefore(
                    ".message.waiting .speaker",
                )
        assertFalse(
            workspaceMedia.contains(
                ".companion-head",
            ),
            "Companion header responsiveness must not depend on outer workspace width; seam resize can narrow Companion while the workspace stays wide",
        )
        assertTrue(
            bundle.javascript.contains(
                "requested === 'PRIVATE'",
            ) &&
                bundle.javascript.contains(
                    "await togglePrivate();",
                ),
            "the engagement selector should route Private through the existing Private Family control",
        )
        assertTrue(
            bundle.javascript.contains(
                "visionButton.disabled = !bound || state.visionInFlight;",
            ) &&
                bundle.javascript.contains(
                    "const label = String(display?.label ?? 'Vision · Held');",
                ),
            "the Swirl Vision icon should project the existing Vision Family Held/Control state",
        )
        assertTrue(
            bundle.javascript.contains(
                "'Engage with Vex'",
            ),
            "the header should name the relationship while the selector names the selected engagement mode",
        )
    }

    @Test
    fun collapseControlsRemainOneAccessibleRelationshipAcrossOrientations() {    @Test
    fun collapseControlsRemainOneAccessibleRelationshipAcrossOrientations() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts
                .generate()

        assertTrue(
            bundle.html.contains(
                "id=\"collapse-before\" aria-label=\"Hide browser\"",
            ),
        )
        assertTrue(
            bundle.html.contains(
                "id=\"collapse-after\" aria-label=\"Hide companion\"",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "state.collapsed = state.collapsed === 'before' ? 'none' : 'before';",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "state.collapsed = state.collapsed === 'after' ? 'none' : 'after';",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "const browserAction = (browserCollapsed ? 'Show' : 'Hide') + ' browser';",
            ) &&
                bundle.javascript.contains(
                    "const companionAction = (companionCollapsed ? 'Show' : 'Hide') + ' companion';",
                ),
            "collapse controls should describe the action that will actually happen",
        )
    }
}

// [VXG RealForever]
