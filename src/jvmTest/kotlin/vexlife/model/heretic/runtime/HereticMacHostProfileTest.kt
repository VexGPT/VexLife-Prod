package vexlife.model.heretic.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import vexlife.model.heretic.HereticProductIdentity

class HereticMacHostProfileTest {
    @Test
    fun exactCurrentVexLifeEligibilityMeaningIsReaddressedToProd() {
        val result =
            HereticMacHostProfile.evaluate(
                HereticMacHostProfile.Observation(
                    platform = "darwin",
                    architecture = "arm64",
                    chipModel = "Apple M4 Pro",
                    totalMemoryBytes =
                        12L *
                            1024L *
                            1024L *
                            1024L,
                ),
            )

        assertEquals(
            HereticProductIdentity.HostExecutionProfileRef,
            result.profileRef,
        )
        assertEquals(
            "profile.repository.vexlife-prod.macos-v1",
            result.profileRef,
        )
        assertEquals(
            HereticMacHostProfile.State.Eligible,
            result.state,
        )
        assertEquals(
            null,
            result.reason,
        )
        assertFalse(
            result.effectsPerformed,
        )
    }

    @Test
    fun eachMismatchedHostPredicateFailsClosedWithoutEffects() {
        val eligible =
            HereticMacHostProfile.Observation(
                platform = "darwin",
                architecture = "arm64",
                chipModel = "Apple M4 Pro",
                totalMemoryBytes =
                    HereticMacHostProfile.MinimumMemoryBytes,
            )

        val cases =
            listOf(
                eligible.copy(
                    platform = "linux",
                ) to
                    "PLATFORM_UNSUPPORTED",
                eligible.copy(
                    architecture = "x86_64",
                ) to
                    "ARCHITECTURE_UNSUPPORTED",
                eligible.copy(
                    chipModel = "Apple M4 Max",
                ) to
                    "CHIP_MODEL_UNSUPPORTED",
                eligible.copy(
                    totalMemoryBytes = null,
                ) to
                    "SYSTEM_MEMORY_UNKNOWN",
                eligible.copy(
                    totalMemoryBytes =
                        HereticMacHostProfile.MinimumMemoryBytes -
                            1,
                ) to
                    "INSUFFICIENT_SYSTEM_MEMORY",
            )

        for (
            (
                observation,
                reason,
            ) in
            cases
        ) {
            val result =
                HereticMacHostProfile.evaluate(
                    observation,
                )

            assertEquals(
                HereticMacHostProfile.State.Unsupported,
                result.state,
            )
            assertEquals(
                reason,
                result.reason,
            )
            assertFalse(
                result.effectsPerformed,
            )
        }
    }
}

// [VXG RealForever]
