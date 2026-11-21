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
    fun companionHeaderKeepsRuntimeAndPersistenceAsDistinctResponsiveRelationships() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts
                .generate()

        assertTrue(
            bundle.html.contains(
                "<span class=\"private-mode-kind\">Conversation</span>",
            ),
            "persistence should identify the human relationship instead of looking like another runtime status",
        )
        assertFalse(
            bundle.html.contains(
                "class=\"private-dot\"",
            ),
            "runtime availability and conversation persistence must not reuse the same status-dot metaphor",
        )
        assertTrue(
            bundle.css.contains(
                "grid-template-areas: \"mode status persistence vision settings\";",
            ),
            "wide Companion header should allocate independent layout cells to runtime and persistence truth",
        )
        assertTrue(
            bundle.css.contains(
                "\"status persistence vision\";",
            ) &&
                bundle.css.contains(
                    "\"persistence vision\";",
                ),
            "narrow Companion layouts should reflow instead of shrinking Ready underneath the mode selector",
        )
        assertTrue(
            bundle.javascript.contains(
                "'Conversation persistence: ' + String(display?.label ?? 'Kept')",
            ),
            "the persistence control should expose its meaning accessibly",
        )
        assertTrue(
            bundle.javascript.contains(
                "'Engage with Vex'",
            ),
            "the header should name the relationship while the selector names the selected engagement mode",
        )
    }

    @Test
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
