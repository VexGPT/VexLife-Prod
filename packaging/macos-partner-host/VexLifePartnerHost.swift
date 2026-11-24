import Foundation
import Security
import CryptoKit
import Darwin

private let schema = "vexlife-prod.partner-native-host/v1"
private let maximumSecretBytes = 2_560
private let maximumRequestBodyBytes = 32 * 1024 * 1024
private let maximumResponseBodyBytes = 32 * 1024 * 1024
private let maximumHeaderBytes = 64 * 1024

private struct Header: Decodable {
    let schemaVersion: String
    let operation: String
    let providerRef: String
    let credentialRef: String
    let secretBytes: Int?
    let expectedRequestUrl: String?
    let dialect: String?
    let publicProtocolHeaders: [String: String]?
    let requestBodySha256: String?
    let requestBodyBytes: Int?
    let redirectPolicy: String?
}

private struct ProtocolEvent: Encodable {
    let schemaVersion: String
    let type: String
    let providerRef: String
    let credentialRef: String
    let present: Bool?
    let httpStatus: Int?
    let retryAfter: String?
    let providerRequestId: String?
    let bodyChunkBase64: String?
    let responseBodySha256: String?
    let failureCode: String?
    let detail: String?
    let redirectFollowed: Bool
    let secretProjected: Bool

    init(
        type: String,
        providerRef: String,
        credentialRef: String,
        present: Bool? = nil,
        httpStatus: Int? = nil,
        retryAfter: String? = nil,
        providerRequestId: String? = nil,
        bodyChunkBase64: String? = nil,
        responseBodySha256: String? = nil,
        failureCode: String? = nil,
        detail: String? = nil
    ) {
        self.schemaVersion = schema
        self.type = type
        self.providerRef = providerRef
        self.credentialRef = credentialRef
        self.present = present
        self.httpStatus = httpStatus
        self.retryAfter = retryAfter
        self.providerRequestId = providerRequestId
        self.bodyChunkBase64 = bodyChunkBase64
        self.responseBodySha256 = responseBodySha256
        self.failureCode = failureCode
        self.detail = detail
        self.redirectFollowed = false
        self.secretProjected = false
    }
}

private struct ProviderDefinition {
    let providerRef: String
    let requestURL: URL
    let dialect: String
    let anthropicVersion: String?

    static func resolve(_ providerRef: String) throws -> ProviderDefinition {
        switch providerRef {
        case "provider.openai.api":
            return ProviderDefinition(
                providerRef: providerRef,
                requestURL: URL(string: "https://api.openai.com/v1/responses")!,
                dialect: "OPENAI_RESPONSES",
                anthropicVersion: nil
            )
        case "provider.anthropic.api":
            return ProviderDefinition(
                providerRef: providerRef,
                requestURL: URL(string: "https://api.anthropic.com/v1/messages")!,
                dialect: "ANTHROPIC_MESSAGES",
                anthropicVersion: "2026-10-21"
            )
        case "provider.kimi.open-platform.international":
            return ProviderDefinition(
                providerRef: providerRef,
                requestURL: URL(string: "https://api.moonshot.ai/v1/chat/completions")!,
                dialect: "OPENAI_CHAT_COMPLETIONS",
                anthropicVersion: nil
            )
        default:
            throw HostError.invalidInput("macOS Partner host does not admit this providerRef")
        }
    }

    func validate(
        expectedRequestURL: String,
        dialect: String,
        headers: [String: String]
    ) throws {
        guard expectedRequestURL == requestURL.absoluteString else {
            throw HostError.invalidInput("macOS Partner host request URL does not match the built-in provider")
        }
        guard dialect == self.dialect else {
            throw HostError.invalidInput("macOS Partner host dialect does not match the built-in provider")
        }

        let normalized = Dictionary(uniqueKeysWithValues: headers.map { ($0.key.lowercased(), $0.value) })
        for key in normalized.keys {
            guard !["authorization", "x-api-key"].contains(key) else {
                throw HostError.invalidInput("macOS Partner host public headers contain credential material")
            }
            guard ["content-type", "accept", "anthropic-version", "anthropic-workspace-id"].contains(key) else {
                throw HostError.invalidInput("macOS Partner host received an unadmitted public protocol header")
            }
        }

        guard normalized["content-type"]?.lowercased() == "application/json" else {
            throw HostError.invalidInput("macOS Partner host requires application/json content type")
        }
        guard normalized["accept"]?.lowercased() == "text/event-stream, application/json" else {
            throw HostError.invalidInput("macOS Partner host requires the source-owned Accept header")
        }

        if let version = anthropicVersion {
            guard normalized["anthropic-version"] == version else {
                throw HostError.invalidInput("macOS Partner host requires the source-owned Anthropic API version")
            }
            if let workspace = normalized["anthropic-workspace-id"] {
                guard workspace.hasPrefix("wrkspc_"), workspace.count <= 256 else {
                    throw HostError.invalidInput("Anthropic workspace ID is not a bounded workspace coordinate")
                }
            }
        } else {
            guard normalized["anthropic-version"] == nil,
                  normalized["anthropic-workspace-id"] == nil else {
                throw HostError.invalidInput("Anthropic-only headers escaped their provider boundary")
            }
        }
    }
}

