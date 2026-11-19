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
            VexLifePersonSettings.ScreenLanguages.map { it.first },
            VexLifeStrings.catalog.completeLanguages("setup"),
        )
    }

    @Test
    fun theSelectedLanguageIsTheMostExplicitCompleteOne() {
        assertEquals("ja", VexLifeStrings.selectLanguage("setup", "ja-JP", "fr", "de"))
        assertEquals("fr", VexLifeStrings.selectLanguage("setup", null, "fr", "de"))
        assertEquals("de", VexLifeStrings.selectLanguage("setup", "xx", null, "de_DE"))
        assertEquals("zh", VexLifeStrings.selectLanguage("setup", null, null, "zh-Hans-CN"))
        assertEquals("zh-Hant", VexLifeStrings.selectLanguage("setup", null, null, "zh-Hant-TW"))
        assertEquals("zh-Hant", VexLifeStrings.selectLanguage("setup", null, null, "zh_TW"))
        assertEquals("zh-Hant", VexLifeStrings.selectLanguage("setup", null, null, "zh-HK"))
        assertEquals("zh", VexLifeStrings.selectLanguage("setup", null, null, "zh-Hans-HK"))
        assertEquals("zh", VexLifeStrings.selectLanguage("setup", null, null, "zh-CN"))
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
    fun workspaceDataKeysNameRealDiagnosticsRefs() {
        val capabilityRefs =
            vexlife.capability.VexLifeCapabilityAtlas.definitions().flatMap { definition ->
                definition.actions.map { "capability:${definition.handle}:${it.action}" }
            } + "capability:trace:USES" +
                listOf(
                    vexlife.model.VexLifeModelsFamily.ControlSelect,
                    vexlife.model.VexLifeModelsFamily.ControlUse,
                    vexlife.model.VexLifeModelsFamily.ControlUseRecommendedRuntimeConfiguration,
                    vexlife.model.VexLifeModelsFamily.ControlSetPromptCacheEntries,
                    vexlife.model.VexLifeModelsFamily.ControlDownload,
                    vexlife.model.VexLifeModelsFamily.ControlRedownload,
                    vexlife.model.VexLifeModelsFamily.ControlRemove,
                    vexlife.model.VexLifeModelsFamily.ControlForgetImportedModel,
                ).map { "model:control:$it" } +
                listOf(
                    vexlife.model.VexLifeModelsFamily.ArtifactPresence.Absent,
                    vexlife.model.VexLifeModelsFamily.ArtifactPresence.Partial,
                    vexlife.model.VexLifeModelsFamily.ArtifactPresence.Present,
                    vexlife.model.VexLifeModelsFamily.ArtifactPresence.Damaged,
                ).map { "model:files:${it.stateRef}" } +
                vexlife.model.heretic.HereticModelCatalog.Standing.entries.map { "model:standing:${it.name}" }
        val voiceInputRefs =
            vexlife.voiceinput.VexLifeVoiceInputFamily.StateRefs.map { "voice-input:state:$it" } +
                vexlife.voiceinput.VexLifeVoiceInputFamily.ControlRefs.map {
                    "voice-input:control:$it"
                } +
                vexlife.voiceinput.VexLifeVoiceInputFamily.HoldReasonRefs.map {
                    "voice-input:reason:$it"
                } +
                vexlife.voiceinput.VexLifeVoiceInputFamily.FailureReasonRefs.map {
                    "voice-input:failure:$it"
                }
        val localAsrRefs =
            listOf(
                vexlife.localasr.VexLifeLocalAsrProvider.StateReady,
                vexlife.localasr.VexLifeLocalAsrProvider.DetailProviderSourceNotCurrent,
                vexlife.localasr.VexLifeLocalAsrProvider.DetailProviderExecutableUnavailable,
                vexlife.localasr.VexLifeLocalAsrProvider.DetailModelUnavailable,
                vexlife.localasr.VexLifeLocalAsrProvider.DetailModelDigestNotCurrent,
            ).map { "local-asr:state:$it" } +
                "local-asr:selection:SAVED"
        val known =
            capabilityRefs.toSet() +
            voiceInputRefs +
            localAsrRefs +
            setOf(
                "diagnostics:state:" + vexlife.diagnostics.VexLifeDiagnosticsFamily.StateRecording,
                "diagnostics:state:" + vexlife.diagnostics.VexLifeDiagnosticsFamily.StateRecordingEnhanced,
                "diagnostics:control:" + vexlife.diagnostics.VexLifeDiagnosticsFamily.ControlStartEnhanced,
                "diagnostics:control:" + vexlife.diagnostics.VexLifeDiagnosticsFamily.ControlStopEnhanced,
                "diagnostics:control:" + vexlife.diagnostics.VexLifeDiagnosticsFamily.ControlClearHistory,
                "diagnostics:export:DEBUG_ZIP",
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
