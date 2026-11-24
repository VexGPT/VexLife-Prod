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
                "/usr/bin/codesign --force --sign - --timestamp=none",
            ) &&
                proof.contains(
                    "/usr/bin/codesign --verify --strict --verbose=2",
                ),
        )
        assertTrue(
            proof.contains(
                "HELPER_A",
            ) &&
                proof.contains(
                    "HELPER_B",
                ) &&
                proof.contains(
                    "observePresentAfterRebuildB",
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
    fun macServiceImageBuildCompilesSignsAndManifestsThePartnerHost() {
        val build =
            Files.readString(
                Path.of(
                    "build.gradle.kts",
                ),
            )

        assertTrue(
            build.contains(
                "packaging/macos-partner-host/VexLifePartnerHost.swift",
            ),
        )
        assertTrue(
            build.contains(
                "\"vexlife-partner-host\"",
            ),
        )
        assertTrue(
            build.contains(
                "\"/usr/bin/xcrun\"",
            ) &&
                build.contains(
                    "\"swiftc\"",
                ),
        )
        assertTrue(
            build.contains(
                "\"/usr/bin/codesign\"",
            ) &&
                build.contains(
                    "\"--verify\"",
                ),
        )
        assertTrue(
            build.contains(
                "\"partnerHost\"",
            ) &&
                build.contains(
                    "\"executableSha256\"",
                ),
        )
        assertTrue(
            build.indexOf(
                "val partnerHostSha256",
            ) <
                build.indexOf(
                    "val payloadSha256",
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
