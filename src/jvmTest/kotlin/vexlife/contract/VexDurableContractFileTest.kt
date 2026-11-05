package vexlife.contract

import java.nio.file.Files
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexDurableContractFileTest {
    @Serializable
    private data class Fixture(
        val schemaVersion: String,
        val state: String,
        val effectsPerformed: Boolean = false,
    )

    @Test
    fun absentReadIsExplicitNullAndWriteRoundTripsStrictContract() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-contract-file-",
            )
                .toAbsolutePath()
        val path =
            root.resolve(
                "recovery/receipt.json",
            )

        try {
            assertNull(
                VexDurableContractFile.read<Fixture>(
                    path,
                ),
            )

            val value =
                Fixture(
                    schemaVersion = "vexlife-prod.test/v1",
                    state = "CURRENT",
                )

            VexDurableContractFile.write(
                path,
                value,
            )

            assertEquals(
                value,
                VexDurableContractFile.read<Fixture>(
                    path,
                ),
            )
            assertTrue(
                Files.readString(
                    path,
                )
                    .contains(
                        "\"effectsPerformed\": false",
                    ),
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun unknownDurableFieldFailsClosed() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-contract-file-unknown-",
            )
                .toAbsolutePath()
        val path =
            root.resolve(
                "receipt.json",
            )

        try {
            Files.writeString(
                path,
                """
                {
                  "schemaVersion": "vexlife-prod.test/v1",
                  "state": "CURRENT",
                  "effectsPerformed": false,
                  "invented": true
                }
                """.trimIndent(),
            )

            assertFailsWith<Exception> {
                VexDurableContractFile.read<Fixture>(
                    path,
                )
            }
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun symlinkNeverBecomesDurableReadWriteOrDeleteAuthority() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-contract-file-symlink-",
            )
                .toAbsolutePath()
        val target =
            root.resolve(
                "target.json",
            )
        val link =
            root.resolve(
                "receipt.json",
            )

        try {
            Files.writeString(
                target,
                "{}",
            )
            Files.createSymbolicLink(
                link,
                target,
            )

            assertFailsWith<IllegalArgumentException> {
                VexDurableContractFile.read<Fixture>(
                    link,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                VexDurableContractFile.write(
                    link,
                    Fixture(
                        schemaVersion = "vexlife-prod.test/v1",
                        state = "CURRENT",
                    ),
                )
            }
            assertFailsWith<IllegalArgumentException> {
                VexDurableContractFile.delete(
                    link,
                )
            }

            assertTrue(
                Files.isSymbolicLink(
                    link,
                ),
            )
            assertTrue(
                Files.exists(
                    target,
                ),
            )
        } finally {
            Files.deleteIfExists(
                link,
            )
            root
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun exactDeleteIsExplicitAndIdempotent() {
        val root =
            Files.createTempDirectory(
                "vexlife-prod-contract-file-delete-",
            )
                .toAbsolutePath()
        val path =
            root.resolve(
                "receipt.json",
            )

        try {
            VexDurableContractFile.write(
                path,
                Fixture(
                    schemaVersion = "vexlife-prod.test/v1",
                    state = "CURRENT",
                ),
            )

            assertTrue(
                VexDurableContractFile.delete(
                    path,
                ),
            )
            assertFalse(
                VexDurableContractFile.delete(
                    path,
                ),
            )
        } finally {
            root
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
