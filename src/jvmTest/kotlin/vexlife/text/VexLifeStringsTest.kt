package vexlife.text

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import vexlife.person.VexLifePersonSettings

class VexLifeStringsTest {
    private val root = Path.of(System.getProperty("user.dir"))

    @Test
    fun stringSourceIsCurrentWithItsManifest() {
        assertEquals(emptyList(), VexLifeStringsSource.check(root))
    }

    @Test
    fun everySourceFileIsAShippedScope() {
        assertEquals(VexLifeStringsSource.scopeNames(root), VexLifeStrings.Scopes.sorted())
    }

    @Test
    fun setupIsCompleteInEveryLanguageVexLifeOffers() {
        assertEquals(
            VexLifePersonSettings.Languages.map { it.first },
            VexLifeStrings.catalog.completeLanguages("setup"),
        )
    }

    @Test
    fun theSelectedLanguageIsTheMostExplicitCompleteOne() {
        assertEquals("ja", VexLifeStrings.selectLanguage("setup", "ja-JP", "fr", "de"))
        assertEquals("fr", VexLifeStrings.selectLanguage("setup", null, "fr", "de"))
        assertEquals("de", VexLifeStrings.selectLanguage("setup", "xx", null, "de_DE"))
        assertEquals("zh", VexLifeStrings.selectLanguage("setup", null, null, "zh-Hans-CN"))
        assertEquals("en", VexLifeStrings.selectLanguage("setup", null, null, null))
    }

    @Test
    fun everyWorkspaceKeyIsUsedAndEveryUsedWorkspaceKeyExists() {
        val literal = Regex("[\"'](workspace\\.(?:button|label|heading|description|status|placeholder|nav|legend)\\.[a-z0-9-]+)[\"']")
        val used =
            Files.walk(root.resolve("src")).use { stream ->
                stream
                    .filter { it.toString().endsWith(".kt") && "Test" !in it.fileName.toString() }
                    .toList()
                    .flatMap { file -> literal.findAll(Files.readString(file)).map { it.groupValues[1] }.toList() }
                    .toSet()
            }
        val defined = VexLifeStrings.catalog.keys.filter { it.startsWith("workspace.") }.toSet()
        assertEquals(emptySet(), used - defined, "workspace keys used but missing")
        assertEquals(emptySet(), defined - used, "workspace keys nothing shows")
    }

    @Test
    fun workspaceDataKeysNameRealSemanticRefs() {
        val capabilityRefs =
            vexlife.capability.VexLifeCapabilityAtlas.definitions().flatMap { definition ->
                definition.actions.map { "capability:${definition.handle}:${it.action}" }
            } + "capability:trace:USES" +
                listOf(
                    vexlife.model.VexLifeModelsFamily.ControlUse,
                    vexlife.model.VexLifeModelsFamily.ControlDownload,
                    vexlife.model.VexLifeModelsFamily.ControlRedownload,
                    vexlife.model.VexLifeModelsFamily.ControlRemove,
                ).map { "model:control:$it" } +
                vexlife.model.VexLifeModelsFamily.Files.entries.map { "model:files:${it.name}" } +
                vexlife.model.heretic.HereticModelCatalog.Standing.entries.map { "model:standing:${it.name}" } +
                vexlife.voiceinput.VexLifeVoiceInputFamily.StateRefs.map { "voice-input:state:$it" } +
                vexlife.voiceinput.VexLifeVoiceInputFamily.ControlRefs.map { "voice-input:control:$it" } +
                vexlife.voiceinput.VexLifeVoiceInputFamily.HoldReasonRefs.map { "voice-input:reason:$it" } +
                vexlife.voiceinput.VexLifeVoiceInputFamily.FailureReasonRefs.map { "voice-input:failure:$it" } +
                vexlife.conversation.VexLifeConversationTurnFamily.StateRefs.map { "conversation-turn:state:$it" } +
                vexlife.conversation.VexLifeConversationTurnFamily.ControlRefs.map { "conversation-turn:control:$it" }
        val known =
            capabilityRefs.toSet() +
            setOf(
                "diagnostics:state:" + vexlife.diagnostics.VexLifeDiagnosticsFamily.StateRecording,
                "diagnostics:state:" + vexlife.diagnostics.VexLifeDiagnosticsFamily.StateRecordingEnhanced,
                "diagnostics:control:" + vexlife.diagnostics.VexLifeDiagnosticsFamily.ControlStartEnhanced,
                "diagnostics:control:" + vexlife.diagnostics.VexLifeDiagnosticsFamily.ControlStopEnhanced,
                "diagnostics:control:" + vexlife.diagnostics.VexLifeDiagnosticsFamily.ControlClearHistory,
                "diagnostics:export:DEBUG_ZIP",
                "local-asr:selection:SAVED",
                "local-asr:state:" + vexlife.localasr.VexLifeLocalAsrProvider.StateReady,
                "local-asr:state:" + vexlife.localasr.VexLifeLocalAsrProvider.DetailProviderSourceNotCurrent,
                "local-asr:state:" + vexlife.localasr.VexLifeLocalAsrProvider.DetailProviderExecutableUnavailable,
                "local-asr:state:" + vexlife.localasr.VexLifeLocalAsrProvider.DetailModelUnavailable,
                "local-asr:state:" + vexlife.localasr.VexLifeLocalAsrProvider.DetailModelDigestNotCurrent,
            )
        VexLifeStrings.catalog.keys
            .filter { it.startsWith("workspace.") }
            .mapNotNull { VexLifeStrings.catalog.dataKey(it) }
            .forEach { assertTrue(it in known, it) }
    }

    @Test
    fun everyKeyIsAnElementIdentityOnAtLeastOneSurface() {
        for (key in VexLifeStrings.catalog.keys) {
            assertTrue(VexLifeStrings.catalog.entry(key).surfaces.isNotEmpty(), key)
        }
    }
}

// [VXG RealForever]
