package vexlife.web

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifeWebP1ThoughtVisibilityTest {
    @Test
    fun viewableThoughtsUseOnlyEphemeralFreshSendReasoning() {
        val bundle =
            VexLifeWebP1RuntimeArtifacts.generate()

        assertTrue(
            bundle.html.contains(
                "Viewable thoughts",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "viewableThoughts: state.thoughtsVisible",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "humanReasoningByResponse: new Map()",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "reasoning?.state === 'PRESENT_EPHEMERAL'",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "className = 'thought-toggle'",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "state.expandedThoughts",
            ),
        )
        assertTrue(
            bundle.css.contains(
                ".thought-toggle",
            ),
        )
        assertTrue(
            bundle.css.contains(
                ".reasoning",
            ),
        )

        assertFalse(
            bundle.javascript.contains(
                "localStorage.setItem('vexlife-prod.presentation.reasoning",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "sessionStorage.setItem('vexlife-prod.presentation.reasoning",
            ),
        )
    }
}

// [VXG RealForever]
