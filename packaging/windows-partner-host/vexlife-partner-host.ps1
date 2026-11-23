#requires -Version 5.1
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$Schema = "vexlife-prod.partner-native-host/v1"
$MaximumSecretBytes = 2560
$MaximumRequestBodyBytes = 32 * 1024 * 1024
$MaximumResponseBodyBytes = 32 * 1024 * 1024
$ChunkBytes = 64 * 1024

Add-Type -AssemblyName System.Net.Http

$nativeSource = @'
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace VexLifePartnerWindowsHost {
    public sealed class HeaderFrame {
        public string HeaderLine;
        public HeaderFrame(string headerLine) {
            HeaderLine = headerLine;
        }
    }

    public static class FrameIO {
        private static readonly UTF8Encoding StrictUtf8 = new UTF8Encoding(false, true);
        public const int MaximumHeaderBytes = 64 * 1024;

        public static HeaderFrame ReadHeader(Stream input) {
            using (var memory = new MemoryStream()) {
                while (true) {
                    int value = input.ReadByte();
                    if (value < 0) {
                        throw new InvalidDataException("native Partner helper stdin ended before the header newline");
                    }
                    if (value == 10) {
                        break;
                    }
                    if (value == 0) {
                        throw new InvalidDataException("native Partner helper header contains NUL");
                    }
                    memory.WriteByte((byte)value);
                    if (memory.Length > MaximumHeaderBytes) {
                        throw new InvalidDataException("native Partner helper header exceeds the bounded size");
                    }
                }
                var bytes = memory.ToArray();
                try {
                    int length = bytes.Length;
                    if (length > 0 && bytes[length - 1] == 13) {
                        length -= 1;
                    }
                    if (length <= 0) {
                        throw new InvalidDataException("native Partner helper header is empty");
                    }
                    return new HeaderFrame(StrictUtf8.GetString(bytes, 0, length));
                } finally {
                    Array.Clear(bytes, 0, bytes.Length);
                }
            }
        }

        public static byte[] ReadPayload(Stream input, int count) {
            if (count < 0) {
                throw new ArgumentOutOfRangeException("count");
            }
            var bytes = new byte[count];
            int offset = 0;
            while (offset < count) {
                int read = input.Read(bytes, offset, count - offset);
                if (read <= 0) {
                    Array.Clear(bytes, 0, bytes.Length);
                    throw new InvalidDataException("native Partner helper stdin ended before the declared payload length");
                }
                offset += read;
            }
            if (input.ReadByte() != -1) {
                Array.Clear(bytes, 0, bytes.Length);
                throw new InvalidDataException("native Partner helper stdin contains trailing bytes after the declared payload");
            }
            return bytes;
        }
    }

    public sealed class CredentialProviderMismatchException : Exception {
        public CredentialProviderMismatchException() : base("protected Partner credential provider binding mismatch") {}
    }

    public static class CredentialManager {
        private const uint CRED_TYPE_GENERIC = 1;
        private const uint CRED_PERSIST_LOCAL_MACHINE = 2;
        private const int ERROR_NOT_FOUND = 1168;
        private const int MaximumSecretBytes = 2560;

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        private struct FILETIME {
            public uint dwLowDateTime;
            public uint dwHighDateTime;
        }

        [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
        private struct CREDENTIAL {
            public uint Flags;
            public uint Type;
            [MarshalAs(UnmanagedType.LPWStr)]
            public string TargetName;
            [MarshalAs(UnmanagedType.LPWStr)]
            public string Comment;
            public FILETIME LastWritten;
            public uint CredentialBlobSize;
            public IntPtr CredentialBlob;
            public uint Persist;
            public uint AttributeCount;
            public IntPtr Attributes;
            [MarshalAs(UnmanagedType.LPWStr)]
            public string TargetAlias;
            [MarshalAs(UnmanagedType.LPWStr)]
            public string UserName;
        }

        [DllImport("advapi32.dll", EntryPoint = "CredWriteW", CharSet = CharSet.Unicode, SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool CredWrite(ref CREDENTIAL credential, uint flags);

        [DllImport("advapi32.dll", EntryPoint = "CredReadW", CharSet = CharSet.Unicode, SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool CredRead(string target, uint type, uint flags, out IntPtr credential);

        [DllImport("advapi32.dll", EntryPoint = "CredDeleteW", CharSet = CharSet.Unicode, SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool CredDelete(string target, uint type, uint flags);

        [DllImport("advapi32.dll", EntryPoint = "CredFree", SetLastError = false)]
        private static extern void CredFree(IntPtr buffer);

        [DllImport("advapi32.dll", EntryPoint = "CredGetSessionTypes", SetLastError = true)]
        [return: MarshalAs(UnmanagedType.Bool)]
        private static extern bool CredGetSessionTypes(uint maximumPersistCount, [Out] uint[] maximumPersist);

        public static string Target(string providerRef, string credentialRef) {
            if (String.IsNullOrWhiteSpace(providerRef) || String.IsNullOrWhiteSpace(credentialRef)) {
                throw new ArgumentException("Partner credential coordinate is incomplete");
            }
            return "VexGPT_VexLife_Partner::" + providerRef + "::" + credentialRef;
        }

        public static void Store(string providerRef, string credentialRef, byte[] secret) {
            if (secret == null || secret.Length < 1 || secret.Length > MaximumSecretBytes) {
                throw new ArgumentException("Partner credential secret exceeds the Windows generic credential bound");
            }

            var persistence = new uint[8];
            if (!CredGetSessionTypes((uint)persistence.Length, persistence)) {
                throw new Win32Exception(Marshal.GetLastWin32Error(), "Windows Credential Manager persistence support could not be observed");
            }
            if (persistence[CRED_TYPE_GENERIC] < CRED_PERSIST_LOCAL_MACHINE) {
                throw new InvalidOperationException("Windows policy does not admit durable local-machine generic credentials");
            }

            var target = Target(providerRef, credentialRef);
            var handle = GCHandle.Alloc(secret, GCHandleType.Pinned);
            try {
                var credential = new CREDENTIAL {
                    Flags = 0,
                    Type = CRED_TYPE_GENERIC,
                    TargetName = target,
                    Comment = "VexLife Partner provider credential",
                    CredentialBlobSize = (uint)secret.Length,
                    CredentialBlob = handle.AddrOfPinnedObject(),
                    Persist = CRED_PERSIST_LOCAL_MACHINE,
                    AttributeCount = 0,
                    Attributes = IntPtr.Zero,
                    TargetAlias = null,
                    UserName = providerRef
                };

                if (!CredWrite(ref credential, 0)) {
                    throw new Win32Exception(Marshal.GetLastWin32Error(), "Windows Credential Manager refused the Partner credential");
                }
            } finally {
                handle.Free();
            }

            if (!Exists(providerRef, credentialRef)) {
                throw new InvalidOperationException("Windows Credential Manager did not retain the Partner credential");
            }
        }

        public static bool Exists(string providerRef, string credentialRef) {
            IntPtr pointer;
            if (!CredRead(Target(providerRef, credentialRef), CRED_TYPE_GENERIC, 0, out pointer)) {
                int error = Marshal.GetLastWin32Error();
                if (error == ERROR_NOT_FOUND) {
                    return false;
                }
                throw new Win32Exception(error, "Windows Credential Manager could not observe the Partner credential");
            }

            try {
                var credential = (CREDENTIAL)Marshal.PtrToStructure(pointer, typeof(CREDENTIAL));
                if (!String.Equals(credential.UserName, providerRef, StringComparison.Ordinal)) {
                    throw new CredentialProviderMismatchException();
                }
                return true;
            } finally {
                CredFree(pointer);
            }
        }

        public static bool Delete(string providerRef, string credentialRef) {
            string target = Target(providerRef, credentialRef);
            if (!CredDelete(target, CRED_TYPE_GENERIC, 0)) {
                int error = Marshal.GetLastWin32Error();
                if (error != ERROR_NOT_FOUND) {
                    throw new Win32Exception(error, "Windows Credential Manager could not delete the Partner credential");
                }
            }
            return !Exists(providerRef, credentialRef);
        }

        public static byte[] ReadSecret(string providerRef, string credentialRef) {
            IntPtr pointer;
            if (!CredRead(Target(providerRef, credentialRef), CRED_TYPE_GENERIC, 0, out pointer)) {
                int error = Marshal.GetLastWin32Error();
                if (error == ERROR_NOT_FOUND) {
                    return null;
                }
                throw new Win32Exception(error, "Windows Credential Manager could not read the Partner credential");
            }

            try {
                var credential = (CREDENTIAL)Marshal.PtrToStructure(pointer, typeof(CREDENTIAL));
                if (!String.Equals(credential.UserName, providerRef, StringComparison.Ordinal)) {
                    throw new CredentialProviderMismatchException();
                }
                if (credential.CredentialBlobSize < 1 || credential.CredentialBlobSize > MaximumSecretBytes) {
                    throw new InvalidDataException("Windows Credential Manager returned an invalid Partner credential blob size");
                }
                var secret = new byte[(int)credential.CredentialBlobSize];
                Marshal.Copy(credential.CredentialBlob, secret, 0, secret.Length);
                return secret;
            } finally {
                CredFree(pointer);
            }
        }
    }

    public sealed class ProviderConfiguration {
        public string ProviderRef;
        public string RequestUrl;
        public string Dialect;
        public string RequiredVersion;

        public static ProviderConfiguration Resolve(string providerRef) {
            switch (providerRef) {
                case "provider.openai.api":
                    return new ProviderConfiguration {
                        ProviderRef = providerRef,
                        RequestUrl = "https://api.openai.com/v1/responses",
                        Dialect = "OPENAI_RESPONSES",
                        RequiredVersion = null
                    };
                case "provider.anthropic.api":
                    return new ProviderConfiguration {
                        ProviderRef = providerRef,
                        RequestUrl = "https://api.anthropic.com/v1/messages",
                        Dialect = "ANTHROPIC_MESSAGES",
                        RequiredVersion = "2026-10-21"
                    };
                case "provider.kimi.open-platform.international":
                    return new ProviderConfiguration {
                        ProviderRef = providerRef,
                        RequestUrl = "https://api.moonshot.ai/v1/chat/completions",
                        Dialect = "OPENAI_CHAT_COMPLETIONS",
                        RequiredVersion = null
                    };
                default:
                    throw new ArgumentException("native Windows Partner host does not admit this providerRef");
            }
        }

        public static void ValidateExecution(
            string providerRef,
            string expectedRequestUrl,
            string dialect,
            IDictionary<string, string> headers
        ) {
            var provider = Resolve(providerRef);
            if (!String.Equals(provider.RequestUrl, expectedRequestUrl, StringComparison.Ordinal)) {
                throw new InvalidDataException("native Windows Partner host request URL does not match the built-in provider");
            }
            if (!String.Equals(provider.Dialect, dialect, StringComparison.Ordinal)) {
                throw new InvalidDataException("native Windows Partner host dialect does not match the built-in provider");
            }

            foreach (var pair in headers) {
                string name = pair.Key.ToLowerInvariant();
                if (name == "authorization" || name == "x-api-key") {
                    throw new InvalidDataException("native Windows Partner host public headers contain credential material");
                }
                if (name != "content-type" &&
                    name != "accept" &&
                    name != "anthropic-version" &&
                    name != "anthropic-workspace-id") {
                    throw new InvalidDataException("native Windows Partner host received an unadmitted public protocol header");
                }
            }

            string contentType;
            if (!headers.TryGetValue("content-type", out contentType) ||
                !String.Equals(contentType, "application/json", StringComparison.OrdinalIgnoreCase)) {
                throw new InvalidDataException("native Windows Partner host requires application/json content type");
            }

            string accept;
            if (!headers.TryGetValue("accept", out accept) ||
                !String.Equals(accept, "text/event-stream, application/json", StringComparison.OrdinalIgnoreCase)) {
                throw new InvalidDataException("native Windows Partner host requires the source-owned Accept header");
            }

            string version;
            if (provider.RequiredVersion != null) {
                if (!headers.TryGetValue("anthropic-version", out version) ||
                    !String.Equals(version, provider.RequiredVersion, StringComparison.Ordinal)) {
                    throw new InvalidDataException("native Windows Partner host requires the source-owned Anthropic API version");
                }
            } else if (headers.ContainsKey("anthropic-version") || headers.ContainsKey("anthropic-workspace-id")) {
                throw new InvalidDataException("Anthropic-only headers escaped their provider boundary");
            }

            string workspace;
            if (headers.TryGetValue("anthropic-workspace-id", out workspace)) {
                if (workspace.Length < 8 ||
                    workspace.Length > 256 ||
                    !workspace.StartsWith("wrkspc_", StringComparison.Ordinal)) {
                    throw new InvalidDataException("Anthropic workspace ID is not a bounded workspace coordinate");
                }
            }
        }
    }

    public sealed class HttpStartResult : IDisposable {
        public string FailureCode;
        public string FailureDetail;
        public HttpClient Client;
        public HttpResponseMessage Response;
        public Stream Stream;

        public bool IsFailure {
            get { return FailureCode != null; }
        }

        public void Dispose() {
            if (Stream != null) {
                Stream.Dispose();
                Stream = null;
            }
            if (Response != null) {
                Response.Dispose();
                Response = null;
            }
            if (Client != null) {
                Client.Dispose();
                Client = null;
            }
        }
    }

    public static class PartnerHttp {
        private static string FailureFromException(Exception error) {
            var web = error as WebException;
            if (web == null && error.InnerException != null) {
                web = error.InnerException as WebException;
            }
            if (web != null) {
                switch (web.Status) {
                    case WebExceptionStatus.NameResolutionFailure:
                        return "DNS_FAILURE";
                    case WebExceptionStatus.ConnectFailure:
                        return "CONNECT_FAILURE";
                    case WebExceptionStatus.TrustFailure:
                    case WebExceptionStatus.SecureChannelFailure:
                        return "TLS_FAILURE";
                    case WebExceptionStatus.Timeout:
                        return "TIMEOUT";
                }
            }
            if (error is TaskCanceledException || error is OperationCanceledException) {
                return "TIMEOUT";
            }
            return "CONNECT_FAILURE";
        }

        public static HttpStartResult Start(
            string providerRef,
            string credentialRef,
            string expectedRequestUrl,
            string dialect,
            IDictionary<string, string> publicHeaders,
            byte[] requestBody
        ) {
            ProviderConfiguration.ValidateExecution(
                providerRef,
                expectedRequestUrl,
                dialect,
                publicHeaders
            );

            byte[] secret = null;
            string token = null;
            HttpClient client = null;
            HttpResponseMessage response = null;
            HttpRequestMessage request = null;

            try {
                secret = CredentialManager.ReadSecret(providerRef, credentialRef);
                if (secret == null) {
                    return new HttpStartResult {
                        FailureCode = "CREDENTIAL_NOT_PRESENT",
                        FailureDetail = "protected Windows Partner credential is not present"
                    };
                }

                try {
                    token = new UTF8Encoding(false, true).GetString(secret);
                } catch (DecoderFallbackException) {
                    return new HttpStartResult {
                        FailureCode = "CREDENTIAL_PROVIDER_MISMATCH",
                        FailureDetail = "protected Windows Partner credential is not valid UTF-8"
                    };
                }

                var handler = new HttpClientHandler {
                    AllowAutoRedirect = false,
                    UseCookies = false,
                    UseDefaultCredentials = false,
                    CheckCertificateRevocationList = true,
                    AutomaticDecompression = DecompressionMethods.GZip | DecompressionMethods.Deflate
                };
                client = new HttpClient(handler);
                client.Timeout = Timeout.InfiniteTimeSpan;

                request = new HttpRequestMessage(HttpMethod.Post, expectedRequestUrl);
                request.Content = new ByteArrayContent(requestBody);
                request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);

                foreach (var pair in publicHeaders) {
                    string name = pair.Key.ToLowerInvariant();
                    if (name == "content-type") {
                        request.Content.Headers.ContentType = MediaTypeHeaderValue.Parse(pair.Value);
                    } else {
                        if (!request.Headers.TryAddWithoutValidation(pair.Key, pair.Value)) {
                            throw new InvalidDataException("native Windows Partner host could not apply a public protocol header");
                        }
                    }
                }

                var cancellation = new CancellationTokenSource(TimeSpan.FromMinutes(10));
                try {
                    response = client
                        .SendAsync(request, HttpCompletionOption.ResponseHeadersRead, cancellation.Token)
                        .GetAwaiter()
                        .GetResult();
                } finally {
                    cancellation.Dispose();
                }

                int status = (int)response.StatusCode;
                request.Headers.Authorization = null;
                response.RequestMessage = null;
                request.Dispose();
                request = null;
                token = null;

                if (status >= 300 && status <= 399) {
                    response.Dispose();
                    response = null;
                    client.Dispose();
                    client = null;
                    return new HttpStartResult {
                        FailureCode = "REDIRECT_REJECTED",
                        FailureDetail = "built-in Partner provider returned a redirect"
                    };
                }

                var stream = response.Content.ReadAsStreamAsync().GetAwaiter().GetResult();
                return new HttpStartResult {
                    Client = client,
                    Response = response,
                    Stream = stream
                };
            } catch (CredentialProviderMismatchException) {
                if (response != null) response.Dispose();
                if (client != null) client.Dispose();
                return new HttpStartResult {
                    FailureCode = "CREDENTIAL_PROVIDER_MISMATCH",
                    FailureDetail = "protected Windows Partner credential belongs to another provider"
                };
            } catch (Exception error) {
                if (request != null) request.Dispose();
                if (response != null) response.Dispose();
                if (client != null) client.Dispose();
                return new HttpStartResult {
                    FailureCode = FailureFromException(error),
                    FailureDetail = "Windows Partner HTTPS effect failed before a usable HTTP response"
                };
            } finally {
                if (secret != null) {
                    Array.Clear(secret, 0, secret.Length);
                }
                token = null;
            }
        }
    }
}
'@

Add-Type -TypeDefinition $nativeSource -Language CSharp

function Write-ProtocolEvent {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable] $Event
    )

    $Event["schemaVersion"] = $Schema
    $Event["redirectFollowed"] = $false
    $Event["secretProjected"] = $false

    $json = $Event | ConvertTo-Json -Compress -Depth 8
    [Console]::Out.WriteLine($json)
    [Console]::Out.Flush()
}

function Get-PublicHeaders {
    param(
        [Parameter(Mandatory = $true)]
        $HeaderObject
    )

    $headers = New-Object "System.Collections.Generic.Dictionary[string,string]" ([System.StringComparer]::OrdinalIgnoreCase)
    if ($null -ne $HeaderObject.publicProtocolHeaders) {
        foreach ($property in $HeaderObject.publicProtocolHeaders.PSObject.Properties) {
            $headers[$property.Name.ToLowerInvariant()] = [string]$property.Value
        }
    }
    return $headers
}

function Get-RequestId {
    param(
        [Parameter(Mandatory = $true)]
        [System.Net.Http.HttpResponseMessage] $Response
    )

    foreach ($name in @("x-request-id", "request-id", "x-moonshot-request-id")) {
        if ($Response.Headers.Contains($name)) {
            $value = ($Response.Headers.GetValues($name) | Select-Object -First 1)
            if (-not [string]::IsNullOrWhiteSpace($value)) {
                return [string]$value
            }
        }
    }
    return $null
}

function Get-RetryAfter {
    param(
        [Parameter(Mandatory = $true)]
        [System.Net.Http.HttpResponseMessage] $Response
    )

    if ($null -ne $Response.Headers.RetryAfter) {
        return [string]$Response.Headers.RetryAfter.ToString()
    }
    return $null
}

$inputStream = [Console]::OpenStandardInput()
$frame = [VexLifePartnerWindowsHost.FrameIO]::ReadHeader($inputStream)
$header = $frame.HeaderLine | ConvertFrom-Json

if ([string]$header.schemaVersion -ne $Schema) {
    throw "native Windows Partner host protocol schema mismatch"
}

$operation = [string]$header.operation
$providerRef = [string]$header.providerRef
$credentialRef = [string]$header.credentialRef
[VexLifePartnerWindowsHost.ProviderConfiguration]::Resolve($providerRef) | Out-Null

switch ($operation) {
    "STORE" {
        $secretBytes = [int]$header.secretBytes
        if ($secretBytes -lt 1 -or $secretBytes -gt $MaximumSecretBytes) {
            throw "native Windows Partner STORE secret byte count is out of bounds"
        }
        $payload = [VexLifePartnerWindowsHost.FrameIO]::ReadPayload($inputStream, $secretBytes)
        try {
            [VexLifePartnerWindowsHost.CredentialManager]::Store($providerRef, $credentialRef, $payload)
            Write-ProtocolEvent @{
                type = "STORED"
                providerRef = $providerRef
                credentialRef = $credentialRef
                present = $true
            }
        } finally {
            [Array]::Clear($payload, 0, $payload.Length)
        }
    }

    "OBSERVE" {
        [VexLifePartnerWindowsHost.FrameIO]::ReadPayload($inputStream, 0) | Out-Null
        $present = [VexLifePartnerWindowsHost.CredentialManager]::Exists($providerRef, $credentialRef)
        Write-ProtocolEvent @{
            type = "OBSERVED"
            providerRef = $providerRef
            credentialRef = $credentialRef
            present = [bool]$present
        }
    }

    "DELETE" {
        [VexLifePartnerWindowsHost.FrameIO]::ReadPayload($inputStream, 0) | Out-Null
        $absent = [VexLifePartnerWindowsHost.CredentialManager]::Delete($providerRef, $credentialRef)
        if (-not $absent) {
            throw "native Windows Partner host did not prove credential deletion"
        }
        Write-ProtocolEvent @{
            type = "DELETED"
            providerRef = $providerRef
            credentialRef = $credentialRef
            present = $false
        }
    }

    "EXECUTE" {
        $requestBodyBytes = [int]$header.requestBodyBytes
        if ($requestBodyBytes -lt 1 -or $requestBodyBytes -gt $MaximumRequestBodyBytes) {
            throw "native Windows Partner request body byte count is out of bounds"
        }
        if ([string]$header.redirectPolicy -ne "DENY") {
            throw "native Windows Partner host admits redirect denial only"
        }

        $requestBody = [VexLifePartnerWindowsHost.FrameIO]::ReadPayload($inputStream, $requestBodyBytes)
        $sha = [System.Security.Cryptography.SHA256]::Create()
        try {
            $requestDigest = (($sha.ComputeHash($requestBody) | ForEach-Object { $_.ToString("x2") }) -join "")
        } finally {
            $sha.Dispose()
        }
        if ($requestDigest -ne [string]$header.requestBodySha256) {
            [Array]::Clear($requestBody, 0, $requestBody.Length)
            throw "native Windows Partner request body does not match its declared SHA-256"
        }

        $headers = Get-PublicHeaders $header
        [VexLifePartnerWindowsHost.ProviderConfiguration]::ValidateExecution(
            $providerRef,
            [string]$header.expectedRequestUrl,
            [string]$header.dialect,
            $headers
        )

        $started = $null
        try {
            $started = [VexLifePartnerWindowsHost.PartnerHttp]::Start(
                $providerRef,
                $credentialRef,
                [string]$header.expectedRequestUrl,
                [string]$header.dialect,
                $headers,
                $requestBody
            )
        } finally {
            [Array]::Clear($requestBody, 0, $requestBody.Length)
        }

        if ($started.IsFailure) {
            try {
                Write-ProtocolEvent @{
                    type = "TRANSPORT_FAILURE"
                    providerRef = $providerRef
                    credentialRef = $credentialRef
                    failureCode = [string]$started.FailureCode
                    detail = [string]$started.FailureDetail
                }
            } finally {
                $started.Dispose()
            }
            break
        }

        try {
            $response = $started.Response
            $status = [int]$response.StatusCode

            Write-ProtocolEvent @{
                type = "RESPONSE_START"
                providerRef = $providerRef
                credentialRef = $credentialRef
                httpStatus = $status
                retryAfter = (Get-RetryAfter $response)
                providerRequestId = (Get-RequestId $response)
            }

            $digest = [System.Security.Cryptography.SHA256]::Create()
            $buffer = New-Object byte[] $ChunkBytes
            [long]$total = 0
            try {
                while ($true) {
                    $read = $started.Stream.Read($buffer, 0, $buffer.Length)
                    if ($read -le 0) {
                        break
                    }

                    $total += $read
                    if ($total -gt $MaximumResponseBodyBytes) {
                        throw "native Windows Partner response exceeds the bounded transport size"
                    }

                    [void]$digest.TransformBlock($buffer, 0, $read, $buffer, 0)
                    $chunk = New-Object byte[] $read
                    [Array]::Copy($buffer, 0, $chunk, 0, $read)
                    try {
                        Write-ProtocolEvent @{
                            type = "BODY_CHUNK"
                            providerRef = $providerRef
                            credentialRef = $credentialRef
                            bodyChunkBase64 = [Convert]::ToBase64String($chunk)
                        }
                    } finally {
                        [Array]::Clear($chunk, 0, $chunk.Length)
                    }
                }

                [void]$digest.TransformFinalBlock([byte[]]@(), 0, 0)
                $responseDigest = (($digest.Hash | ForEach-Object { $_.ToString("x2") }) -join "")

                Write-ProtocolEvent @{
                    type = "COMPLETED"
                    providerRef = $providerRef
                    credentialRef = $credentialRef
                    responseBodySha256 = $responseDigest
                }
            } finally {
                [Array]::Clear($buffer, 0, $buffer.Length)
                $digest.Dispose()
            }
        } finally {
            $started.Dispose()
        }
    }

    default {
        throw "native Windows Partner host operation is not admitted"
    }
}

# [VXG RealForever]
