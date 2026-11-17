package vexlife.model.heretic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HereticRuntimeRealizationSequenceTest {
    @Test
    fun everyRealizationUsesOneOrderedRecipe() {
        val expectedStages =
            HereticRuntimeRealizationSequence.Stage.entries

        HereticRuntimeRealization.Entries.forEach {
            realization ->
            val plan =
                HereticRuntimeRealizationSequence.plan(
                    realization,
                )

            assertEquals(
                expectedStages,
                plan.steps.map {
                    it.stage
                },
            )
            assertEquals(
                expectedStages.indices.map {
                    it + 1
                },
                plan.steps.map {
                    it.ordinal
                },
            )
            assertEquals(
                realization.realizationRef,
                plan.realizationRef,
            )
            assertEquals(
                realization.target.targetRef,
                plan.targetRef,
            )
            assertTrue(
                !plan.externalEffectsPerformed,
            )
        }
    }

    @Test
    fun MacAndWindowsDifferByDeclaredDependenciesNotByRecipeShape() {
        val mac =
            HereticRuntimeRealizationSequence.plan(
                HereticRuntimeRealization.legacyMacV1(
                    HereticProductIdentity.ModelRef,
                ),
            )
        val windows =
            HereticRuntimeRealizationSequence.plan(
                HereticRuntimeRealization.Windows4B,
            )

        assertEquals(
            mac.steps.map {
                it.stage
            },
            windows.steps.map {
                it.stage
            },
        )

        val macEnvironment =
            mac.steps.single {
                it.stage == HereticRuntimeRealizationSequence.Stage.ENVIRONMENT
            }
        val macRuntimeBinary =
            mac.steps.single {
                it.stage == HereticRuntimeRealizationSequence.Stage.RUNTIME_BINARY
            }
        val windowsEnvironment =
            windows.steps.single {
                it.stage == HereticRuntimeRealizationSequence.Stage.ENVIRONMENT
            }
        val windowsRuntimeBinary =
            windows.steps.single {
                it.stage == HereticRuntimeRealizationSequence.Stage.RUNTIME_BINARY
            }

        assertEquals(
            HereticRuntimeRealizationSequence.RequirementClass.REQUIRED,
            macEnvironment.requirementClass,
        )
        assertEquals(
            HereticRuntimeRealizationSequence.RequirementClass.NOT_REQUIRED,
            macRuntimeBinary.requirementClass,
        )
        assertEquals(
            HereticRuntimeRealizationSequence.RequirementClass.NOT_REQUIRED,
            windowsEnvironment.requirementClass,
        )
        assertEquals(
            HereticRuntimeRealizationSequence.RequirementClass.REQUIRED,
            windowsRuntimeBinary.requirementClass,
        )
    }

    @Test
    fun processAndReceiptKeepTheDeclaredLifetimeOwnerVisible() {
        HereticRuntimeRealization.Entries.forEach {
            realization ->
            val plan =
                HereticRuntimeRealizationSequence.plan(
                    realization,
                )
            val process =
                plan.steps.single {
                    it.stage == HereticRuntimeRealizationSequence.Stage.PROCESS
                }
            val receipt =
                plan.steps.single {
                    it.stage == HereticRuntimeRealizationSequence.Stage.RECEIPT
                }

            when (val requirement = realization.dependencies.process) {
                is HereticRuntimeRealization.Requirement.Required -> {
                    assertEquals(
                        requirement.value.lifetimeOwnerRef,
                        process.ownerRef,
                    )
                    assertEquals(
                        requirement.value.lifetimeOwnerRef,
                        receipt.ownerRef,
                    )
                }

                is HereticRuntimeRealization.Requirement.NotRequired ->
                    assertEquals(
                        null,
                        receipt.ownerRef,
                    )
            }
        }
    }
}

// [VXG RealForever]
