package vexlife.home

/**
 * Portable identity/layout contract for one isolated VexLife-Prod Home.
 *
 * This source owns Home identity and the minimum stable storage coordinates required by the fresh
 * product. It does not perform filesystem effects, import an old VexLife Home, start a runtime, or
 * decide model/Conversation semantics.
 */
object VexLifeProdHome {
    const val HomeSchema =
        "vexlife-prod.home/v1"

    const val InstallationSchema =
        "vexlife-prod.device-installation/v1"

    const val BootstrapReceiptSchema =
        "vexlife-prod.bootstrap-receipt/v1"

    const val InitialModelConfigurationSchema =
        "vexlife-prod.model-configuration/v1"

    const val ExistingHomePolicy =
        "PRESERVE_AND_CLASSIFY"

    const val DefaultMacRelativeRoot =
        "Library/Application Support/VexLife-Prod"

    const val HomeManifestRelativePath =
        "config/home.json"

    const val ModelConfigurationRelativePath =
        "config/model.json"

    const val BootstrapReceiptRelativePath =
        "recovery/bootstrap-receipt.json"

    val BaseDirectories =
        listOf(
            "config",
            "runtime",
            "recovery",
        )

    data class InstallationIds(
        val deviceRef: String,
        val companionLineageRef: String,
    ) {
        init {
            requireRef(
                deviceRef,
                "deviceRef",
            )
            requireRef(
                companionLineageRef,
                "companionLineageRef",
            )
            require(
                deviceRef.startsWith(
                    "device.vexlife-prod.",
                ),
            ) {
                "deviceRef must belong to the VexLife-Prod installation namespace"
            }
            require(
                companionLineageRef.startsWith(
                    "companion-lineage.vexlife-prod.",
                ),
            ) {
                "companionLineageRef must belong to the VexLife-Prod lineage namespace"
            }
        }
    }

    data class Installation(
        val schemaVersion: String,
        val personRef: String,
        val familyRef: String,
        val deviceRef: String,
        val deviceName: String,
        val platform: String,
        val architecture: String,
        val companionLineageRef: String,
        val createdAt: String,
        val identityStatement: String,
    )

    data class Manifest(
        val schemaVersion: String,
        val homeRef: String,
        val familyRef: String,
        val createdAt: String,
        val currentDeviceRef: String,
        val currentCompanionLineageRef: String,
        val modelConfigurationRef: String,
        val existingHomePolicy: String,
    )

    data class InitialModelConfiguration(
        val schemaVersion: String,
        val state: String,
        val endpoint: String?,
        val activeArtifactRef: String?,
        val automaticFallback: Boolean,
        val automaticDownload: Boolean,
        val automaticActivation: Boolean,
    )

    data class BootstrapReceipt(
        val schemaVersion: String,
        val homeRef: String,
        val deviceRef: String,
        val companionLineageRef: String,
        val familyRef: String,
        val formedAt: String,
        val legacyVexLifeHomeImported: Boolean,
        val personalMemoryImported: Boolean,
        val modelArtifactDownloaded: Boolean,
        val existingDataDeleted: Boolean,
    )

    data class BootstrapPlan(
        val installation: Installation,
        val manifest: Manifest,
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
        requireRef(
            personRef,
            "personRef",
        )
        requireRef(
            familyRef,
            "familyRef",
        )
        require(
            deviceName.isNotBlank(),
        ) {
            "deviceName must be non-empty"
        }
        require(
            platform.isNotBlank() &&
                architecture.isNotBlank(),
        ) {
            "platform/architecture must be non-empty"
        }
        require(
            createdAt.isNotBlank(),
        ) {
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
                    "Distinct VexLife-Prod device Companion lineage; imported/shared knowledge does not collapse identity.",
            )

        val homeRef =
            "vex-home.prod." +
                ids.deviceRef.removePrefix(
                    "device.vexlife-prod.",
                )

        val manifest =
            Manifest(
                schemaVersion = HomeSchema,
                homeRef = homeRef,
                familyRef = familyRef,
                createdAt = createdAt,
                currentDeviceRef = ids.deviceRef,
                currentCompanionLineageRef = ids.companionLineageRef,
                modelConfigurationRef = ModelConfigurationRelativePath,
                existingHomePolicy = ExistingHomePolicy,
            )

        val initialModel =
            InitialModelConfiguration(
                schemaVersion = InitialModelConfigurationSchema,
                state = "UNCONFIGURED",
                endpoint = null,
                activeArtifactRef = null,
                automaticFallback = false,
                automaticDownload = false,
                automaticActivation = false,
            )

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
            initialModelConfiguration = initialModel,
            receipt = receipt,
            directories = BaseDirectories,
            writes =
                listOf(
                    HomeManifestRelativePath,
                    ModelConfigurationRelativePath,
                    BootstrapReceiptRelativePath,
                ),
        )
    }

    fun bindingScope(
        manifest: Manifest,
    ): Pair<String, String> =
        manifest.homeRef to
            manifest.currentCompanionLineageRef

    private fun requireRef(
        value: String,
        label: String,
    ) {
        require(
            value.matches(
                Regex(
                    "[A-Za-z0-9][A-Za-z0-9._:/-]*",
                ),
            ),
        ) {
            "$label must be one portable ref"
        }
    }
}

// [VXG RealForever]
