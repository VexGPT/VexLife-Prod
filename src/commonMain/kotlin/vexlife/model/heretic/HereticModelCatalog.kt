package vexlife.model.heretic

/**
 * Source-owned catalog of the local Heretic models a person can choose.
 *
 * Why it exists now: while the Product offered one model, identity lived in one static owner and a
 * catalog would have been an unearned registry. Victor asked for selectable models (a quicker,
 * smaller model for Macs without high-end memory, beside the certified 27B), so offered models,
 * their exact bytes and their default are owned here.
 *
 * The catalog answers: which models are offered, what each one's exact identity and bytes are,
 * where its files live in the Home, how much memory it wants, and how it stands. It does not own
 * artifact bytes on disk (HereticArtifactMaterialization), the selected model of a Home
 * (FileHereticModelConfigurationStore), runtime liveness (HereticPersistentRuntime), receipts,
 * capability, personality or any effect authority.
 *
 * Standing:
 * - CERTIFIED: lived-qualified in this Product on Victor's Mac.
 * - CANDIDATE: exact bytes pinned (every member's size and SHA-256 at one provider revision, one
 *   content-set SHA-256), same runtime class and Product-pinned mlx-lm; it is qualified at every
 *   start by the same runtime qualification as the certified model, and its lived comparison is
 *   still owed. Selecting a candidate is a human choice, never an automatic fallback.
 */
object HereticModelCatalog {
    enum class Standing {
        CERTIFIED,
        CANDIDATE,
    }

    data class Entry(
        val identity: HereticProductIdentity.Value,
        /** Short stable name used by Setup, Settings and keyed words. */
        val handle: String,
        val humanLabel: String,
        val providerRepository: String,
        val providerRevision: String,
        val members: List<HereticArtifactCatalog.Member>,
        /** Folder under the Home; the provider revision is the folder below it. */
        val modelRootRelativePath: String,
        /** Unified memory this model wants to run comfortably beside macOS and VexLife. */
        val recommendedMemoryBytes: Long,
        /** Complete provider/model representation relationship; never inherited implicitly. */
        val companionProfile: HereticCompanionProvider.Profile,
        val standing: Standing,
        /**
         * Whether Setup may start a Home on this model by itself. A newly added model family is
         * the person's choice until its lived comparison has been made.
         */
        val autoRecommended: Boolean = true,
    ) {
        val totalBytes: Long
            get() = members.sumOf(HereticArtifactCatalog.Member::expectedBytes)

        val modelRef: String
            get() = identity.modelRef

        init {
            require(handle.matches(Regex("[a-z][a-z0-9-]*"))) {
                "model handle must be one short name"
            }
            require(providerRevision.matches(Regex("[0-9a-f]{40}"))) {
                "provider revision must be one exact commit"
            }
            require(
                members.isNotEmpty() && members.map { it.filename }.distinct().size == members.size
            ) {
                "model members must be non-empty and unique"
            }
            require(members.all { it.sourceRef.endsWith("." + providerRevision) }) {
                "every model member must come from the pinned provider revision"
            }
            require(modelRootRelativePath.matches(Regex("models/heretic/[a-z0-9.-]+"))) {
                "model root must be one folder under models/heretic"
            }
        }
    }

    private const val NineBSource =
        "hf.foresee.qwen3.8-9b-heretic-uncensored-5bit-mlx.8a3eec7d4b298cc5100a191bbb870d2fb7446bb5"
    private const val FourBSource =
        "hf.yachen4ever.qwen3.8-4b-distill-heretic-abliterated-mlx-4bit.f47bef776bcd49b552219c14c4da949c8b49e336"
    private const val GptOss20BSource =
        "hf.d8a.gpt-oss-20b-heretic-ara-v3-mlx-8bit.227eba2dcf295c662074309446929c4a5226d587"

    private fun member(
        source: String,
        role: String,
        artifact: String,
        filename: String,
        mediaType: String,
        sha256: String,
        bytes: Long,
    ) =
        HereticArtifactCatalog.Member(
            roleRef = role,
            artifactRef = artifact,
            filename = filename,
            mediaType = mediaType,
            sha256 = sha256,
            expectedBytes = bytes,
            sourceRef = source,
            licenseRef = HereticArtifactCatalog.LicenseRef,
        )

