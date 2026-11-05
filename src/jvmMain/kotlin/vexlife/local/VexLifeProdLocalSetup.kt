package vexlife.local

import java.net.InetAddress
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.time.OffsetDateTime
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticArtifactDelivery
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.model.heretic.runtime.HereticMacHostProfile
import vexlife.model.heretic.runtime.HereticPersistentRuntime
import vexlife.model.heretic.runtime.HereticPythonEnvironment
import vexlife.model.heretic.runtime.MacHereticRuntimePlatform
import vexlife.platform.python.ManagedPython

/**
 * Application-level composition for the isolated local VexLife-Prod substrate.
 *
 * This owner contributes sequencing only. Every durable identity, artifact fact, environment rule,
 * process/lifetime effect, and model-configuration transition remains owned by the component that
 * answers that question.
 */
object VexLifeProdLocalSetup {
    const val RuntimeAuthorityRef =
        "authority.vexlife-prod.local-setup.heretic-runtime.001"

    data class Result(
        val state: String,
        val productHome: String,
        val homeDisposition: String,
        val modelSelectionDisposition: String,
        val managedPythonDisposition: String,
        val artifactDisposition: String,
        val environmentDisposition: String,
        val runtimeDisposition: String,
        val modelBindingDisposition: String,
        val modelConfigurationState: HereticModelConfiguration.State,
        val endpoint: String,
        val requestModel: String,
    )

    internal data class Hooks(
        val observeHost: () -> HereticMacHostProfile.Result = {
            HereticMacHostProfile.observeCurrent()
        },
        val deviceName: () -> String = {
            currentDeviceName()
        },
        val now: () -> String = {
            OffsetDateTime.now()
                .toString()
        },
        val ensureManagedPython: (
            productHome: Path,
            allowAcquisition: Boolean,
        ) -> ManagedPython.Result = {
            productHome,
            allowAcquisition,
            ->
            ManagedPython.ensure(
                productHome = productHome,
                pythonVersion = HereticProductIdentity.PythonVersion,
                allowAcquisition = allowAcquisition,
            )
        },
        val materializeArtifacts: (
            productHome: Path,
            allowAcquisition: Boolean,
        ) -> HereticArtifactMaterialization.Result = {
            productHome,
            allowAcquisition,
            ->
            HereticArtifactMaterialization.materializeCurrent(
                productHome = productHome,
                allowAcquisition = allowAcquisition,
                acquirer = HereticArtifactDelivery.Acquirer(),
            )
        },
        val ensureHereticEnvironment: (
            managedPython: ManagedPython.Result,
            productHome: Path,
            allowCreation: Boolean,
        ) -> HereticPythonEnvironment.Result = {
            managedPython,
            productHome,
            allowCreation,
            ->
            HereticPythonEnvironment.ensure(
                managedPython = managedPython,
                productHome = productHome,
                allowCreation = allowCreation,
            )
        },
        val startRuntime: (
            productHome: Path,
            binding: HereticPersistentRuntime.Binding,
            store: HereticPersistentRuntime.ReceiptStore,
        ) -> HereticPersistentRuntime.StartResult = {
            productHome,
            binding,
            store,
            ->
            HereticPersistentRuntime.startOrReuse(
                binding = binding,
                store = store,
                platform =
                    MacHereticRuntimePlatform(
                        productHome = productHome,
                    ),
            )
        },
    )

    /**
     * Ordinary product route. The Home coordinate and all local topology are fixed by source.
     *
     * [allowAcquisition] is the one explicit human authorization carried by this local setup edge.
     */
    fun establish(
        allowAcquisition: Boolean,
    ): Result =
        establishAt(
            productHome =
                VexLifeProdHomeFiles.defaultMacRoot(
                    Path.of(
                        System.getProperty(
                            "user.home",
                        ),
                    ),
                ),
            allowAcquisition = allowAcquisition,
            hooks = Hooks(),
        )

