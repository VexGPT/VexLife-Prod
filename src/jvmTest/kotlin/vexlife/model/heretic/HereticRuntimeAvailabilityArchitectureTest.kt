package vexlife.model.heretic

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson

class HereticRuntimeAvailabilityArchitectureTest {
    @Test
    fun checkedInProjectionIsTheExactCurrentSourceOwnedArchitectureSnapshot() {
        val expected =
            VexContractJson.canonicalEncode(
                HereticRuntimeAvailabilityArchitecture.current(),
            )
        val checkedIn =
            Files.readString(
                Path.of(
                    System.getProperty(
                        "user.dir",
                    ),
                    "architecture",
                    "heretic-runtime-availability.json",
                ),
                StandardCharsets.UTF_8,
            )
                .trim()

        assertEquals(
            expected,
            checkedIn,
        )

        val decoded =
            VexContractJson.decode<HereticRuntimeAvailabilityArchitecture.Snapshot>(
                checkedIn,
            )

        assertEquals(
            HereticRuntimeAvailabilityArchitecture.ProjectionRef,
            decoded.projectionRef,
        )
        assertEquals(
            HereticRuntimeAvailabilityArchitecture.FamilyRef,
            decoded.familyRef,
        )
        assertFalse(
            decoded.externalEffectsPerformed,
        )
        assertEquals(
            12,
            decoded.nodes.size,
        )
        assertEquals(
            15,
            decoded.relationships.size,
        )
    }

    @Test
    fun projectionPreservesTheBranchThatMakesRuntimeAvailabilitySemanticallyMeaningful() {
        val projection =
            HereticRuntimeAvailabilityArchitecture.current()

        assertTrue(
            projection.relationships.any {
                it.relationshipClass ==
                    HereticRuntimeAvailabilityArchitecture.RelationshipClass.ADMITS_WITHOUT_PRODUCT_EFFECT
            },
        )
        assertTrue(
            projection.relationships.any {
                it.relationshipClass ==
                    HereticRuntimeAvailabilityArchitecture.RelationshipClass.REQUESTS_IF_ACQUISITION_ALLOWED
            },
        )
        assertTrue(
            projection.relationships.any {
                it.relationshipClass ==
                    HereticRuntimeAvailabilityArchitecture.RelationshipClass.REQUESTS_IF_OBSERVATION_ONLY
            },
        )
        assertTrue(
            projection.relationships.any {
                it.relationshipClass ==
                    HereticRuntimeAvailabilityArchitecture.RelationshipClass.DELEGATES_TO_OWNER
            },
        )
        assertTrue(
            projection.relationships.all {
                it.sourceRefs.isNotEmpty()
            },
        )
    }
}

// [VXG RealForever]
