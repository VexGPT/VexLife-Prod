using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

namespace VexLife.WebShell;

internal static class Program
{
    internal const int ClosePresentationExitCode = 0;
    internal const int QuitProductExitCode = 42;
    internal const int FailureExitCode = 70;

    [STAThread]
    private static int Main(string[] args)
    {
        try
        {
            var values = ParseExactArgs(args);
            var origin = RequireExactProductOrigin(values["--origin"]);
            var quitLabel = RequireText(values["--quit-label"], "--quit-label");

            ApplicationConfiguration.Initialize();
            using var window = new ProductWindow(origin, quitLabel);
            Application.Run(window);
            if (window.InitializationFailed)
            {
                return FailureExitCode;
            }
            return window.QuitProductRequested ? QuitProductExitCode : ClosePresentationExitCode;
        }
        catch (Exception error)
        {
            try
            {
                MessageBox.Show(
                    error.GetType().Name + ": " + error.Message,
                    "VexLife could not open",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error
                );
            }
            catch
            {
                // The shell has no second fallback presentation owner.
            }

            return FailureExitCode;
        }
    }

    private static Dictionary<string, string> ParseExactArgs(string[] args)
    {
        if (args.Length != 4)
        {
            throw new ArgumentException(
                "VexLife WebView2 shell requires exactly --origin <url> --quit-label <text>."
            );
        }

        var values = new Dictionary<string, string>(StringComparer.Ordinal);
        for (var index = 0; index < args.Length; index += 2)
        {
            var key = args[index];
            var value = args[index + 1];
            if (key is not ("--origin" or "--quit-label") ||
                !values.TryAdd(key, value))
            {
                throw new ArgumentException("VexLife WebView2 shell arguments are invalid.");
            }
        }

        if (!values.ContainsKey("--origin") || !values.ContainsKey("--quit-label"))
        {
            throw new ArgumentException("VexLife WebView2 shell arguments are incomplete.");
        }

        return values;
    }

    private static Uri RequireExactProductOrigin(string value)
    {
        if (!Uri.TryCreate(value, UriKind.Absolute, out var uri) ||
            uri.Scheme != Uri.UriSchemeHttp ||
            uri.Host != "127.0.0.1" ||
            uri.Port != 18210 ||
            (uri.AbsolutePath != "/" && uri.AbsolutePath != string.Empty) ||
            !string.IsNullOrEmpty(uri.Query) ||
            !string.IsNullOrEmpty(uri.Fragment))
        {
            throw new ArgumentException(
                "VexLife WebView2 shell admits only the exact local Product origin."
            );
        }

        return new Uri("http://127.0.0.1:18210/");
    }

    private static string RequireText(string value, string label)
    {
        if (string.IsNullOrWhiteSpace(value))
        {
            throw new ArgumentException(label + " must be non-empty.");
        }

        return value;
    }
}

internal sealed class ProductWindow : Form
{
    private readonly Uri productOrigin;
    private readonly WebView2 webView = new();

    internal bool QuitProductRequested { get; private set; }
    internal bool InitializationFailed { get; private set; }

    internal ProductWindow(Uri productOrigin, string quitLabel)
    {
        this.productOrigin = productOrigin;

        Text = "VexLife";
        Width = 1200;
        Height = 800;
        MinimumSize = new Size(760, 520);
        StartPosition = FormStartPosition.CenterScreen;

        var menu = new MenuStrip();
        var file = new ToolStripMenuItem("&File");
        var quit = new ToolStripMenuItem(quitLabel)
        {
            ShortcutKeys = Keys.Control | Keys.Q,
        };
        quit.Click += (_, _) =>
        {
            QuitProductRequested = true;
            Close();
        };
        file.DropDownItems.Add(quit);
        menu.Items.Add(file);

        webView.Dock = DockStyle.Fill;
        webView.CreationProperties = new CoreWebView2CreationProperties
        {
            UserDataFolder = ProductUserDataFolder(),
        };

        Controls.Add(webView);
        Controls.Add(menu);
        MainMenuStrip = menu;

        Shown += async (_, _) => await InitializeProductAsync();
    }

