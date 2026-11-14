package vexlife.local

import java.net.InetAddress
import java.nio.file.Path
import java.time.OffsetDateTime
import vexlife.logging.VexLogger
import vexlife.conversation.DirectConversationStore
import vexlife.home.VexLifeProdHome
import vexlife.home.VexLifeProdHomeFiles
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticModelConfiguration
import vexlife.model.heretic.HereticProductIdentity
import vexlife.model.heretic.HereticRuntimeAvailability
import vexlife.model.heretic.runtime.FileHereticModelConfigurationStore
import vexlife.model.heretic.runtime.FileHereticRuntimeReceiptStore
import vexlife.model.heretic.runtime.HereticArtifactDelivery
import vexlife.model.heretic.runtime.HereticArtifactMaterialization
import vexlife.model.heretic.runtime.HereticMacHostProfile
import vexlife.model.heretic.runtime.HereticPersistentRuntime
import vexlife.model.heretic.runtime.HereticPythonEnvironment
import vexlife.model.heretic.runtime.MacHereticRuntimePlatform
import vexlife.model.heretic.runtime.StableVexLifeHereticRuntimeCompatibility
import vexlife.platform.python.ManagedPython

/**
 * Application-level composition for VexLife-Prod runtime machinery beneath the canonical VexLife
 * Home/identity.
 *
 * This owner contributes sequencing only. Explicit first setup may form the canonical VexLife Home,
 * device and Companion lineage; it never mints a parallel Product identity. Every artifact fact,
 * environment rule, process/lifetime effect and runtime-binding transition remains owned by the
 * component that answers that question.
 */
