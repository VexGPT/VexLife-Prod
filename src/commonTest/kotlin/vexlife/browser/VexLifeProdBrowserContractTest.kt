package vexlife.browser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VexLifeProdBrowserContractTest {
    @Test
    fun admittedVexPagesMapToAnIsolatedFrameOrigin() {
        assertEquals(
            "http://vgong24.github.io.vex.localhost:18211/Vextreme/pages/terrain-map.html?group=direct_contact&page=direct-contact",
            VexLifeProdBrowserContract.frameUrlFor(
                VexLifeProdBrowserContract.InitialPageUrl,
            ),
        )
        assertEquals(
            "vgong24.github.io",
            VexLifeProdBrowserContract.admittedUpstreamHost(
                "vgong24.github.io.vex.localhost",
            ),
        )
    }

    @Test
    fun arbitraryOrLocalPagesAreNotAdmitted() {
        for (
            value in
            listOf(
                "http://127.0.0.1:18210/",
                "https://example.com/",
                "https://localhost/",
                "https://vgong24.github.io.evil.example/",
            )
        ) {
            assertFailsWith<IllegalArgumentException> {
                VexLifeProdBrowserContract.admitPageUrl(
                    value,
                )
            }
        }
    }
}

// [VXG RealForever]
