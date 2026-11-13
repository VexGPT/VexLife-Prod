package vexlife.home

import kotlinx.serialization.Serializable
import vexlife.continuity.VexLifeContinuity

/**
 * Portable compatibility contract for the canonical lived VexLife Home.
 *
 * New Beginnings may replace implementation machinery, but a repository/product name does not mint
 * another human Home, device, Companion lineage, Conversation or Memory identity. Product-specific
 * construction/runtime records live beneath the canonical Home without replacing its manifest.
 */
object VexLifeProdHome {
    const val HomeSchema = VexLifeContinuity.HomeSchema

    const val InstallationSchema = VexLifeContinuity.DeviceInstallationSchema

    const val BootstrapReceiptSchema = "vexlife.bootstrap-receipt/v0"

    const val CanonicalModelConfigurationSchema = "vexlife.model-configuration/v0"

    const val InitialModelConfigurationSchema = "vexlife-prod.runtime-model-binding/v1"

    const val DefaultPersonRef = "person.local-user"

    const val DefaultFamilyRef = "vex-family.local-user"

    const val ExistingHomePolicy = "PRESERVE_AND_CLASSIFY"

    const val DefaultMacRelativeRoot = VexLifeContinuity.CanonicalMacHomeRelativeRoot

    const val HomeManifestRelativePath = VexLifeContinuity.HomeManifestRelativePath

    const val CanonicalModelConfigurationRelativePath = "config/model.json"

    const val ModelConfigurationRelativePath = VexLifeContinuity.ProductModelBindingRelativePath

    const val BootstrapReceiptRelativePath = "recovery/bootstrap-receipt.json"

    val BaseDirectories = listOf("config", "devices", "runtime", "recovery")

    /**
     * No-effect classification of one human-selected Vex Home coordinate.
     *
     * The predecessor lifecycle already distinguished an absent/empty selected folder (first setup
     * may form a Home there) from an existing folder it could not identify (preserve; never
     * overwrite, repair or delete). This vocabulary keeps that lived distinction as Home-owner
     * meaning instead of letting an unreadable coordinate surface as an exception.
     *
     * HELD_* never admits a Home effect. Only [Absent] and [EmptyDirectory] admit formation, and
     * only [Canonical] carries Home/device/Companion identity.
     */
    enum class LocationDisposition(
        val wireValue: String,
        val formationAllowed: Boolean,
        val identityPresent: Boolean,
    ) {
        Absent("ABSENT_CANONICAL_VEXLIFE_HOME", formationAllowed = true, identityPresent = false),
        EmptyDirectory(
            "EMPTY_DIRECTORY_VEXLIFE_HOME_FORMABLE",
            formationAllowed = true,
            identityPresent = false,
        ),
        Canonical(
            "EXISTING_CANONICAL_VEXLIFE_HOME_BOUND",
            formationAllowed = false,
            identityPresent = true,
        ),
        HeldNoncanonicalContent(
            "HELD_NONCANONICAL_CONTENT",
            formationAllowed = false,
            identityPresent = false,
        ),
        HeldInvalidCanonicalRecords(
            "HELD_INVALID_CANONICAL_RECORDS",
            formationAllowed = false,
            identityPresent = false,
        ),
        HeldNotDirectory("HELD_NOT_A_DIRECTORY", formationAllowed = false, identityPresent = false),
        HeldSymbolicLink("HELD_SYMBOLIC_LINK", formationAllowed = false, identityPresent = false),
        HeldLocationNotWritable(
            "HELD_LOCATION_NOT_WRITABLE",
            formationAllowed = false,
            identityPresent = false,
        ),
        HeldLocationUnreadable(
            "HELD_LOCATION_UNREADABLE",
            formationAllowed = false,
            identityPresent = false,
        );

        val held: Boolean
            get() = !formationAllowed && !identityPresent

        companion object {
            fun forWireValue(wireValue: String): LocationDisposition? = entries.firstOrNull {
                it.wireValue == wireValue
            }
        }
    }