private enum HostError: Error {
    case invalidInput(String)
    case keychain(OSStatus, String)
}

private func service(_ providerRef: String) -> String {
    "com.vexlife.prod.partner." + providerRef
}

private func keychainQuery(
    providerRef: String,
    credentialRef: String
) -> [String: Any] {
    [
        kSecClass as String: kSecClassGenericPassword,
        kSecAttrService as String: service(providerRef),
        kSecAttrAccount as String: credentialRef,
    ]
}

private func storeCredential(
    providerRef: String,
    credentialRef: String,
    secret: Data
) throws {
    guard !secret.isEmpty, secret.count <= maximumSecretBytes else {
        throw HostError.invalidInput("macOS Partner credential exceeds the Product secret bound")
    }
    var query = keychainQuery(providerRef: providerRef, credentialRef: credentialRef)
    query[kSecValueData as String] = secret
    query[kSecAttrLabel as String] = "VexLife Partner provider credential"

    let status = SecItemAdd(query as CFDictionary, nil)
    guard status == errSecSuccess else {
        throw HostError.keychain(status, "macOS Keychain refused the Partner credential")
    }

    guard try observeCredential(providerRef: providerRef, credentialRef: credentialRef) else {
        throw HostError.invalidInput("macOS Keychain did not retain the Partner credential")
    }
}

private func observeCredential(
    providerRef: String,
    credentialRef: String
) throws -> Bool {
    var query = keychainQuery(providerRef: providerRef, credentialRef: credentialRef)
    query[kSecMatchLimit as String] = kSecMatchLimitOne
    query[kSecReturnAttributes as String] = true

    var item: CFTypeRef?
    let status = SecItemCopyMatching(query as CFDictionary, &item)
    if status == errSecItemNotFound {
        return false
    }
    guard status == errSecSuccess else {
        throw HostError.keychain(status, "macOS Keychain could not observe the Partner credential")
    }
    return true
}

private func deleteCredential(
    providerRef: String,
    credentialRef: String
) throws {
    let status = SecItemDelete(
        keychainQuery(
            providerRef: providerRef,
            credentialRef: credentialRef
        ) as CFDictionary
    )
    guard status == errSecSuccess || status == errSecItemNotFound else {
        throw HostError.keychain(status, "macOS Keychain could not delete the Partner credential")
    }
    guard try !observeCredential(providerRef: providerRef, credentialRef: credentialRef) else {
        throw HostError.invalidInput("macOS Keychain did not prove Partner credential deletion")
    }
}

private func readCredential(
    providerRef: String,
    credentialRef: String
) throws -> Data? {
    var query = keychainQuery(providerRef: providerRef, credentialRef: credentialRef)
    query[kSecMatchLimit as String] = kSecMatchLimitOne
    query[kSecReturnData as String] = true

    var item: CFTypeRef?
    let status = SecItemCopyMatching(query as CFDictionary, &item)
    if status == errSecItemNotFound {
        return nil
    }
    guard status == errSecSuccess else {
        throw HostError.keychain(status, "macOS Keychain could not read the Partner credential")
    }
    guard let data = item as? Data,
          !data.isEmpty,
          data.count <= maximumSecretBytes else {
        throw HostError.invalidInput("macOS Keychain returned an invalid Partner credential")
    }
    return data
}

private func readHeader(from input: FileHandle) throws -> String {
    var bytes = Data()
    while true {
        let byte = input.readData(ofLength: 1)
        guard !byte.isEmpty else {
            throw HostError.invalidInput("macOS Partner helper stdin ended before the header newline")
        }
        if byte[byte.startIndex] == 10 {
            break
        }
        bytes.append(byte)
        guard bytes.count <= maximumHeaderBytes else {
            throw HostError.invalidInput("macOS Partner helper header exceeds the bounded size")
        }
    }
    if bytes.last == 13 {
        bytes.removeLast()
    }
    guard !bytes.isEmpty,
          let header = String(data: bytes, encoding: .utf8) else {
        throw HostError.invalidInput("macOS Partner helper header is not valid UTF-8")
    }
    return header
}

