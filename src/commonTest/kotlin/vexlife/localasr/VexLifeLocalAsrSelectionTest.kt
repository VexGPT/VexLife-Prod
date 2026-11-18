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
        val multilingual =
            VexLifeLocalAsrSelection.WhisperBaseMultilingual
        assertEquals(
            VexLifeLocalAsrSelection.WhisperBaseMultilingualModelRef,
            multilingual.modelRef,
        )
        assertEquals(
            listOf(
                "en",
                "ja",
                "zh",
                "ko",
                "fr",
                "de",
                "es",
                "pt",
                "ar",
                "hi",
            ),
            multilingual.languageCodes,
        )
        assertEquals(
            147_951_465L,
            multilingual.modelBytes,
        )
        assertEquals(
            2,
            VexLifeLocalAsrSelection.Options.size,
        )
        assertNotNull(
            VexLifeLocalAsrSelection.option(
                multilingual.providerRef,
                multilingual.modelRef,
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