    private val NineBMembers: List<HereticArtifactCatalog.Member> =
        listOf(
            member(
                NineBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-9b.mlx5.member.gitattributes",
                "artifact.vexlife-prod.qwen3.8-9b-heretic-mlx-5bit.8a3eec7d.member.gitattributes",
                ".gitattributes",
                "text/plain",
                "34448b82c17d60fec9b65b1f093c115ddbaadc04beb1b0140b6bfed2e012a930",
                1_570L,
            ),
            member(
                NineBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-9b.mlx5.member.readme",
                "artifact.vexlife-prod.qwen3.8-9b-heretic-mlx-5bit.8a3eec7d.member.readme",
                "README.md",
                "text/markdown",
                "f1178c30c406f52d168321e3e9ac1dd3fcce9024faad0d3c5ee1e6b9a4e2c2a6",
                1_556L,
            ),
            member(
                NineBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-9b.mlx5.member.chat-template",
                "artifact.vexlife-prod.qwen3.8-9b-heretic-mlx-5bit.8a3eec7d.member.chat-template",
                "chat_template.jinja",
                "text/plain",
                "a4aee8afcf2e0711942cf848899be66016f8d14a889ff9ede07bca099c28f715",
                7_756L,
            ),
            member(
                NineBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-9b.mlx5.member.config",
                "artifact.vexlife-prod.qwen3.8-9b-heretic-mlx-5bit.8a3eec7d.member.config",
                "config.json",
                "application/json",
                "46e9588177621c3e6d07c29cc3524e16e22ac698f326c75299a0952f4394c0b8",
                3_090L,
            ),
            member(
                NineBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-9b.mlx5.member.generation-config",
                "artifact.vexlife-prod.qwen3.8-9b-heretic-mlx-5bit.8a3eec7d.member.generation-config",
                "generation_config.json",
                "application/json",
                "2b74f0e736d274777db8ad400235c4a6e767821d27e7597eacec4b2d6b9a707e",
                174L,
            ),
            member(
                NineBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-9b.mlx5.member.model-00001-of-00002-safetensors",
                "artifact.vexlife-prod.qwen3.8-9b-heretic-mlx-5bit.8a3eec7d.member.model-00001-of-00002-safetensors",
                "model-00001-of-00002.safetensors",
                "application/octet-stream",
                "72c76a4646e5fc56e0749c5712225e060a1cf1ec7f3fffef1efec2137e4a1bd8",
                5_355_676_988L,
            ),
            member(
                NineBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-9b.mlx5.member.model-00002-of-00002-safetensors",
                "artifact.vexlife-prod.qwen3.8-9b-heretic-mlx-5bit.8a3eec7d.member.model-00002-of-00002-safetensors",
                "model-00002-of-00002.safetensors",
                "application/octet-stream",
                "297e3724b2a4baa405a8cd6caf92d5549988126a1d1d5f73b8526eaf0bb5132b",
                1_361_123_465L,
            ),
            member(
                NineBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-9b.mlx5.member.model-safetensors-index",
                "artifact.vexlife-prod.qwen3.8-9b-heretic-mlx-5bit.8a3eec7d.member.model-safetensors-index",
                "model.safetensors.index.json",
                "application/json",
                "c37c9db5b81ec021af8dbd0b4380fc4f94aa01f9b7911cab8e9a909fd71b004b",
                95_099L,
            ),
            member(
                NineBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-9b.mlx5.member.tokenizer",
                "artifact.vexlife-prod.qwen3.8-9b-heretic-mlx-5bit.8a3eec7d.member.tokenizer",
                "tokenizer.json",
                "application/json",
                "06b9509352d2af50381ab2247e083b80d32d5c0aba91c272ca9ff729b6a0e523",
                19_989_325L,
            ),
            member(
                NineBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-9b.mlx5.member.tokenizer-config",
                "artifact.vexlife-prod.qwen3.8-9b-heretic-mlx-5bit.8a3eec7d.member.tokenizer-config",
                "tokenizer_config.json",
                "application/json",
                "8f781f0d7369fb2532a77284368c2a5f9eccd6413a381dfa3795787e20ca4ca4",
                1_202L,
            ),
        )

