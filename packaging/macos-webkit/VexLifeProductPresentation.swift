import AppKit
import Darwin
import WebKit

private enum ExitCode {
    static let closePresentation: Int32 = 0
    static let quitProduct: Int32 = 42
    static let failure: Int32 = 70
}

private struct Arguments {
    let origin: URL
    let quitLabel: String
    let iconPath: String

    static func parse(_ argv: [String]) throws -> Arguments {
        guard argv.count == 6 else {
            throw PresentationError.invalidArguments
        }

        var values: [String: String] = [:]
        var index = 0
        while index < argv.count {
            let key = argv[index]
            let value = argv[index + 1]
            guard ["--origin", "--quit-label", "--icon"].contains(key),
                  values[key] == nil else {
                throw PresentationError.invalidArguments
            }
            values[key] = value
            index += 2
        }

        guard let originText = values["--origin"],
              let origin = URL(string: originText),
              origin.scheme == "http",
              origin.host == "127.0.0.1",
              origin.port == 18210,
              origin.path.isEmpty || origin.path == "/",
              origin.query == nil,
              origin.fragment == nil,
              let quitLabel = values["--quit-label"],
              !quitLabel.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let iconPath = values["--icon"],
              !iconPath.isEmpty else {
            throw PresentationError.invalidArguments
        }

        return Arguments(
            origin: URL(string: "http://127.0.0.1:18210/")!,
            quitLabel: quitLabel,
            iconPath: iconPath
        )
    }
}

private enum PresentationError: Error {
    case invalidArguments
    case iconUnavailable
}