    data class InstallationIds(val deviceRef: String, val companionLineageRef: String) {
        init {
            requireRef(deviceRef, "deviceRef")
            requireRef(companionLineageRef, "companionLineageRef")
            require(deviceRef.startsWith("device.vexlife.")) {
                "deviceRef must preserve the canonical VexLife installation namespace"
            }
            require(companionLineageRef.startsWith("companion-lineage.vexlife.")) {
                "companionLineageRef must preserve the canonical VexLife lineage namespace"
            }
        }
    }

    @Serializable
    data class Installation(
        val schemaVersion: String,
        val personRef: String,
        val familyRef: String,
        val deviceRef: String,
        val deviceName: String,
        val platform: String,
        val architecture: String,
        val companionLineageRef: String,
        val rhythmRef: String? = null,
        val scoreProjectionRef: String? = null,
        val currentInstanceRef: String? = null,
        val createdAt: String,
        val identityStatement: String,
    )

    @Serializable
    data class Manifest(
        val schemaVersion: String,
        val homeRef: String,
        val familyRef: String,
        val createdAt: String,
        val currentDeviceRef: String,
        val currentCompanionLineageRef: String,
        val cultureManifestRef: String? = null,
        val modelConfigurationRef: String = CanonicalModelConfigurationRelativePath,
        val existingHomePolicy: String = ExistingHomePolicy,
    )

    @Serializable
    data class CanonicalModelConfiguration(
        val schemaVersion: String,
        val state: String,
        val endpoint: String?,
        val activeArtifactRef: String?,
        val automaticDownload: Boolean,
        val automaticActivation: Boolean,
    )

    /**
     * Product-runtime model binding stored beneath runtime/vexlife-prod.
     *
     * This is deliberately not the canonical Home's config/model.json contract.
     */
    @Serializable
    data class InitialModelConfiguration(
        val schemaVersion: String,
        val state: String,
        val endpoint: String?,
        val activeArtifactRef: String?,
        val automaticFallback: Boolean,
        val automaticDownload: Boolean,
        val automaticActivation: Boolean,
    )

    @Serializable
    data class BootstrapReceipt(
        val schemaVersion: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val familyRef: String,
        val culturePackRef: String? = null,
        val cultureSha256: String? = null,
        val formedAt: String,
        val legacyVexLifeHomeImported: Boolean = false,
        val personalMemoryImported: Boolean,
        val modelArtifactDownloaded: Boolean,
        val existingDataDeleted: Boolean,
    )

    /**
     * Pure future/bootstrap plan retained for tests and explicit source design.
     *
     * The current product setup does not execute this plan. It must bind an already-established
     * canonical VexLife Home until the accepted VexLife bootstrap itself is ported exactly.
     */
    data class BootstrapPlan(
        val installation: Installation,
        val manifest: Manifest,
        val canonicalModelConfiguration: CanonicalModelConfiguration,
        val initialModelConfiguration: InitialModelConfiguration,
        val receipt: BootstrapReceipt,
        val directories: List<String>,
        val writes: List<String>,
    )