    private val GptOss20BMembers: List<HereticArtifactCatalog.Member> =
        listOf(
            member(
                GptOss20BSource,
                "artifact-role.vexlife-prod.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.member.gitattributes",
                "artifact.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.227eba2d.member.gitattributes",
                ".gitattributes",
                "text/plain",
                "34448b82c17d60fec9b65b1f093c115ddbaadc04beb1b0140b6bfed2e012a930",
                1_570L,
            ),
            member(
                GptOss20BSource,
                "artifact-role.vexlife-prod.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.member.readme",
                "artifact.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.227eba2d.member.readme",
                "README.md",
                "text/markdown",
                "54a7967dce665d0602183614b221ebde813c1622061918b8d83ed1d021516495",
                1_126L,
            ),
            member(
                GptOss20BSource,
                "artifact-role.vexlife-prod.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.member.chat-template",
                "artifact.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.227eba2d.member.chat-template",
                "chat_template.jinja",
                "text/plain",
                "a4c9919cbbd4acdd51ccffe22da049264b1b73e59055fa58811a99efbd7c8146",
                16_738L,
            ),
            member(
                GptOss20BSource,
                "artifact-role.vexlife-prod.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.member.config",
                "artifact.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.227eba2d.member.config",
                "config.json",
                "application/json",
                "3a722e195356fd7f97c60c3667d28d667b386ab160b23eb9a0ead9a2cfc2313b",
                34_018L,
            ),
            member(
                GptOss20BSource,
                "artifact-role.vexlife-prod.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.member.generation-config",
                "artifact.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.227eba2d.member.generation-config",
                "generation_config.json",
                "application/json",
                "0bc1a359a9a2cbedc1c2c4345fd268f3f86c0c4b49a7d4440f4bcb8af5f240ff",
                172L,
            ),
            member(
                GptOss20BSource,
                "artifact-role.vexlife-prod.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.member.weights-1",
                "artifact.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.227eba2d.member.weights-1",
                "model-00001-of-00003.safetensors",
                "application/octet-stream",
                "1fa8934ab04eb75a761416407665c3ea8797191ef7c69dccb9c01256ac27594a",
                5_303_719_858L,
            ),
            member(
                GptOss20BSource,
                "artifact-role.vexlife-prod.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.member.weights-2",
                "artifact.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.227eba2d.member.weights-2",
                "model-00002-of-00003.safetensors",
                "application/octet-stream",
                "06e9a197c2bb6c1ce7d9398854c13b448eb3a0b1f55425a4be894150930af0a3",
                5_281_581_915L,
            ),
            member(
                GptOss20BSource,
                "artifact-role.vexlife-prod.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.member.weights-3",
                "artifact.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.227eba2d.member.weights-3",
                "model-00003-of-00003.safetensors",
                "application/octet-stream",
                "49a68850355f0217fe334de912b816a104e36788b7e39e6dbbd01a61263e7f77",
                1_490_905_779L,
            ),
            member(
                GptOss20BSource,
                "artifact-role.vexlife-prod.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.member.weights-index",
                "artifact.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.227eba2d.member.weights-index",
                "model.safetensors.index.json",
                "application/json",
                "6fa724aa7a9130561f9b97debcc06c56ba150d8fbd58fc1168f53a22d6a5490a",
                67_046L,
            ),
            member(
                GptOss20BSource,
                "artifact-role.vexlife-prod.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.member.special-tokens-map",
                "artifact.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.227eba2d.member.special-tokens-map",
                "special_tokens_map.json",
                "application/json",
                "8464cabd6eda239fe46ebf8ae63b46c417721784a961a022f6b59174a2cda0e2",
                440L,
            ),
            member(
                GptOss20BSource,
                "artifact-role.vexlife-prod.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.member.tokenizer",
                "artifact.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.227eba2d.member.tokenizer",
                "tokenizer.json",
                "application/json",
                "0614fe83cadab421296e664e1f48f4261fa8fef6e03e63bb75c20f38e37d07d3",
                27_868_174L,
            ),
            member(
                GptOss20BSource,
                "artifact-role.vexlife-prod.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.member.tokenizer-config",
                "artifact.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.227eba2d.member.tokenizer-config",
                "tokenizer_config.json",
                "application/json",
                "9279e942392b742d633c7adbb89ebe002c98399db8926a7af5125c726f404070",
                4_200L,
            ),
        )

