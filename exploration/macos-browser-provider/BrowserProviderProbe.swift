import AppKit
import Foundation
import WebKit

private enum ProbeEvent {
    static let ready = "VEXLIFE_BROWSER_PROBE_READY"
    static let navigation = "VEXLIFE_BROWSER_PROBE_NAVIGATION"
    static let failure = "VEXLIFE_BROWSER_PROBE_FAILURE"
    static let vextremeChrome = "VEXLIFE_BROWSER_PROBE_VEXTREME_CHROME"
    static let terminated = "VEXLIFE_BROWSER_PROBE_CONTENT_PROCESS_TERMINATED"
}

@MainActor
private final class BrowserProbeController: NSObject, NSApplicationDelegate, WKNavigationDelegate, WKUIDelegate {
    private static let defaultPage =
        "https://vgong24.github.io/Vextreme/pages/terrain-map.html?group=direct_contact&page=direct-contact"

    private let window: NSWindow
    private let webView: WKWebView
    private let addressField = NSTextField()
    private let backButton = NSButton(title: "Back", target: nil, action: nil)
    private let forwardButton = NSButton(title: "Forward", target: nil, action: nil)
    private let reloadButton = NSButton(title: "Reload", target: nil, action: nil)

    override init() {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()

        webView = WKWebView(
            frame: .zero,
            configuration: configuration
        )
        window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 1220, height: 820),
            styleMask: [.titled, .closable, .miniaturizable, .resizable],
            backing: .buffered,
            defer: false
        )

        super.init()

        webView.navigationDelegate = self
        webView.uiDelegate = self
        configureWindow()
    }

    func applicationDidFinishLaunching(
        _ notification: Notification
    ) {
        NSApp.setActivationPolicy(.regular)
        window.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)

        let raw =
            ProcessInfo.processInfo.arguments
                .dropFirst()
                .first
                ?? Self.defaultPage

        guard let url = admittedPublicWebURL(raw) else {
            emit(
                ProbeEvent.failure,
                "INITIAL_URL",
                "Only non-local HTTP(S) URLs are admitted by this proof."
            )
            NSApp.terminate(nil)
            return
        }

        addressField.stringValue = url.absoluteString
        webView.load(
            URLRequest(
                url: url
            )
        )
        emit(
            ProbeEvent.ready,
            url.absoluteString
        )
    }

    func applicationShouldTerminateAfterLastWindowClosed(
        _ sender: NSApplication
    ) -> Bool {
        true
    }

    private func configureWindow() {
        window.title = "VexLife Browser Provider Probe"
        window.center()

        let content = NSView()
        content.translatesAutoresizingMaskIntoConstraints = false
        window.contentView = content

        let controls = NSStackView()
        controls.orientation = .horizontal
        controls.alignment = .centerY
        controls.spacing = 8
        controls.translatesAutoresizingMaskIntoConstraints = false

        backButton.target = self
        backButton.action = #selector(goBack)
        forwardButton.target = self
        forwardButton.action = #selector(goForward)
        reloadButton.target = self
        reloadButton.action = #selector(reload)

        addressField.placeholderString = "Type a public web address"
        addressField.target = self
        addressField.action = #selector(navigateFromAddress)
        addressField.translatesAutoresizingMaskIntoConstraints = false
        addressField.setContentHuggingPriority(.defaultLow, for: .horizontal)

        controls.addArrangedSubview(backButton)
        controls.addArrangedSubview(forwardButton)
        controls.addArrangedSubview(reloadButton)
        controls.addArrangedSubview(addressField)

        webView.translatesAutoresizingMaskIntoConstraints = false

        content.addSubview(controls)
        content.addSubview(webView)

        NSLayoutConstraint.activate([
            controls.leadingAnchor.constraint(equalTo: content.leadingAnchor, constant: 12),
            controls.trailingAnchor.constraint(equalTo: content.trailingAnchor, constant: -12),
            controls.topAnchor.constraint(equalTo: content.topAnchor, constant: 12),

            addressField.widthAnchor.constraint(greaterThanOrEqualToConstant: 420),

            webView.leadingAnchor.constraint(equalTo: content.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: content.trailingAnchor),
            webView.topAnchor.constraint(equalTo: controls.bottomAnchor, constant: 10),
            webView.bottomAnchor.constraint(equalTo: content.bottomAnchor)
        ])

        updateNavigationState()
    }

    @objc
    private func navigateFromAddress() {
        guard let url = admittedPublicWebURL(addressField.stringValue) else {
            emit(
                ProbeEvent.failure,
                "ADDRESS",
                "Only non-local HTTP(S) URLs are admitted by this proof."
            )
            NSSound.beep()
            return
        }

        addressField.stringValue = url.absoluteString
        webView.load(
            URLRequest(
                url: url
            )
        )
    }

    @objc
    private func goBack() {
        guard webView.canGoBack else {
            return
        }
        webView.goBack()
    }

    @objc
    private func goForward() {
        guard webView.canGoForward else {
            return
        }
        webView.goForward()
    }

    @objc
    private func reload() {
        if webView.url != nil {
            webView.reload()
        }
    }

    func webView(
        _ webView: WKWebView,
        didCommit navigation: WKNavigation!
    ) {
        publishNavigation(
            phase: "COMMIT"
        )
    }

    func webView(
        _ webView: WKWebView,
        didFinish navigation: WKNavigation!
    ) {
        publishNavigation(
            phase: "FINISH"
        )
        publishVextremeChromeIfApplicable()
    }

    func webView(
        _ webView: WKWebView,
        didFail navigation: WKNavigation!,
        withError error: Error
    ) {
        publishFailure(
            phase: "NAVIGATION",
            error: error
        )
    }

    func webView(
        _ webView: WKWebView,
        didFailProvisionalNavigation navigation: WKNavigation!,
        withError error: Error
    ) {
        publishFailure(
            phase: "PROVISIONAL",
            error: error
        )
    }

    func webViewWebContentProcessDidTerminate(
        _ webView: WKWebView
    ) {
        emit(
            ProbeEvent.terminated,
            webView.url?.absoluteString ?? "NONE"
        )
        updateNavigationState()
    }

    func webView(
        _ webView: WKWebView,
        createWebViewWith configuration: WKWebViewConfiguration,
        for navigationAction: WKNavigationAction,
        windowFeatures: WKWindowFeatures
    ) -> WKWebView? {
        guard
            navigationAction.targetFrame == nil,
            let url = navigationAction.request.url,
            admittedPublicWebURL(url.absoluteString) != nil
        else {
            return nil
        }

        webView.load(
            navigationAction.request
        )
        return nil
    }

    private func publishNavigation(
        phase: String
    ) {
        updateNavigationState()

        emit(
            ProbeEvent.navigation,
            phase,
            webView.url?.absoluteString ?? "NONE",
            "canGoBack=\(webView.canGoBack)",
            "canGoForward=\(webView.canGoForward)"
        )
    }

    private func publishVextremeChromeIfApplicable() {
        guard
            let current = webView.url,
            current.host?.lowercased() == "vgong24.github.io",
            current.path == "/Vextreme/pages/terrain-map.html"
        else {
            return
        }

        let script =
            """
            (() => {
              const nav = document.querySelector('#vex-site-nav');
              const links = nav
                ? Array.from(nav.querySelectorAll('a')).map((link) => ({
                    text: String(link.textContent || '').trim(),
                    href: String(link.href || '')
                  }))
                : [];
              return JSON.stringify({
                present: Boolean(nav),
                links
              });
            })()
            """

        webView.evaluateJavaScript(
            script
        ) {
            [weak self] value,
            error in

            guard let self else {
                return
            }

            if let error {
                self.emit(
                    ProbeEvent.failure,
                    "VEXTREME_CHROME_OBSERVATION",
                    error.localizedDescription
                )
                return
            }

            self.emit(
                ProbeEvent.vextremeChrome,
                value as? String ?? "{\"present\":false,\"links\":[]}"
            )
        }
    }

    private func publishFailure(
        phase: String,
        error: Error
    ) {
        updateNavigationState()
        emit(
            ProbeEvent.failure,
            phase,
            error.localizedDescription
        )
    }

    private func updateNavigationState() {
        backButton.isEnabled = webView.canGoBack
        forwardButton.isEnabled = webView.canGoForward

        if let url = webView.url {
            addressField.stringValue = url.absoluteString
        }
    }

    private func admittedPublicWebURL(
        _ raw: String
    ) -> URL? {
        let trimmed =
            raw.trimmingCharacters(
                in: .whitespacesAndNewlines
            )
        guard !trimmed.isEmpty else {
            return nil
        }

        let candidate =
            trimmed.range(
                of: "^[A-Za-z][A-Za-z0-9+.-]*:",
                options: .regularExpression
            ) == nil
                ? "https://\(trimmed)"
                : trimmed

        guard
            let url = URL(
                string: candidate
            ),
            let scheme =
                url.scheme?
                    .lowercased(),
            scheme == "http" ||
                scheme == "https",
            url.user == nil,
            url.password == nil,
            let host =
                url.host?
                    .lowercased(),
            !host.isEmpty,
            host != "localhost",
            !host.hasSuffix(".localhost"),
            host != "127.0.0.1",
            host != "0.0.0.0",
            host != "::1"
        else {
            return nil
        }

        return url
    }

    private func emit(
        _ fields: String...
    ) {
        let line =
            fields
                .map {
                    field in
                    field
                        .replacingOccurrences(
                            of: "\t",
                            with: " "
                        )
                        .replacingOccurrences(
                            of: "\n",
                            with: " "
                        )
                        .replacingOccurrences(
                            of: "\r",
                            with: " "
                        )
                }
                .joined(
                    separator: "\t"
                ) +
            "\n"

        FileHandle.standardOutput.write(
            Data(
                line.utf8
            )
        )
    }
}

@main
private struct BrowserProviderProbeApp {
    @MainActor
    static func main() {
        let app = NSApplication.shared
        let controller = BrowserProbeController()
        app.delegate = controller
        app.run()
    }
}

// [VXG RealForever]
