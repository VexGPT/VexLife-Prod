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
                "if (message.role !== 'vex') return null;",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "humanReasoningByResponse: new Map()",
            ),
        )
        assertTrue(
            bundle.javascript.contains(
                "message.messageRef ?? message.eventRef ?? ''",
            ),
            "the existing completed thought renderer should accept Private's existing presentation-local event identity",
        )
        assertTrue(
            bundle.javascript.contains(
                "const reasoning = String(exchange.sealedHumanReasoning ?? '').trim();",
            ) &&
                bundle.javascript.contains(
                    "state.humanReasoningByResponse.set(vexKey, reasoning);",
                ),
            "Private reasoning should enter the existing ephemeral reasoning presentation map",
        )
        assertTrue(
            bundle.javascript.contains(
                "const pruneInactiveEphemeralReasoning = (activePrefix) =>",
            ) &&
                bundle.javascript.contains(
                    "key.startsWith('private.') || key.startsWith('vex-labs.')",
                ) &&
                bundle.javascript.contains(
                    "if (!ephemeral || (activePrefix && key.startsWith(activePrefix))) continue;",
                ) &&
                bundle.javascript.contains(
                    "state.reasoningViewports.delete('response.' + key);",
                ),
            "the shared Conversation surface must retire only inactive ephemeral reasoning while preserving the active Private or Vex Labs surface",
        )
        assertFalse(
            bundle.javascript.contains(
                "privateThoughtNode",
            ) ||
                bundle.javascript.contains(
                    "privateReasoningByResponse",
                ),
            "Private must reuse the existing thought renderer and reasoning map rather than fork them",
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
                "appendProcessReasoning(event.delta);",
            ),
            "visual thought containers must observe reasoning deltas without replacing the raw stream",
        )
        assertTrue(
            bundle.javascript.contains(
                "projectProcessReach(use);",
            ),
            "capability request/result facts must become meaningful process entries",
        )
        assertTrue(
            bundle.javascript.contains(
                "freezeActiveProcessThought();",
            ),
            "a real relationship boundary must freeze the prior thought container before the next one can open",
        )
        assertTrue(
            bundle.javascript.contains(
                "speech.stream.thought = speakFresh(state.streamedReasoning",
            ),
            "Speech/Kokoro must keep consuming the raw reasoning stream rather than visual containers",
        )
        assertTrue(
            bundle.javascript.contains(
                "speech.stream.reply = speakFresh(state.streamedContent",
            ),
            "Speech/Kokoro must keep consuming the raw final-answer stream",
        )
        assertFalse(
            bundle.javascript.contains(
                "speakFresh(state.liveProcessEntries",
            ),
            "visual process history must never become Speech input",
        )
        assertFalse(
            bundle.javascript.contains(
                "providerStageIndex",
            ),
            "presentation must not invent or expose provider-stage ownership",
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
                "if (state.liveProcessManuallyToggled && state.liveProcessExpanded)",
            ),
            "completion must preserve an explicit human choice to keep the live process open",
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
                "const syncLiveProcessPanel = (thought) =>",
            ),
            "live process rendering must synchronize one mounted panel instead of reconstructing it per delta",
        )
        assertTrue(
            bundle.javascript.contains(
                "const syncLiveProcessHistory = (history) =>",
            ),
            "streaming process entries must append/update stable child nodes",
        )
        assertTrue(
            bundle.javascript.contains(
                "node.dataset.processEntry = key;",
            ),
            "each live process entry needs stable DOM identity for the duration of the Send",
        )
        assertTrue(
            bundle.javascript.contains(
                "let body = node.querySelector('.text-presentation');",
            ),
            "active reasoning must keep presentation inside the existing scrollable viewport",
        )
        assertTrue(
            bundle.javascript.contains(
                "scheduleLiveTextPresentation(",
            ),
            "active reasoning presentation should be frame-batched without replacing its viewport",
        )
        assertTrue(
            bundle.javascript.contains(
                "applyReasoningViewport(node, viewportKey, true, false);",
            ),
            "reasoning geometry changes must return to the reasoning viewport owner",
        )
        assertFalse(
            bundle.javascript.contains(
                "node.textContent = entry.text",
            ),
            "raw reasoning replacement must not overwrite the mounted TextPresentation subtree",
        )
        assertTrue(
            bundle.javascript.contains(
                "history.hidden = !expanded;",
            ),
            "collapsing process history must hide rather than destroy mounted streaming nodes",
        )
        assertTrue(
            bundle.javascript.contains(
                "if (thought) syncLiveProcessPanel(thought);",
            ),
            "stream updates must converge into the existing live process DOM",
        )
        assertFalse(
            bundle.javascript.contains(
                "thought?.querySelector('.live-process-panel')?.remove();",
            ),
            "never destroy the live process panel while a human may be scrolling it",
        )
        assertFalse(
            bundle.javascript.contains(
                "const process = liveProcessPanel();",
            ),
            "the old rebuild-per-delta process projection must not return",
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
                "const liveTail = element.dataset.liveTail === 'true';",
            ),
            "only the currently active thought container may follow its live tail",
        )
        assertTrue(
            bundle.javascript.contains(
                "if (key === 'live' || key.startsWith('live.process.'))",
            ),
            "a fresh Send must retire only prior live-process viewport state",
        )
        assertTrue(
            bundle.javascript.contains(
                "resetLiveProcessViewports();",
            ),
            "fresh Send must begin with fresh visual process viewport state",
        )
        assertTrue(
            bundle.javascript.contains(
                "applyTimelineViewport(false);",
            ),
            "streaming content must not write Conversation scrollTop while the human is reading history",
        )
        assertFalse(
            bundle.javascript.contains(
                "state.reasoningViewports.set('live', { followTail: true, scrollTop: 0 });",
            ),
            "the obsolete single live-thought viewport must not stand in for process containers",
        )
        assertTrue(
            bundle.javascript.contains(
                "const conversationRenderIdentity = (conversation) =>",
            ),
            "idle Conversation polling must compare projection identity before rebuilding the timeline",
        )
        assertTrue(
            Regex("""if \(renderNow && priorIdentity !== nextIdentity\) \{\s*renderTimeline\(\);""")
                .containsMatchIn(bundle.javascript),
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
