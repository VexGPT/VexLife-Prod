package vexlife.setup

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.io.path.walk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.entry.VexLifeProdEntryFamily
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.home.VexLifeProdHome
import vexlife.person.VexLifePersonSettings
import vexlife.person.VexLifePersonSettingsStore
import vexlife.text.VexLifeStrings
import vexlife.text.VexLifeText

class VexLifeProdSetupLanguageTest {
    private val root = Path.of(System.getProperty("user.dir"))
    private val catalog = VexLifeStrings.catalog
    private val sha = "a".repeat(40)
    private val languages = VexLifePersonSettings.Languages.map { it.first }

    private val windowSource =
        root
            .resolve("packaging/private-service-image/setup/vexlife-prod-setup-window.applescript")
            .readText()

    private fun keysIn(text: VexLifeText): List<String> =
        listOf(text.key) + text.parts.values.flatMap(::keysIn)

    private fun states(): List<VexLifeProdEntryFamily.EntryState> =
        listOf(
            VexLifeProdEntryFamily.Current(sha),
            VexLifeProdEntryFamily.UpdateAvailable(sha),
            VexLifeProdEntryFamily.RepairRequired(sha),
            VexLifeProdEntryFamily.UpdateRepairRequired(sha),
            VexLifeProdEntryFamily.StartRequired(sha, priorServiceSourceSha = null),
            VexLifeProdEntryFamily.StartRequired(sha, priorServiceSourceSha = "b".repeat(40)),
            VexLifeProdEntryFamily.HomeFormationRequired(sha),
            VexLifeProdEntryFamily.SetupRequired(sha),
            VexLifeProdEntryFamily.StorageHeld(
                sha,
                requiredFreeBytes = 20L shl 30,
                freeBytes = 5L shl 30,
            ),
            VexLifeProdEntryFamily.ModelFilesHeld(sha, invalidBytes = 1L shl 30),
            VexLifeProdEntryFamily.HeldUnknown(sha, "ownership is not exact"),
        ) +
            VexLifeProdHome.LocationDisposition.entries
                .filter { it.held }
                .map { VexLifeProdEntryFamily.HomeHeld(sha, it, "observed") } +
            listOf(
                    "PLATFORM_UNSUPPORTED",
                    "ARCHITECTURE_UNSUPPORTED",
                    "CHIP_MODEL_UNSUPPORTED",
                    "SYSTEM_MEMORY_UNKNOWN",
                    "INSUFFICIENT_SYSTEM_MEMORY",
                )
                .flatMap { reason ->
                    listOf(true, false).map { homePresent ->
                        VexLifeProdEntryFamily.HostHeld(
                            desiredSourceSha = sha,
                            reasonRef = reason,
                            chipModel = if (homePresent) "Apple M2" else null,
                            totalMemoryBytes = if (homePresent) 16L shl 30 else null,
                            requiredChipModel = "Apple M4 Pro",
                            requiredMemoryBytes = 12L shl 30,
                            homePresent = homePresent,
                        )
                    }
                }