    private async Task InitializeProductAsync()
    {
        try
        {
            await webView.EnsureCoreWebView2Async();

            webView.CoreWebView2.NavigationStarting += (_, args) =>
            {
                if (!Uri.TryCreate(args.Uri, UriKind.Absolute, out var target) ||
                    !IsExactProductTopLevel(target))
                {
                    args.Cancel = true;
                }
            };

            webView.CoreWebView2.FrameNavigationStarting += (_, args) =>
            {
                if (!Uri.TryCreate(args.Uri, UriKind.Absolute, out var target) ||
                    (target.Scheme != Uri.UriSchemeHttp &&
                        target.Scheme != Uri.UriSchemeHttps))
                {
                    args.Cancel = true;
                }
            };

            webView.CoreWebView2.LaunchingExternalUriScheme += (_, args) =>
            {
                // Custom OS schemes remain held until an explicit Product/browser policy owns them.
                args.Cancel = true;
            };

            webView.CoreWebView2.NewWindowRequested += (_, args) =>
            {
                // External/new-window navigation must earn a Browser-region owner later.
                args.Handled = true;
            };

            webView.CoreWebView2.DownloadStarting += (_, args) =>
            {
                if (!Uri.TryCreate(args.DownloadOperation.Uri, UriKind.Absolute, out var source) ||
                    !IsExactProductTopLevel(source))
                {
                    args.Cancel = true;
                    args.Handled = true;
                    return;
                }

                using var dialog = new SaveFileDialog
                {
                    FileName = Path.GetFileName(args.ResultFilePath),
                    OverwritePrompt = true,
                    CheckPathExists = true,
                    AddExtension = true,
                };
                if (dialog.ShowDialog(this) != DialogResult.OK ||
                    String.IsNullOrWhiteSpace(dialog.FileName))
                {
                    args.Cancel = true;
                    args.Handled = true;
                    return;
                }

                args.ResultFilePath = Path.GetFullPath(dialog.FileName);
                args.Handled = true;
            };

            webView.CoreWebView2.PermissionRequested += (_, args) =>
            {
                if (!Uri.TryCreate(args.Uri, UriKind.Absolute, out var requester) ||
                    !IsExactProductTopLevel(requester))
                {
                    args.State = CoreWebView2PermissionState.Deny;
                    args.SavesInProfile = false;
                    return;
                }

                switch (args.PermissionKind)
                {
                    case CoreWebView2PermissionKind.Microphone:
                        // VoiceInput owns why audio is requested. Default keeps the ordinary
                        // WebView2 human permission prompt and Product-local profile choice.
                        args.State = CoreWebView2PermissionState.Default;
                        args.SavesInProfile = true;
                        break;

                    case CoreWebView2PermissionKind.Autoplay:
                    case CoreWebView2PermissionKind.PersistentStorage:
                        // Preserve the current Product web experience without widening to device
                        // sensors/files. These remain ordinary Product-local web-profile policy.
                        args.State = CoreWebView2PermissionState.Default;
                        break;

                    default:
                        args.State = CoreWebView2PermissionState.Deny;
                        args.SavesInProfile = false;
                        break;
                }
            };

            webView.Source = productOrigin;
        }
        catch (Exception error)
        {
            InitializationFailed = true;
            try
            {
                MessageBox.Show(
                    error.GetType().Name + ": " + error.Message,
                    "VexLife could not open",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error
                );
            }
            finally
            {
                Close();
            }
        }
    }

    private static bool IsExactProductTopLevel(Uri target) =>
        target.Scheme == Uri.UriSchemeHttp &&
        target.Host == "127.0.0.1" &&
        target.Port == 18210;

    private static string ProductUserDataFolder()
    {
        var localAppData =
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        if (string.IsNullOrWhiteSpace(localAppData))
        {
            throw new InvalidOperationException("LocalAppData is unavailable for VexLife WebView2.");
        }

        return Path.Combine(localAppData, "VexLife", "WebView2", "Product");
    }
}
