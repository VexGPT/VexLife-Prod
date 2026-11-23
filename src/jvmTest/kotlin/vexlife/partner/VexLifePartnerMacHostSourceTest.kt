package vexlife.partner

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifePartnerMacHostSourceTest {
    private val path =
        Path.of(
            "packaging/macos-partner-host/VexLifePartnerHost.swift",
        )

    private fun source(): String {
        assertTrue(
            Files.isRegularFile(
                path,
            ),
        )
        return Files.readString(
            path,
        )
    }

    @Test
    fun macHostUsesSecurityFrameworkGenericPasswordLifecycle() {
        val source =
            source()

        listOf(
            "import Security",
            "kSecClassGenericPassword",
            "kSecAttrService",
            "kSecAttrAccount",
            "kSecValueData",
            "SecItemAdd",
            "SecItemCopyMatching",
            "SecItemDelete",
        ).forEach {
            assertTrue(
                source.contains(
                    it,
                ),
            )
        }
        assertFalse(
            source.contains(
                "add-generic-password",
            ),
        )
    }

    @Test
    fun macHostRefusesRedirectsThroughUrlSessionDelegate() {
        val source =
            source()

        assertTrue(
            source.contains(
                "willPerformHTTPRedirection",
            ),
        )
        assertTrue(
            source.contains(
                "completionHandler(nil)",
            ),
        )
        assertTrue(
            source.contains(
                "REDIRECT_REJECTED",
            ),
        )
    }

    @Test
    fun macHostKeepsExactBuiltInProviderAllowlist() {
        val source =
            source()

        listOf(
            "https://api.openai.com/v1/responses",
            "https://api.anthropic.com/v1/messages",
            "https://api.moonshot.ai/v1/chat/completions",
            "2026-10-21",
        ).forEach {
            assertTrue(
                source.contains(
                    it,
                ),
            )
        }
        assertTrue(
            source.contains(
                "anthropic-workspace-id",
            ),
        )
    }

    @Test
    fun macProofCompilesExactSwiftSourceBeforeSyntheticKeychainLifecycle() {
        val proof =
            Files.readString(
                Path.of(
                    "tools/macos-partner-host/PROVE-CREDENTIAL-HOST.command",
                ),
            )

        assertTrue(
            proof.contains(
                "xcrun --sdk macosx swiftc",
            ),
        )
        assertTrue(
            proof.contains(
                "vexlife-partner-proof-",
            ),
        )
        assertTrue(
            proof.contains(
                "CREDENTIAL_NOT_PRESENT",
            ),
        )
        assertTrue(
            proof.contains(
                "trap cleanup EXIT",
            ),
        )
        assertFalse(
            proof.contains(
                "OPENAI_API_KEY",
            ),
        )
        assertFalse(
            proof.contains(
                "ANTHROPIC_API_KEY",
            ),
        )
    }

    @Test
    fun macHostSourceHasNoApiKeyEnvironmentOrTempSecretPath() {
        val source =
            source()

        listOf(
            "OPENAI_API_KEY",
            "ANTHROPIC_API_KEY",
            "KIMI_API_KEY",
            "MOONSHOT_API_KEY",
            "Process(",
            "temporaryDirectory",
        ).forEach {
            assertFalse(
                source.contains(
                    it,
                ),
            )
        }

        assertTrue(
            source.contains(
                "FileHandle.standardInput",
            ),
        )
        assertTrue(
            source.contains(
                "resetBytes",
            ),
        )
    }
}

// [VXG RealForever]
