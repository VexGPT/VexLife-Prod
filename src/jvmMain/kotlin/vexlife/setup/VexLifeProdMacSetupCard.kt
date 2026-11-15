package vexlife.setup

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.entry.VexLifeProdEntryFamily
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.runtime.HereticSetupStorage
import vexlife.person.VexLifePersonSettings
import vexlife.service.VexLifeProdServiceContract
import vexlife.text.VexLifeStrings
import vexlife.text.VexLifeText

/**
 * Concrete native presentation meaning for one Setup Journey card.
 *
 * Family State -> DisplayState -> **this card** -> AppKit manifestation.
 *
 * The card composes the Family's DisplayState with the observed facts the person needs to decide
 * (where the Home is, whether this Mac and drive meet the requirements, what setup will download).
 * Every button carries either a Family-authored Control ref or one of the renderer-local navigation
 * refs (choose another Home, choose a language, check again, close). Text is resolved here, in the
 * person's selected language, from the keyed string source; the native renderer only shows these
 * strings and returns the chosen ref. It does not author copy, decide which buttons exist, or map
 * anything to an Effect.
 */
object VexLifeProdMacSetupCard {
    const val ActionControlPrefix = "CONTROL:"
    const val ActionChooseHome = "CHOOSE_HOME"
    const val ActionChooseLanguage = "CHOOSE_LANGUAGE"
    const val ActionChooseModel = "CHOOSE_MODEL"
    const val ActionRecheck = "RECHECK"
    const val ActionClose = "CLOSE"

    private val RendererLocalActions =
        setOf(ActionChooseHome, ActionChooseLanguage, ActionChooseModel, ActionRecheck, ActionClose)

    /**
     * [key] is the button's element identity in the string source; it maps to [actionRef] through
     * the key's `dataKey`, whatever language [label] is in.
     */
    @Serializable
    data class Button(val label: String, val actionRef: String, val key: String) {
        init {
            require(
                label.isNotBlank() &&
                    VexLifeText.KeyPattern.matches(key) &&
                    (actionRef.startsWith(ActionControlPrefix) || actionRef in RendererLocalActions)
            ) {
                "Setup Journey button is invalid"
            }
        }
    }

    /**
     * Words already resolved in [language]; the native renderer shows them as they are. [titleKey]
     * and each [Button.key] keep the elements identifiable in every language.
     */
    @Serializable
    data class Card(
        val language: String,
        val titleKey: String,
        val title: String,
        val rail: String,
        val body: String,
        val buttons: List<Button>,
    ) {
        init {
            require(
                VexLifePersonSettings.Languages.any { it.first == language } &&
                    title.isNotBlank() &&
                    rail.isNotBlank() &&
                    body.isNotBlank() &&
                    buttons.size in 1..7 &&
                    buttons.last().actionRef == ActionClose &&
                    buttons.count {
                        it.actionRef == ActionClose
                    } == 1 &&
                    buttons
                        .map {
                            it.label
                        }
                        .toSet()
                        .size == buttons.size
            ) {
                "Setup Journey card is invalid"
            }
        }
    }

    data class Facts(
        val displayState: VexLifeProdEntryFamily.EntryDisplayState,
        val semanticStateRef: String,
        val currentState: VexLifeProdServiceContract.CurrentStateProjection,
        val location: VexLifeProdHomeFiles.Location,
        val setupPlan: VexLifeProdMacFrontDoor.SetupPlan?,
        val formationPreview: HereticSetupStorage.Requirement?,
        val currentServiceSourceSha: String?,
        val requiredChipModel: String,
        val requiredMemoryBytes: Long,
        val protectedLocation: VexLifeText?,
        /** The model this setup is about (the window's, the Home's, or the one that fits this Mac). */
        val modelLabel: String? = null,
    )

