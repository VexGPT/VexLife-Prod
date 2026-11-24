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
 * When [stagedTargetOrNull] is present, it is the already-decided process-local human target owned
 * by Models. This projection revalidates that exact coordinate against current Local + Partner rows
 * before re-marking the target, so stale source/model/provider identity fails closed.
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
        stagedTargetOrNull: VexLifeInferenceSelection.Coordinate? = null,
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
            stagedTargetOrNull
                ?.let {
                    stageTarget(
                        observed = observed,
                        target = it,
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
        target: VexLifeInferenceSelection.Coordinate,
    ): VexLifeModelSourceProjection.Projection {
        val current =
            VexLifeInferenceSelection.requireTarget(
                models = observed,
                handle = target.handle,
            )
        require(
            current ==
                target,
        ) {
            "staged inference target is stale or does not match one exact current Models row"
        }
        val rows =
            observed.rows.map { row ->
                row.copy(
                    targeted =
                        row.handle ==
                            current.handle,
                )
            }

        return VexLifeModelSourceProjection.Projection(
            rows = rows,
            availableSources = observed.availableSources,
            targetedSource = current.sourceKind,
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