    private val FourBMembers: List<HereticArtifactCatalog.Member> =
        listOf(
            member(
                FourBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.member.gitattributes",
                "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-mlx-4bit.f47bef77.member.gitattributes",
                ".gitattributes",
                "text/plain",
                "34448b82c17d60fec9b65b1f093c115ddbaadc04beb1b0140b6bfed2e012a930",
                1_570L,
            ),
            member(
                FourBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.member.readme",
                "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-mlx-4bit.f47bef77.member.readme",
                "README.md",
                "text/markdown",
                "8ff09e890a2a7980aa1f7fd3a4e090f822e61f46610877c654bfc362a464595d",
                6_018L,
            ),
            member(
                FourBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.member.chat-template",
                "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-mlx-4bit.f47bef77.member.chat-template",
                "chat_template.jinja",
                "text/plain",
                "a4aee8afcf2e0711942cf848899be66016f8d14a889ff9ede07bca099c28f715",
                7_756L,
            ),
            member(
                FourBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.member.config",
                "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-mlx-4bit.f47bef77.member.config",
                "config.json",
                "application/json",
                "05c0558e1eee06e97d2e73f7b01d8cb0d94322cdd4e310a90a16234f9d63cf53",
                3_857L,
            ),
            member(
                FourBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.member.generation-config",
                "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-mlx-4bit.f47bef77.member.generation-config",
                "generation_config.json",
                "application/json",
                "2cee8242d29b39b8684970e78098eb7664859f62c42c764500719ed01842a0ed",
                174L,
            ),
            member(
                FourBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.member.model-safetensors",
                "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-mlx-4bit.f47bef77.member.model-safetensors",
                "model.safetensors",
                "application/octet-stream",
                "ce5a70fa86c09f709a662c656e5a171bacc2aac2c5e4717d96336ff10cf7c000",
                3_034_299_183L,
            ),
            member(
                FourBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.member.model-safetensors-index",
                "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-mlx-4bit.f47bef77.member.model-safetensors-index",
                "model.safetensors.index.json",
                "application/json",
                "840634e215ba74eb1ff53c03660b82443d249845884910e87b1a99a258225951",
                101_944L,
            ),
            member(
                FourBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.member.preprocessor-config",
                "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-mlx-4bit.f47bef77.member.preprocessor-config",
                "preprocessor_config.json",
                "application/json",
                "27225450ac9c6529872ee1924fcb0962ff5634834f817040f444118116f4e516",
                390L,
            ),
            member(
                FourBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.member.processor-config",
                "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-mlx-4bit.f47bef77.member.processor-config",
                "processor_config.json",
                "application/json",
                "27959590c1d52d08c2b4cefe09b522a489475deb2cd9ca5e3b97f27cf23e2308",
                989L,
            ),
            member(
                FourBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.member.tokenizer",
                "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-mlx-4bit.f47bef77.member.tokenizer",
                "tokenizer.json",
                "application/json",
                "6f32ce20dc35f57a7f9ad1eac03525bd7d30f9df8cea6507e958279cc3657706",
                19_989_492L,
            ),
            member(
                FourBSource,
                "artifact-role.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.member.tokenizer-config",
                "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-mlx-4bit.f47bef77.member.tokenizer-config",
                "tokenizer_config.json",
                "application/json",
                "dcb88c4a1dff8096b6fd919e458d987351b499f86db65d1b997ed515a90cc35c",
                1_269L,
            ),
        )

    private const val GiB = 1024L * 1024L * 1024L

    /**
     * Named model definitions owned by this catalog.
     *
     * Identity names stay stable as qualification changes: certification/candidacy belongs to
     * [Entry.standing], not to the model's name.
     *
     * To offer the next model: define one [Entry] here, then register it once in [Entries].
     * Setup, Settings, artifact delivery/materialization and runtime selection consume [Entries];
     * they do not need model-specific branches merely because another model is offered.
     */
    object Models {
        val Qwen27B =
            Entry(
                identity = HereticProductIdentity.Current,
                handle = "heretic-27b",
                humanLabel = HereticProductIdentity.HumanModelLabel,
                providerRepository = "donedynamics/Qwen3.8-27B-heretic-MLX-4bit",
                providerRevision = "808b82a2bd04b2bac362b5d0b7a1197aeea59e57",
                members = HereticArtifactCatalog.Members,
                modelRootRelativePath = "models/heretic/qwen3.8-27b-mlx4",
                recommendedMemoryBytes = 32 * HereticModelCatalog.GiB,
                companionProfile = HereticCompanionProvider.Profiles.Qwen,
                standing = Standing.CERTIFIED,
            )