private func readPayload(
    from input: FileHandle,
    count: Int
) throws -> Data {
    guard count >= 0 else {
        throw HostError.invalidInput("macOS Partner helper payload length is negative")
    }
    var data = Data()
    data.reserveCapacity(count)
    while data.count < count {
        let next = input.readData(ofLength: count - data.count)
        guard !next.isEmpty else {
            throw HostError.invalidInput("macOS Partner helper stdin ended before the declared payload length")
        }
        data.append(next)
    }
    guard input.readData(ofLength: 1).isEmpty else {
        throw HostError.invalidInput("macOS Partner helper stdin contains trailing bytes")
    }
    return data
}

private let outputLock = NSLock()

private func emit(_ event: ProtocolEvent) {
    outputLock.lock()
    defer { outputLock.unlock() }

    do {
        let encoder = JSONEncoder()
        let data = try encoder.encode(event)
        FileHandle.standardOutput.write(data)
        FileHandle.standardOutput.write(Data([10]))
    } catch {
        FileHandle.standardError.write(Data("macOS Partner helper could not encode protocol output\n".utf8))
        exit(1)
    }
}

private func sha256Hex(_ data: Data) -> String {
    SHA256.hash(data: data).map { String(format: "%02x", $0) }.joined()
}

private func providerRequestID(_ response: HTTPURLResponse) -> String? {
    for name in ["x-request-id", "request-id", "x-moonshot-request-id"] {
        if let value = response.value(forHTTPHeaderField: name), !value.isEmpty {
            return value
        }
    }
    return nil
}

private func mapURLFailure(_ error: Error) -> (String, String) {
    let ns = error as NSError
    guard ns.domain == NSURLErrorDomain else {
        return ("HOST_UNAVAILABLE", "macOS Partner host failed before a usable HTTP response")
    }
    let code = URLError.Code(rawValue: ns.code)
    switch code {
    case .cannotFindHost, .dnsLookupFailed:
        return ("DNS_FAILURE", "macOS Partner host could not resolve the provider")
    case .cannotConnectToHost:
        return ("CONNECT_FAILURE", "macOS Partner host could not connect to the provider")
    case .secureConnectionFailed,
         .serverCertificateHasBadDate,
         .serverCertificateUntrusted,
         .serverCertificateHasUnknownRoot,
         .serverCertificateNotYetValid,
         .clientCertificateRejected,
         .clientCertificateRequired:
        return ("TLS_FAILURE", "macOS Partner host TLS validation failed")
    case .timedOut:
        return ("TIMEOUT", "macOS Partner host request timed out")
    default:
        return ("HOST_UNAVAILABLE", "macOS Partner host failed before a usable HTTP response")
    }
}

private final class SessionDelegate: NSObject, URLSessionDataDelegate, URLSessionTaskDelegate {
    let providerRef: String
    let credentialRef: String
    let completion = DispatchSemaphore(value: 0)

    private let stateLock = NSLock()
    private var responseStarted = false
    private var redirectRejected = false
    private var terminalEmitted = false
    private var totalBytes = 0
    private var hasher = SHA256()

    init(providerRef: String, credentialRef: String) {
        self.providerRef = providerRef
        self.credentialRef = credentialRef
    }

    private func isTerminal() -> Bool {
        stateLock.lock()
        defer { stateLock.unlock() }
        return terminalEmitted
    }

    private func claimTerminal() -> Bool {
        stateLock.lock()
        defer { stateLock.unlock() }
        if terminalEmitted {
            return false
        }
        terminalEmitted = true
        return true
    }

