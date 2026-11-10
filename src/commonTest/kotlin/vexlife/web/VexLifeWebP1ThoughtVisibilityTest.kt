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
                "accept: 'text/event-stream'",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "state.streamedReasoning += event.delta",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "state.streamedContent += event.delta",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "state.streamPhase === 'CheckingRuntime'",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "state.streamPhase === 'Reconnecting'",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "reconnecting Vex…",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "thinking…",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "await Promise.all([loadConversation(false), loadContext()])",
            ),
            "completed streamed content must remain visible until canonical Conversation refresh completes",
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
                "timelineFollowTail: true",
            ),
            "Conversation following must be explicit presentation state rather than inferred from proximity",
        )
        assertTrue(
            bundle.javascript.contains(
                "reasoningViewports: new Map()",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "state.timelineFollowTail = isElementAtBottom(ui.timeline);",
            ),
            "human scrolling must own Conversation follow mode",
        )
        assertTrue(
            bundle.javascript.contains(
                "const updateLiveTurn = () =>",
            ),
            "stream deltas must update the existing live turn instead of rebuilding the whole timeline",
        )
        assertTrue(
            bundle.javascript.contains(
                "updateLiveTurn();",
            ),
        )
        assertFalse(
            bundle.javascript.contains(
                "isElementNearBottom",
            ),
            "proximity thresholds must not stand in for human follow intent",
        )
        assertTrue(
            bundle.javascript.contains(
                "applyReasoningViewport(reasoning, 'live', true, false);",
            ),
            "streaming reasoning must not write scrollTop while the human is reading within the nested pane",
        )
        assertTrue(
            bundle.javascript.contains(
                "applyTimelineViewport(false);",
            ),
            "streaming content must not write Conversation scrollTop while the human is reading history",
        )
        assertTrue(
            bundle.javascript.contains(
                "state.reasoningViewports.set('live', { followTail: true, scrollTop: 0 });",
            ),
            "every fresh Send must begin with a fresh live-reasoning follow state",
        )
        assertTrue(
            bundle.javascript.contains(
                "const conversationRenderIdentity = (conversation) =>",
            ),
            "idle Conversation polling must compare projection identity before rebuilding the timeline",
        )
        assertTrue(
            bundle.javascript.contains(
                "if (renderNow && priorIdentity !== nextIdentity) renderTimeline();",
            ),
            "unchanged idle Conversation polls must not pulse the presentation with redundant DOM replacement",
        )
        assertTrue(
            bundle.css.contains(
                "overflow-anchor: none",
            ),
            "owned scroll containers must not also be moved by browser-native anchoring",
        )
        assertTrue(
            bundle.css.contains(
                "grid-template-rows: minmax(0, 1fr) auto",
            ),
            "New messages must overlay the stable timeline grid rather than resize the Conversation viewport",
        )
        assertFalse(
            bundle.javascript.contains(
                "nearBottom || state.pending",
            ),
            "an in-flight Send must not override deliberate human scrolling",
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