        /** Same Qwen3.8 Heretic family, 9B at 5-bit: about 6.7 GB of weights. */
        val Qwen9B =
            Entry(
                identity =
                    HereticModelCatalog.providerPublished(
                        modelRef = "model.vexlife-prod.heretic.qwen3.8-9b.mlx5.5901a94ba7c7",
                        modelBundleRef =
                            "model-bundle.vexlife.heretic.qwen3.8-9b.mlx5.5901a94ba7c7.001",
                        modelProfileRef =
                            "model-profile.vexlife-prod.qwen38-9b-heretic-mlx5.candidate.20261114A",
                        artifactSetRef = "artifact.vexlife-prod.qwen3.8-9b-heretic-mlx-5bit.8a3eec7d",
                        contentSetSha256 =
                            "5901a94ba7c70a1bbf4d4d46a15e76d1f29e148921bf2d5a95098a918859318d",
                        providerRevision = "8a3eec7d4b298cc5100a191bbb870d2fb7446bb5",
                        custodyRef =
                            "resolution.vexlife-prod.artifact-pin.qwen38-9b-heretic-mlx5.20261114A",
                    ),
                handle = "heretic-9b",
                humanLabel = "Heretic 9B (5-bit, MLX)",
                providerRepository = "Foresee/Qwen3.8-9B-heretic-uncensored-5bit-MLX",
                providerRevision = "8a3eec7d4b298cc5100a191bbb870d2fb7446bb5",
                members = HereticModelCatalog.NineBMembers,
                modelRootRelativePath = "models/heretic/qwen3.8-9b-mlx5",
                recommendedMemoryBytes = 16 * HereticModelCatalog.GiB,
                companionProfile = HereticCompanionProvider.Profiles.Qwen,
                standing = Standing.CANDIDATE,
            )

        /**
         * Smallest offered: a 4B distilled from Qwen3.8 on a Qwen3.5 base, Heretic + abliterated,
         * 4-bit: about 3 GB of weights, for 8-16 GB Macs. A different lineage than 27B/9B, said
         * plainly.
         */
        val Qwen4B =
            Entry(
                identity =
                    HereticModelCatalog.providerPublished(
                        modelRef = "model.vexlife-prod.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20",
                        modelBundleRef =
                            "model-bundle.vexlife.heretic.qwen3.8-4b-distill.mlx4.d2f8f4028f20.001",
                        modelProfileRef =
                            "model-profile.vexlife-prod.qwen38-4b-distill-heretic-mlx4.candidate.20261114A",
                        artifactSetRef =
                            "artifact.vexlife-prod.qwen3.8-4b-distill-heretic-mlx-4bit.f47bef77",
                        contentSetSha256 =
                            "d2f8f4028f2030f994c6182a8c52cc429cab0d5b635dfa2801218ba736b89aea",
                        providerRevision = "f47bef776bcd49b552219c14c4da949c8b49e336",
                        custodyRef =
                            "resolution.vexlife-prod.artifact-pin.qwen38-4b-distill-heretic-mlx4.20261114A",
                    ),
                handle = "heretic-4b",
                humanLabel = "Heretic 4B Distill (4-bit, MLX)",
                providerRepository = "yachen4ever/Qwen3.8-4B-Distill-Heretic-Abliterated-MLX-4bit",
                providerRevision = "f47bef776bcd49b552219c14c4da949c8b49e336",
                members = HereticModelCatalog.FourBMembers,
                modelRootRelativePath = "models/heretic/qwen3.8-4b-distill-mlx4",
                recommendedMemoryBytes = 8 * HereticModelCatalog.GiB,
                companionProfile = HereticCompanionProvider.Profiles.Qwen,
                standing = Standing.CANDIDATE,
            )

        /**
         * OpenAI gpt-oss-20b decensored by p-e-w with Heretic's Arbitrary-Rank Ablation (ARA v3; card:
         * KL divergence 0.0554, refusals 3/100), converted to MLX (MXFP4 experts, 8-bit attention) with
         * mlx-lm 0.29.1. A mixture of experts: 21B parameters, about 3.6B active per token, so it
         * answers quickly for its size. Harmony response format; not started by default.
         */
        val GptOss20B =
            Entry(
                identity =
                    HereticModelCatalog.providerPublished(
                        modelRef = "model.vexlife-prod.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.10c6bad646aa",
                        modelBundleRef =
                            "model-bundle.vexlife.heretic.gpt-oss-20b-ara-v3.mlx-mxfp4.10c6bad646aa.001",
                        modelProfileRef =
                            "model-profile.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.candidate.20261115A",
                        artifactSetRef =
                            "artifact.vexlife-prod.gpt-oss-20b-heretic-ara-v3-mlx.227eba2d",
                        contentSetSha256 =
                            "10c6bad646aa379476f3d8c1c595b77ce618d7e106665cfcefd06ad12935bf75",
                        providerRevision = "227eba2dcf295c662074309446929c4a5226d587",
                        custodyRef =
                            "resolution.vexlife-prod.artifact-pin.gpt-oss-20b-heretic-ara-v3-mlx.20261115A",
                    ),
                handle = "gpt-oss-20b",
                humanLabel = "Heretic gpt-oss 20B (MXFP4, MLX)",
                providerRepository = "D8A/gpt-oss-20b-heretic-ara-v3-mlx-8Bit",
                providerRevision = "227eba2dcf295c662074309446929c4a5226d587",
                members = HereticModelCatalog.GptOss20BMembers,
                modelRootRelativePath = "models/heretic/gpt-oss-20b-ara-v3-mlx",
                recommendedMemoryBytes = 24 * HereticModelCatalog.GiB,
                companionProfile = HereticCompanionProvider.Profiles.GptOssHarmony,
                standing = Standing.CANDIDATE,
                autoRecommended = false,
            )
    }