    func forceTimeout() {
        guard claimTerminal() else {
            return
        }
        emit(
            ProtocolEvent(
                type: "TRANSPORT_FAILURE",
                providerRef: providerRef,
                credentialRef: credentialRef,
                failureCode: "TIMEOUT",
                detail: "macOS Partner helper exceeded its bounded execution timeout"
            )
        )
    }

    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        willPerformHTTPRedirection response: HTTPURLResponse,
        newRequest request: URLRequest,
        completionHandler: @escaping (URLRequest?) -> Void
    ) {
        redirectRejected = true
        completionHandler(nil)
    }

    func urlSession(
        _ session: URLSession,
        dataTask: URLSessionDataTask,
        didReceive response: URLResponse,
        completionHandler: @escaping (URLSession.ResponseDisposition) -> Void
    ) {
        guard let http = response as? HTTPURLResponse else {
            completionHandler(.cancel)
            return
        }

        if redirectRejected || (300...399).contains(http.statusCode) {
            redirectRejected = true
            completionHandler(.cancel)
            return
        }

        responseStarted = true
        emit(
            ProtocolEvent(
                type: "RESPONSE_START",
                providerRef: providerRef,
                credentialRef: credentialRef,
                httpStatus: http.statusCode,
                retryAfter: http.value(forHTTPHeaderField: "Retry-After"),
                providerRequestId: providerRequestID(http)
            )
        )
        completionHandler(.allow)
    }

    func urlSession(
        _ session: URLSession,
        dataTask: URLSessionDataTask,
        didReceive data: Data
    ) {
        guard responseStarted, !redirectRejected, !isTerminal() else {
            return
        }

        totalBytes += data.count
        if totalBytes > maximumResponseBodyBytes {
            if claimTerminal() {
                emit(
                    ProtocolEvent(
                        type: "TRANSPORT_FAILURE",
                        providerRef: providerRef,
                        credentialRef: credentialRef,
                        failureCode: "HOST_UNAVAILABLE",
                        detail: "macOS Partner response exceeds the bounded transport size"
                    )
                )
            }
            dataTask.cancel()
            return
        }

        hasher.update(data: data)
        emit(
            ProtocolEvent(
                type: "BODY_CHUNK",
                providerRef: providerRef,
                credentialRef: credentialRef,
                bodyChunkBase64: data.base64EncodedString()
            )
        )
    }

    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        didCompleteWithError error: Error?
    ) {
        defer {
            completion.signal()
        }

        guard claimTerminal() else {
            return
        }

        if redirectRejected {
            emit(
                ProtocolEvent(
                    type: "TRANSPORT_FAILURE",
                    providerRef: providerRef,
                    credentialRef: credentialRef,
                    failureCode: "REDIRECT_REJECTED",
                    detail: "built-in Partner provider returned a redirect"
                )
            )
            return
        }

        if let error {
            let mapped = mapURLFailure(error)
            emit(
                ProtocolEvent(
                    type: "TRANSPORT_FAILURE",
                    providerRef: providerRef,
                    credentialRef: credentialRef,
                    failureCode: mapped.0,
                    detail: mapped.1
                )
            )
            return
        }

        guard responseStarted else {
            emit(
                ProtocolEvent(
                    type: "TRANSPORT_FAILURE",
                    providerRef: providerRef,
                    credentialRef: credentialRef,
                    failureCode: "HOST_UNAVAILABLE",
                    detail: "macOS Partner host completed without an HTTP response"
                )
            )
            return
        }

        let digest = hasher.finalize().map { String(format: "%02x", $0) }.joined()
        emit(
            ProtocolEvent(
                type: "COMPLETED",
                providerRef: providerRef,
                credentialRef: credentialRef,
                responseBodySha256: digest
            )
        )
    }
}

