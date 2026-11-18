package vexlife.workspace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VexLifeWorkspaceAppsTest {
    @Test
    fun sourceTemplateIsVictorsLiveFamilyOnAdmittedHosts() {
        assertEquals(listOf("vextreme", "vexlife", "vexwork"), VexLifeSourceTemplate.Members.map { it.key })
        assertEquals(
            listOf("source-template", "archives", "browser"),
            VexLifeWorkspaceApp.entries.map { it.key },
        )
        assertFailsWith<IllegalArgumentException> {
            VexLifeSourceTemplate.Member("x", "X", "https://example.com/a", "https://example.com/")
        }
    }
}

// [VXG RealForever]
