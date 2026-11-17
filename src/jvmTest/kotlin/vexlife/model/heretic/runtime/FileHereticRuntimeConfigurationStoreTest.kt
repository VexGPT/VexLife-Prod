package vexlife.model.heretic.runtime

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import vexlife.home.CanonicalVexLifeHomeFixture
import vexlife.model.heretic.HereticModelCatalog
import vexlife.model.heretic.HereticRuntimeConfiguration

class FileHereticRuntimeConfigurationStoreTest {
    @Test
    fun configurationIsPerModelAndDefaultsDoNotMutateHome() {
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

            assertEquals(
                1,
                store
                    .effective(
                        HereticModelCatalog.Models.Qwen4B,
                    )
                    .promptCacheEntries,
            )
            assertEquals(
                false,
                Files.exists(
                    home.resolve(
                        FileHereticRuntimeConfigurationStore.RelativePath,
                    ),
                ),
            )

            val recommended =
                store.useRecommended(
                    HereticModelCatalog.Models.Qwen4B,
                )
            assertEquals(
                3,
                recommended.promptCacheEntries,
            )
            assertEquals(
                HereticRuntimeConfiguration.ModeRecommended,
                recommended.mode,
            )

            val reread =
                FileHereticRuntimeConfigurationStore(
                    home,
                )
            assertEquals(
                3,
                reread
                    .effective(
                        HereticModelCatalog.Models.Qwen4B,
                    )
                    .promptCacheEntries,
            )
            assertEquals(
                1,
                reread
                    .effective(
                        HereticModelCatalog.Models.Qwen27B,
                    )
                    .promptCacheEntries,
            )

            val explicit =
                reread.setPromptCacheEntries(
                    model = HereticModelCatalog.Models.Qwen4B,
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
        } finally {
            parent
                .toFile()
                .deleteRecursively()
        }
    }
}

// [VXG RealForever]
