package vexlife.model.heretic.runtime

import java.nio.file.Path
import vexlife.contract.VexContractJson
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHome
import vexlife.model.heretic.HereticModelConfiguration

/**
 * Durable owner for the single VexLife-Prod model configuration coordinate.
 *
 * The Home owner forms the initial UNCONFIGURED contract. This owner may explicitly select Heretic
 * into PENDING and may bind it only from qualified runtime evidence. Runtime liveness/currentness
 * remains outside this file.
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

    data class Transition(
        val state: String,
        val value: HereticModelConfiguration.Value,
        val writePerformed: Boolean,
    )

    private val productHome =
        VexOwnedPath.requireRoot(
            productHome,
            "VexLife-Prod Home",
        )

    fun read(
        manifest: VexLifeProdHome.Manifest,
    ): Snapshot {
        val currentManifest =
            VexLifeProdHome.validateManifest(
                manifest,
            )
        val path =
            configurationPath(
                currentManifest,
            )
        val text =
            requireNotNull(
                VexDurableContractFile.readText(
                    ownedRoot = productHome,
                    path = path,
                ),
            ) {
                "VexLife-Prod Home has no model configuration at its current manifest coordinate"
            }

        return when (
            VexContractJson.schemaVersion(
                text,
            )
        ) {
            VexLifeProdHome.InitialModelConfigurationSchema ->
                Snapshot.Unconfigured(
                    VexLifeProdHome.validateInitialModelConfiguration(
                        VexContractJson.decode(
                            text,
                        ),
                    ),
                )

            HereticModelConfiguration.Schema -> {
                val scope =
                    bindingScope(
                        currentManifest,
                    )
                Snapshot.Heretic(
                    HereticModelConfiguration.validate(
                        value =
                            VexContractJson.decode(
                                text,
                            ),
                        homeIdentity = scope,
                    ),
                )
            }

            else ->
                error(
                    "VexLife-Prod model configuration schema is not admitted",
                )
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
                    state = "HERETIC_SELECTED_PENDING",
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
                            "HERETIC_ALREADY_BOUND"
                        } else {
                            "HERETIC_ALREADY_SELECTED_PENDING"
                        },
                    value = current.value,
                    writePerformed = false,
                )
        }
    }

    fun bindQualified(
        manifest: VexLifeProdHome.Manifest,
        qualifiedRuntime: HereticModelConfiguration.QualifiedRuntimeEvidence,
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
            "Heretic model must be explicitly selected before qualified runtime can bind it"
        }

        val bound =
            HereticModelConfiguration.bound(
                homeIdentity =
                    bindingScope(
                        currentManifest,
                    ),
                qualifiedRuntime = qualifiedRuntime,
            )

        if (
            current.value ==
            bound
        ) {
            return Transition(
                state = "HERETIC_ALREADY_BOUND",
                value = current.value,
                writePerformed = false,
            )
        }

        require(
            current.value.state ==
                HereticModelConfiguration.State.EstablishmentPending,
        ) {
            "current Heretic model configuration cannot transition to bound"
        }

        write(
            currentManifest,
            bound,
        )

        return Transition(
            state = "HERETIC_BOUND_TO_QUALIFIED_RUNTIME",
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
        val current =
            HereticModelConfiguration.validate(
                value,
                scope,
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

    private fun configurationPath(
        manifest: VexLifeProdHome.Manifest,
    ): Path =
        VexOwnedPath.resolveInside(
            productHome,
            manifest.modelConfigurationRef,
        )

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
