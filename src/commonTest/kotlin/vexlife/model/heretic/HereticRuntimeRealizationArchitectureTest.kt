package vexlife.model.heretic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HereticRuntimeRealizationArchitectureTest {
    @Test
    fun projectionCarriesExactFamilyAttachedRealizationFormula() {
        val snapshot = HereticRuntimeRealizationArchitecture.current()
        val windows = HereticRuntimeRealization.Windows4B

        assertEquals(
            HereticRuntimeAvailabilityArchitecture.FamilyRef,
            snapshot.familyRef,
        )
        assertTrue(
            snapshot.nodes.any {
                it.nodeRef == windows.realizationRef &&
                    it.role == HereticRuntimeRealizationArchitecture.Role.REALIZATION
            },
        )
        assertTrue(
            snapshot.relationships.any {
                it.relationshipClass ==
                    HereticRuntimeRealizationArchitecture.RelationshipClass.MODEL_REALIZED_BY &&
                    it.fromRef == windows.modelRef &&
                    it.toRef == windows.realizationRef
            },
        )
        assertTrue(
            snapshot.relationships.any {
                it.relationshipClass ==
                    HereticRuntimeRealizationArchitecture.RelationshipClass.REALIZATION_TARGETS &&
                    it.fromRef == windows.realizationRef &&
                    it.toRef == windows.target.targetRef
            },
        )
        assertTrue(
            snapshot.relationships.any {
                it.relationshipClass ==
                    HereticRuntimeRealizationArchitecture.RelationshipClass.EXPLICITLY_NOT_REQUIRED &&
                    it.fromRef == windows.realizationRef &&
                    it.toRef.contains("native-llama-cpp-no-python-environment")
            },
        )
        assertTrue(
            snapshot.relationships.any {
                it.relationshipClass ==
                    HereticRuntimeRealizationArchitecture.RelationshipClass.REQUIRES &&
                    it.fromRef == windows.realizationRef &&
                    it.toRef == windows.qualification.qualificationRef
            },
        )
    }
}

// [VXG RealForever]
