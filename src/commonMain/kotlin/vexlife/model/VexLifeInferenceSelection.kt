package vexlife.model

import kotlinx.serialization.Serializable

/**
 * Read-only current inference-selection projection over one source-aware Models projection.
 *
 * This owner does not persist model choice, start/stop a runtime, invoke a provider, mutate Home,
 * own Conversation/Memory, manage provider cache, or create Multi-Vex participant identity.
 *
 * Permanent:
 *
 * MODEL SELECTION != VEX IDENTITY
 * TARGETED MODEL != SERVING MODEL
 * SOURCE SWITCH != RUNTIME AUTHORITY
 * SOURCE SWITCH != PROVIDER FALLBACK
 */
object VexLifeInferenceSelection {
    @Serializable
    data class Coordinate(
        val handle: String,
        val modelRef: String,
        val sourceKind: VexLifeModelSourceProjection.SourceKind,
        val providerRef: String?,
    ) {
        init {
            requireRef(
                handle,
                "handle",
            )
            requireRef(
                modelRef,
                "modelRef",
            )

            when (
                sourceKind
            ) {
                VexLifeModelSourceProjection.SourceKind.LOCAL ->
                    require(
                        providerRef ==
                            null,
                    ) {
                        "Local inference coordinate may not carry a Partner providerRef"
                    }

                VexLifeModelSourceProjection.SourceKind.PARTNER ->
                    requireNotNull(
                        providerRef,
                    ) {
                        "Partner inference coordinate requires providerRef"
                    }
                        .let {
                            requireRef(
                                it,
                                "providerRef",
                            )
                        }
            }
        }
    }

    @Serializable
    data class Projection(
        val targeted: Coordinate?,
        val serving: Coordinate?,
        val switchPending: Boolean,
        val sourceSwitchPending: Boolean,
    ) {
        init {
            require(
                switchPending ==
                    (
                        targeted !=
                            null &&
                            targeted !=
                            serving
                    ),
            ) {
                "switchPending must derive from exact targeted/serving coordinates"
            }
            require(
                sourceSwitchPending ==
                    (
                        targeted !=
                            null &&
                            serving !=
                            null &&
                            targeted.sourceKind !=
                            serving.sourceKind
                    ),
            ) {
                "sourceSwitchPending must derive from exact targeted/serving source identity"
            }
            require(
                !sourceSwitchPending ||
                    switchPending,
            ) {
                "cross-source switching cannot exist without a pending switch"
            }
        }
    }

    fun project(
        models: VexLifeModelSourceProjection.Projection,
    ): Projection {
        val targeted =
            models.rows
                .singleOrNull {
                    it.targeted
                }
                ?.coordinate()
        val serving =
            models.rows
                .singleOrNull {
                    it.serving
                }
                ?.coordinate()

        return Projection(
            targeted = targeted,
            serving = serving,
            switchPending =
                targeted !=
                    null &&
                    targeted !=
                        serving,
            sourceSwitchPending =
                targeted !=
                    null &&
                    serving !=
                        null &&
                    targeted.sourceKind !=
                        serving.sourceKind,
        )
    }

    /**
     * Resolve one human-selected current Models row into its exact inference coordinate.
     *
     * This is no-effect target resolution only. It does not persist the target or perform a switch.
     */
    fun requireTarget(
        models: VexLifeModelSourceProjection.Projection,
        handle: String,
    ): Coordinate =
        requireNotNull(
            models.rows.singleOrNull {
                it.handle ==
                    handle
            },
        ) {
            "target model handle is not one current Models row"
        }
            .coordinate()

    private fun VexLifeModelSourceProjection.Row.coordinate(): Coordinate =
        Coordinate(
            handle = handle,
            modelRef = modelRef,
            sourceKind = sourceKind,
            providerRef = providerRef,
        )

    private fun requireRef(
        value: String,
        field: String,
    ) {
        require(
            value.isNotBlank() &&
                value.length <=
                512 &&
                value.none(
                    Char::isWhitespace,
                ),
        ) {
            "$field must be one bounded non-blank reference"
        }
    }
}

// [VXG RealForever]