    fun plan(
        personRef: String,
        familyRef: String,
        deviceName: String,
        platform: String,
        architecture: String,
        ids: InstallationIds,
        createdAt: String,
    ): BootstrapPlan {
        requireRef(personRef, "personRef")
        requireRef(familyRef, "familyRef")
        require(deviceName.isNotBlank()) {
            "deviceName must be non-empty"
        }
        require(platform.isNotBlank() && architecture.isNotBlank()) {
            "platform/architecture must be non-empty"
        }
        require(createdAt.isNotBlank()) {
            "createdAt must be non-empty"
        }

        val installation =
            Installation(
                schemaVersion = InstallationSchema,
                personRef = personRef,
                familyRef = familyRef,
                deviceRef = ids.deviceRef,
                deviceName = deviceName,
                platform = platform,
                architecture = architecture,
                companionLineageRef = ids.companionLineageRef,
                createdAt = createdAt,
                identityStatement =
                    "Canonical VexLife device Companion lineage preserved across implementation replacement.",
            )

        val homeRef = "vex-home.${ids.deviceRef}"

        val manifest =
            Manifest(
                schemaVersion = HomeSchema,
                homeRef = homeRef,
                familyRef = familyRef,
                createdAt = createdAt,
                currentDeviceRef = ids.deviceRef,
                currentCompanionLineageRef = ids.companionLineageRef,
                cultureManifestRef = null,
                modelConfigurationRef = CanonicalModelConfigurationRelativePath,
                existingHomePolicy = ExistingHomePolicy,
            )

        val canonicalModel = initialCanonicalModelConfiguration()
        val initialModel = initialProductModelConfiguration()

        val receipt =
            BootstrapReceipt(
                schemaVersion = BootstrapReceiptSchema,
                homeRef = homeRef,
                deviceRef = ids.deviceRef,
                companionLineageRef = ids.companionLineageRef,
                familyRef = familyRef,
                formedAt = createdAt,
                legacyVexLifeHomeImported = false,
                personalMemoryImported = false,
                modelArtifactDownloaded = false,
                existingDataDeleted = false,
            )

        return BootstrapPlan(
            installation = installation,
            manifest = manifest,
            canonicalModelConfiguration = canonicalModel,
            initialModelConfiguration = initialModel,
            receipt = receipt,
            directories = BaseDirectories,
            writes =
                listOf(
                    HomeManifestRelativePath,
                    "devices/${ids.deviceRef}.json",
                    CanonicalModelConfigurationRelativePath,
                    ModelConfigurationRelativePath,
                    BootstrapReceiptRelativePath,
                ),
        )
    }

    fun validateBootstrapPlan(plan: BootstrapPlan): BootstrapPlan {
        val installation = validateInstallation(plan.installation)
        val manifest = validateManifest(plan.manifest)
        val canonicalModel = validateCanonicalModelConfiguration(plan.canonicalModelConfiguration)
        val initialModel = validateInitialModelConfiguration(plan.initialModelConfiguration)
        val receipt = plan.receipt

        require(
            manifest.homeRef == "vex-home.${installation.deviceRef}" &&
                manifest.familyRef == installation.familyRef &&
                manifest.createdAt == installation.createdAt &&
                manifest.currentDeviceRef == installation.deviceRef &&
                manifest.currentCompanionLineageRef == installation.companionLineageRef
        ) {
            "VexLife installation and Home manifest identities disagree"
        }

        require(
            receipt.schemaVersion == BootstrapReceiptSchema &&
                receipt.homeRef == manifest.homeRef &&
                receipt.deviceRef == installation.deviceRef &&
                receipt.companionLineageRef == installation.companionLineageRef &&
                receipt.familyRef == installation.familyRef &&
                receipt.formedAt == installation.createdAt &&
                !receipt.legacyVexLifeHomeImported &&
                !receipt.personalMemoryImported &&
                !receipt.modelArtifactDownloaded &&
                !receipt.existingDataDeleted
        ) {
            "bootstrap receipt does not describe exact preservation-safe Home formation"
        }

        require(
            plan.directories == BaseDirectories &&
                plan.writes ==
                    listOf(
                        HomeManifestRelativePath,
                        "devices/${installation.deviceRef}.json",
                        CanonicalModelConfigurationRelativePath,
                        ModelConfigurationRelativePath,
                        BootstrapReceiptRelativePath,
                    )
        ) {
            "VexLife bootstrap filesystem plan drift"
        }

        require(canonicalModel.state == "UNCONFIGURED" && initialModel.state == "UNCONFIGURED")

        return plan
    }

    fun validateManifest(manifest: Manifest): Manifest {
        require(manifest.schemaVersion == HomeSchema) {
            "VexLife Home manifest schema is not current"
        }
        requireRef(manifest.homeRef, "homeRef")
        requireRef(manifest.familyRef, "familyRef")
        require(manifest.createdAt.isNotBlank()) {
            "VexLife Home manifest requires formed time"
        }
        requireRef(manifest.currentDeviceRef, "currentDeviceRef")
        require(manifest.currentDeviceRef.startsWith("device.vexlife.")) {
            "currentDeviceRef must preserve the canonical VexLife installation namespace"
        }
        requireRef(manifest.currentCompanionLineageRef, "currentCompanionLineageRef")
        require(manifest.currentCompanionLineageRef.startsWith("companion-lineage.vexlife.")) {
            "currentCompanionLineageRef must preserve the canonical VexLife lineage namespace"
        }
        require(manifest.homeRef == "vex-home.${manifest.currentDeviceRef}") {
            "homeRef must preserve the canonical VexLife Home/device relationship"
        }
        require(manifest.modelConfigurationRef == CanonicalModelConfigurationRelativePath) {
            "canonical VexLife model configuration coordinate drift"
        }
        require(manifest.existingHomePolicy == ExistingHomePolicy) {
            "VexLife Home preservation policy drift"
        }

        return manifest
    }

