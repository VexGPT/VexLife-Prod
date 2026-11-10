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
        assertFalse(
            bundle.javascript.contains(
                "viewableThoughts",
            ),
            "Viewable thoughts must remain local presentation state and never enter the Send wire request",
        )
        assertTrue(
            bundle.javascript.contains(
                "if (!state.thoughtsVisible || message.role !== 'vex') return null;",
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
            bundle.javascript.contains(
                "vexlife-prod.presentation.thoughts-visible",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "localStorage.getItem(THOUGHT_VISIBILITY_KEY)",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "localStorage.setItem(THOUGHT_VISIBILITY_KEY, String(state.thoughtsVisible))",
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
