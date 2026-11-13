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
    fun humanNavigationAcceptsOrdinaryWebWithoutTurningItIntoContextAdmission() {
        assertEquals(
            listOf(
                "http:",
                "https:",
            ),
            VexLifeProdBrowserContract.HumanPageSchemes,
        )
        assertEquals(
            ".localhost",
            VexLifeProdBrowserContract.RejectedHumanPageHostSuffix,
        )
        assertEquals(
            "https://example.com/products?q=vex",
            VexLifeProdBrowserContract.admitHumanPageUrl(
                "https://example.com/products?q=vex",
            ),
        )
        assertEquals(
            "http://example.com:8080/",
            VexLifeProdBrowserContract.admitHumanPageUrl(
                "http://example.com:8080/",
            ),
        )

        for (
            value in
            listOf(
                "http://127.0.0.1:18210/",
                "https://localhost/",
                "http://anything.localhost:9000/",
                "http://0.0.0.0:18210/",
                "file:///tmp/private.txt",
            )
        ) {
            assertFailsWith<IllegalArgumentException> {
                VexLifeProdBrowserContract.admitHumanPageUrl(
                    value,
                )
            }
        }
    }

    @Test
    fun vexContextAdmissionRemainsNarrowerThanHumanNavigation() {
        assertEquals(
            "https://vgong24.github.io/Vextreme/pages/terrain-map.html",
            VexLifeProdBrowserContract.admitContextPageUrl(
                "https://vgong24.github.io/Vextreme/pages/terrain-map.html",
            ),
        )

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
                VexLifeProdBrowserContract.admitContextPageUrl(
                    value,
                )
            }
        }
    }
}

// [VXG RealForever]