    fun form(facts: Facts, language: String): Card {
        fun say(text: VexLifeText) = VexLifeStrings.resolve(text, language)

        fun button(label: VexLifeText, actionRef: String) =
            Button(label = say(label), actionRef = actionRef, key = label.key)

        val display = facts.displayState
        val buttons = buildList {
            display.primaryControlRef?.let {
                add(button(requireNotNull(display.primaryLabel), ActionControlPrefix + it))
            }
            display.secondaryControlRef?.let {
                add(button(requireNotNull(display.secondaryLabel), ActionControlPrefix + it))
            }
            display.homeChoiceLabel?.let {
                add(button(it, ActionChooseHome))
            }
            display.recheckLabel?.let {
                add(button(it, ActionRecheck))
            }
            // The model can be chosen whenever this Mac can run VexLife at all.
            if (facts.currentState.hostEligibilityState == "ELIGIBLE") {
                add(button(text("setup.button.change-model"), ActionChooseModel))
            }
            add(button(text("setup.button.change-language"), ActionChooseLanguage))
            add(button(display.cancelLabel, ActionClose))
        }

        return Card(
            language = language,
            titleKey = display.title.key,
            title = say(display.title),
            rail = rail(display.journey, language),
            body =
                listOfNotNull(
                        say(display.detail),
                        section("setup.heading.found", foundLines(facts).map(::Line), language),
                        facts.setupPlan?.let {
                            section("setup.heading.plan", planLines(it), language)
                        },
                        facts.formationPreview?.let {
                            section("setup.heading.up-next", upNextLines(it).map(::Line), language)
                        },
                        noticeSection(facts, language),
                    )
                    .joinToString("\n\n"),
            buttons = buttons,
        )
    }

    fun rail(journey: List<VexLifeProdEntryFamily.JourneyStep>, language: String): String =
        journey.joinToString("   ") { step ->
            val mark =
                when (step.statusRef) {
                    VexLifeProdEntryFamily.Complete -> "✓"
                    VexLifeProdEntryFamily.JourneyCurrent -> "●"
                    VexLifeProdEntryFamily.JourneyHeld -> "⊘"
                    else -> "○"
                }
            mark + " " + VexLifeStrings.resolve(step.label, language)
        }

    /** A heading and its indented lines; [Line.bullet] marks a step setup will take. */
    private data class Line(val text: VexLifeText, val bullet: Boolean = false)

    private fun section(headingKey: String, lines: List<Line>, language: String): String =
        VexLifeStrings.text(headingKey, language) +
            "\n" +
            lines.joinToString("\n") {
                "  " + (if (it.bullet) "• " else "") + VexLifeStrings.resolve(it.text, language)
            }

    private fun text(key: String) = VexLifeText(key)

    private fun foundLines(facts: Facts): List<VexLifeText> {
        val current = facts.currentState
        val lines = mutableListOf<VexLifeText>()

        lines +=
            VexLifeText(
                key = "setup.label.found-home",
                args = mapOf("path" to current.homePath),
                parts = mapOf("status" to homeStatus(facts.location)),
            )

        val meets = current.hostEligibilityState == "ELIGIBLE"
        lines +=
            VexLifeText(
                key = "setup.label.found-mac",
                args = current.chipModel?.let { mapOf("chip" to it) } ?: emptyMap(),
                parts =
                    buildMap {
                        if (current.chipModel == null) put("chip", text("setup.label.unknown-chip"))
                        put("memory", VexLifeProdEntryFamily.memoryText(current.totalMemoryBytes))
                        put(
                            "verdict",
                            if (meets) {
                                text("setup.status.mac-meets")
                            } else {
                                VexLifeText(
                                    key = "setup.status.mac-needs",
                                    args = mapOf("chip" to facts.requiredChipModel),
                                    gib = mapOf("memory" to facts.requiredMemoryBytes),
                                )
                            },
                        )
                    },
            )

        lines +=
            VexLifeText(
                key = "setup.label.found-free-space",
                parts =
                    mapOf(
                        "amount" to
                            (current.freeStorageBytes?.let {
                                VexLifeText(
                                    key = "setup.label.gib-amount",
                                    gib = mapOf("amount" to it),
                                )
                            } ?: text("setup.status.unknown"))
                    ),
            )

        facts.modelLabel?.let {
            lines += VexLifeText(key = "setup.label.found-chosen-model", args = mapOf("model" to it))
        }

        if (facts.location is VexLifeProdHomeFiles.Location.Existing) {
            lines +=
                VexLifeText(
                    key = "setup.label.found-model",
                    parts = mapOf("status" to modelStatus(current)),
                )
            serviceStatus(facts)?.let {
                lines +=
                    VexLifeText(key = "setup.label.found-service", parts = mapOf("status" to it))
            }
        }

        return lines
    }