@MainActor
private final class ProductPresentation:
    NSObject,
    NSApplicationDelegate,
    NSWindowDelegate,
    WKNavigationDelegate,
    WKUIDelegate,
    WKDownloadDelegate
{
    private let arguments: Arguments
    private var window: NSWindow?
    private var webView: WKWebView?

    init(arguments: Arguments) {
        self.arguments = arguments
    }

    func applicationDidFinishLaunching(_ notification: Notification) {
        guard let icon = NSImage(contentsOfFile: arguments.iconPath) else {
            Darwin.exit(ExitCode.failure)
        }
        NSApp.applicationIconImage = icon

        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()

        let webView = WKWebView(
            frame: NSRect(x: 0, y: 0, width: 1180, height: 760),
            configuration: configuration
        )
        webView.navigationDelegate = self
        webView.uiDelegate = self

        let style: NSWindow.StyleMask = [
            .titled,
            .closable,
            .miniaturizable,
            .resizable,
        ]
        let window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 1180, height: 760),
            styleMask: style,
            backing: .buffered,
            defer: false
        )
        window.title = "VexLife"
        window.minSize = NSSize(width: 760, height: 520)
        window.contentView = webView
        window.delegate = self
        window.center()

        self.webView = webView
        self.window = window

        installApplicationMenu()
        window.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)

        webView.load(URLRequest(url: arguments.origin))
    }

    private func installApplicationMenu() {
        let mainMenu = NSMenu()
        let applicationItem = NSMenuItem()
        let applicationMenu = NSMenu()
        let quitItem = NSMenuItem(
            title: arguments.quitLabel,
            action: #selector(requestProductQuit),
            keyEquivalent: "q"
        )
        quitItem.keyEquivalentModifierMask = [.command]
        quitItem.target = self
        applicationMenu.addItem(quitItem)
        applicationItem.submenu = applicationMenu
        mainMenu.addItem(applicationItem)
        NSApp.mainMenu = mainMenu
    }

    @objc
    private func requestProductQuit() {
        Darwin.exit(ExitCode.quitProduct)
    }

    func windowWillClose(_ notification: Notification) {
        Darwin.exit(ExitCode.closePresentation)
    }

    private func isExactProductOrigin(_ origin: WKSecurityOrigin) -> Bool {
        origin.`protocol` == "http" &&
            origin.host == "127.0.0.1" &&
            origin.port == 18210
    }

    private func isExactProductURL(_ url: URL) -> Bool {
        url.scheme == "http" &&
            url.host == "127.0.0.1" &&
            url.port == 18210
    }

    func webView(
        _ webView: WKWebView,
        requestMediaCapturePermissionFor origin: WKSecurityOrigin,
        initiatedByFrame frame: WKFrameInfo,
        type: WKMediaCaptureType,
        decisionHandler: @escaping (WKPermissionDecision) -> Void
    ) {
        // VoiceInput owns why VexLife requests audio. This presentation owner only realizes the
        // exact local Product-origin microphone request. Camera and media requests from Browser
        // subframes remain denied rather than inheriting native-app authority.
        let decision: WKPermissionDecision =
            isExactProductOrigin(origin) && type == .microphone
                ? .prompt
                : .deny
        decisionHandler(decision)
    }

    func webView(
        _ webView: WKWebView,
        requestDeviceOrientationAndMotionPermissionFor origin: WKSecurityOrigin,
        initiatedByFrame frame: WKFrameInfo,
        decisionHandler: @escaping (WKPermissionDecision) -> Void
    ) {
        // No current Product capability owns device motion/orientation.
        decisionHandler(.deny)
    }

    func webView(
        _ webView: WKWebView,
        runOpenPanelWith parameters: WKOpenPanelParameters,
        initiatedByFrame frame: WKFrameInfo,
        completionHandler: @escaping ([URL]?) -> Void
    ) {
        guard let frameURL = frame.request.url,
              isExactProductURL(frameURL),
              !parameters.allowsDirectories else {
            completionHandler(nil)
            return
        }

        let panel = NSOpenPanel()
        panel.canChooseFiles = true
        panel.canChooseDirectories = false
        panel.allowsMultipleSelection = parameters.allowsMultipleSelection
        panel.canCreateDirectories = false

        completionHandler(
            panel.runModal() == .OK
                ? panel.urls
                : nil
        )
    }

    func webView(
        _ webView: WKWebView,
        decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        guard let target = navigationAction.request.url else {
            decisionHandler(.cancel)
            return
        }

        if navigationAction.shouldPerformDownload {
            // Product-owned exports may cross the native filesystem boundary only after the human
            // chooses one exact destination. Browser/external downloads remain held.
            decisionHandler(isExactProductURL(target) ? .download : .cancel)
            return
        }

        guard let targetFrame = navigationAction.targetFrame else {
            // A new top-level window must earn the separate Browser-region owner.
            decisionHandler(.cancel)
            return
        }

        if targetFrame.isMainFrame {
            decisionHandler(isExactProductURL(target) ? .allow : .cancel)
            return
        }

        // Existing Browser/Context lives in subframes. Preserve ordinary HTTP(S) web navigation
        // only; local-file, data, javascript and custom-scheme frames remain held.
        let frameScheme = target.scheme?.lowercased()
        decisionHandler(frameScheme == "http" || frameScheme == "https" ? .allow : .cancel)
    }

    func webView(
        _ webView: WKWebView,
        navigationAction: WKNavigationAction,
        didBecome download: WKDownload
    ) {
        guard let url = download.originalRequest?.url, isExactProductURL(url) else {
            download.cancel(nil)
            return
        }
        download.delegate = self
    }

    func webView(
        _ webView: WKWebView,
        navigationResponse: WKNavigationResponse,
        didBecome download: WKDownload
    ) {
        guard let url = navigationResponse.response.url, isExactProductURL(url) else {
            download.cancel(nil)
            return
        }
        download.delegate = self
    }

    func download(
        _ download: WKDownload,
        decideDestinationUsing response: URLResponse,
        suggestedFilename: String,
        completionHandler: @escaping (URL?) -> Void
    ) {
        let panel = NSSavePanel()
        panel.nameFieldStringValue = suggestedFilename
        panel.canCreateDirectories = true
        panel.isExtensionHidden = false

        guard panel.runModal() == .OK, let destination = panel.url else {
            completionHandler(nil)
            return
        }

        // NSSavePanel owns the human overwrite decision. WKDownload requires the destination not
        // to exist, so realize the confirmed replacement immediately before handing it to WebKit.
        if FileManager.default.fileExists(atPath: destination.path) {
            do {
                try FileManager.default.removeItem(at: destination)
            } catch {
                completionHandler(nil)
                return
            }
        }
        completionHandler(destination)
    }

    func download(
        _ download: WKDownload,
        willPerformHTTPRedirection response: HTTPURLResponse,
        newRequest request: URLRequest,
        decisionHandler: @escaping (WKDownload.RedirectPolicy) -> Void
    ) {
        decisionHandler(
            request.url.map(isExactProductURL) == true
                ? .allow
                : .cancel
        )
    }
}

@main
private enum VexLifeProductPresentation {
    @MainActor
    static func main() {
        do {
            let arguments = try Arguments.parse(Array(CommandLine.arguments.dropFirst()))
            let app = NSApplication.shared
            app.setActivationPolicy(.regular)
            let presentation = ProductPresentation(arguments: arguments)
            app.delegate = presentation
            withExtendedLifetime(presentation) {
                app.run()
            }
            Darwin.exit(ExitCode.failure)
        } catch {
            Darwin.exit(ExitCode.failure)
        }
    }
}

// [VXG RealForever]