object VexLifeProdLocalSetup {
    const val RuntimeAuthorityRef = "authority.vexlife-prod.local-setup.heretic-runtime.001"

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
        val qualifiedRuntimeEvidence: HereticModelConfiguration.QualifiedRuntimeEvidence,
        val endpoint: String,
        val requestModel: String,
    )

    /**
     * Construction/lifetime capability for long-lived product consumers.
     *
     * The public result remains the setup projection. This Session retains only the capability to
     * ask the rightful Heretic runtime owner for fresh currentness before later effects.
     */
    internal class Session
    internal constructor(
        val result: Result,
        private val currentRuntimeEvidence:
            () -> HereticModelConfiguration.QualifiedRuntimeEvidence,
        private val recoverRuntimeEvidence:
            (() -> HereticModelConfiguration.QualifiedRuntimeEvidence)? =
            null,
    ) {
        /**
         * Current qualified runtime evidence, recovering once when exact process/substrate
         * currentness cannot be proven.
         *
         * Readiness/health is deliberately not part of currentness. Recovery is therefore entered
         * for an actual ownership/lifetime loss, not because a live generation engine was slow to
         * answer. The chronology records lost -> requested -> completed/failed around that bounded
         * lifecycle recovery.
         */
        fun currentQualifiedRuntimeEvidence(
            onRecovery: (() -> Unit)? = null
        ): HereticModelConfiguration.QualifiedRuntimeEvidence =
            try {
                currentRuntimeEvidence()
            } catch (original: RuntimeException) {
                VexLogger.record(
                    VexLogger.Fact.RuntimeCurrentnessLost,
                    detail = original.javaClass.simpleName + ": " + (original.message ?: ""),
                )
                val recover = recoverRuntimeEvidence ?: throw original
                VexLogger.record(VexLogger.Fact.RuntimeRecoveryRequested)
                onRecovery?.invoke()
                try {
                    recover().also {
                        VexLogger.record(
                            VexLogger.Fact.RuntimeRecoveryCompleted,
                            mapOf("runtimeReceiptRef" to it.runtimeReceiptRef),
                        )
                    }
                } catch (error: Throwable) {
                    VexLogger.record(
                        VexLogger.Fact.RuntimeRecoveryFailed,
                        detail = error.javaClass.simpleName + ": " + (error.message ?: ""),
                    )
                    throw error
                }
            }

        /**
         * Read-only currentness observation for an already-started human turn.
         *
         * This intentionally has no recovery path. A later provider stage may prove that the same
         * runtime receipt/process is still current, but it may not replace the runtime underneath
         * a turn that already began.
         */
        fun observeCurrentQualifiedRuntimeEvidence(): HereticModelConfiguration.QualifiedRuntimeEvidence =
            currentRuntimeEvidence()
    }

    private data class Established(
        val result: Result,
        val currentRuntimeEvidence: () -> HereticModelConfiguration.QualifiedRuntimeEvidence,
        val recoverRuntimeEvidence: (() -> HereticModelConfiguration.QualifiedRuntimeEvidence)?,
    )

    internal data class Hooks(
        val progress: VexLifeProdSetupProgress = VexLifeProdSetupProgress.Silent,
        val observeHost: () -> HereticMacHostProfile.Result = {
            HereticMacHostProfile.observeCurrent()
        },
        val deviceName: () -> String = {
            currentDeviceName()
        },
        val now: () -> String = {
            OffsetDateTime.now().toString()
        },
        val observeConversationWriter:
            (productHome: Path) -> DirectConversationStore.ThreadWriterCurrentness =
            { productHome ->
                DirectConversationStore(productHome).threadWriterCurrentness()
            },
        val recoverConversationWriter:
            (productHome: Path, instanceRef: String, leaseSha256: String) -> Boolean =
            { productHome, instanceRef, leaseSha256 ->
                DirectConversationStore(productHome)
                    .recoverAbandonedWriterLease(
                        expectedAbandonedInstanceRef = instanceRef,
                        expectedLeaseSha256 = leaseSha256,
                    )
            },
        val ensureManagedPython:
            (productHome: Path, allowAcquisition: Boolean) -> ManagedPython.Result =
            { productHome, allowAcquisition ->
                ManagedPython.ensure(
                    productHome = productHome,
                    pythonVersion = HereticProductIdentity.PythonVersion,
                    allowAcquisition = allowAcquisition,
                )
            },
        val materializeArtifacts:
            (
                productHome: Path,
                allowAcquisition: Boolean,
                model: HereticModelCatalog.Entry,
            ) -> HereticArtifactMaterialization.Result =
            { productHome, allowAcquisition, model ->
                HereticArtifactMaterialization.materializeCurrent(
                    productHome = productHome,
                    allowAcquisition = allowAcquisition,
                    acquirer =
                        artifactAcquirerReporting(productHome = productHome, progress = progress, model = model),
                    model = model,
                )
            },
        val ensureHereticEnvironment:
            (
                managedPython: ManagedPython.Result,
                productHome: Path,
                allowCreation: Boolean,
            ) -> HereticPythonEnvironment.Result =
            { managedPython, productHome, allowCreation ->
                HereticPythonEnvironment.ensure(
                    managedPython = managedPython,
                    productHome = productHome,
                    allowCreation = allowCreation,
                )
            },
        val observeStableRuntime:
            (
                productHome: Path,
                binding: HereticPersistentRuntime.Binding,
            ) -> StableVexLifeHereticRuntimeCompatibility.Observation? =
            { productHome, binding ->
                StableVexLifeHereticRuntimeCompatibility.observe(
                    productHome = productHome,
                    binding = binding,
                    platform = MacHereticRuntimePlatform(productHome = productHome),
                )
            },
        val observeCurrentProductRuntime:
            (
                productHome: Path,
                binding: HereticPersistentRuntime.Binding,
                store: HereticPersistentRuntime.ReceiptStore,
            ) -> HereticModelConfiguration.QualifiedRuntimeEvidence =
            { productHome, binding, store ->
                HereticPersistentRuntime.observeCurrentQualified(
                    binding = binding,
                    store = store,
                    platform = MacHereticRuntimePlatform(productHome = productHome),
                )
            },
        val startRuntime:
            (
                productHome: Path,
                binding: HereticPersistentRuntime.Binding,
                store: HereticPersistentRuntime.ReceiptStore,
            ) -> HereticPersistentRuntime.StartResult =
            { productHome, binding, store ->
                HereticPersistentRuntime.startOrReuse(
                    binding = binding,
                    store = store,
                    platform = MacHereticRuntimePlatform(productHome = productHome),
                )
            },
    )

    /**
     * Explicit first-setup identity effect.
     *
     * This may form only the canonical VexLife Home/device/Companion identity after host
     * eligibility. It performs no model/runtime acquisition and does not install/start Product.
     */
    fun formFreshHome(): VexLifeProdHomeFiles.Result =
        formFreshHome(VexLifeProdHomeFiles.defaultMacRoot(Path.of(System.getProperty("user.home"))))

    fun formFreshHome(productHome: Path): VexLifeProdHomeFiles.Result {
        require(productHome.isAbsolute) {
            "selected Vex Home path must be absolute"
        }
        val selectedHome = productHome.normalize()
        return VexLifeProdLocalSetupLease.acquire(selectedHome).use {
            formFreshHomeAt(productHome = selectedHome, hooks = Hooks())
        }
    }

    internal fun formFreshHomeAt(productHome: Path, hooks: Hooks): VexLifeProdHomeFiles.Result {
        val host = hooks.observeHost()

        require(host.state == HereticMacHostProfile.State.Eligible && !host.effectsPerformed) {
            "current Mac does not satisfy the certified VexLife-Prod host profile for fresh Home formation: " +
                (host.reason ?: "UNKNOWN")
        }

        when (val location = VexLifeProdHomeFiles.observeLocation(productHome)) {
            is VexLifeProdHomeFiles.Location.Existing -> return location.home

            is VexLifeProdHomeFiles.Location.Held ->
                throw IllegalArgumentException(
                    "selected Vex Home is preserved and cannot be formed: " +
                        location.disposition.wireValue +
                        " — " +
                        location.detail
                )

            is VexLifeProdHomeFiles.Location.Formable -> Unit
        }

        val plan =
            VexLifeProdHome.plan(
                personRef = VexLifeProdHome.DefaultPersonRef,
                familyRef = VexLifeProdHome.DefaultFamilyRef,
                deviceName = hooks.deviceName(),
                platform = host.observation.platform,
                architecture = host.observation.architecture,
                ids = VexLifeProdHomeFiles.newInstallationIds(),
                createdAt = hooks.now(),
            )

        return VexLifeProdHomeFiles.bootstrap(productHome = productHome, plan = plan)
    }

    /**
     * Ordinary product route. The selected Vex Home coordinate is carried explicitly through the
     * Product relationship; [allowAcquisition] authorizes only missing model/runtime prerequisites.
     */
    fun establish(allowAcquisition: Boolean): Result = establishSession(allowAcquisition).result

    internal fun establishSession(allowAcquisition: Boolean): Session =
        establishSession(
            productHome =
                VexLifeProdHomeFiles.defaultMacRoot(Path.of(System.getProperty("user.home"))),
            allowAcquisition = allowAcquisition,
        )

    /**
     * [allowAcquisition] authorizes downloading/creating missing model and runtime prerequisites.
     * [allowRuntimeStart] authorizes starting the exact product-owned local runtime from
     * prerequisites that are already present. Acquisition implies start; start never implies
     * acquisition. The long-lived service process keeps both false (reuse-only).
     */
    internal fun establishSession(
        productHome: Path,
        allowAcquisition: Boolean,
        allowRuntimeStart: Boolean = allowAcquisition,
        progress: VexLifeProdSetupProgress = VexLifeProdSetupProgress.Silent,
    ): Session {
        require(productHome.isAbsolute) {
            "selected Vex Home path must be absolute"
        }
        require(!allowAcquisition || allowRuntimeStart) {
            "acquisition authority without runtime-start authority is not one lawful setup relationship"
        }
        val selectedHome = productHome.normalize()
        return VexLifeProdLocalSetupLease.acquire(selectedHome).use {
            formSession(
                establishDetailedAt(
                    productHome = selectedHome,
                    allowAcquisition = allowAcquisition,
                    allowRuntimeStart = allowRuntimeStart,
                    hooks = Hooks(progress = progress),
                )
            )
        }
    }

    internal fun establishSerializedAt(
        productHome: Path,
        allowAcquisition: Boolean,
        hooks: Hooks,
    ): Result =
        VexLifeProdLocalSetupLease.acquire(productHome).use {
            establishDetailedAt(
                    productHome = productHome,
                    allowAcquisition = allowAcquisition,
                    allowRuntimeStart = allowAcquisition,
                    hooks = hooks,
                )
                .result
        }

    internal fun establishAt(
        productHome: Path,
        allowAcquisition: Boolean,
        hooks: Hooks,
        allowRuntimeStart: Boolean = allowAcquisition,
    ): Result =
        establishDetailedAt(
                productHome = productHome,
                allowAcquisition = allowAcquisition,
                allowRuntimeStart = allowRuntimeStart,
                hooks = hooks,
            )
            .result

    private fun establishDetailedAt(
        productHome: Path,
        allowAcquisition: Boolean,
        allowRuntimeStart: Boolean,
        hooks: Hooks,
    ): Established {
        require(!allowAcquisition || allowRuntimeStart) {
            "acquisition authority without runtime-start authority is not one lawful setup relationship"
        }
        hooks.progress.phase(VexLifeProdSetupProgress.Phase.CheckingHome)
        val host = hooks.observeHost()

        require(host.state == HereticMacHostProfile.State.Eligible && !host.effectsPerformed) {
            "current Mac does not satisfy the certified VexLife-Prod Heretic host profile: " +
                (host.reason ?: "UNKNOWN")
        }

        val home =
            requireNotNull(VexLifeProdHomeFiles.observeExisting(productHome)) {
                "selected canonical Vex Home is absent at $productHome; Product setup requires the separate Home-formation effect first"
            }

        val canonicalHome = Path.of(home.productHome)

        convergeConversationWriter(productHome = canonicalHome, hooks = hooks)

        val modelConfiguration = FileHereticModelConfigurationStore(canonicalHome)

        // A Home that has not chosen a model yet starts with the largest offered model that fits
        // this Mac; a Home that chose keeps its choice (a bound 27B Home stays exactly as it was).
        val selected =
            when (modelConfiguration.read(home.manifest)) {
                is FileHereticModelConfigurationStore.Snapshot.Unconfigured ->
                    modelConfiguration.selectModel(
                        home.manifest,
                        HereticModelCatalog.recommendedFor(host.observation.totalMemoryBytes),
                    )
                is FileHereticModelConfigurationStore.Snapshot.Heretic ->
                    modelConfiguration.selectHeretic(home.manifest)
            }
        // The Home's own choice decides which catalog model is materialized and run. A Home that
        // never chose is given the certified default, exactly as before the catalog existed.
        val model = HereticModelConfiguration.selected(selected.value)

        hooks.progress.phase(VexLifeProdSetupProgress.Phase.PreparingPython)
        val managedPython = hooks.ensureManagedPython(Path.of(home.productHome), allowAcquisition)

        hooks.progress.phase(VexLifeProdSetupProgress.Phase.CheckingModelFiles)
        val materialized = hooks.materializeArtifacts(Path.of(home.productHome), allowAcquisition, model)

        hooks.progress.phase(VexLifeProdSetupProgress.Phase.PreparingModelRuntime)
        val environment =
            hooks.ensureHereticEnvironment(
                managedPython,
                Path.of(home.productHome),
                allowAcquisition,
            )

        val binding =
            HereticPersistentRuntime.bind(
                authorityRef = RuntimeAuthorityRef,
                materialization = materialized,
                environment = environment,
            )

        val borrowedStableRuntime = hooks.observeStableRuntime(canonicalHome, binding)

        val availabilityAct =
            requireNotNull(
                HereticRuntimeAvailability.resolve(
                    currentState =
                        borrowedStableRuntime?.let { stable ->
                            HereticRuntimeAvailability.State.StableBorrowable(
                                evidence = stable.evidence,
                                runtimeReceiptPath = stable.receiptRelativePath,
                            )
                        } ?: HereticRuntimeAvailability.State.StablePredecessorUnavailable,
                    control =
                        HereticRuntimeAvailability.Control.EnsureAvailable(
                            // Runtime availability's AcquisitionAllowed mode authorizes the
                            // product-owned runtime owner to start/reuse the exact runtime from
                            // the already-ensured environment above. No download happens at this
                            // step, so it follows runtime-start authority, not download authority.
                            productOwnedMode =
                                if (allowRuntimeStart) {
                                    HereticRuntimeAvailability.ProductOwnedMode.AcquisitionAllowed
                                } else {
                                    HereticRuntimeAvailability.ProductOwnedMode.ObservationOnly
                                }
                        ),
                )
            ) {
                "Heretic runtime availability did not resolve one lawful relationship"
            }

        val availabilityAndDisposition =
            when (val admitted = availabilityAct.admittedState) {
                is HereticRuntimeAvailability.State.Qualified -> {
                    require(
                        availabilityAct.effect == null &&
                            admitted.source ==
                                HereticRuntimeAvailability.Source.BorrowedStableVexLife
                    ) {
                        "borrowed stable Heretic runtime availability must not request a product-owned effect"
                    }

                    admitted to "BORROWED_STABLE_VEXLIFE_QUALIFIED_RUNTIME"
                }

                null -> {
                    val productRuntimeStore = FileHereticRuntimeReceiptStore(canonicalHome)

                    when (availabilityAct.effect) {
                        HereticRuntimeAvailability.Effect.EnsureProductOwnedRuntime -> {
                            hooks.progress.phase(VexLifeProdSetupProgress.Phase.StartingLocalModel)
                            val runtime =
                                hooks.startRuntime(canonicalHome, binding, productRuntimeStore)

                            HereticRuntimeAvailability.interpretProductOwnedQualified(
                                HereticPersistentRuntime.configurationEvidence(
                                    binding = binding,
                                    receipt = runtime.receipt,
                                )
                            ) to runtime.disposition
                        }

                        HereticRuntimeAvailability.Effect.ObserveCurrentProductOwnedRuntime -> {
                            val evidence =
                                hooks.observeCurrentProductRuntime(
                                    canonicalHome,
                                    binding,
                                    productRuntimeStore,
                                )

                            HereticRuntimeAvailability.interpretProductOwnedQualified(evidence) to
                                "REOBSERVED_CURRENT_PRODUCT_QUALIFIED_RUNTIME"
                        }

                        null ->
                            error(
                                "unresolved Heretic runtime availability lacks one product-owned effect"
                            )
                    }
                }

                else ->
                    error("Heretic runtime availability admitted a non-qualified terminal state")
            }

        val availability = availabilityAndDisposition.first
        val runtimeDisposition = availabilityAndDisposition.second
        val qualifiedRuntimeEvidence = availability.evidence
        val runtimeReceiptPath = availability.runtimeReceiptPath

        val currentRuntimeEvidence: () -> HereticModelConfiguration.QualifiedRuntimeEvidence =
            when (availability.source) {
                HereticRuntimeAvailability.Source.BorrowedStableVexLife -> {
                    {
                        requireNotNull(hooks.observeStableRuntime(canonicalHome, binding)) {
                                "borrowed stable VexLife Heretic runtime is no longer current"
                            }
                            .evidence
                    }
                }

                HereticRuntimeAvailability.Source.VexLifeProdOwned -> {
                    {
                        hooks.observeCurrentProductRuntime(
                            canonicalHome,
                            binding,
                            FileHereticRuntimeReceiptStore(canonicalHome),
                        )
                    }
                }
            }

        val recoverRuntimeEvidence: (() -> HereticModelConfiguration.QualifiedRuntimeEvidence)? =
            when (availability.source) {
                HereticRuntimeAvailability.Source.VexLifeProdOwned -> {
                    {
                        val recovered =
                            hooks.startRuntime(
                                canonicalHome,
                                binding,
                                FileHereticRuntimeReceiptStore(canonicalHome),
                            )
                        HereticPersistentRuntime.configurationEvidence(
                            binding = binding,
                            receipt = recovered.receipt,
                        )
                    }
                }

                HereticRuntimeAvailability.Source.BorrowedStableVexLife -> null
            }

        val bound =
            modelConfiguration.bindQualified(
                manifest = home.manifest,
                qualifiedRuntime = qualifiedRuntimeEvidence,
                runtimeReceiptPath = runtimeReceiptPath,
            )

        return Established(
            result =
                Result(
                    state = "LOCAL_HERETIC_FOUNDATION_READY",
                    productHome = home.productHome,
                    homeDisposition = home.state,
                    modelSelectionDisposition = selected.state,
                    managedPythonDisposition = managedPython.state,
                    artifactDisposition = materialized.state,
                    environmentDisposition = environment.state,
                    runtimeDisposition = runtimeDisposition,
                    modelBindingDisposition = bound.state,
                    modelConfigurationState = bound.value.state,
                    qualifiedRuntimeEvidence = qualifiedRuntimeEvidence,
                    endpoint = qualifiedRuntimeEvidence.endpoint,
                    requestModel = qualifiedRuntimeEvidence.requestModel,
                ),
            currentRuntimeEvidence = currentRuntimeEvidence,
            recoverRuntimeEvidence = recoverRuntimeEvidence,
        )
    }

    private fun convergeConversationWriter(productHome: Path, hooks: Hooks) {
        val before = hooks.observeConversationWriter(productHome)

        if (before.state == "IDLE") {
            require(
                !before.recoveryRequired &&
                    before.ownerState == null &&
                    before.instanceRef == null &&
                    before.leaseSha256 == null
            ) {
                "idle VexLife Conversation writer currentness is internally inconsistent"
            }
            return
        }

        if (
            before.state == "RECOVERY_REQUIRED" &&
                before.ownerState == "ABSENT" &&
                before.recoveryRequired
        ) {
            val instanceRef =
                requireNotNull(before.instanceRef) {
                    "abandoned VexLife Conversation writer lacks exact instance identity"
                }
            val leaseSha256 =
                requireNotNull(before.leaseSha256) {
                    "abandoned VexLife Conversation writer lacks exact lease identity"
                }

            require(hooks.recoverConversationWriter(productHome, instanceRef, leaseSha256)) {
                "abandoned VexLife Conversation writer recovery did not perform the exact effect"
            }

            val after = hooks.observeConversationWriter(productHome)
            require(
                after.state == "IDLE" &&
                    !after.recoveryRequired &&
                    after.ownerState == null &&
                    after.instanceRef == null &&
                    after.leaseSha256 == null
            ) {
                "abandoned VexLife Conversation writer recovery did not converge to IDLE"
            }
            return
        }

        error(
            "Open Vex thread writer is not safely startable: " +
                before.state +
                "/" +
                (before.ownerState ?: "NONE")
        )
    }

    private fun formSession(established: Established): Session {
        return Session(
            result = established.result,
            currentRuntimeEvidence = established.currentRuntimeEvidence,
            recoverRuntimeEvidence = established.recoverRuntimeEvidence,
        )
    }

    /**
     * Binds the existing per-member delivery progress callback to one whole-model byte projection.
     * Members already complete by size at the start are counted once; a resumed member reports its
     * cumulative bytes including the resumed prefix. Projection only — the materialization owner
     * still verifies every member before anything is admitted.
     */
    private fun artifactAcquirerReporting(
        productHome: Path,
        progress: VexLifeProdSetupProgress,
        model: HereticModelCatalog.Entry,
    ): HereticArtifactDelivery.Acquirer {
        if (progress === VexLifeProdSetupProgress.Silent) {
            return HereticArtifactDelivery.Acquirer()
        }
        val presentAtStart = runCatching {
            HereticArtifactMaterialization.observePresence(productHome, model)
        }
            .getOrNull()
        val totalBytes =
            presentAtStart?.totalBytes ?: HereticArtifactMaterialization.plan(model).totalBytes
        val baseBytes = presentAtStart?.presentBySizeBytes ?: 0L
        val inFlight = mutableMapOf<String, Long>()
        return HereticArtifactDelivery.Acquirer(
            onProgress = { member, delivered ->
                inFlight[member.artifactRef] = delivered.bytes
                progress.artifactBytes(
                    completedBytes = baseBytes + inFlight.values.sum(),
                    totalBytes = totalBytes,
                )
            }
        )
    }

    private fun currentDeviceName(): String {
        val computerName =
            try {
                val process = ProcessBuilder("/usr/sbin/scutil", "--get", "ComputerName").start()
                val value = process.inputStream.bufferedReader().readText().trim()
                process.errorStream.bufferedReader().readText()

                value.takeIf {
                    process.waitFor() == 0 && it.isNotBlank()
                }
            } catch (_: Exception) {
                null
            }

        return computerName
            ?: InetAddress.getLocalHost().hostName.takeIf(String::isNotBlank)
            ?: "VexLife-Prod Mac"
    }
}

// [VXG RealForever]