    private fun homeStatus(location: VexLifeProdHomeFiles.Location): VexLifeText =
        when (location) {
            is VexLifeProdHomeFiles.Location.Existing -> text("setup.status.home-existing")

            // The observer's detail is technical evidence (English), quoted as it is.
            is VexLifeProdHomeFiles.Location.Held ->
                VexLifeText(
                    key = "setup.status.home-preserved",
                    args = mapOf("detail" to location.detail),
                )

            is VexLifeProdHomeFiles.Location.Formable ->
                when {
                    location.disposition == VexLifeProdHome.LocationDisposition.EmptyDirectory ->
                        text("setup.status.home-empty-folder")

                    location.missingAncestorCount == 1 ->
                        VexLifeText(
                            key = "setup.status.home-new-one-folder",
                            args = mapOf("count" to "1"),
                        )

                    location.missingAncestorCount > 1 ->
                        VexLifeText(
                            key = "setup.status.home-new-folders",
                            args = mapOf("count" to location.missingAncestorCount.toString()),
                        )

                    else -> text("setup.status.home-new")
                }
        }

    private fun modelStatus(
        current: VexLifeProdServiceContract.CurrentStateProjection
    ): VexLifeText =
        when (current.modelArtifactState) {
            "NOT_SELECTED" -> text("setup.status.model-not-set-up")

            "ABSENT" -> text("setup.status.model-not-downloaded")

            "PARTIAL_BY_SIZE" ->
                VexLifeText(
                    key = "setup.status.model-partial",
                    gib =
                        mapOf(
                            "present" to (current.modelArtifactPresentBySizeBytes ?: 0L),
                            "total" to (current.modelArtifactTotalBytes ?: 0L),
                        ),
                )

            "COMPLETE_BY_SIZE_NOT_HASHED" ->
                VexLifeText(
                    key = "setup.status.model-present",
                    gib = mapOf("total" to (current.modelArtifactTotalBytes ?: 0L)),
                )

            "INVALID_SIZE" ->
                VexLifeText(
                    key = "setup.status.model-size-mismatch",
                    gib = mapOf("affected" to (current.modelArtifactInvalidSizeBytes ?: 0L)),
                )

            else -> text("setup.status.unknown")
        }

    private fun serviceStatus(facts: Facts): VexLifeText? =
        when (facts.semanticStateRef) {
            VexLifeProdEntryFamily.StateCurrent -> text("setup.status.service-current")

            VexLifeProdEntryFamily.StateUpdateAvailable ->
                VexLifeText(
                    key = "setup.status.service-earlier",
                    args = mapOf("version" to (facts.currentServiceSourceSha?.take(7) ?: "?")),
                )

            VexLifeProdEntryFamily.StateRepairRequired,
            VexLifeProdEntryFamily.StateUpdateRepairRequired ->
                text("setup.status.service-not-responding")

            VexLifeProdEntryFamily.StateStartRequired -> text("setup.status.service-not-running")

            VexLifeProdEntryFamily.StateHeldUnknown -> text("setup.status.service-unverified")

            else -> null
        }

