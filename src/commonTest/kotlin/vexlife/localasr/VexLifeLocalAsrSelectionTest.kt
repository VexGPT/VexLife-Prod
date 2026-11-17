package vexlife.localasr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class VexLifeLocalAsrSelectionTest {
    @Test
    fun onlyQualifiedProviderModelCoordinatesAreSelectable() {
        val selected =
            VexLifeLocalAsrSelection.Default
        val option =
            VexLifeLocalAsrSelection.option(
                selected,
            )

        assertEquals(
            VexLifeLocalAsrSelection.WhisperCppProviderRef,
            selected.providerRef,
        )
        assertEquals(
            VexLifeLocalAsrSelection.WhisperBaseEnglishModelRef,
            selected.modelRef,
        )
        assertEquals(
            listOf(
                "en",
            ),
            option.languageCodes,
        )
        assertEquals(
            "QUALIFIED_A002_A003",
            option.standing,
        )
        assertNotNull(
            VexLifeLocalAsrSelection.option(
                selected.providerRef,
                selected.modelRef,
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            VexLifeLocalAsrSelection.Value(
                schemaVersion =
                    VexLifeLocalAsrSelection.Schema,
                providerRef =
                    "provider.local-asr.unqualified",
                modelRef =
                    "model.local-asr.unqualified",
            )
        }
    }
}

// [VXG RealForever]
