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
    WKNavigationDelegate
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

    func webView(
        _ webView: WKWebView,
        decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        guard let target = navigationAction.request.url else {
            decisionHandler(.cancel)
            return
        }

        guard let targetFrame = navigationAction.targetFrame else {
            // A new top-level window must earn the separate Browser-region owner.
            decisionHandler(.cancel)
            return
        }

        if targetFrame.isMainFrame {
            let allowed =
                target.scheme == "http" &&
                target.host == "127.0.0.1" &&
                target.port == 18210
            decisionHandler(allowed ? .allow : .cancel)
            return
        }

        // Subframes remain web presentation. They receive no native bridge or Product-effect API.
        decisionHandler(.allow)
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