    private fun planLines(plan: VexLifeProdMacFrontDoor.SetupPlan): List<Line> =
        listOf(
            Line(
                if (plan.modelArtifactMissingBytes > 0) {
                    VexLifeText(
                        key = "setup.label.plan-download",
                        args = mapOf("model" to plan.modelLabel),
                        gib = mapOf("missing" to plan.modelArtifactMissingBytes),
                    )
                } else {
                    VexLifeText(
                        key = "setup.label.plan-reuse",
                        args = mapOf("model" to plan.modelLabel),
                    )
                },
                bullet = true,
            ),
            Line(text("setup.label.plan-runtime"), bullet = true),
            Line(text("setup.label.plan-start"), bullet = true),
            Line(
                VexLifeText(
                    key = "setup.label.plan-needs",
                    gib = mapOf("required" to plan.requiredFreeStorageBytes),
                    parts =
                        mapOf(
                            "available" to
                                (plan.currentFreeStorageBytes?.let {
                                    VexLifeText(
                                        key = "setup.label.plan-available",
                                        gib = mapOf("amount" to it),
                                    )
                                } ?: text("setup.label.plan-free-unknown"))
                        ),
                )
            ),
        )

    /**
     * Walk-through honesty: before the Home-only step, say what the *next* step will need, so a
     * person is not surprised by a 14 GiB download after committing a folder.
     */
    private fun upNextLines(preview: HereticSetupStorage.Requirement): List<VexLifeText> =
        listOf(
            VexLifeText(
                key = "setup.label.up-next-model",
                gib =
                    mapOf(
                        "download" to preview.missingModelBytes,
                        "free" to preview.requiredFreeBytes,
                    ),
            )
        )

    private fun noticeSection(facts: Facts, language: String): String? {
        val notices = mutableListOf<VexLifeText>()

        if (
            facts.protectedLocation != null &&
                facts.semanticStateRef in
                    setOf(
                        VexLifeProdEntryFamily.StateHomeFormationRequired,
                        VexLifeProdEntryFamily.StateHomeHeld,
                    )
        ) {
            notices +=
                VexLifeText(
                    key = "setup.description.notice-protected-location",
                    parts = mapOf("place" to facts.protectedLocation),
                )
        }

        facts.formationPreview
            ?.takeIf { it.shortfallObserved }
            ?.let {
                notices +=
                    VexLifeText(
                        key = "setup.description.notice-storage-shortfall",
                        gib =
                            mapOf(
                                "required" to it.requiredFreeBytes,
                                "free" to (it.observedFreeBytes ?: 0L),
                            ),
                    )
            }

        return notices
            .takeIf {
                it.isNotEmpty()
            }
            ?.joinToString("\n\n") { VexLifeStrings.resolve(it, language) }
    }

    /**
     * Names the macOS privacy-protected (TCC) area that contains [selectedHome], if any.
     *
     * Lived evidence: proof artifacts living in ~/Downloads triggered a Files & Folders prompt. A
     * Vex Home is read later by a launchd background service, so a protected coordinate is a real
     * capability risk that must be explained before Home formation, not discovered afterwards.
     */
    fun protectedLocationName(userHome: Path, selectedHome: Path): VexLifeText? {
        val home = userHome.normalize()
        val selected = selectedHome.normalize()
        val key =
            when {
                selected.startsWith(home.resolve("Desktop")) -> "setup.label.place-desktop"
                selected.startsWith(home.resolve("Documents")) -> "setup.label.place-documents"
                selected.startsWith(home.resolve("Downloads")) -> "setup.label.place-downloads"
                selected.startsWith(home.resolve("Library/Mobile Documents")) ->
                    "setup.label.place-icloud"
                selected.startsWith(home.resolve("Library/CloudStorage")) ->
                    "setup.label.place-cloud-storage"
                selected.startsWith(Path.of("/Volumes")) -> "setup.label.place-external-drive"
                else -> null
            }
        return key?.let(::VexLifeText)
    }
}

// [VXG RealForever]