    fun validateInstallation(installation: Installation): Installation {
        require(installation.schemaVersion == InstallationSchema) {
            "VexLife device installation schema is not current"
        }
        requireRef(installation.personRef, "installation.personRef")
        requireRef(installation.familyRef, "installation.familyRef")
        requireRef(installation.deviceRef, "installation.deviceRef")
        require(installation.deviceRef.startsWith("device.vexlife.")) {
            "installation.deviceRef is outside the canonical VexLife namespace"
        }
        require(
            installation.deviceName.isNotBlank() &&
                installation.platform.isNotBlank() &&
                installation.architecture.isNotBlank() &&
                installation.createdAt.isNotBlank() &&
                installation.identityStatement.isNotBlank()
        ) {
            "VexLife installation identity is incomplete"
        }
        requireRef(installation.companionLineageRef, "installation.companionLineageRef")
        require(installation.companionLineageRef.startsWith("companion-lineage.vexlife.")) {
            "installation Companion lineage is outside the canonical VexLife namespace"
        }
        installation.rhythmRef?.let {
            requireRef(it, "installation.rhythmRef")
        }
        installation.scoreProjectionRef?.let {
            requireRef(it, "installation.scoreProjectionRef")
        }
        installation.currentInstanceRef?.let {
            requireRef(it, "installation.currentInstanceRef")
        }

        return installation
    }

    fun initialCanonicalModelConfiguration(): CanonicalModelConfiguration =
        CanonicalModelConfiguration(
            schemaVersion = CanonicalModelConfigurationSchema,
            state = "UNCONFIGURED",
            endpoint = null,
            activeArtifactRef = null,
            automaticDownload = false,
            automaticActivation = false,
        )

    fun validateCanonicalModelConfiguration(
        value: CanonicalModelConfiguration
    ): CanonicalModelConfiguration {
        require(
            value.schemaVersion == CanonicalModelConfigurationSchema &&
                value.state == "UNCONFIGURED" &&
                value.endpoint == null &&
                value.activeArtifactRef == null &&
                !value.automaticDownload &&
                !value.automaticActivation
        ) {
            "initial canonical VexLife model configuration is not the exact unconfigured state"
        }

        return value
    }

    fun initialProductModelConfiguration(): InitialModelConfiguration =
        InitialModelConfiguration(
            schemaVersion = InitialModelConfigurationSchema,
            state = "UNCONFIGURED",
            endpoint = null,
            activeArtifactRef = null,
            automaticFallback = false,
            automaticDownload = false,
            automaticActivation = false,
        )

    fun validateInitialModelConfiguration(
        value: InitialModelConfiguration
    ): InitialModelConfiguration {
        require(
            value.schemaVersion == InitialModelConfigurationSchema &&
                value.state == "UNCONFIGURED" &&
                value.endpoint == null &&
                value.activeArtifactRef == null &&
                !value.automaticFallback &&
                !value.automaticDownload &&
                !value.automaticActivation
        ) {
            "initial VexLife-Prod runtime model binding is not the exact unconfigured state"
        }

        return value
    }

    fun bindingScope(manifest: Manifest): Pair<String, String> {
        val current = validateManifest(manifest)

        return current.homeRef to current.currentCompanionLineageRef
    }

    private fun requireRef(value: String, label: String) {
        require(value.matches(Regex("[A-Za-z0-9][A-Za-z0-9._:/-]*"))) {
            "$label must be one portable ref"
        }
    }
}

// [VXG RealForever]
