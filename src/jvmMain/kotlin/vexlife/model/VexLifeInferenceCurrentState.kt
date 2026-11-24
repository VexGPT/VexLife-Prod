package vexlife.model

import java.nio.file.Path
import kotlinx.serialization.Serializable
import vexlife.model.heretic.HereticModelOfferings

/**
 * Read-only current inference view over the existing Local Models owner and Partner currentness.
 *
 * This object does not own staged-target persistence, durable model choice, runtime switching,
 * provider invocation, prompt/provider cache, Home mutation, or Multi-Vex participant identity.
 *
 * When [stagedTargetHandleOrNull] is present, it is an already-decided process-local human target
 * supplied by a rightful caller. This projection only re-marks the exact current row so Local and
 * Partner can share one no-effect target/serving view.
 */
object VexLifeInferenceCurrentState {
    @Serializable
    data class Projection(
        val models: VexLifeModelSourceProjection.Projection,
        val inference: VexLifeInferenceSelection.Projection,
    )

    fun project(
        productHome: Path,
        local: VexLifeModels.Projection,
        partner: VexLifePartnerModelsCurrentState.Projection,
        stagedTargetHandleOrNull: String? = null,
    ): Projection {
        val home =
            productHome
                .toAbsolutePath()
                .normalize()

        val localTargetedHandle =
            local.models
                .singleOrNull {
                    it.targeted
                }
                ?.handle
        val localServingHandle =
            local.models
                .singleOrNull {
                    it.serving
                }
                ?.handle

        require(
            local.selectedHandle ==
                localTargetedHandle,
        ) {
            "Local Models selectedHandle must match its exact targeted row"
        }
        require(
            local.servingHandle ==
                localServingHandle,
        ) {
            "Local Models servingHandle must match its exact serving row"
        }

        val localOfferings =
            local.models.map { row ->
                val entry =
                    requireNotNull(
                        HereticModelOfferings.byHandle(
                            productHome = home,
                            handle = row.handle,
                        ),
                    ) {
                        "Local Models row is not one current Home offering: " +
                            row.handle
                    }

                VexLifeModelSourceProjection.LocalOffering(
                    handle = row.handle,
                    modelRef = entry.modelRef,
                    targeted = row.targeted,
                    serving = row.serving,
                )
            }

        val observed =
            VexLifeModelSourceProjection.project(
                local = localOfferings,
                partner = partner.offerings,
            )

        val models =
            stagedTargetHandleOrNull
                ?.let {
                    stageTarget(
                        observed = observed,
                        handle = it,
                    )
                }
                ?: observed

        return Projection(
            models = models,
            inference =
                VexLifeInferenceSelection.project(
                    models,
                ),
        )
    }

    private fun stageTarget(
        observed: VexLifeModelSourceProjection.Projection,
        handle: String,
    ): VexLifeModelSourceProjection.Projection {
        val target =
            VexLifeInferenceSelection.requireTarget(
                models = observed,
                handle = handle,
            )
        val rows =
            observed.rows.map { row ->
                row.copy(
                    targeted =
                        row.handle ==
                            target.handle,
                )
            }

        return VexLifeModelSourceProjection.Projection(
            rows = rows,
            availableSources = observed.availableSources,
            targetedSource = target.sourceKind,
            servingSource =
                rows
                    .singleOrNull {
                        it.serving
                    }
                    ?.sourceKind,
        )
    }
}

// [VXG RealForever]