    /** Offered models, by the memory each wants, largest first. */
    val Entries: List<Entry> =
        listOf(
            Models.Qwen27B,
            Models.GptOss20B,
            Models.Qwen9B,
            Models.Qwen4B,
        )

    /**
     * What a Home without a choice gets: the certified model, unchanged from before the catalog.
     */
    val Default: Entry = Models.Qwen27B

    fun entry(modelRef: String): Entry? = Entries.firstOrNull { it.modelRef == modelRef }

    fun requireEntry(modelRef: String): Entry =
        requireNotNull(entry(modelRef)) { "model $modelRef is not offered by this Product" }

    fun byHandle(handle: String): Entry? = Entries.firstOrNull { it.handle == handle }

    /** Whether [entry] wants no more memory than this Mac has. Unknown memory fits nothing. */
    fun fits(entry: Entry, totalMemoryBytes: Long?): Boolean =
        totalMemoryBytes != null && totalMemoryBytes >= entry.recommendedMemoryBytes

    /**
     * The model a Home that has not chosen yet starts with on this Mac: the largest
     * [Entry.autoRecommended] model that fits its memory (the certified 27B on a Mac like
     * Victor's), the smallest such model when none fits, the certified default when memory is
     * unknown. The person can choose any offered model.
     */
    fun recommendedFor(totalMemoryBytes: Long?): Entry {
        if (totalMemoryBytes == null) return Default
        val startable = Entries.filter { it.autoRecommended }
        return startable.firstOrNull { fits(it, totalMemoryBytes) }
            ?: startable.minBy { it.recommendedMemoryBytes }
    }

    /**
     * A provider-published model shares the certified model's runtime relationship (adapter, class,
     * transport, host profile, Python, mlx-lm, endpoint, request model, receipt coordinate) and
     * differs only in what it is. Its trainer source is the provider revision that published the
     * weights, because no separate trainer commit is known for it.
     */
    private fun providerPublished(
        modelRef: String,
        modelBundleRef: String,
        modelProfileRef: String,
        artifactSetRef: String,
        contentSetSha256: String,
        providerRevision: String,
        custodyRef: String,
    ): HereticProductIdentity.Value =
        HereticProductIdentity.Current.copy(
            modelBundleRef = modelBundleRef,
            generationRef =
                "generation.vexlife-prod.provider-published." + providerRevision.take(12),
            modelRef = modelRef,
            modelProfileRef = modelProfileRef,
            artifactSetRef = artifactSetRef,
            artifactCustodyEvidenceRef = custodyRef,
            artifactContentSetSha256 = contentSetSha256,
            trainerSourceCommit = providerRevision,
        )

    init {
        require(Entries.map { it.modelRef }.distinct().size == Entries.size) {
            "model refs must be unique"
        }
        require(Entries.map { it.handle }.distinct().size == Entries.size) {
            "model handles must be unique"
        }
        require(Entries.all { it.companionProfile.profileRef.isNotBlank() }) {
            "every offered model requires one explicit Companion provider profile"
        }
        require(Entries.map { it.modelRootRelativePath }.distinct().size == Entries.size) {
            "model folders must be unique"
        }
        require(
            Entries.flatMap { e -> e.members.map { it.artifactRef } }
                .let { it.distinct().size == it.size }
        ) {
            "artifact refs must be unique across models"
        }
        require(
            Entries.map { it.identity.artifactContentSetSha256 }.distinct().size == Entries.size
        ) {
            "content sets must be unique across models"
        }
        require(Default.standing == Standing.CERTIFIED) { "the default model must be certified" }
        require(Default.autoRecommended) { "the default model must be startable by default" }
    }
}

// [VXG RealForever]
