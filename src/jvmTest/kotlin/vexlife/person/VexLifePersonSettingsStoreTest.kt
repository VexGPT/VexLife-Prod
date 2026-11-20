package vexlife.person

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VexLifePersonSettingsStoreTest {
    @Test
    fun aV1HomeReadsAsFollowingItsReplyLanguageAndIsRewrittenAsV2() {
        val home = Files.createTempDirectory("vexlife-person-settings-")
        try {
            val file = home.resolve(VexLifePersonSettingsStore.RelativePath)
            Files.createDirectories(file.parent)
            Files.writeString(file, """{"schemaVersion":"${VexLifePersonSettings.SchemaV1}","replyLanguage":"zh"}""")
            val store = VexLifePersonSettingsStore(home)
            val read = store.read()
            assertEquals(VexLifePersonSettings.Schema, read.schemaVersion)
            assertEquals(null, read.screenLanguage)
            assertEquals(VexLifePersonSettings.TraditionalChinese, VexLifePersonSettings.screenLanguageFor(read), "legacy zh stays Traditional")
            store.write(read.copy(screenLanguage = "ja"))
            assertTrue(Files.readString(file).contains(VexLifePersonSettings.Schema))
            assertEquals("ja", store.read().screenLanguage)
            assertEquals("zh", store.read().replyLanguage)
        } finally {
            home.toFile().deleteRecursively()
        }
    }
}

// [VXG RealForever]