private func execute(
    header: Header,
    body: Data
) throws {
    let provider = try ProviderDefinition.resolve(header.providerRef)
    guard let expectedRequestURL = header.expectedRequestUrl,
          let dialect = header.dialect,
          let requestBodySHA = header.requestBodySha256,
          header.redirectPolicy == "DENY" else {
        throw HostError.invalidInput("macOS Partner EXECUTE header is incomplete")
    }

    let headers = header.publicProtocolHeaders ?? [:]
    try provider.validate(
        expectedRequestURL: expectedRequestURL,
        dialect: dialect,
        headers: headers
    )

    guard requestBodySHA == sha256Hex(body) else {
        throw HostError.invalidInput("macOS Partner request body does not match its declared SHA-256")
    }

    guard var secret = try readCredential(
        providerRef: header.providerRef,
        credentialRef: header.credentialRef
    ) else {
        emit(
            ProtocolEvent(
                type: "TRANSPORT_FAILURE",
                providerRef: header.providerRef,
                credentialRef: header.credentialRef,
                failureCode: "CREDENTIAL_NOT_PRESENT",
                detail: "protected macOS Partner credential is not present"
            )
        )
        return
    }
    defer {
        if !secret.isEmpty {
            secret.resetBytes(in: 0..<secret.count)
        }
    }

    guard var token = String(data: secret, encoding: .utf8), !token.isEmpty else {
        emit(
            ProtocolEvent(
                type: "TRANSPORT_FAILURE",
                providerRef: header.providerRef,
                credentialRef: header.credentialRef,
                failureCode: "CREDENTIAL_PROVIDER_MISMATCH",
                detail: "protected macOS Partner credential is not valid UTF-8"
            )
        )
        return
    }
    defer {
        token.removeAll(keepingCapacity: false)
    }

    var request = URLRequest(
        url: provider.requestURL,
        cachePolicy: .reloadIgnoringLocalCacheData,
        timeoutInterval: 600
    )
    request.httpMethod = "POST"
    request.httpBody = body
    request.setValue("Bearer " + token, forHTTPHeaderField: "Authorization")
    for (name, value) in headers {
        request.setValue(value, forHTTPHeaderField: name)
    }

    let delegate = SessionDelegate(
        providerRef: header.providerRef,
        credentialRef: header.credentialRef
    )
    let configuration = URLSessionConfiguration.ephemeral
    configuration.httpCookieStorage = nil
    configuration.urlCache = nil
    configuration.httpShouldSetCookies = false
    configuration.requestCachePolicy = .reloadIgnoringLocalCacheData
    configuration.timeoutIntervalForRequest = 600
    configuration.timeoutIntervalForResource = 610

    let queue = OperationQueue()
    queue.maxConcurrentOperationCount = 1
    let session = URLSession(
        configuration: configuration,
        delegate: delegate,
        delegateQueue: queue
    )
    defer {
        session.invalidateAndCancel()
    }

    let task = session.dataTask(with: request)
    request.setValue(nil, forHTTPHeaderField: "Authorization")
    token.removeAll(keepingCapacity: false)
    task.resume()

    if delegate.completion.wait(timeout: .now() + .seconds(620)) == .timedOut {
        delegate.forceTimeout()
        task.cancel()
        _ = delegate.completion.wait(timeout: .now() + .seconds(5))
    }
}

do {
    let input = FileHandle.standardInput
    let headerLine = try readHeader(from: input)
    let decoder = JSONDecoder()
    let header = try decoder.decode(
        Header.self,
        from: Data(headerLine.utf8)
    )

    guard header.schemaVersion == schema else {
        throw HostError.invalidInput("macOS Partner host protocol schema mismatch")
    }
    _ = try ProviderDefinition.resolve(header.providerRef)

    switch header.operation {
    case "STORE":
        guard let count = header.secretBytes,
              count > 0,
              count <= maximumSecretBytes else {
            throw HostError.invalidInput("macOS Partner STORE secret byte count is out of bounds")
        }
        var secret = try readPayload(from: input, count: count)
        defer {
            if !secret.isEmpty {
                secret.resetBytes(in: 0..<secret.count)
            }
        }
        try storeCredential(
            providerRef: header.providerRef,
            credentialRef: header.credentialRef,
            secret: secret
        )
        emit(
            ProtocolEvent(
                type: "STORED",
                providerRef: header.providerRef,
                credentialRef: header.credentialRef,
                present: true
            )
        )

    case "OBSERVE":
        _ = try readPayload(from: input, count: 0)
        emit(
            ProtocolEvent(
                type: "OBSERVED",
                providerRef: header.providerRef,
                credentialRef: header.credentialRef,
                present: try observeCredential(
                    providerRef: header.providerRef,
                    credentialRef: header.credentialRef
                )
            )
        )

    case "DELETE":
        _ = try readPayload(from: input, count: 0)
        try deleteCredential(
            providerRef: header.providerRef,
            credentialRef: header.credentialRef
        )
        emit(
            ProtocolEvent(
                type: "DELETED",
                providerRef: header.providerRef,
                credentialRef: header.credentialRef,
                present: false
            )
        )

    case "EXECUTE":
        guard let count = header.requestBodyBytes,
              count > 0,
              count <= maximumRequestBodyBytes else {
            throw HostError.invalidInput("macOS Partner request body byte count is out of bounds")
        }
        var body = try readPayload(from: input, count: count)
        defer {
            if !body.isEmpty {
                body.resetBytes(in: 0..<body.count)
            }
        }
        try execute(
            header: header,
            body: body
        )

    default:
        throw HostError.invalidInput("macOS Partner host operation is not admitted")
    }
} catch {
    let message = "macOS Partner host failed to process the source-owned protocol\n"
    FileHandle.standardError.write(Data(message.utf8))
    exit(1)
}

// [VXG RealForever]