    internal fun establishAt(
        productHome: Path,
        allowAcquisition: Boolean,
        hooks: Hooks,
    ): Result {
        val host =
            hooks.observeHost()

        require(
            host.state ==
                HereticMacHostProfile.State.Eligible &&
                !host.effectsPerformed,
        ) {
            "current Mac does not satisfy the certified VexLife-Prod Heretic host profile: " +
                (
                    host.reason
                        ?: "UNKNOWN"
                )
        }

        val homeAlreadyExists =
            Files.exists(
                productHome,
                LinkOption.NOFOLLOW_LINKS,
            )

        if (
            !homeAlreadyExists
        ) {
            require(
                allowAcquisition,
            ) {
                "fresh VexLife-Prod setup requires explicit local model/runtime acquisition authorization before Home formation"
            }
        }

        val home =
            VexLifeProdHomeFiles.bootstrap(
                productHome = productHome,
                plan =
                    VexLifeProdHome.plan(
                        personRef = VexLifeProdHome.DefaultPersonRef,
                        familyRef = VexLifeProdHome.DefaultFamilyRef,
                        deviceName = hooks.deviceName(),
                        platform = host.observation.platform,
                        architecture = host.observation.architecture,
                        ids =
                            VexLifeProdHomeFiles.newInstallationIds(),
                        createdAt = hooks.now(),
                    ),
            )

        val modelConfiguration =
            FileHereticModelConfigurationStore(
                Path.of(
                    home.productHome,
                ),
            )

        val selected =
            modelConfiguration.selectHeretic(
                home.manifest,
            )

        val managedPython =
            hooks.ensureManagedPython(
                Path.of(
                    home.productHome,
                ),
                allowAcquisition,
            )

        val materialized =
            hooks.materializeArtifacts(
                Path.of(
                    home.productHome,
                ),
                allowAcquisition,
            )

        val environment =
            hooks.ensureHereticEnvironment(
                managedPython,
                Path.of(
                    home.productHome,
                ),
                allowAcquisition,
            )

        val binding =
            HereticPersistentRuntime.bind(
                authorityRef = RuntimeAuthorityRef,
                materialization = materialized,
                environment = environment,
            )

        val runtime =
            hooks.startRuntime(
                Path.of(
                    home.productHome,
                ),
                binding,
                FileHereticRuntimeReceiptStore(
                    Path.of(
                        home.productHome,
                    ),
                ),
            )

        val bound =
            modelConfiguration.bindQualified(
                manifest = home.manifest,
                qualifiedRuntime =
                    HereticPersistentRuntime.configurationEvidence(
                        binding = binding,
                        receipt = runtime.receipt,
                    ),
            )

        return Result(
            state = "LOCAL_HERETIC_FOUNDATION_READY",
            productHome = home.productHome,
            homeDisposition = home.state,
            modelSelectionDisposition = selected.state,
            managedPythonDisposition = managedPython.state,
            artifactDisposition = materialized.state,
            environmentDisposition = environment.state,
            runtimeDisposition = runtime.disposition,
            modelBindingDisposition = bound.state,
            modelConfigurationState = bound.value.state,
            endpoint = runtime.endpoint,
            requestModel = runtime.requestModel,
        )
    }

    private fun currentDeviceName(): String {
        val computerName =
            try {
                val process =
                    ProcessBuilder(
                        "/usr/sbin/scutil",
                        "--get",
                        "ComputerName",
                    )
                        .start()
                val value =
                    process.inputStream
                        .bufferedReader()
                        .readText()
                        .trim()
                process.errorStream
                    .bufferedReader()
                    .readText()

                value.takeIf {
                    process.waitFor() ==
                        0 &&
                        it.isNotBlank()
                }
            } catch (
                _: Exception,
            ) {
                null
            }

        return computerName
            ?: InetAddress
                .getLocalHost()
                .hostName
                .takeIf(
                    String::isNotBlank,
                )
            ?: "VexLife-Prod Mac"
    }
}

// [VXG RealForever]
