package vexlife.workspace

import vexlife.browser.VexLifeProdBrowserContract
import vexlife.text.VexLifeText

/**
 * What the workspace's left pane can show, chosen with the header's app selector.
 *
 * Each app is a different way of looking at records with Vex beside them:
 * - Source template: Victor's own Vextreme, VexLife and VexWork, live and read-only, as the worked
 *   example of how records, products and work can be organised.
 * - Archives: the person's own records, kept in their Vex Home.
 * - Vex context: what shaped the last reply and the person's chosen profile for constructing the next
 *   turn, projected from the rightful context owners rather than copied into a second history.
 * - Browser: an ordinary read-only web view Vex can look at alongside the person.
 *
 * Choosing an app is presentation state. It owns no record and changes nothing.
 */
enum class VexLifeWorkspaceApp(
    val key: String,
    val label: VexLifeText,
) {
    SourceTemplate(
        "source-template",
        VexLifeText(
            "workspace.nav.app-source-template",
        ),
    ),
    Archives(
        "archives",
        VexLifeText(
            "workspace.nav.app-archives",
        ),
    ),
    VexContext(
        "vex-context",
        VexLifeText(
            "workspace.nav.app-vex-context",
        ),
    ),
    Browser(
        "browser",
        VexLifeText(
            "workspace.nav.app-browser",
        ),
    ),
}

/**
 * Victor's family pages shown in Source template. They are served live from GitHub Pages so a
 * person follows Victor's actual progress; nothing here is copied into the product.
 */
object VexLifeSourceTemplate {
    /** One family member: [home] opens it, [scope] says which pages belong to it. */
    data class Member(val key: String, val label: String, val home: String, val scope: String) {
        init {
            require(home.startsWith(scope) && scope.startsWith("https://") && scope.endsWith("/")) {
                "Source template member $key is not one https scope"
            }
            require(
                scope.removePrefix("https://").substringBefore('/') in
                    VexLifeProdBrowserContract.AdmittedHosts
            ) {
                "Source template member $key is not on an admitted host"
            }
        }
    }

    val Members: List<Member> =
        listOf(
            Member(
                key = "vextreme",
                label = "Vextreme",
                home = "https://vgong24.github.io/Vextreme/pages/terrain-map.html",
                scope = "https://vgong24.github.io/Vextreme/",
            ),
            Member(
                key = "vexlife",
                label = "VexLife",
                home = "https://vgong24.github.io/VexLife/",
                scope = "https://vgong24.github.io/VexLife/",
            ),
            Member(
                key = "vexwork",
                label = "VexWork",
                home = "https://vexgpt.github.io/VexWork/",
                scope = "https://vexgpt.github.io/VexWork/",
            ),
        )
}

// [VXG RealForever]
