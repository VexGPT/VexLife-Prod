package vexlife.model.heretic.runtime

import java.nio.file.Path
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHome
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticModelOfferings
import vexlife.model.heretic.HereticProductIdentity

/**
 * Durable VexLife-Prod implementation binding for the certified Heretic runtime.
 *
 * The binding lives under runtime/vexlife-prod. It deliberately does not read, replace or reinterpret
 * the canonical VexLife `config/model.json` contract merely because this implementation uses the same
 * local model. Runtime liveness/currentness remains outside this file.
 */
class FileHereticModelConfigurationStore(
    productHome: Path,
) {
    sealed interface Snapshot {
        data class Unconfigured(
            val value: VexLifeProdHome.InitialModelConfiguration,
        ) : Snapshot

        data class Heretic(
            val value: HereticModelConfiguration.Value,
        ) : Snapshot
    }

    sealed interface TransitionState {
        val wire: String

        data object SelectedPending : TransitionState {
            override val wire = "HERETIC_SELECTED_PENDING"
        }

        data object AlreadyBound : TransitionState {
            override val wire = "HERETIC_ALREADY_BOUND"
        }

        data object AlreadySelectedPending : TransitionState {
            override val wire = "HERETIC_ALREADY_SELECTED_PENDING"
        }

        data object DefaultedUnsupportedSelectionPending : TransitionState {
            override val wire = "HERETIC_DEFAULTED_UNSUPPORTED_SELECTION_PENDING"
        }

        data object ModelSwitchedPending : TransitionState {
            override val wire = "HERETIC_MODEL_SWITCHED_PENDING"
        }

        data object ModelReplacedStaleIdentityPending : TransitionState {
            override val wire = "HERETIC_MODEL_REPLACED_STALE_IDENTITY_PENDING"
        }

        data object ReboundToQualifiedRuntime : TransitionState {
            override val wire = "HERETIC_REBOUND_TO_QUALIFIED_RUNTIME"
        }

        data object BoundToQualifiedRuntime : TransitionState {
            override val wire = "HERETIC_BOUND_TO_QUALIFIED_RUNTIME"
        }
    }

    data class Transition(
        val state: TransitionState,
        val value: HereticModelConfiguration.Value,
        val writePerformed: Boolean,
    )

    private sealed interface ScopedSnapshot {
        data object Unconfigured : ScopedSnapshot

        data class Heretic(
            val value: HereticModelConfiguration.Value,
        ) : ScopedSnapshot
    }

    private val productHome =
        VexOwnedPath.requireRoot(
            productHome,
            "canonical VexLife Home",
        )

    fun read(
        manifest: VexLifeProdHome.Manifest,
    ): Snapshot {
        val currentManifest =
            VexLifeProdHome.validateManifest(
                manifest,
            )
        return when (
            val scoped =
                readSelectionScope(
                    currentManifest,
                )
        ) {
            ScopedSnapshot.Unconfigured ->
                Snapshot.Unconfigured(
                    VexLifeProdHome.initialProductModelConfiguration(),
                )

            is ScopedSnapshot.Heretic ->
                Snapshot.Heretic(
                    HereticModelConfiguration.validate(
                        value = scoped.value,
                        homeIdentity =
                            bindingScope(
                                currentManifest,
                            ),
                        model =
                            HereticModelOfferings.requireEntry(
                                productHome,
                                scoped.value.modelRef,
                            ),
                    ),
                )
        }
    }

    /**
     * Converges a Home onto one model this Product can actually serve now.
     *
     * A stale/no-longer-offered selection is historical state, not a prerequisite. Setup replaces
     * it with the current recommended offered model and writes a fresh pending binding. The old
     * record's Home/Companion scope must still be exact, and no automatic authority bit is accepted
     * from that record.
     */
    fun ensureSupportedSelection(
        manifest: VexLifeProdHome.Manifest,
        totalMemoryBytes: Long?,
    ): Transition {
        val currentManifest =
            VexLifeProdHome.validateManifest(
                manifest,
            )
        val fallback =
            HereticModelCatalog.recommendedFor(
                totalMemoryBytes,
            )
        return when (
            val scoped =
                readSelectionScope(
                    currentManifest,
                )
        ) {
            ScopedSnapshot.Unconfigured -> {
                val pending =
                    HereticModelConfiguration.pending(
                        bindingScope(
                            currentManifest,
                        ),
                        fallback,
                    )
                write(
                    currentManifest,
                    pending,
                )
                Transition(
                    state = TransitionState.SelectedPending,
                    value = pending,
                    writePerformed = true,
                )
            }

            is ScopedSnapshot.Heretic -> {
                val offered =
                    HereticModelOfferings.entry(
                        productHome,
                        scoped.value.modelRef,
                    )
                if (
                    offered != null &&
                        HereticModelConfiguration.productIdentityCurrent(
                            value = scoped.value,
                            model = offered,
                        )
                ) {
                    Transition(
                        state =
                            if (
                                HereticModelConfiguration.isBound(
                                    scoped.value,
                                )
                            ) {
                                TransitionState.AlreadyBound
                            } else {
                                TransitionState.AlreadySelectedPending
                            },
                        value = scoped.value,
                        writePerformed = false,
                    )
                } else {
                    val pending =
                        HereticModelConfiguration.pending(
                            bindingScope(
                                currentManifest,
                            ),
                            fallback,
                        )
                    write(
                        currentManifest,
                        pending,
                    )
                    Transition(
                        state = TransitionState.DefaultedUnsupportedSelectionPending,
                        value = pending,
                        writePerformed = true,
                    )
                }
            }
        }
    }

    fun selectHeretic(
        manifest: VexLifeProdHome.Manifest,
    ): Transition {
        val currentManifest =
            VexLifeProdHome.validateManifest(
                manifest,
            )

        return when (
            val current =
                read(
                    currentManifest,
                )
        ) {
            is Snapshot.Unconfigured -> {
                val pending =
                    HereticModelConfiguration.pending(
                        bindingScope(
                            currentManifest,
                        ),
                    )
                write(
                    currentManifest,
                    pending,
                )
                Transition(
                    state = TransitionState.SelectedPending,
                    value = pending,
                    writePerformed = true,
                )
            }

            is Snapshot.Heretic ->
                Transition(
                    state =
                        if (
                            HereticModelConfiguration.isBound(
                                current.value,
                            )
                        ) {
                            TransitionState.AlreadyBound
                        } else {
                            TransitionState.AlreadySelectedPending
                        },
                    value = current.value,
                    writePerformed = false,
                )
        }
    }

    /**
     * The person's model choice for this Home. Choosing is the only effect: the value becomes
     * HERETIC_ESTABLISHMENT_PENDING for the chosen catalog model. Acquiring its files and starting
     * its runtime stay with Setup's own lawful Controls; a runtime already serving another model is
     * switched by the runtime owner at the next establishment, never here.
     *
     * Choosing the model a Home already has changes nothing (bound stays bound).
     */
    fun selectModel(
        manifest: VexLifeProdHome.Manifest,
        model: HereticModelCatalog.Entry,
    ): Transition =
        selectModelFromExplicitHumanChoice(
            manifest = manifest,
            model = model,
        )

    /**
     * Explicit human choice may replace a stale Product model identity, but never a foreign Home or
     * Companion scope. The historical record is not treated as current; the new selected model is
     * written fresh as EstablishmentPending.
     */
    fun selectModelFromExplicitHumanChoice(
        manifest: VexLifeProdHome.Manifest,
        model: HereticModelCatalog.Entry,
    ): Transition {
        val currentManifest =
            VexLifeProdHome.validateManifest(
                manifest,
            )
        return when (
            val scoped =
                readSelectionScope(
                    currentManifest,
                )
        ) {
            ScopedSnapshot.Unconfigured -> {
                val pending =
                    HereticModelConfiguration.pending(
                        bindingScope(
                            currentManifest,
                        ),
                        model,
                    )
                write(
                    currentManifest,
                    pending,
                )
                Transition(
                    state = TransitionState.SelectedPending,
                    value = pending,
                    writePerformed = true,
                )
            }

            is ScopedSnapshot.Heretic -> {
                if (
                    HereticModelConfiguration.productIdentityCurrent(
                        scoped.value,
                    ) &&
                    scoped.value.modelRef ==
                    model.modelRef
                ) {
                    Transition(
                        state =
                            if (
                                HereticModelConfiguration.isBound(
                                    scoped.value,
                                )
                            ) {
                                TransitionState.AlreadyBound
                            } else {
                                TransitionState.AlreadySelectedPending
                            },
                        value = scoped.value,
                        writePerformed = false,
                    )
                } else {
                    val pending =
                        HereticModelConfiguration.pending(
                            bindingScope(
                                currentManifest,
                            ),
                            model,
                        )
                    write(
                        currentManifest,
                        pending,
                    )
                    Transition(
                        state =
                            if (
                                HereticModelConfiguration.productIdentityCurrent(
                                    scoped.value,
                                )
                            ) {
                                TransitionState.ModelSwitchedPending
                            } else {
                                TransitionState.ModelReplacedStaleIdentityPending
                            },
                        value = pending,
                        writePerformed = true,
                    )
                }
            }
        }
    }

    /** The model this Home has chosen, including Home imports, or the Product default if unconfigured. */
    fun selectedModel(manifest: VexLifeProdHome.Manifest): HereticModelCatalog.Entry =
        when (val current = read(manifest)) {
            is Snapshot.Heretic ->
                HereticModelOfferings.requireEntry(
                    productHome,
                    current.value.modelRef,
                )

            is Snapshot.Unconfigured -> HereticModelCatalog.Default
        }

    fun bindQualified(
        manifest: VexLifeProdHome.Manifest,
        qualifiedRuntime: HereticModelConfiguration.QualifiedRuntimeEvidence,
        runtimeReceiptPath: String =
            HereticProductIdentity.StableRuntimeReceiptRelativePath,
    ): Transition {
        val currentManifest =
            VexLifeProdHome.validateManifest(
                manifest,
            )
        val current =
            read(
                currentManifest,
            )

        require(
            current is
                Snapshot.Heretic,
        ) {
            "Heretic runtime must be explicitly selected before qualified evidence can bind it"
        }
        // A runtime serving one model never binds a Home that chose another.
        require(current.value.modelRef == qualifiedRuntime.modelRef) {
            "qualified runtime serves ${qualifiedRuntime.modelRef}, but this Home chose ${current.value.modelRef}"
        }

        val model =
            HereticModelOfferings.requireEntry(
                productHome,
                current.value.modelRef,
            )
        val bound =
            HereticModelConfiguration.bound(
                homeIdentity =
                    bindingScope(
                        currentManifest,
                    ),
                qualifiedRuntime = qualifiedRuntime,
                runtimeReceiptPath = runtimeReceiptPath,
                model = model,
            )

        if (
            current.value ==
            bound
        ) {
            return Transition(
                state = TransitionState.AlreadyBound,
                value = current.value,
                writePerformed = false,
            )
        }

        require(
            current.value.state in
                setOf(
                    HereticModelConfiguration.State.EstablishmentPending,
                    HereticModelConfiguration.State.BoundProductModel,
                ),
        ) {
            "current Heretic runtime binding cannot transition to qualified"
        }

        write(
            currentManifest,
            bound,
        )

        return Transition(
            state =
                if (
                    current.value.state ==
                    HereticModelConfiguration.State.BoundProductModel
                ) {
                    TransitionState.ReboundToQualifiedRuntime
                } else {
                    TransitionState.BoundToQualifiedRuntime
                },
            value = bound,
            writePerformed = true,
        )
    }

    fun path(
        manifest: VexLifeProdHome.Manifest,
    ): Path =
        configurationPath(
            VexLifeProdHome.validateManifest(
                manifest,
            ),
        )

    private fun write(
        manifest: VexLifeProdHome.Manifest,
        value: HereticModelConfiguration.Value,
    ) {
        val scope =
            bindingScope(
                manifest,
            )
        val model =
            HereticModelOfferings.requireEntry(
                productHome,
                value.modelRef,
            )
        val current =
            HereticModelConfiguration.validate(
                value = value,
                homeIdentity = scope,
                model = model,
            )

        VexDurableContractFile.write(
            ownedRoot = productHome,
            path =
                configurationPath(
                    manifest,
                ),
            value = current,
        )
    }

    private fun readSelectionScope(
        manifest: VexLifeProdHome.Manifest,
    ): ScopedSnapshot {
        val path =
            configurationPath(
                manifest,
            )
        val text =
            VexDurableContractFile.readText(
                ownedRoot = productHome,
                path = path,
            )
                ?: return ScopedSnapshot.Unconfigured

        return when (
            VexContractJson.schemaVersion(
                text,
            )
        ) {
            VexLifeProdHome.InitialModelConfigurationSchema ->
                ScopedSnapshot.Unconfigured

            HereticModelConfiguration.Schema ->
                ScopedSnapshot.Heretic(
                    HereticModelConfiguration.validateSelectionScope(
                        value =
                            VexContractJson.decode(
                                text,
                            ),
                        homeIdentity =
                            bindingScope(
                                manifest,
                            ),
                    ),
                )

            else ->
                error(
                    "VexLife-Prod runtime model binding schema is not admitted",
                )
        }
    }

    private fun configurationPath(
        manifest: VexLifeProdHome.Manifest,
    ): Path {
        VexLifeProdHome.validateManifest(
            manifest,
        )

        return VexOwnedPath.resolveInside(
            productHome,
            VexLifeProdHome.ModelConfigurationRelativePath,
        )
    }

    private fun bindingScope(
        manifest: VexLifeProdHome.Manifest,
    ): HereticModelConfiguration.BindingScope {
        val (
            homeRef,
            companionLineageRef,
        ) =
            VexLifeProdHome.bindingScope(
                manifest,
            )

        return HereticModelConfiguration.BindingScope(
            homeRef = homeRef,
            companionLineageRef = companionLineageRef,
        )
    }
}

// [VXG RealForever]
