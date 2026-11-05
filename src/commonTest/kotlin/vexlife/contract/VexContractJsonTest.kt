package vexlife.contract

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VexContractJsonTest {
    @Serializable
    private data class Fixture(
        val schemaVersion: String,
        val state: String,
        val effectsPerformed: Boolean = false,
        val optionalRef: String? = null,
    )

    @Test
    fun defaultsAndNullsStayVisibleInDurableJson() {
        val encoded =
            VexContractJson.encode(
                Fixture(
                    schemaVersion = "vexlife-prod.test/v1",
                    state = "CURRENT",
                ),
            )

        assertTrue(
            encoded.contains(
                "\"effectsPerformed\": false",
            ),
        )
        assertTrue(
            encoded.contains(
                "\"optionalRef\": null",
            ),
        )
    }

    @Test
    fun strictRoundTripPreservesExactContractFields() {
        val input =
            Fixture(
                schemaVersion = "vexlife-prod.test/v1",
                state = "CURRENT",
                effectsPerformed = false,
                optionalRef = "source.test.001",
            )

        assertEquals(
            input,
            VexContractJson.decode<Fixture>(
                VexContractJson.encode(
                    input,
                ),
            ),
        )
    }

    @Test
    fun unknownFieldsFailClosedInsteadOfBeingSilentlyDiscarded() {
        val text =
            """
            {
              "schemaVersion": "vexlife-prod.test/v1",
              "state": "CURRENT",
              "effectsPerformed": false,
              "optionalRef": null,
              "inventedField": "must-not-disappear"
            }
            """.trimIndent()

        assertFailsWith<SerializationException> {
            VexContractJson.decode<Fixture>(
                text,
            )
        }
    }
}

// [VXG RealForever]