    @Test
    fun everyStateReadsInEveryLanguageWithItsOwnWords() {
        for (state in states()) {
            val display = VexLifeProdEntryFamily.Semantics.displayStateFor(state)
            val texts =
                listOfNotNull(
                    display.title,
                    display.detail,
                    display.primaryLabel,
                    display.secondaryLabel,
                    display.cancelLabel,
                    display.homeChoiceLabel,
                    display.recheckLabel,
                ) + display.journey.map { it.label }
            for (language in languages) {
                for (text in texts) {
                    val words = VexLifeStrings.resolve(text, language)
                    assertFalse(words.contains('{'), "${text.key} in $language left a placeholder")
                    if (language != "en") {
                        assertTrue(
                            catalog.entry(text.key).strings.containsKey(language),
                            "${text.key} has no $language translation",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun aCardsButtonsReadDifferentlyInEveryLanguage() {
        for (state in states()) {
            val display = VexLifeProdEntryFamily.Semantics.displayStateFor(state)
            val buttons =
                listOfNotNull(
                    display.primaryLabel,
                    display.secondaryLabel,
                    display.homeChoiceLabel,
                    display.recheckLabel,
                    VexLifeText("setup.button.change-language"),
                    display.cancelLabel,
                )
            for (language in languages) {
                val words = buttons.map { VexLifeStrings.resolve(it, language) }
                assertEquals(
                    words.size,
                    words.toSet().size,
                    "${state.stateRef} in $language: $words",
                )
            }
        }
    }

    @Test
    fun aDisplayedControlsWordsMapToTheControlTheyCarry() {
        for (state in states()) {
            val display = VexLifeProdEntryFamily.Semantics.displayStateFor(state)
            listOfNotNull(
                    display.primaryControlRef?.let { it to requireNotNull(display.primaryLabel) },
                    display.secondaryControlRef?.let {
                        it to requireNotNull(display.secondaryLabel)
                    },
                )
                .forEach { (controlRef, label) ->
                    assertEquals("entry:control:$controlRef", catalog.dataKey(label.key), label.key)
                }
            assertEquals("entry:state:${state.stateRef}", catalog.dataKey(display.title.key))
            assertEquals("entry:state:${state.stateRef}", catalog.dataKey(display.detail.key))
            display.journey.forEach { step ->
                assertEquals("entry:step:${step.stepRef}", catalog.dataKey(step.label.key))
            }
        }
    }

    @Test
    fun everyDataKeyNamesARefItsOwnerHas() {
        for (key in catalog.keys) {
            val dataKey = catalog.dataKey(key) ?: continue
            assertTrue(dataKey in VexLifeProdSetupElements.DataKeys, "$key names unknown $dataKey")
        }
    }

    @Test
    fun everyKeyIsUsedAndEveryUsedKeyExists() {
        val literal =
            Regex(
                "\"(setup\\.(?:button|label|heading|description|status|placeholder|nav|legend)\\.[a-z0-9-]+)\""
            )
        val used =
            root
                .resolve("src")
                .walk()
                .filter { it.extension == "kt" && "Test" !in it.fileName.toString() }
                .flatMap { file -> literal.findAll(file.readText()).map { it.groupValues[1] } }
                .toSet() + literal.findAll(windowSource).map { it.groupValues[1] }.toSet()
        assertEquals(emptySet(), used - catalog.keys, "keys used but missing from the source")
        assertEquals(
            emptySet(),
            catalog.keys.filter { it.startsWith("setup.") }.toSet() - used,
            "keys nothing shows",
        )
    }

    @Test
    fun theWindowAsksOnlyForItsOwnSurfaceAndGetsAllOfIt() {
        val asked =
            Regex("chromeText\\(\"([^\"]+)\"\\)")
                .findAll(windowSource)
                .map { it.groupValues[1] }
                .toSet()
        assertEquals(catalog.keysOn(VexLifeProdSetupLanguage.WindowSurface).toSet(), asked)
        for (language in languages) {
            assertEquals(asked, VexLifeProdSetupLanguage.window(language).keys)
        }
    }

    @Test
    fun theFirstStepOffersEveryCompleteLanguageAndSavesTheChoiceOnceTheHomeExists() {
        val parent = Files.createTempDirectory("vexlife-language-").toAbsolutePath()
        try {
            val absent = parent.resolve("new-home")
            val first = VexLifeProdSetupLanguage.observe(absent, explicit = null, system = "ko-KR")
            assertEquals("ko", first.language)
            assertFalse(first.languageChosen)
            assertEquals(languages, first.languages.map { it.code })
            assertEquals(
                VexLifeStrings.text("setup.heading.choose-language", "ko"),
                first.window["setup.heading.choose-language"],
            )

            val beforeHome = VexLifeProdSetupLanguage.choose(absent, "ja")
            assertEquals("ja", beforeHome.language)
            assertFalse(beforeHome.savedInHome)
            assertFalse(beforeHome.effectsPerformed)
            assertFalse(Files.exists(absent), "choosing a language never creates a Home")

            val home = parent.resolve(".vexlife")
            CanonicalVexLifeHomeFixture.form(home)
            val saved = VexLifeProdSetupLanguage.choose(home, "fr")
            assertTrue(saved.savedInHome)
            assertTrue(saved.effectsPerformed)
            assertEquals("fr", VexLifePersonSettingsStore(home).read().replyLanguage)

            val returning = VexLifeProdSetupLanguage.observe(home, explicit = null, system = "de")
            assertEquals("fr", returning.language, "the Home's saved language outranks the Mac's")
            assertTrue(returning.languageChosen)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun progressFailuresAndTheReadyNoticeSpeakTheSelectedLanguage() {
        val lines = mutableListOf<String>()
        val progress =
            vexlife.local.VexLifeProdSetupProgress.Lines(sink = { lines += it }, language = "de")
        progress.phase(vexlife.local.VexLifeProdSetupProgress.Phase.FormingHome)
        progress.artifactBytes(3L shl 29, 10L shl 30)
        assertEquals(
            vexlife.local.VexLifeProdSetupProgress.LinePrefix +
                VexLifeStrings.text("setup.status.progress-forming-home", "de"),
            lines.first(),
        )
        assertTrue(lines.last().contains("1,5"), lines.last())

        val failure = VexLifeProdSetupFailure.classify(java.net.ConnectException("refused"), "es")
        assertEquals(VexLifeStrings.text("setup.heading.failure-network", "es"), failure.title)
        assertTrue(failure.technicalDetail!!.contains("ConnectException"))

        val notice = VexLifeProdMacFrontDoorProcess.readyNotice("hi")
        assertEquals(VexLifeStrings.text("setup.heading.ready", "hi"), notice.title)
        assertTrue(notice.detail.contains("http://127.0.0.1:"))
    }
}

// [VXG RealForever]
