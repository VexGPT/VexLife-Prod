package vexlife.model.heretic

/**
 * Exact certified artifact composition for [HereticProductIdentity.Current].
 *
 * This catalog is identity/provenance source only. It owns no network acquisition, filesystem
 * materialization or runtime activation effect.
 */
object HereticArtifactCatalog {
    const val CertifiedMemberCount =
        11

    const val CertifiedBytes =
        15_153_242_506L

    const val ProviderSourceRef =
        "hf.donedynamics.qwen3.8-27b-heretic-mlx-4bit.808b82a2bd04b2bac362b5d0b7a1197aeea59e57"

    const val LicenseRef =
        "license.apache-2.0.qwen3.8-heretic"

    data class Member(
        val roleRef: String,
        val artifactRef: String,
        val filename: String,
        val mediaType: String,
        val sha256: String,
        val expectedBytes: Long,
        val sourceRef: String,
        val licenseRef: String,
    ) {
        init {
            requirePortableRef(
                roleRef,
                "roleRef",
            )
            requirePortableRef(
                artifactRef,
                "artifactRef",
            )
            require(
                filename.isNotBlank() &&
                    !filename.contains("/") &&
                    !filename.contains("\\"),
            ) {
                "Heretic artifact filename must be one safe leaf name: $filename"
            }
            require(
                mediaType.isNotBlank(),
            )
            requireSha256(
                sha256,
                "artifact.sha256",
            )
            require(
                expectedBytes > 0,
            ) {
                "Heretic artifact expectedBytes must be positive"
            }
            requirePortableRef(
                sourceRef,
                "sourceRef",
            )
            requirePortableRef(
                licenseRef,
                "licenseRef",
            )
        }
    }

    val Members: List<Member> =
        listOf(
            Member(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.gitattributes",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.gitattributes",
                filename = ".gitattributes",
                mediaType = "text/plain",
                sha256 = "34448b82c17d60fec9b65b1f093c115ddbaadc04beb1b0140b6bfed2e012a930",
                expectedBytes = 1_570L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            Member(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.readme",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.readme",
                filename = "README.md",
                mediaType = "text/markdown",
                sha256 = "43d4f27187bedb6668bee8e29578e3279fdef77b76ad176580c2e60cb1953b1a",
                expectedBytes = 3_686L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            Member(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.chat-template",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.chat-template",
                filename = "chat_template.jinja",
                mediaType = "text/plain",
                sha256 = "c3cf9e34abf4f9e36c2d72165aa9c132d3e2a725b6c2586aaa3a8af9d7a81041",
                expectedBytes = 8_952L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            Member(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.config",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.config",
                filename = "config.json",
                mediaType = "application/json",
                sha256 = "21ec916f33652b2433e289cc999a0a75ea42958b2770c0973d31d6006941bb94",
                expectedBytes = 4_192L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            Member(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.generation-config",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.generation-config",
                filename = "generation_config.json",
                mediaType = "application/json",
                sha256 = "d0d0ed2e37cdfafef4a5067d5ea2407b05f4fb50526e47c008a5b235d50240fb",
                expectedBytes = 214L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            Member(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.model-00001-of-00003-safetensors",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.model-00001-of-00003-safetensors",
                filename = "model-00001-of-00003.safetensors",
                mediaType = "application/octet-stream",
                sha256 = "075eac5fbba3951bc4870c1ac65d684c32e0abb24284201bd27f49df56735963",
                expectedBytes = 5_328_325_648L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            Member(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.model-00002-of-00003-safetensors",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.model-00002-of-00003-safetensors",
                filename = "model-00002-of-00003.safetensors",
                mediaType = "application/octet-stream",
                sha256 = "d936fce99536decc4e0584590305acf2af461d867d8c9a79e2dfbdd328b14e5c",
                expectedBytes = 5_354_185_130L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            Member(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.model-00003-of-00003-safetensors",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.model-00003-of-00003-safetensors",
                filename = "model-00003-of-00003.safetensors",
                mediaType = "application/octet-stream",
                sha256 = "139547bce74259fa4181a9c1b6664c1d14ffa3763e998ea52f0be7fdf12955bb",
                expectedBytes = 4_450_532_735L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            Member(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.model-safetensors-index",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.model-safetensors-index",
                filename = "model.safetensors.index.json",
                mediaType = "application/json",
                sha256 = "a2161f6a6c9f7c434145f97a4a4262a74eb3e16a95088e713754eb108b198511",
                expectedBytes = 189_789L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            Member(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.tokenizer",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.tokenizer",
                filename = "tokenizer.json",
                mediaType = "application/json",
                sha256 = "06b9509352d2af50381ab2247e083b80d32d5c0aba91c272ca9ff729b6a0e523",
                expectedBytes = 19_989_325L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
            Member(
                roleRef = "artifact-role.devex.heretic.qwen3.8-27b.mlx4.member.tokenizer-config",
                artifactRef = "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member.tokenizer-config",
                filename = "tokenizer_config.json",
                mediaType = "application/json",
                sha256 = "5d9c0d3314a253b714ddbbfeef7a55e9295c45c3da9f5fd9907dae6ebe6cc731",
                expectedBytes = 1_265L,
                sourceRef = ProviderSourceRef,
                licenseRef = LicenseRef,
            ),
        )

    init {
        require(
            Members.size ==
                CertifiedMemberCount,
        ) {
            "Heretic certified artifact member count drift"
        }
        require(
            Members.sumOf(
                Member::expectedBytes,
            ) ==
                CertifiedBytes,
        ) {
            "Heretic certified artifact byte total drift"
        }
        require(
            Members
                .map(
                    Member::artifactRef,
                )
                .distinct()
                .size ==
                Members.size,
        ) {
            "Heretic certified artifact refs must remain unique"
        }
        require(
            Members
                .map(
                    Member::filename,
                )
                .distinct()
                .size ==
                Members.size,
        ) {
            "Heretic certified artifact filenames must remain unique"
        }
    }
}

// [VXG RealForever]
