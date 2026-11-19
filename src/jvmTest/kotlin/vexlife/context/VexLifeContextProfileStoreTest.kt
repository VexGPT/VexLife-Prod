package vexlife.context

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import vexlife.contract.VexContractJson
import vexlife.home.CanonicalVexLifeHomeFixture

class VexLifeContextProfileStoreTest {
    private val productFoundation =
        VexLifeContextProfile.ProductFoundationIdentity(
            version = "0.5.2",
            bodySha256 = "a".repeat(64),
        )

    @Test
    fun namedProfilesKeepImmutableRevisionsAndRestoreDefaultWithoutDeletingThem() {
        val parent = Files.createTempDirectory("vexlife-context-profile-").toAbsolutePath()
        val home = parent.resolve(".vexlife")

        try {
            CanonicalVexLifeHomeFixture.form(home)
            val store = VexLifeContextProfileStore(home)

            val initial = store.readActive(productFoundation)
            assertTrue(initial.productDefault)
            assertEquals(VexLifeContextProfile.ProductDefaultRef, initial.profileRef)

            val first =
                store.save(
                    profileRef = null,
                    definition =
                        VexLifeContextProfile.Definition(
                            name = "Learning together",
                            foundationMode =
                                VexLifeContextProfile.FoundationMode.EXTEND_PRODUCT_DEFAULT,
                            foundationText = "Explain relationships in words I can follow.",
                        ),
                    productFoundation = productFoundation,
                    formedAt = Instant.parse("2026-11-17T20:00:00Z"),
                )
            assertFalse(first.productDefault)
            assertEquals(1, first.revision)
            assertEquals(first.profileRef, store.readActive(productFoundation).profileRef)

            val firstRevision =
                home.resolve(
                    VexLifeContextProfileStore.RootRelativePath +
                        "/profiles/" +
                        first.profileRef +
                        "/revisions/" +
                        first.fingerprint +
                        ".json",
                )
            assertTrue(Files.isRegularFile(firstRevision))
            val firstBytes = Files.readString(firstRevision)

            val second =
                store.save(
                    profileRef = first.profileRef,
                    definition =
                        first.definition.copy(
                            foundationText =
                                "Explain relationships in words I can follow, then show the exact evidence when I ask.",
                        ),
                    productFoundation = productFoundation,
                    formedAt = Instant.parse("2026-11-17T20:05:00Z"),
                )
            assertEquals(2, second.revision)
            assertNotEquals(first.fingerprint, second.fingerprint)
            assertEquals(firstBytes, Files.readString(firstRevision))
            assertEquals(listOf(second.profileRef), store.listSaved(productFoundation).map { it.profileRef })

            val restored = store.restoreProductDefault(productFoundation)
            assertTrue(restored.productDefault)
            assertEquals(1, store.listSaved(productFoundation).size)

            val selected = store.selectSaved(first.profileRef, productFoundation)
            assertEquals(second.fingerprint, selected.fingerprint)
            assertEquals(second.profileRef, store.readActive(productFoundation).profileRef)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun portableImportKeepsConfigurationOnlyAndPreservesItsFoundationBasis() {
        val parent = Files.createTempDirectory("vexlife-context-profile-import-").toAbsolutePath()
        val home = parent.resolve(".vexlife")

        try {
            CanonicalVexLifeHomeFixture.form(home)
            val store = VexLifeContextProfileStore(home)
            val importedBasis =
                VexLifeContextProfile.ProductFoundationIdentity(
                    version = "0.4.0",
                    bodySha256 = "b".repeat(64),
                )
            val portable =
                VexLifeContextProfile.Portable(
                    name = "Shared learning profile",
                    foundationMode =
                        VexLifeContextProfile.FoundationMode.EXTEND_PRODUCT_DEFAULT,
                    foundationText = "Use this shared foundation extension.",
                    inputTokenBudget = 64_000,
                    basedOnProductFoundation = importedBasis,
                )

            val imported =
                store.importAsNew(
                    portable = portable,
                    productFoundation = productFoundation,
                    formedAt = Instant.parse("2026-11-17T20:10:00Z"),
                )
            assertEquals(importedBasis, imported.basedOnProductFoundation)
            assertTrue(imported.productFoundationChangedSinceSave)

            val exported = store.portable(imported)
            assertEquals(portable, exported)
            assertEquals("Shared learning profile", exported.name)
            assertEquals("Use this shared foundation extension.", exported.foundationText)
            assertEquals(64_000, exported.inputTokenBudget)
            assertEquals(VexLifeContextProfile.Schema, imported.definition.schemaVersion)
        } finally {
            parent.toFile().deleteRecursively()
        }
    }

    @Test
    fun legacyProfileShapeRemainsReadableAndDoesNotGainTheNewDefaultField() {
        val legacy =
            VexLifeContextProfile.Definition(
                schemaVersion =
                    VexLifeContextProfile.LegacySchema,
                name = "Legacy profile",
            )
        val encoded =
            VexContractJson.canonicalEncode(
                legacy,
            )

        assertFalse(
            "inputTokenBudget" in
                encoded,
        )
        assertTrue(
            VexLifeContextProfile.LegacySchema in
                encoded,
        )

        val legacyPortable =
            VexLifeContextProfile.Portable(
                schemaVersion =
                    VexLifeContextProfile.LegacyPortableSchema,
                name = "Legacy portable",
                foundationMode =
                    VexLifeContextProfile.FoundationMode.PRODUCT_DEFAULT,
                foundationText = "",
                basedOnProductFoundation =
                    productFoundation,
            )
        val current =
            legacyPortable.definition()
        assertEquals(
            VexLifeContextProfile.Schema,
            current.schemaVersion,
        )
        assertNull(
            current.inputTokenBudget,
        )
    }

}

// [VXG RealForever]
