package vexlife.partner

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VexLifePartnerConnectionSettingsStoreTest {
    @Test
    fun absentFileMeansNoConfiguredPartnerConnections() {
        val home =
            Files.createTempDirectory(
                "vexlife-partner-settings-empty-",
            )
        try {
            val store =
                VexLifePartnerConnectionSettingsStore(
                    home,
                )
            assertEquals(
                VexLifePartnerConnectionSettings.Empty,
                store.read(),
            )
            assertNull(
                store.readOrNull(),
            )
        } finally {
            home.toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun roundTripPersistsOnlyOpaqueRefsAndNonSecretAccountScope() {
        val home =
            Files.createTempDirectory(
                "vexlife-partner-settings-roundtrip-",
            )
        try {
            val store =
                VexLifePartnerConnectionSettingsStore(
                    home,
                )
            val value =
                VexLifePartnerConnectionSettings.form(
                    listOf(
                        VexLifePartnerConnectionSettings.Entry(
                            providerRef =
                                VexLifePartnerProviderCatalog.OpenAiRef,
                            credentialRef =
                                "credential.vexlife.partner.openai-fixture-0001",
                            accountScope =
                                VexLifePartnerAccountScope.Selection(
                                    providerRef =
                                        VexLifePartnerProviderCatalog.OpenAiRef,
                                ),
                        ),
                        VexLifePartnerConnectionSettings.Entry(
                            providerRef =
                                VexLifePartnerProviderCatalog.AnthropicRef,
                            credentialRef =
                                "credential.vexlife.partner.anthropic-fixture-0001",
                            accountScope =
                                VexLifePartnerAccountScope.Selection(
                                    providerRef =
                                        VexLifePartnerProviderCatalog.AnthropicRef,
                                    workspaceId =
                                        "wrkspc_fixture_0001",
                                ),
                        ),
                    ),
                )

            store.write(
                value,
            )

            assertEquals(
                value,
                store.read(),
            )

            val file =
                home.resolve(
                    VexLifePartnerConnectionSettingsStore.RelativePath,
                )
            val raw =
                Files.readString(
                    file,
                )
            assertTrue(
                raw.contains(
                    "credential.vexlife.partner.openai-fixture-0001",
                ),
            )
            assertTrue(
                raw.contains(
                    "wrkspc_fixture_0001",
                ),
            )
            assertFalse(
                raw.contains(
                    "synthetic-provider-secret",
                ),
            )
            assertFalse(
                raw.contains(
                    "\"apiKey\"",
                ),
            )
            assertFalse(
                raw.contains(
                    "\"secret\"",
                ),
            )
        } finally {
            home.toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun strictDurableContractRejectsInjectedSecretField() {
        val home =
            Files.createTempDirectory(
                "vexlife-partner-settings-secret-field-",
            )
        try {
            val file =
                home.resolve(
                    VexLifePartnerConnectionSettingsStore.RelativePath,
                )
            Files.createDirectories(
                file.parent,
            )
            val injected =
                "{" +
                    "\"schemaVersion\":\"" +
                    VexLifePartnerConnectionSettings.Schema +
                    "\"," +
                    "\"entries\":[{" +
                    "\"providerRef\":\"" +
                    VexLifePartnerProviderCatalog.OpenAiRef +
                    "\"," +
                    "\"credentialRef\":\"credential.vexlife.partner.openai-fixture-0001\"," +
                    "\"accountScope\":{" +
                    "\"providerRef\":\"" +
                    VexLifePartnerProviderCatalog.OpenAiRef +
                    "\"," +
                    "\"workspaceId\":null}," +
                    "\"apiKey\":\"synthetic-provider-secret\"" +
                    "}]}"

            Files.writeString(
                file,
                injected,
            )

            assertFails {
                VexLifePartnerConnectionSettingsStore(
                    home,
                )
                    .read()
            }
        } finally {
            home.toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun writeUsesTheCanonicalPrivateSettingsPath() {
        val home =
            Files.createTempDirectory(
                "vexlife-partner-settings-path-",
            )
        try {
            val store =
                VexLifePartnerConnectionSettingsStore(
                    home,
                )
            store.write(
                VexLifePartnerConnectionSettings.Empty,
            )

            assertTrue(
                Files.isRegularFile(
                    home.resolve(
                        "runtime/vexlife-prod/settings/partner-connections.json",
                    ),
                ),
            )
        } finally {
            home.toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
