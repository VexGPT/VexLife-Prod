package vexlife.partner

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VexLifePartnerWindowsHostSourceTest {
    private val path =
        Path.of(
            "packaging/windows-partner-host/vexlife-partner-host.ps1",
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
    fun windowsHostUsesCredentialManagerGenericCredentialLifecycle() {
        val source =
            source()

        listOf(
            "CredWriteW",
            "CredReadW",
            "CredDeleteW",
            "CredFree",
            "CredGetSessionTypes",
            "CRED_TYPE_GENERIC",
            "CRED_PERSIST_LOCAL_MACHINE",
            "CredentialBlobSize",
        ).forEach {
            assertTrue(
                source.contains(
                    it,
                ),
                "Windows Partner host source lacks $it",
            )
        }
    }

    @Test
    fun windowsHostDeniesRedirectsAndKeepsProviderAllowlistExact() {
        val source =
            source()

        assertTrue(
            source.contains(
                "AllowAutoRedirect = false",
            ),
        )
        listOf(
            "https://api.openai.com/v1/responses",
            "https://api.anthropic.com/v1/messages",
            "https://api.moonshot.ai/v1/chat/completions",
        ).forEach {
            assertTrue(
                source.contains(
                    it,
                ),
            )
        }
        assertTrue(
            source.contains(
                "REDIRECT_REJECTED",
            ),
        )
    }

    @Test
    fun windowsHostSourceDoesNotDeclareSecretCommandLineOrEnvironmentCarriage() {
        val source =
            source()

        listOf(
            "ANTHROPIC_API_KEY",
            "OPENAI_API_KEY",
            "MOONSHOT_API_KEY",
            "KIMI_API_KEY",
            "Start-Transcript",
            "Write-Host",
            "SetEnvironmentVariable",
        ).forEach {
            assertFalse(
                source.contains(
                    it,
                ),
                "Windows Partner host contains forbidden secret/log carriage token: $it",
            )
        }
        assertTrue(
            source.contains(
                "[Console]::OpenStandardInput()",
            ),
        )
        assertTrue(
            source.contains(
                "ReadPayload",
            ),
        )
        assertTrue(
            source.contains(
                "[Array]::Clear",
            ),
        )
    }

    @Test
    fun windowsHostPreservesProviderHttpErrorsForP1AndA5() {
        val source =
            source()

        assertTrue(
            source.contains(
                "RESPONSE_START",
            ),
        )
        assertTrue(
            source.contains(
                "BODY_CHUNK",
            ),
        )
        assertTrue(
            source.contains(
                "COMPLETED",
            ),
        )
        assertTrue(
            source.contains(
                "TRANSPORT_FAILURE",
            ),
        )
        assertTrue(
            source.contains(
                "status >= 300 && status <= 399",
            ),
        )
    }
}

// [VXG RealForever]
