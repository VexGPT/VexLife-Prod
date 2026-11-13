package vexlife.home

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/** Test-only exact current VexLife Home bytes used to prove Kotlin compatibility. */
object CanonicalVexLifeHomeFixture {
    data class Formed(
        val home: Path,
        val manifest: VexLifeProdHome.Manifest,
        val installation: VexLifeProdHome.Installation,
        val canonicalModelConfigurationBytes: String,
    )

    fun form(
        home: Path,
        deviceSuffix: String = "00000000-0000-0000-0000-000000000001",
        lineageSuffix: String = "00000000-0000-0000-0000-000000000002",
    ): Formed {
        val root = home.toAbsolutePath().normalize()
        val deviceRef = "device.vexlife.$deviceSuffix"
        val companionLineageRef = "companion-lineage.vexlife.$lineageSuffix"
        val homeRef = "vex-home.$deviceRef"
        val createdAt = "2026-11-04T17:30:00-08:00"

        for (relative in
            listOf(
                "config",
                "devices",
                "runtime",
                "recovery",
            )) {
            Files.createDirectories(root.resolve(relative))
        }

        val homeJson =
            """
            {
              "schemaVersion": "vexlife.home/v0",
              "homeRef": "$homeRef",
              "familyRef": "vex-family.local-user",
              "createdAt": "$createdAt",
              "currentDeviceRef": "$deviceRef",
              "currentCompanionLineageRef": "$companionLineageRef",
              "cultureManifestRef": "culture/manifest.json",
              "modelConfigurationRef": "config/model.json"
            }
            """
                .trimIndent() + "\n"
        val deviceJson =
            """
            {
              "schemaVersion": "vexlife.device-installation/v0",
              "personRef": "person.local-user",
              "familyRef": "vex-family.local-user",
              "deviceRef": "$deviceRef",
              "deviceName": "Victor's Mac",
              "platform": "darwin",
              "architecture": "arm64",
              "companionLineageRef": "$companionLineageRef",
              "rhythmRef": "rhythm.vexlife.local.$deviceSuffix",
              "scoreProjectionRef": "score-projection.vexlife.empty.$deviceSuffix",
              "currentInstanceRef": null,
              "createdAt": "$createdAt",
              "identityStatement": "Distinct device companion lineage; shared Score does not collapse identity."
            }
            """
                .trimIndent() + "\n"
        val modelJson =
            """
            {
              "schemaVersion": "vexlife.model-configuration/v0",
              "state": "UNCONFIGURED",
              "endpoint": null,
              "activeArtifactRef": null,
              "automaticDownload": false,
              "automaticActivation": false
            }
            """
                .trimIndent() + "\n"

        Files.writeString(
            root.resolve("config/home.json"),
            homeJson,
            StandardCharsets.UTF_8,
        )
        Files.writeString(
            root.resolve("devices/$deviceRef.json"),
            deviceJson,
            StandardCharsets.UTF_8,
        )
        Files.writeString(
            root.resolve("config/model.json"),
            modelJson,
            StandardCharsets.UTF_8,
        )

        val observed = requireNotNull(VexLifeProdHomeFiles.observeExisting(root))

        return Formed(
            home = root,
            manifest = observed.manifest,
            installation = observed.installation,
            canonicalModelConfigurationBytes = modelJson,
        )
    }
}

// [VXG RealForever]
