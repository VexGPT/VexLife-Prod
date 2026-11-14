package vexlife.model.heretic

/**
 * Exact source-managed delivery channels for the certified Heretic artifact catalog.
 *
 * Artifact byte identity remains owned by [HereticArtifactCatalog]. This owner answers only how
 * each exact artifact may be obtained by the current fresh product. Transport code consumes these
 * channels; it does not reconstruct provider topology from remembered strings.
 */
object HereticArtifactDeliveryCatalog {
    const val TransportClass =
        "DIRECT_HTTPS_FILE_V1"

    data class Channel(
        val artifactRef: String,
        val channelRef: String,
        val transportClass: String,
        val url: String,
    ) {
        init {
            requirePortableRef(
                artifactRef,
                "artifactRef",
            )
            requirePortableRef(
                channelRef,
                "channelRef",
            )
            require(
                transportClass ==
                    TransportClass,
            ) {
                "Heretic delivery transport class drift"
            }
            require(
                url.startsWith(
                    "https://huggingface.co/",
                ) &&
                    !url.contains(
                        "@",
                    ) &&
                    !url.contains(
                        "#",
                    ),
            ) {
                "Heretic delivery channel must be one credential-free fixed HTTPS source"
            }
        }
    }

    val Channels: List<Channel> =
        listOf(
            channel(
                artifactSuffix = "gitattributes",
                role = "gitattributes",
                filename = ".gitattributes",
            ),
            channel(
                artifactSuffix = "readme",
                role = "readme",
                filename = "README.md",
            ),
            channel(
                artifactSuffix = "chat-template",
                role = "chat-template",
                filename = "chat_template.jinja",
            ),
            channel(
                artifactSuffix = "config",
                role = "config",
                filename = "config.json",
            ),
            channel(
                artifactSuffix = "generation-config",
                role = "generation-config",
                filename = "generation_config.json",
            ),
            channel(
                artifactSuffix = "model-00001-of-00003-safetensors",
                role = "model-00001-of-00003-safetensors",
                filename = "model-00001-of-00003.safetensors",
            ),
            channel(
                artifactSuffix = "model-00002-of-00003-safetensors",
                role = "model-00002-of-00003-safetensors",
                filename = "model-00002-of-00003.safetensors",
            ),
            channel(
                artifactSuffix = "model-00003-of-00003-safetensors",
                role = "model-00003-of-00003-safetensors",
                filename = "model-00003-of-00003.safetensors",
            ),
            channel(
                artifactSuffix = "model-safetensors-index",
                role = "model-safetensors-index",
                filename = "model.safetensors.index.json",
            ),
            channel(
                artifactSuffix = "tokenizer",
                role = "tokenizer",
                filename = "tokenizer.json",
            ),
            channel(
                artifactSuffix = "tokenizer-config",
                role = "tokenizer-config",
                filename = "tokenizer_config.json",
            ),
        ) +
            HereticModelCatalog.Entries
                .filter { it !== HereticModelCatalog.Certified27B }
                .flatMap(::channelsFor)

    /**
     * Channels of a catalog model other than the certified one: the same fixed, credential-free
     * provider coordinate (repository + exact revision + filename) its members are pinned to.
     */
    private fun channelsFor(entry: HereticModelCatalog.Entry): List<Channel> =
        entry.members.map { member ->
            Channel(
                artifactRef = member.artifactRef,
                channelRef =
                    "channel.vexlife-prod.heretic." + entry.handle + "." +
                        member.roleRef.substringAfterLast(".member.") + ".hf-fixed.001",
                transportClass = TransportClass,
                url =
                    "https://huggingface.co/" + entry.providerRepository + "/resolve/" + entry.providerRevision +
                        "/" + member.filename + "?download=true",
            )
        }

    private val ByArtifactRef =
        Channels.associateBy(
            Channel::artifactRef,
        )

    init {
        require(
            Channels.size ==
                HereticModelCatalog.Entries.sumOf { it.members.size },
        ) {
            "Heretic delivery channel count must match the catalog's artifact count"
        }
        require(
            ByArtifactRef.size ==
                Channels.size,
        ) {
            "Heretic delivery artifact refs must be unique"
        }
        require(
            Channels
                .map(
                    Channel::channelRef,
                )
                .distinct()
                .size ==
                Channels.size,
        ) {
            "Heretic delivery channel refs must be unique"
        }
        require(
            HereticModelCatalog.Entries
                .flatMap { it.members }
                .map(
                    HereticArtifactCatalog.Member::artifactRef,
                )
                .toSet() ==
                ByArtifactRef.keys,
        ) {
            "Heretic delivery catalog must cover every catalog model's artifacts exactly"
        }
    }

    fun channelFor(
        artifactRef: String,
    ): Channel =
        requireNotNull(
            ByArtifactRef[
                artifactRef
            ],
        ) {
            "no current Heretic delivery channel for $artifactRef"
        }

    private fun channel(
        artifactSuffix: String,
        role: String,
        filename: String,
    ): Channel =
        Channel(
            artifactRef =
                "artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.member." +
                    artifactSuffix,
            channelRef =
                "channel.vexlife-prod.heretic.qwen3.8-27b-mlx4." +
                    role +
                    ".hf-fixed.001",
            transportClass = TransportClass,
            url =
                "https://huggingface.co/" +
                    "donedynamics/Qwen3.8-27B-heretic-MLX-4bit" +
                    "/resolve/" +
                    "808b82a2bd04b2bac362b5d0b7a1197aeea59e57" +
                    "/" +
                    filename +
                    "?download=true",
        )
}

// [VXG RealForever]
