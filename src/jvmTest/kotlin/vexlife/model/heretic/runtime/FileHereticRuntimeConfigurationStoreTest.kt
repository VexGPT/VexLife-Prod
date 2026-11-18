package vexlife.model.heretic.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticRuntimeConfiguration
import vexlife.model.heretic.HereticRuntimeRealization

class FileHereticRuntimeConfigurationStoreTest {
    @Test
    fun mlxConfigurationIsPerModelAndRecommendedWritesOneExplicitHomeValue() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-runtime-configuration-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        try {
            CanonicalVexLifeHomeFixture.form(
                home,
            )
            val store =
                FileHereticRuntimeConfigurationStore(
                    home,
                )
            val four =
                HereticModelCatalog.Models.Qwen4B
            val fourMac =
                HereticRuntimeRealization.require(
                    modelRef = four.modelRef,
                    target = HereticRuntimeRealization.MacArm64,
                )
            val twentySeven =
                HereticModelCatalog.Models.Qwen27B
            val twentySevenMac =
                HereticRuntimeRealization.require(
                    modelRef = twentySeven.modelRef,
                    target = HereticRuntimeRealization.MacArm64,
                )

            assertEquals(
                1,
                assertIs<HereticRuntimeConfiguration.Applicability.Applicable>(
                    store.effective(
                        model = four,
                        realization = fourMac,
                    ),
                )
                    .effective
                    .promptCacheEntries,
            )
            assertFalse(
                Files.exists(
                    home.resolve(
                        FileHereticRuntimeConfigurationStore.RelativePath,
                    ),
                ),
            )

            val explicit =
                store.setPromptCacheEntries(
                    model = four,
                    realization = fourMac,
                    promptCacheEntries = 2,
                )
            assertEquals(
                2,
                explicit.promptCacheEntries,
            )
            assertEquals(
                HereticRuntimeConfiguration.ModeExplicit,
                explicit.mode,
            )

            val recommended =
                store.useRecommended(
                    model = four,
                    realization = fourMac,
                )
            assertEquals(
                1,
                recommended.promptCacheEntries,
            )
            assertEquals(
                HereticRuntimeConfiguration.ModeExplicit,
                recommended.mode,
            )

            val reread =
                FileHereticRuntimeConfigurationStore(
                    home,
                )
            assertEquals(
                1,
                reread.requireEffective(
                    model = four,
                    realization = fourMac,
                )
                    .promptCacheEntries,
            )
            assertEquals(
                HereticRuntimeConfiguration.ModeExplicit,
                reread.requireEffective(
                    model = four,
                    realization = fourMac,
                )
                    .mode,
            )
            assertEquals(
                1,
                reread.requireEffective(
                    model = twentySeven,
                    realization = twentySevenMac,
                )
                    .promptCacheEntries,
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }

    @Test
    fun windowsLlamaCppCannotApplyOrWriteTheStoredMlxPromptCacheSetting() {
        val parent =
            Files.createTempDirectory(
                "vexlife-prod-runtime-configuration-windows-",
            )
                .toAbsolutePath()
        val home =
            parent.resolve(
                ".vexlife",
            )
        try {
            CanonicalVexLifeHomeFixture.form(
                home,
            )
            val store =
                FileHereticRuntimeConfigurationStore(
                    home,
                )
            val model =
                HereticModelCatalog.Models.Qwen4B
            val windows =
                HereticRuntimeRealization.WindowsQwen4B

            assertIs<HereticRuntimeConfiguration.Applicability.NotApplicable>(
                store.effective(
                    model = model,
                    realization = windows,
                ),
            )
            assertFalse(
                Files.exists(
                    home.resolve(
                        FileHereticRuntimeConfigurationStore.RelativePath,
                    ),
                ),
            )

            assertFailsWith<IllegalStateException> {
                store.setPromptCacheEntries(
                    model = model,
                    realization = windows,
                    promptCacheEntries = 2,
                )
            }
            assertFalse(
                Files.exists(
                    home.resolve(
                        FileHereticRuntimeConfigurationStore.RelativePath,
                    ),
                ),
                "non-applicable Windows configuration must not create a Home setting",
            )
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
