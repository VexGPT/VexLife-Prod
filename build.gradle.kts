import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService

plugins {
    id("jvm-toolchains")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    jvm()

    js(IR) {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        // Keyed string source (VexArchitecture localization law) ships inside the JVM build:
        // localization/strings/source/<scope>.json -> classpath /strings/source/<scope>.json.
        jvmMain {
            resources.srcDir("localization")
        }
    }
}

fun sha256(
    path: Path,
): String {
    val digest =
        MessageDigest.getInstance(
            "SHA-256",
        )

    Files.newInputStream(
        path,
    )
        .use {
            input ->
            val buffer =
                ByteArray(
                    1024 * 1024,
                )
            while (
                true
            ) {
                val read =
                    input.read(
                        buffer,
                    )
                if (
                    read <
                    0
                ) {
                    break
                }
                digest.update(
                    buffer,
                    0,
                    read,
                )
            }
        }

    return digest
        .digest()
        .joinToString(
            "",
        ) {
            "%02x".format(
                it.toInt() and
                    0xff,
            )
        }
}

fun runExactCommand(
    command: List<String>,
): String {
    val process =
        ProcessBuilder(
            command,
        )
            .redirectErrorStream(
                true,
            )
            .start()
    val output =
        process.inputStream
            .bufferedReader(
                StandardCharsets.UTF_8,
            )
            .readText()
    val status =
        process.waitFor()

    require(
        status ==
            0,
    ) {
        "command failed ($status): " +
            command.joinToString(
                " ",
            ) +
            "\n" +
            output
    }

    return output.trim()
}

fun requiredJsonString(
    text: String,
    field: String,
): String {
    val prefix =
        "\"" +
            field +
            "\""
    val sourceLine =
        requireNotNull(
            text
                .lineSequence()
                .map(
                    String::trim,
                )
                .singleOrNull {
                    it.startsWith(
                        prefix,
                    )
                },
        ) {
            "required JSON string field is missing or duplicated: $field"
        }
    val separator =
        sourceLine.indexOf(
            ':',
        )
    require(
        separator >
            0,
    ) {
        "required JSON string field has no separator: $field"
    }

    val encoded =
        sourceLine
            .substring(
                separator +
                    1,
            )
            .trim()
            .removeSuffix(
                ",",
            )
    require(
        encoded.length >=
            2 &&
            encoded.first() ==
            '"' &&
            encoded.last() ==
            '"' &&
            '\\' !in
            encoded,
    ) {
        "required JSON string field must be one simple source-owned string: $field"
    }

    return encoded.substring(
        1,
        encoded.length -
            1,
    )
}

fun deleteTreeNoFollow(
    root: Path,
) {
    if (
        !Files.exists(
            root,
            LinkOption.NOFOLLOW_LINKS,
        )
    ) {
        return
    }

    Files.walk(
        root,
    )
        .use {
            stream ->
            stream
                .toList()
                .sortedByDescending {
                    it.nameCount
                }
                .forEach {
                    path ->
                    Files.deleteIfExists(
                        path,
                    )
                }
        }
}

fun payloadFingerprint(
    root: Path,
    excludedRelativePaths: Set<String> =
        emptySet(),
): String {
    val digest =
        MessageDigest.getInstance(
            "SHA-256",
        )

    Files.walk(
        root,
    )
        .use {
            stream ->
            stream
                .sorted()
                .forEach {
                    path ->
                    val relative =
                        root
                            .relativize(
                                path,
                            )
                            .toString()
                            .replace(
                                '\\',
                                '/',
                            )

                    if (
                        relative.isEmpty() ||
                        relative in
                        excludedRelativePaths
                    ) {
                        return@forEach
                    }

                    val permissions =
                        try {
                            Files.getPosixFilePermissions(
                                path,
                                LinkOption.NOFOLLOW_LINKS,
                            )
                                .map {
                                    it.name
                                }
                                .sorted()
                                .joinToString(
                                    ",",
                                )
                        } catch (
                            _: UnsupportedOperationException,
                        ) {
                            ""
                        }

                    val descriptor =
                        when {
                            Files.isSymbolicLink(
                                path,
                            ) ->
                                "L\u0000" +
                                    relative +
                                    "\u0000" +
                                    permissions +
                                    "\u0000" +
                                    Files.readSymbolicLink(
                                        path,
                                    )
                                        .toString()

                            Files.isRegularFile(
                                path,
                                LinkOption.NOFOLLOW_LINKS,
                            ) ->
                                "F\u0000" +
                                    relative +
                                    "\u0000" +
                                    permissions +
                                    "\u0000" +
                                    sha256(
                                        path,
                                    )

                            Files.isDirectory(
                                path,
                                LinkOption.NOFOLLOW_LINKS,
                            ) ->
                                "D\u0000" +
                                    relative +
                                    "\u0000" +
                                    permissions

                            else ->
                                error(
                                    "service image contains unsupported filesystem entry: $relative",
                                )
                        }

                    digest.update(
                        descriptor
                            .toByteArray(
                                StandardCharsets.UTF_8,
                            ),
                    )
                    digest.update(
                        0.toByte(),
                    )
                }
        }

    return digest
        .digest()
        .joinToString(
            "",
        ) {
            "%02x".format(
                it.toInt() and
                    0xff,
            )
        }
}

val jvmMainCompilation =
    kotlin.targets
        .getByName(
            "jvm",
        )
        .compilations
        .getByName(
            "main",
        )

val jvmJarTask =
    tasks.named<Jar>(
        "jvmJar",
    )

tasks.register<JavaExec>(
    "observeVexLifeProdCurrentState",
) {
    group =
        "verification"
    description =
        "Print one no-effect current machine/Home/model awareness projection before setup effects."

    dependsOn(
        jvmMainCompilation.compileTaskProvider,
    )

    classpath =
        files(
            jvmMainCompilation.output.allOutputs,
            jvmMainCompilation.runtimeDependencyFiles,
        )

    mainClass.set(
        "vexlife.service.VexLifeProdCurrentStateProcessKt",
    )
}

val serviceImageJdk21 =
    extensions
        .getByType(
            JavaToolchainService::class.java,
        )
        .compilerFor {
            languageVersion.set(
                JavaLanguageVersion.of(
                    21,
                ),
            )
        }

val serviceLauncherSource =
    layout.projectDirectory.file(
        "packaging/private-service-image/bin/vexlife-prod-service",
    )

val setupLauncherSource =
    layout.projectDirectory.file(
        "packaging/private-service-image/bin/vexlife-prod-setup",
    )

val setupWindowSource =
    layout.projectDirectory.file(
        "packaging/private-service-image/setup/vexlife-prod-setup-window.applescript",
    )

val releaseContractSource =
    layout.projectDirectory.file(
        "packaging/private-service-image/release-contract.json",
    )

val macApplicationReleaseContractSource =
    layout.projectDirectory.file(
        "packaging/macos-app/release-contract.json",
    )

val macApplicationBrandMarkSource =
    layout.projectDirectory.file(
        "packaging/macos-app/vextreme-mark.svg",
    )

val macApplicationIconArtworkSource =
    layout.projectDirectory.file(
        "packaging/macos-app/vextreme-app-icon.svg",
    )

val macApplicationVersionProperty =
    providers.gradleProperty(
        "vexlifeAppVersion",
    )

val visionRuntimeContractSource =
    layout.projectDirectory.file(
        "packaging/private-service-image/vision-runtime-contract.json",
    )

val localAsrProviderContractSource =
    layout.projectDirectory.file(
        "packaging/private-service-image/local-asr-provider-contract.json",
    )

val visionRuntimeArtifactProperty =
    providers.gradleProperty(
        "vexVisionRuntimeArtifact",
    )

val releaseContractText =
    releaseContractSource
        .asFile
        .readText(
            StandardCharsets.UTF_8,
        )

val releaseSchemaVersion =
    requiredJsonString(
        releaseContractText,
        "schemaVersion",
    )
val releaseRepository =
    requiredJsonString(
        releaseContractText,
        "repository",
    )
val releasePlatform =
    requiredJsonString(
        releaseContractText,
        "platform",
    )
val releaseCurrentChannel =
    requiredJsonString(
        releaseContractText,
        "currentChannel",
    )
val releaseTagPrefix =
    requiredJsonString(
        releaseContractText,
        "releaseTagPrefix",
    )
val releaseArchiveNamePrefix =
    requiredJsonString(
        releaseContractText,
        "archiveNamePrefix",
    )
val releaseArchiveSuffix =
    requiredJsonString(
        releaseContractText,
        "archiveSuffix",
    )
val releaseReceiptSuffix =
    requiredJsonString(
        releaseContractText,
        "receiptSuffix",
    )
val releaseBootstrapNamePrefix =
    requiredJsonString(
        releaseContractText,
        "bootstrapNamePrefix",
    )
val releaseBootstrapSuffix =
    requiredJsonString(
        releaseContractText,
        "bootstrapSuffix",
    )
val releaseCurrentBootstrapAssetName =
    requiredJsonString(
        releaseContractText,
        "currentBootstrapAssetName",
    )
val releaseCurrentBootstrapSha256AssetName =
    requiredJsonString(
        releaseContractText,
        "currentBootstrapSha256AssetName",
    )
val releaseSetupWindowRelativePath =
    requiredJsonString(
        releaseContractText,
        "setupWindowRelativePath",
    )
val releaseSetupLauncherRelativePath =
    requiredJsonString(
        releaseContractText,
        "setupLauncherRelativePath",
    )
val releaseServiceLauncherRelativePath =
    requiredJsonString(
        releaseContractText,
        "serviceLauncherRelativePath",
    )
val releaseImageManifestRelativePath =
    requiredJsonString(
        releaseContractText,
        "imageManifestRelativePath",
    )
val bootstrapTemplateSource =
    layout.projectDirectory.file(
        requiredJsonString(
            releaseContractText,
            "bootstrapTemplateRelativePath",
        ),
    )

val macApplicationReleaseContractText =
    macApplicationReleaseContractSource
        .asFile
        .readText(
            StandardCharsets.UTF_8,
        )

val macApplicationReleaseSchemaVersion =
    requiredJsonString(
        macApplicationReleaseContractText,
        "schemaVersion",
    )
val macApplicationHumanName =
    requiredJsonString(
        macApplicationReleaseContractText,
        "humanName",
    )
val macApplicationBundleIdentifier =
    requiredJsonString(
        macApplicationReleaseContractText,
        "bundleIdentifier",
    )
val macApplicationPlatform =
    requiredJsonString(
        macApplicationReleaseContractText,
        "platform",
    )
val macApplicationName =
    requiredJsonString(
        macApplicationReleaseContractText,
        "applicationName",
    )
val macApplicationDiskImageName =
    requiredJsonString(
        macApplicationReleaseContractText,
        "diskImageName",
    )
val macApplicationDiskImageVolumeIconName =
    requiredJsonString(
        macApplicationReleaseContractText,
        "diskImageVolumeIconName",
    )
val macApplicationEmbeddedImageRelativePath =
    requiredJsonString(
        macApplicationReleaseContractText,
        "embeddedImageRelativePath",
    )
val macApplicationEmbeddedSetupLauncherRelativePath =
    requiredJsonString(
        macApplicationReleaseContractText,
        "embeddedSetupLauncherRelativePath",
    )
val macApplicationDistributionTrust =
    requiredJsonString(
        macApplicationReleaseContractText,
        "distributionTrust",
    )
val macApplicationMinimumMacOSVersion =
    requiredJsonString(
        macApplicationReleaseContractText,
        "minimumMacOSVersion",
    )

require(
    macApplicationReleaseSchemaVersion ==
        "vexlife-prod.macos-application-release/v1" &&
        macApplicationHumanName ==
        "VexLife" &&
        macApplicationBundleIdentifier ==
        "com.vexlife.prod" &&
        macApplicationPlatform ==
        "darwin-arm64" &&
        macApplicationName ==
        "VexLife.app" &&
        macApplicationDiskImageName ==
        "VexLife-macOS-AppleSilicon.dmg" &&
        macApplicationEmbeddedImageRelativePath ==
        "private-service-image" &&
        macApplicationEmbeddedSetupLauncherRelativePath ==
        "private-service-image/bin/vexlife-prod-setup" &&
        macApplicationDistributionTrust ==
        "AD_HOC_SIGNATURE_NO_DEVELOPER_ID_NO_NOTARIZATION" &&
        macApplicationMinimumMacOSVersion ==
        "14.0"
) {
    "VexLife-Prod macOS application release contract identity drift"
}

val visionRuntimeContractText =
    visionRuntimeContractSource
        .asFile
        .readText(
            StandardCharsets.UTF_8,
        )

val visionRuntimeSchemaVersion =
    requiredJsonString(
        visionRuntimeContractText,
        "schemaVersion",
    )
val visionRuntimePlatform =
    requiredJsonString(
        visionRuntimeContractText,
        "platform",
    )
val visionRuntimeArtifactTruthClass =
    requiredJsonString(
        visionRuntimeContractText,
        "artifactTruthClass",
    )
val visionRuntimeReleaseDisposition =
    requiredJsonString(
        visionRuntimeContractText,
        "releaseDisposition",
    )
val visionRuntimeArchiveName =
    requiredJsonString(
        visionRuntimeContractText,
        "archiveName",
    )
val visionRuntimeArchiveSha256 =
    requiredJsonString(
        visionRuntimeContractText,
        "archiveSha256",
    )
val visionRuntimeArtifactManifestRelativePath =
    requiredJsonString(
        visionRuntimeContractText,
        "artifactManifestRelativePath",
    )
val visionRuntimeArtifactManifestSha256 =
    requiredJsonString(
        visionRuntimeContractText,
        "artifactManifestSha256",
    )
val visionRuntimeSdkSourceCommit =
    requiredJsonString(
        visionRuntimeContractText,
        "sdkSourceCommit",
    )
val visionRuntimeSdkSourceTree =
    requiredJsonString(
        visionRuntimeContractText,
        "sdkSourceTree",
    )
val visionRuntimeExecutableRelativePath =
    requiredJsonString(
        visionRuntimeContractText,
        "runtimeExecutableRelativePath",
    )
val visionRuntimeExecutableSha256 =
    requiredJsonString(
        visionRuntimeContractText,
        "runtimeExecutableSha256",
    )
val visionRuntimeHelperExecutableRelativePath =
    requiredJsonString(
        visionRuntimeContractText,
        "helperExecutableRelativePath",
    )
val visionRuntimeHelperExecutableSha256 =
    requiredJsonString(
        visionRuntimeContractText,
        "helperExecutableSha256",
    )
val visionRuntimeNodeVersion =
    requiredJsonString(
        visionRuntimeContractText,
        "nodeVersion",
    )
val visionRuntimeNodeArchiveSha256 =
    requiredJsonString(
        visionRuntimeContractText,
        "nodeArchiveSha256",
    )

val localAsrProviderContractText =
    localAsrProviderContractSource
        .asFile
        .readText(
            StandardCharsets.UTF_8,
        )

val localAsrProviderSchemaVersion =
    requiredJsonString(
        localAsrProviderContractText,
        "schemaVersion",
    )
val localAsrProviderRef =
    requiredJsonString(
        localAsrProviderContractText,
        "providerRef",
    )
val localAsrProviderVersion =
    requiredJsonString(
        localAsrProviderContractText,
        "providerVersion",
    )
val localAsrProviderPlatform =
    requiredJsonString(
        localAsrProviderContractText,
        "platform",
    )
val localAsrProviderSourceRepository =
    requiredJsonString(
        localAsrProviderContractText,
        "sourceRepository",
    )
val localAsrProviderSourceCommit =
    requiredJsonString(
        localAsrProviderContractText,
        "sourceCommit",
    )
val localAsrProviderSourceTree =
    requiredJsonString(
        localAsrProviderContractText,
        "sourceTree",
    )
val localAsrProviderCmakeVersion =
    requiredJsonString(
        localAsrProviderContractText,
        "cmakeVersion",
    )
val localAsrProviderCmakeArchiveName =
    requiredJsonString(
        localAsrProviderContractText,
        "cmakeArchiveName",
    )
val localAsrProviderCmakeArchiveUrl =
    requiredJsonString(
        localAsrProviderContractText,
        "cmakeArchiveUrl",
    )
val localAsrProviderCmakeArchiveSha256 =
    requiredJsonString(
        localAsrProviderContractText,
        "cmakeArchiveSha256",
    )
val localAsrProviderRootRelativePath =
    requiredJsonString(
        localAsrProviderContractText,
        "serviceImageRootRelativePath",
    )
val localAsrProviderSourceIdentityRelativePath =
    requiredJsonString(
        localAsrProviderContractText,
        "sourceIdentityRelativePath",
    )
val localAsrProviderSourceTreeIdentityRelativePath =
    requiredJsonString(
        localAsrProviderContractText,
        "sourceTreeIdentityRelativePath",
    )
val localAsrProviderExecutableRelativePath =
    requiredJsonString(
        localAsrProviderContractText,
        "runtimeExecutableRelativePath",
    )
val localAsrProviderReleaseDisposition =
    requiredJsonString(
        localAsrProviderContractText,
        "releaseDisposition",
    )

require(
    localAsrProviderSchemaVersion ==
        "vexlife-prod.local-asr-provider-input/v1" &&
        localAsrProviderRef ==
        "provider.local-asr.whispercpp" &&
        localAsrProviderVersion ==
        "v1.9.5" &&
        localAsrProviderPlatform ==
        "darwin-arm64" &&
        localAsrProviderSourceRepository ==
        "ggml-org/whisper.cpp" &&
        localAsrProviderSourceCommit ==
        "d1be6fde11ac6e0407606b4e42fe72d34add8037" &&
        localAsrProviderSourceTree ==
        "6fe2755682242a5c01aba65a22b3c0305d747841" &&
        localAsrProviderCmakeVersion ==
        "4.5.0" &&
        localAsrProviderCmakeArchiveName ==
        "cmake-4.5.0-macos-universal.tar.gz" &&
        localAsrProviderCmakeArchiveUrl.startsWith(
            "https://github.com/Kitware/CMake/releases/download/",
        ) &&
        localAsrProviderCmakeArchiveSha256.matches(
            Regex(
                "[0-9a-f]{64}",
            ),
        ) &&
        localAsrProviderRootRelativePath ==
        "local-asr-provider/whispercpp/v1.9.5" &&
        localAsrProviderSourceIdentityRelativePath ==
        localAsrProviderRootRelativePath +
        "/provider-source.txt" &&
        localAsrProviderSourceTreeIdentityRelativePath ==
        localAsrProviderRootRelativePath +
        "/provider-tree.txt" &&
        localAsrProviderExecutableRelativePath ==
        localAsrProviderRootRelativePath +
        "/bin/whisper-server" &&
        localAsrProviderReleaseDisposition ==
        "BUNDLED_PRIVATE_SERVICE_IMAGE"
) {
    "VexLife-Prod Local-ASR provider input contract identity drift"
}

require(
    visionRuntimeSchemaVersion ==
        "vexlife-prod.vision-runtime-input/v1" &&
        visionRuntimePlatform ==
        "darwin-arm64" &&
        visionRuntimeArtifactTruthClass ==
        "LOCAL_QUALIFIED_NOT_DISTRIBUTABLE" &&
        visionRuntimeReleaseDisposition ==
        "LOCAL_ONLY_NO_RELEASE" &&
        visionRuntimeArchiveName.matches(
            Regex(
                "[A-Za-z0-9._-]+\\.tar\\.xz",
            ),
        ) &&
        visionRuntimeArchiveSha256.matches(
            Regex(
                "[0-9a-f]{64}",
            ),
        ) &&
        visionRuntimeArtifactManifestRelativePath ==
        "vision-runtime/vision-runtime.json" &&
        visionRuntimeArtifactManifestSha256.matches(
            Regex(
                "[0-9a-f]{64}",
            ),
        ) &&
        visionRuntimeSdkSourceCommit.matches(
            Regex(
                "[0-9a-f]{40}",
            ),
        ) &&
        visionRuntimeSdkSourceTree.matches(
            Regex(
                "[0-9a-f]{40}",
            ),
        ) &&
        visionRuntimeExecutableRelativePath ==
        "vision-runtime/bin/vexlife-vision-runtime" &&
        visionRuntimeExecutableSha256.matches(
            Regex(
                "[0-9a-f]{64}",
            ),
        ) &&
        visionRuntimeHelperExecutableRelativePath ==
        "vision-runtime/helper/VexLifeVisionSelectedScopeAgent.app/Contents/MacOS/VexLifeVisionSelectedScopeAgent" &&
        visionRuntimeHelperExecutableSha256.matches(
            Regex(
                "[0-9a-f]{64}",
            ),
        ) &&
        visionRuntimeNodeVersion ==
        "v26.8.1" &&
        visionRuntimeNodeArchiveSha256.matches(
            Regex(
                "[0-9a-f]{64}",
            ),
        )
) {
    "VexLife-Prod Vision runtime input contract identity drift"
}

require(
    releaseSchemaVersion ==
        "vexlife-prod.private-service-release/v1" &&
        releaseRepository ==
        "VexGPT/VexLife-Prod" &&
        releasePlatform ==
        "darwin-arm64" &&
        releaseCurrentChannel ==
        "github-release-latest" &&
        releaseCurrentBootstrapAssetName.matches(
            Regex(
                "[A-Za-z0-9._-]+\\.command",
            ),
        ) &&
        releaseCurrentBootstrapSha256AssetName ==
        releaseCurrentBootstrapAssetName +
        ".sha256" &&
        releaseSetupWindowRelativePath ==
        "setup/vexlife-prod-setup-window.applescript" &&
        releaseSetupLauncherRelativePath ==
        "bin/vexlife-prod-setup" &&
        releaseServiceLauncherRelativePath ==
        "bin/vexlife-prod-service" &&
        releaseImageManifestRelativePath ==
        "service-image.json",
) {
    "VexLife-Prod private-service release contract identity drift"
}

val macLocalAsrProviderOutputRoot =
    layout.buildDirectory.dir(
        "vexlife-local-asr-provider/darwin-arm64",
    )

val formVexLifeProdMacLocalAsrProvider =
    tasks.register(
        "formVexLifeProdMacLocalAsrProvider",
    ) {
        group =
            "distribution"
        description =
            "Form the exact pinned macOS arm64 whisper.cpp provider for the private service image."

        inputs.file(
            localAsrProviderContractSource,
        )
        outputs.dir(
            macLocalAsrProviderOutputRoot,
        )
        outputs.upToDateWhen {
            false
        }

        doLast {
            require(
                System.getProperty(
                    "os.name",
                ) ==
                    "Mac OS X" &&
                    System.getProperty(
                        "os.arch",
                    ) in
                    setOf(
                        "aarch64",
                        "arm64",
                    ),
            ) {
                "Mac Local-ASR provider must be formed on macOS arm64"
            }

            val output =
                macLocalAsrProviderOutputRoot
                    .get()
                    .asFile
                    .toPath()
                    .toAbsolutePath()
                    .normalize()
            if (
                Files.exists(
                    output,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                deleteTreeNoFollow(
                    output,
                )
            }
            Files.createDirectories(
                output,
            )

            val work =
                layout.buildDirectory
                    .dir(
                        "tmp/vexlife-local-asr-provider",
                    )
                    .get()
                    .asFile
                    .toPath()
                    .toAbsolutePath()
                    .normalize()
            if (
                Files.exists(
                    work,
                    LinkOption.NOFOLLOW_LINKS,
                )
            ) {
                deleteTreeNoFollow(
                    work,
                )
            }
            Files.createDirectories(
                work,
            )

            val cmakeArchive =
                work.resolve(
                    localAsrProviderCmakeArchiveName,
                )
            runExactCommand(
                listOf(
                    "/usr/bin/curl",
                    "-fL",
                    "--retry",
                    "3",
                    "--retry-delay",
                    "2",
                    "--connect-timeout",
                    "20",
                    localAsrProviderCmakeArchiveUrl,
                    "-o",
                    cmakeArchive.toString(),
                ),
            )
            require(
                Files.isRegularFile(
                    cmakeArchive,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    sha256(
                        cmakeArchive,
                    ) ==
                    localAsrProviderCmakeArchiveSha256
            ) {
                "Mac Local-ASR CMake archive identity drift"
            }

            val cmakeRoot =
                work.resolve(
                    "cmake",
                )
            Files.createDirectories(
                cmakeRoot,
            )
            runExactCommand(
                listOf(
                    "/usr/bin/tar",
                    "-xzf",
                    cmakeArchive.toString(),
                    "-C",
                    cmakeRoot.toString(),
                ),
            )
            val cmake =
                Files.walk(
                    cmakeRoot,
                )
                    .use {
                        stream ->
                        stream
                            .filter {
                                candidate ->
                                Files.isRegularFile(
                                    candidate,
                                    LinkOption.NOFOLLOW_LINKS,
                                ) &&
                                    Files.isExecutable(
                                        candidate,
                                    ) &&
                                    candidate
                                        .toString()
                                        .replace(
                                            '\\',
                                            '/',
                                        )
                                        .endsWith(
                                            "/CMake.app/Contents/bin/cmake",
                                        )
                            }
                            .toList()
                            .singleOrNull()
                    }
                    ?: error(
                        "pinned Mac Local-ASR CMake executable was not formed",
                    )

            val providerSource =
                work.resolve(
                    "whisper.cpp",
                )
            runExactCommand(
                listOf(
                    "/usr/bin/git",
                    "clone",
                    "--no-checkout",
                    "--filter=blob:none",
                    "https://github.com/" +
                        localAsrProviderSourceRepository +
                        ".git",
                    providerSource.toString(),
                ),
            )
            runExactCommand(
                listOf(
                    "/usr/bin/git",
                    "-C",
                    providerSource.toString(),
                    "checkout",
                    "--detach",
                    localAsrProviderSourceCommit,
                ),
            )
            require(
                runExactCommand(
                    listOf(
                        "/usr/bin/git",
                        "-C",
                        providerSource.toString(),
                        "rev-parse",
                        "HEAD",
                    ),
                ) ==
                    localAsrProviderSourceCommit &&
                    runExactCommand(
                        listOf(
                            "/usr/bin/git",
                            "-C",
                            providerSource.toString(),
                            "rev-parse",
                            "HEAD^{tree}",
                        ),
                    ) ==
                    localAsrProviderSourceTree
            ) {
                "Mac Local-ASR whisper.cpp source identity drift"
            }

            val providerBuild =
                providerSource.resolve(
                    "build",
                )
            runExactCommand(
                listOf(
                    cmake.toString(),
                    "-S",
                    providerSource.toString(),
                    "-B",
                    providerBuild.toString(),
                    "-DCMAKE_BUILD_TYPE=Release",
                    "-DWHISPER_BUILD_TESTS=OFF",
                    "-DWHISPER_BUILD_SERVER=ON",
                    "-DWHISPER_BUILD_EXAMPLES=ON",
                ),
            )
            runExactCommand(
                listOf(
                    cmake.toString(),
                    "--build",
                    providerBuild.toString(),
                    "--config",
                    "Release",
                    "--target",
                    "whisper-cli",
                    "whisper-server",
                    "--parallel",
                    Runtime
                        .getRuntime()
                        .availableProcessors()
                        .coerceAtLeast(
                            1,
                        )
                        .toString(),
                ),
            )

            val builtBin =
                providerBuild.resolve(
                    "bin",
                )
            val builtServer =
                builtBin.resolve(
                    "whisper-server",
                )
            require(
                Files.isDirectory(
                    builtBin,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    Files.isRegularFile(
                        builtServer,
                        LinkOption.NOFOLLOW_LINKS,
                    ) &&
                    Files.isExecutable(
                        builtServer,
                    )
            ) {
                "Mac Local-ASR whisper-server was not formed"
            }

            val outputBin =
                output.resolve(
                    "bin",
                )
            runExactCommand(
                listOf(
                    "/usr/bin/ditto",
                    builtBin.toString(),
                    outputBin.toString(),
                ),
            )
            Files.writeString(
                output.resolve(
                    "provider-source.txt",
                ),
                localAsrProviderSourceCommit +
                    "\n",
                StandardCharsets.UTF_8,
            )
            Files.writeString(
                output.resolve(
                    "provider-tree.txt",
                ),
                localAsrProviderSourceTree +
                    "\n",
                StandardCharsets.UTF_8,
            )
            Files.copy(
                providerSource.resolve(
                    "LICENSE",
                ),
                output.resolve(
                    "LICENSE.whisper.cpp",
                ),
                StandardCopyOption.REPLACE_EXISTING,
            )

            val packagedServer =
                output.resolve(
                    "bin/whisper-server",
                )
            require(
                Files.isRegularFile(
                    packagedServer,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    Files.isExecutable(
                        packagedServer,
                    )
            ) {
                "Mac Local-ASR packaged whisper-server is not executable"
            }
            runExactCommand(
                listOf(
                    "/usr/bin/env",
                    "DYLD_LIBRARY_PATH=" +
                        outputBin,
                    packagedServer.toString(),
                    "--help",
                ),
            )

            println(
                "VEXLIFE_PROD_MAC_LOCAL_ASR_PROVIDER\t" +
                    localAsrProviderSourceCommit +
                    "\t" +
                    localAsrProviderSourceTree +
                    "\t" +
                    output,
            )
        }
    }

val serviceImageOutputRoot =
    layout.buildDirectory.dir(
        "vexlife-prod-service-image/images",
    )

val serviceImageSourceIdentityFile =
    layout.buildDirectory.file(
        "vexlife-prod-service-image/source-identity.txt",
    )

val verifyVexLifeProdServiceImageSource =
    tasks.register(
        "verifyVexLifeProdServiceImageSource",
    ) {
        group =
            "verification"
        description =
            "Witness one clean exact VexLife-Prod Git source identity before service-image assembly."

        outputs.file(
            serviceImageSourceIdentityFile,
        )
        outputs.upToDateWhen {
            false
        }

        doLast {
            val repositoryRoot =
                rootDir
                    .toPath()
                    .toAbsolutePath()
                    .normalize()
            val sourceSha =
                runExactCommand(
                    listOf(
                        "git",
                        "-C",
                        repositoryRoot.toString(),
                        "rev-parse",
                        "HEAD",
                    ),
                )
            require(
                sourceSha.matches(
                    Regex(
                        "[0-9a-f]{40}",
                    ),
                ),
            ) {
                "current VexLife-Prod checkout did not resolve to one exact Git commit"
            }

            val status =
                runExactCommand(
                    listOf(
                        "git",
                        "-C",
                        repositoryRoot.toString(),
                        "status",
                        "--porcelain=v1",
                        "--untracked-files=all",
                    ),
                )
            require(
                status.isEmpty(),
            ) {
                "VexLife-Prod service image requires a clean exact-source checkout:\n" +
                    status
            }

            val identityFile =
                serviceImageSourceIdentityFile
                    .get()
                    .asFile
                    .toPath()
            Files.createDirectories(
                requireNotNull(
                    identityFile.parent,
                ),
            )
            Files.writeString(
                identityFile,
                sourceSha +
                    "\n",
                StandardCharsets.UTF_8,
            )
        }
    }

jvmJarTask.configure {
    mustRunAfter(
        verifyVexLifeProdServiceImageSource,
    )
}

tasks.register(
    "assembleVexLifeProdServiceImage",
) {
    group =
        "distribution"
    description =
        "Build the private self-contained macOS arm64 VexLife-Prod service image."

    dependsOn(
        verifyVexLifeProdServiceImageSource,
        jvmJarTask,
        formVexLifeProdMacLocalAsrProvider,
    )

    inputs.file(
        jvmJarTask.flatMap {
            it.archiveFile
        },
    )
    inputs.files(
        jvmMainCompilation.runtimeDependencyFiles,
    )
    inputs.file(
        serviceLauncherSource,
    )
    inputs.file(
        setupLauncherSource,
    )
    inputs.file(
        setupWindowSource,
    )
    inputs.file(
        localAsrProviderContractSource,
    )
    inputs.dir(
        macLocalAsrProviderOutputRoot,
    )
    inputs.file(
        visionRuntimeContractSource,
    )
    inputs.property(
        "vexVisionRuntimeArtifact",
        visionRuntimeArtifactProperty.orElse(
            "",
        ),
    )
    outputs.dir(
        serviceImageOutputRoot,
    )
    outputs.upToDateWhen {
        false
    }

    doLast {
        val repositoryRoot =
            rootDir
                .toPath()
                .toAbsolutePath()
                .normalize()
        val admittedSourceSha =
            Files.readString(
                serviceImageSourceIdentityFile
                    .get()
                    .asFile
                    .toPath(),
                StandardCharsets.UTF_8,
            )
                .trim()
        val sourceSha =
            runExactCommand(
                listOf(
                    "git",
                    "-C",
                    repositoryRoot.toString(),
                    "rev-parse",
                    "HEAD",
                ),
            )

        require(
            sourceSha ==
                admittedSourceSha &&
                sourceSha.matches(
                    Regex(
                        "[0-9a-f]{40}",
                    ),
                ),
        ) {
            "VexLife-Prod source identity changed after package-source admission"
        }

        val worktreeStatus =
            runExactCommand(
                listOf(
                    "git",
                    "-C",
                    repositoryRoot.toString(),
                    "status",
                    "--porcelain=v1",
                    "--untracked-files=all",
                ),
            )
        require(
            worktreeStatus.isEmpty(),
        ) {
            "VexLife-Prod source changed during service-image assembly:\n" +
                worktreeStatus
        }

        require(
            System.getProperty(
                "os.name",
            ) ==
                "Mac OS X",
        ) {
            "VexLife-Prod service image must be assembled on macOS"
        }
        require(
            System.getProperty(
                "os.arch",
            ) in
                setOf(
                    "aarch64",
                    "arm64",
                ),
        ) {
            "VexLife-Prod service image must be assembled on macOS arm64"
        }
        val serviceImageJava =
            serviceImageJdk21
                .get()
        require(
            serviceImageJava
                .metadata
                .languageVersion
                .asInt() ==
                21,
        ) {
            "VexLife-Prod service image requires an exact JDK 21 toolchain"
        }

        val javaHome =
            serviceImageJava
                .metadata
                .installationPath
                .asFile
                .toPath()
                .toAbsolutePath()
                .normalize()
        val jdeps =
            javaHome.resolve(
                "bin/jdeps",
            )
        val jlink =
            javaHome.resolve(
                "bin/jlink",
            )

        require(
            Files.isExecutable(
                jdeps,
            ) &&
                Files.isExecutable(
                    jlink,
                ),
        ) {
            "JDK 21 jdeps/jlink tools are required"
        }

        val appJar =
            jvmJarTask
                .get()
                .archiveFile
                .get()
                .asFile
                .toPath()
                .toAbsolutePath()
                .normalize()

        val runtimeDependencies =
            requireNotNull(
                jvmMainCompilation.runtimeDependencyFiles,
            ) {
                "JVM service runtime dependency collection is unavailable"
            }
                .files
                .map {
                    it.toPath()
                        .toAbsolutePath()
                        .normalize()
                }
                .sortedBy {
                    it.fileName
                        .toString()
                }

        require(
            runtimeDependencies.isNotEmpty(),
        ) {
            "JVM service runtime dependencies are empty"
        }
        runtimeDependencies.forEach {
            dependency ->
            require(
                Files.isRegularFile(
                    dependency,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    dependency.fileName
                        .toString()
                        .endsWith(
                            ".jar",
                        ),
            ) {
                "service runtime dependency must be one regular JAR: $dependency"
            }
        }

        val duplicateDependencyNames =
            runtimeDependencies
                .groupBy {
                    it.fileName
                        .toString()
                }
                .filterValues {
                    it.size >
                        1
                }
                .keys
        require(
            duplicateDependencyNames.isEmpty(),
        ) {
            "service runtime dependency filenames collide: " +
                duplicateDependencyNames
                    .sorted()
                    .joinToString(
                        ", ",
                    )
        }

        val classpath =
            runtimeDependencies.joinToString(
                separator =
                    File.pathSeparator,
            )

        val modules =
            runExactCommand(
                listOf(
                    jdeps.toString(),
                    "--multi-release",
                    "21",
                    "--print-module-deps",
                    "--class-path",
                    classpath,
                    appJar.toString(),
                ),
            )
                .lineSequence()
                .lastOrNull {
                    it.isNotBlank()
                }
                ?.trim()
                ?: error(
                    "jdeps produced no module dependency set",
                )

        require(
            modules.matches(
                Regex(
                    "[A-Za-z0-9_.]+(?:,[A-Za-z0-9_.]+)*",
                ),
            ),
        ) {
            "jdeps produced an invalid module dependency set: $modules"
        }

        val runtimeProviderModules =
            setOf(
                "jdk.crypto.ec",
            )
        val moduleSet =
            (
                modules
                    .split(
                        ",",
                    )
                    .toSet() +
                    runtimeProviderModules
            )
                .sorted()
        require(
            "jdk.httpserver" in
                moduleSet &&
                "java.net.http" in
                moduleSet &&
                "jdk.crypto.ec" in
                moduleSet,
        ) {
            "service runtime image must include jdk.httpserver + java.net.http + Java 21 SunEC provider"
        }

        val output =
            serviceImageOutputRoot
                .get()
                .asFile
                .toPath()
                .resolve(
                    sourceSha,
                )
                .toAbsolutePath()
                .normalize()

        if (
            Files.exists(
                output,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            deleteTreeNoFollow(
                output,
            )
        }

        val runtimeRoot =
            output.resolve(
                "runtime",
            )
        val libRoot =
            output.resolve(
                "lib",
            )
        val binRoot =
            output.resolve(
                "bin",
            )
        val setupRoot =
            output.resolve(
                "setup",
            )

        Files.createDirectories(
            output,
        )
        Files.createDirectories(
            libRoot,
        )
        Files.createDirectories(
            binRoot,
        )
        Files.createDirectories(
            setupRoot,
        )

        runExactCommand(
            listOf(
                jlink.toString(),
                "--add-modules",
                moduleSet.joinToString(
                    ",",
                ),
                "--strip-debug",
                "--no-header-files",
                "--no-man-pages",
                "--output",
                runtimeRoot.toString(),
            ),
        )

        val appJarTarget =
            libRoot.resolve(
                "vexlife-prod.jar",
            )
        Files.copy(
            appJar,
            appJarTarget,
            StandardCopyOption.REPLACE_EXISTING,
        )

        runtimeDependencies.forEach {
            dependency ->
            Files.copy(
                dependency,
                libRoot.resolve(
                    dependency.fileName,
                ),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }

        val launcher =
            binRoot.resolve(
                "vexlife-prod-service",
            )
        Files.copy(
            serviceLauncherSource
                .asFile
                .toPath(),
            launcher,
            StandardCopyOption.REPLACE_EXISTING,
        )
        Files.setPosixFilePermissions(
            launcher,
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            ),
        )

        val setupLauncher =
            binRoot.resolve(
                "vexlife-prod-setup",
            )
        Files.copy(
            setupLauncherSource
                .asFile
                .toPath(),
            setupLauncher,
            StandardCopyOption.REPLACE_EXISTING,
        )
        Files.setPosixFilePermissions(
            setupLauncher,
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            ),
        )

        val setupWindow =
            setupRoot.resolve(
                "vexlife-prod-setup-window.applescript",
            )
        Files.copy(
            setupWindowSource
                .asFile
                .toPath(),
            setupWindow,
            StandardCopyOption.REPLACE_EXISTING,
        )

        val localAsrProviderInput =
            macLocalAsrProviderOutputRoot
                .get()
                .asFile
                .toPath()
                .toAbsolutePath()
                .normalize()
        val localAsrProviderRoot =
            output
                .resolve(
                    localAsrProviderRootRelativePath,
                )
                .normalize()
        require(
            localAsrProviderRoot.startsWith(
                output,
            ) &&
                Files.isDirectory(
                    localAsrProviderInput,
                    LinkOption.NOFOLLOW_LINKS,
                )
        ) {
            "Mac Local-ASR provider input is unavailable or escaped the private service image"
        }
        Files.createDirectories(
            requireNotNull(
                localAsrProviderRoot.parent,
            ),
        )
        runExactCommand(
            listOf(
                "/usr/bin/ditto",
                localAsrProviderInput.toString(),
                localAsrProviderRoot.toString(),
            ),
        )

        val localAsrProviderSourceIdentity =
            output.resolve(
                localAsrProviderSourceIdentityRelativePath,
            )
        val localAsrProviderSourceTreeIdentity =
            output.resolve(
                localAsrProviderSourceTreeIdentityRelativePath,
            )
        val localAsrProviderExecutable =
            output.resolve(
                localAsrProviderExecutableRelativePath,
            )
        require(
            Files.isRegularFile(
                localAsrProviderSourceIdentity,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                Files.readString(
                    localAsrProviderSourceIdentity,
                    StandardCharsets.UTF_8,
                )
                    .trim() ==
                localAsrProviderSourceCommit &&
                Files.isRegularFile(
                    localAsrProviderSourceTreeIdentity,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                Files.readString(
                    localAsrProviderSourceTreeIdentity,
                    StandardCharsets.UTF_8,
                )
                    .trim() ==
                localAsrProviderSourceTree &&
                Files.isRegularFile(
                    localAsrProviderExecutable,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                Files.isExecutable(
                    localAsrProviderExecutable,
                )
        ) {
            "bundled Mac Local-ASR provider identity drift"
        }

        val localAsrProviderPayloadSha256 =
            payloadFingerprint(
                localAsrProviderRoot,
            )
        val localAsrProviderExecutableSha256 =
            sha256(
                localAsrProviderExecutable,
            )
        val localAsrProviderManifestJson =
            buildString {
                appendLine(
                    "{",
                )
                appendLine(
                    "    \"providerRef\": \"" +
                        localAsrProviderRef +
                        "\",",
                )
                appendLine(
                    "    \"providerVersion\": \"" +
                        localAsrProviderVersion +
                        "\",",
                )
                appendLine(
                    "    \"releaseDisposition\": \"" +
                        localAsrProviderReleaseDisposition +
                        "\",",
                )
                appendLine(
                    "    \"sourceRepository\": \"" +
                        localAsrProviderSourceRepository +
                        "\",",
                )
                appendLine(
                    "    \"sourceCommit\": \"" +
                        localAsrProviderSourceCommit +
                        "\",",
                )
                appendLine(
                    "    \"sourceTree\": \"" +
                        localAsrProviderSourceTree +
                        "\",",
                )
                appendLine(
                    "    \"providerRoot\": \"" +
                        localAsrProviderRootRelativePath +
                        "\",",
                )
                appendLine(
                    "    \"providerPayloadSha256\": \"" +
                        localAsrProviderPayloadSha256 +
                        "\",",
                )
                appendLine(
                    "    \"runtimeExecutable\": \"" +
                        localAsrProviderExecutableRelativePath +
                        "\",",
                )
                append(
                    "    \"runtimeExecutableSha256\": \"" +
                        localAsrProviderExecutableSha256 +
                        "\"\n  }",
                )
            }

        var visionRuntimeManifestJson =
            "null"
        val visionRuntimeArtifactInput =
            visionRuntimeArtifactProperty
                .orNull
                ?.trim()
                ?.takeIf(
                    String::isNotEmpty,
                )

        if (
            visionRuntimeArtifactInput !=
            null
        ) {
            val visionArchive =
                Path.of(
                    visionRuntimeArtifactInput,
                )
                    .toAbsolutePath()
                    .normalize()

            require(
                Files.isRegularFile(
                    visionArchive,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    !Files.isSymbolicLink(
                        visionArchive,
                    ) &&
                    visionArchive.fileName
                        .toString() ==
                    visionRuntimeArchiveName &&
                    sha256(
                        visionArchive,
                    ) ==
                    visionRuntimeArchiveSha256
            ) {
                "Vision runtime artifact does not match the exact committed input contract"
            }

            val archiveMembers =
                runExactCommand(
                    listOf(
                        "/usr/bin/tar",
                        "-tJf",
                        visionArchive.toString(),
                    ),
                )
                    .lineSequence()
                    .map(
                        String::trim,
                    )
                    .filter(
                        String::isNotEmpty,
                    )
                    .toList()

            require(
                archiveMembers.isNotEmpty() &&
                    archiveMembers.all {
                        member ->
                        (
                            member ==
                            "vision-runtime" ||
                                member.startsWith(
                                    "vision-runtime/",
                                )
                        ) &&
                            !member.startsWith(
                                "/",
                            ) &&
                            !member.contains(
                                "\\",
                            ) &&
                            !member.contains(
                                "//",
                            ) &&
                            member
                                .split(
                                    "/",
                                )
                                .none {
                                    segment ->
                                    segment ==
                                        ".."
                                }
                    }
            ) {
                "Vision runtime archive contains a path outside vision-runtime/"
            }

            runExactCommand(
                listOf(
                    "/usr/bin/tar",
                    "-xJf",
                    visionArchive.toString(),
                    "-C",
                    output.toString(),
                ),
            )

            val artifactManifest =
                output.resolve(
                    visionRuntimeArtifactManifestRelativePath,
                )
            require(
                Files.isRegularFile(
                    artifactManifest,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    !Files.isSymbolicLink(
                        artifactManifest,
                    ) &&
                    sha256(
                        artifactManifest,
                    ) ==
                    visionRuntimeArtifactManifestSha256
            ) {
                "Vision runtime internal manifest identity drift"
            }

            val artifactManifestText =
                Files.readString(
                    artifactManifest,
                    StandardCharsets.UTF_8,
                )
            require(
                requiredJsonString(
                    artifactManifestText,
                    "schemaVersion",
                ) ==
                    "vexlife-vision.product-runtime-artifact/v1" &&
                    requiredJsonString(
                        artifactManifestText,
                        "truthClass",
                    ) ==
                    visionRuntimeArtifactTruthClass &&
                    requiredJsonString(
                        artifactManifestText,
                        "platform",
                    ) ==
                    visionRuntimePlatform &&
                    requiredJsonString(
                        artifactManifestText,
                        "sourceCommit",
                    ) ==
                    visionRuntimeSdkSourceCommit &&
                    requiredJsonString(
                        artifactManifestText,
                        "sourceTree",
                    ) ==
                    visionRuntimeSdkSourceTree &&
                    requiredJsonString(
                        artifactManifestText,
                        "nodeVersion",
                    ) ==
                    visionRuntimeNodeVersion &&
                    requiredJsonString(
                        artifactManifestText,
                        "nodeArchiveSha256",
                    ) ==
                    visionRuntimeNodeArchiveSha256
            ) {
                "Vision runtime internal manifest does not match the committed Product contract"
            }

            val visionRuntimeExecutable =
                output.resolve(
                    visionRuntimeExecutableRelativePath,
                )
            val visionRuntimeHelperExecutable =
                output.resolve(
                    visionRuntimeHelperExecutableRelativePath,
                )

            require(
                Files.isRegularFile(
                    visionRuntimeExecutable,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    !Files.isSymbolicLink(
                        visionRuntimeExecutable,
                    ) &&
                    Files.isExecutable(
                        visionRuntimeExecutable,
                    ) &&
                    sha256(
                        visionRuntimeExecutable,
                    ) ==
                    visionRuntimeExecutableSha256 &&
                    Files.isRegularFile(
                        visionRuntimeHelperExecutable,
                        LinkOption.NOFOLLOW_LINKS,
                    ) &&
                    !Files.isSymbolicLink(
                        visionRuntimeHelperExecutable,
                    ) &&
                    Files.isExecutable(
                        visionRuntimeHelperExecutable,
                    ) &&
                    sha256(
                        visionRuntimeHelperExecutable,
                    ) ==
                    visionRuntimeHelperExecutableSha256
            ) {
                "Vision runtime executable/helper identity drift"
            }

            visionRuntimeManifestJson =
                buildString {
                    appendLine(
                        "{",
                    )
                    appendLine(
                        "    \"truthClass\": \"" +
                            visionRuntimeArtifactTruthClass +
                            "\",",
                    )
                    appendLine(
                        "    \"releaseDisposition\": \"" +
                            visionRuntimeReleaseDisposition +
                            "\",",
                    )
                    appendLine(
                        "    \"sdkSourceCommit\": \"" +
                            visionRuntimeSdkSourceCommit +
                            "\",",
                    )
                    appendLine(
                        "    \"sdkSourceTree\": \"" +
                            visionRuntimeSdkSourceTree +
                            "\",",
                    )
                    appendLine(
                        "    \"archiveSha256\": \"" +
                            visionRuntimeArchiveSha256 +
                            "\",",
                    )
                    appendLine(
                        "    \"manifestSha256\": \"" +
                            visionRuntimeArtifactManifestSha256 +
                            "\",",
                    )
                    appendLine(
                        "    \"runtimeExecutable\": \"" +
                            visionRuntimeExecutableRelativePath +
                            "\",",
                    )
                    appendLine(
                        "    \"runtimeExecutableSha256\": \"" +
                            visionRuntimeExecutableSha256 +
                            "\",",
                    )
                    appendLine(
                        "    \"helperExecutable\": \"" +
                            visionRuntimeHelperExecutableRelativePath +
                            "\",",
                    )
                    append(
                        "    \"helperExecutableSha256\": \"" +
                            visionRuntimeHelperExecutableSha256 +
                            "\"\n  }",
                    )
                }
        }

        val payloadSha256 =
            payloadFingerprint(
                output,
                excludedRelativePaths =
                    setOf(
                        "service-image.json",
                    ),
            )

        val artifacts =
            (
                listOf(
                    appJarTarget,
                ) +
                    runtimeDependencies.map {
                        libRoot.resolve(
                            it.fileName,
                        )
                    }
            )
                .sortedBy {
                    it.fileName
                        .toString()
                }

        val artifactJson =
            artifacts.joinToString(
                separator =
                    ",\n",
            ) {
                artifact ->
                "    {\"path\":\"lib/" +
                    artifact.fileName +
                    "\",\"sha256\":\"" +
                    sha256(
                        artifact,
                    ) +
                    "\",\"bytes\":" +
                    Files.size(
                        artifact,
                    ) +
                    "}"
            }

        val moduleJson =
            moduleSet
                .joinToString(
                    separator =
                        ",",
                ) {
                    module ->
                    "\"" +
                        module +
                        "\""
                }

        val manifest =
            buildString {
                appendLine(
                    "{",
                )
                appendLine(
                    "  \"schemaVersion\": \"" +
                        "vexlife-prod.private-service-image/v5" +
                        "\",",
                )
                appendLine(
                    "  \"truthClass\": \"SOURCE_BOUND_PRIVATE_SERVICE_IMAGE\",",
                )
                appendLine(
                    "  \"sourceSha\": \"" +
                        sourceSha +
                        "\",",
                )
                appendLine(
                    "  \"platform\": \"darwin-arm64\",",
                )
                appendLine(
                    "  \"javaRuntimeMajor\": 21,",
                )
                appendLine(
                    "  \"mainClass\": \"vexlife.service.VexLifeProdServiceProcessKt\",",
                )
                appendLine(
                    "  \"launcher\": \"bin/vexlife-prod-service\",",
                )
                appendLine(
                    "  \"setupLauncher\": \"bin/vexlife-prod-setup\",",
                )
                appendLine(
                    "  \"setupWindow\": \"setup/vexlife-prod-setup-window.applescript\",",
                )
                appendLine(
                    "  \"runtimeModules\": [" +
                        moduleJson +
                        "],",
                )
                appendLine(
                    "  \"payloadSha256\": \"" +
                        payloadSha256 +
                        "\",",
                )
                appendLine(
                    "  \"artifacts\": [",
                )
                appendLine(
                    artifactJson,
                )
                appendLine(
                    "  ],",
                )
                appendLine(
                    "  \"localAsrProvider\": " +
                        localAsrProviderManifestJson +
                        ",",
                )
                appendLine(
                    "  \"visionRuntime\": " +
                        visionRuntimeManifestJson,
                )
                appendLine(
                    "}",
                )
            }

        Files.writeString(
            output.resolve(
                "service-image.json",
            ),
            manifest,
            StandardCharsets.UTF_8,
        )

        println(
            "VEXLIFE_PROD_SERVICE_IMAGE\t" +
                sourceSha +
                "\t" +
                output +
                "\t" +
                payloadSha256,
        )
    }
}

val serviceArchiveOutputRoot =
    layout.buildDirectory.dir(
        "vexlife-prod-service-image/archives",
    )

tasks.register(
    "archiveVexLifeProdServiceImage",
) {
    group =
        "distribution"
    description =
        "Archive the exact private macOS service image for source-addressed provider carriage."

    dependsOn(
        "assembleVexLifeProdServiceImage",
    )

    inputs.file(
        serviceImageSourceIdentityFile,
    )
    inputs.dir(
        serviceImageOutputRoot,
    )
    inputs.file(
        releaseContractSource,
    )
    inputs.file(
        bootstrapTemplateSource,
    )
    outputs.dir(
        serviceArchiveOutputRoot,
    )
    outputs.upToDateWhen {
        false
    }

    doLast {
        val sourceSha =
            Files.readString(
                serviceImageSourceIdentityFile
                    .get()
                    .asFile
                    .toPath(),
                StandardCharsets.UTF_8,
            )
                .trim()

        require(
            sourceSha.matches(
                Regex(
                    "[0-9a-f]{40}",
                ),
            ),
        ) {
            "service archive requires one admitted exact source SHA"
        }

        val imageRoot =
            serviceImageOutputRoot
                .get()
                .asFile
                .toPath()
                .resolve(
                    sourceSha,
                )
                .toAbsolutePath()
                .normalize()
        require(
            Files.isDirectory(
                imageRoot,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                !Files.isSymbolicLink(
                    imageRoot,
                ),
        ) {
            "service archive requires one real assembled private image"
        }

        val imageManifest =
            Files.readString(
                imageRoot.resolve(
                    "service-image.json",
                ),
                StandardCharsets.UTF_8,
            )
        val manifestSourceSha =
            Regex(
                "\\\"sourceSha\\\"\\s*:\\s*\\\"([0-9a-f]{40})\\\"",
            )
                .find(
                    imageManifest,
                )
                ?.groupValues
                ?.get(
                    1,
                )
        val payloadSha256 =
            Regex(
                "\\\"payloadSha256\\\"\\s*:\\s*\\\"([0-9a-f]{64})\\\"",
            )
                .find(
                    imageManifest,
                )
                ?.groupValues
                ?.get(
                    1,
                )
        val localOnlyVisionRuntime =
            Regex(
                "\\\"releaseDisposition\\\"\\s*:\\s*\\\"LOCAL_ONLY_NO_RELEASE\\\"",
            )
                .containsMatchIn(
                    imageManifest,
                )

        require(
            !localOnlyVisionRuntime,
        ) {
            "local-only VexVision runtime cannot enter a GitHub release archive"
        }

        require(
            manifestSourceSha ==
                sourceSha &&
                payloadSha256 !=
                null,
        ) {
            "service image manifest is not bound to the admitted archive source"
        }

        val outputRoot =
            serviceArchiveOutputRoot
                .get()
                .asFile
                .toPath()
                .resolve(
                    sourceSha,
                )
                .toAbsolutePath()
                .normalize()

        if (
            Files.exists(
                outputRoot,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            deleteTreeNoFollow(
                outputRoot,
            )
        }
        Files.createDirectories(
            outputRoot,
        )

        val releaseTag =
            releaseTagPrefix +
                sourceSha
        val archiveName =
            releaseArchiveNamePrefix +
                sourceSha +
                releaseArchiveSuffix
        val archive =
            outputRoot.resolve(
                archiveName,
            )

        runExactCommand(
            listOf(
                "/usr/bin/env",
                "COPYFILE_DISABLE=1",
                "/usr/bin/tar",
                "-czf",
                archive.toString(),
                "-C",
                imageRoot.toString(),
                ".",
            ),
        )

        require(
            Files.isRegularFile(
                archive,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                Files.size(
                    archive,
                ) >
                0,
        ) {
            "service-image archive was not formed"
        }

        val archiveSha256 =
            sha256(
                archive,
            )
        val archiveBytes =
            Files.size(
                archive,
            )

        Files.writeString(
            outputRoot.resolve(
                archiveName +
                    ".sha256",
            ),
            archiveSha256 +
                "  " +
                archiveName +
                "\n",
            StandardCharsets.UTF_8,
        )

        val bootstrapName =
            releaseBootstrapNamePrefix +
                sourceSha +
                releaseBootstrapSuffix
        val bootstrapPath =
            outputRoot.resolve(
                bootstrapName,
            )
        val bootstrapTemplate =
            Files.readString(
                bootstrapTemplateSource
                    .asFile
                    .toPath(),
                StandardCharsets.UTF_8,
            )
        val bootstrap =
            mapOf(
                "@REPOSITORY@" to releaseRepository,
                "@SOURCE_SHA@" to sourceSha,
                "@RELEASE_TAG@" to releaseTag,
                "@ARCHIVE_NAME@" to archiveName,
                "@ARCHIVE_SHA256@" to archiveSha256,
                "@SETUP_WINDOW_RELATIVE_PATH@" to releaseSetupWindowRelativePath,
                "@SETUP_LAUNCHER_RELATIVE_PATH@" to releaseSetupLauncherRelativePath,
                "@IMAGE_MANIFEST_RELATIVE_PATH@" to releaseImageManifestRelativePath,
            )
                .entries
                .fold(
                    bootstrapTemplate,
                ) {
                    current,
                    replacement,
                    ->
                    current.replace(
                        replacement.key,
                        replacement.value,
                    )
                }

        require(
            !Regex(
                "@[A-Z0-9_]+@",
            )
                .containsMatchIn(
                    bootstrap,
                ),
        ) {
            "VexLife-Prod bootstrap template still contains unresolved source tokens"
        }

        Files.writeString(
            bootstrapPath,
            bootstrap,
            StandardCharsets.UTF_8,
        )
        Files.setPosixFilePermissions(
            bootstrapPath,
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            ),
        )

        val bootstrapSha256 =
            sha256(
                bootstrapPath,
            )
        val bootstrapBytes =
            Files.size(
                bootstrapPath,
            )
        Files.writeString(
            outputRoot.resolve(
                bootstrapName +
                    ".sha256",
            ),
            bootstrapSha256 +
                "  " +
                bootstrapName +
                "\n",
            StandardCharsets.UTF_8,
        )

        val currentBootstrapPath =
            outputRoot.resolve(
                releaseCurrentBootstrapAssetName,
            )
        Files.copy(
            bootstrapPath,
            currentBootstrapPath,
            StandardCopyOption.REPLACE_EXISTING,
        )
        Files.setPosixFilePermissions(
            currentBootstrapPath,
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            ),
        )
        require(
            sha256(
                currentBootstrapPath,
            ) ==
                bootstrapSha256 &&
                Files.size(
                    currentBootstrapPath,
                ) ==
                bootstrapBytes,
        ) {
            "current-channel bootstrap alias differs from the exact source bootstrap"
        }
        Files.writeString(
            outputRoot.resolve(
                releaseCurrentBootstrapSha256AssetName,
            ),
            bootstrapSha256 +
                "  " +
                releaseCurrentBootstrapAssetName +
                "\n",
            StandardCharsets.UTF_8,
        )

        val receiptName =
            releaseArchiveNamePrefix +
                sourceSha +
                releaseReceiptSuffix
        val receipt =
            buildString {
                appendLine(
                    "{",
                )
                appendLine(
                    "  \"schemaVersion\": \"vexlife-prod.private-service-image-archive-receipt/v3\",",
                )
                appendLine(
                    "  \"truthClass\": \"SOURCE_BOUND_PRIVATE_SERVICE_IMAGE_ARCHIVE\",",
                )
                appendLine(
                    "  \"sourceSha\": \"" +
                        sourceSha +
                        "\",",
                )
                appendLine(
                    "  \"platform\": \"darwin-arm64\",",
                )
                appendLine(
                    "  \"archiveName\": \"" +
                        archiveName +
                        "\",",
                )
                appendLine(
                    "  \"archiveSha256\": \"" +
                        archiveSha256 +
                        "\",",
                )
                appendLine(
                    "  \"archiveBytes\": " +
                        archiveBytes +
                        ",",
                )
                appendLine(
                    "  \"releaseTag\": \"" +
                        releaseTag +
                        "\",",
                )
                appendLine(
                    "  \"currentChannel\": \"" +
                        releaseCurrentChannel +
                        "\",",
                )
                appendLine(
                    "  \"bootstrapName\": \"" +
                        bootstrapName +
                        "\",",
                )
                appendLine(
                    "  \"bootstrapSha256\": \"" +
                        bootstrapSha256 +
                        "\",",
                )
                appendLine(
                    "  \"currentBootstrapAssetName\": \"" +
                        releaseCurrentBootstrapAssetName +
                        "\",",
                )
                appendLine(
                    "  \"currentBootstrapSha256AssetName\": \"" +
                        releaseCurrentBootstrapSha256AssetName +
                        "\",",
                )
                appendLine(
                    "  \"bootstrapBytes\": " +
                        bootstrapBytes +
                        ",",
                )
                appendLine(
                    "  \"imagePayloadSha256\": \"" +
                        payloadSha256 +
                        "\"",
                )
                appendLine(
                    "}",
                )
            }
        Files.writeString(
            outputRoot.resolve(
                receiptName,
            ),
            receipt,
            StandardCharsets.UTF_8,
        )

        println(
            "VEXLIFE_PROD_SERVICE_ARCHIVE\t" +
                sourceSha +
                "\t" +
                archive +
                "\t" +
                archiveSha256 +
                "\t" +
                bootstrapPath +
                "\t" +
                bootstrapSha256 +
                "\t" +
                currentBootstrapPath,
        )
    }
}

val macApplicationOutputRoot =
    layout.buildDirectory.dir(
        "vexlife-prod-mac-application",
    )

tasks.register(
    "archiveVexLifeProdMacApplication",
) {
    group =
        "distribution"
    description =
        "Form the source-bound ad-hoc-signed VexLife.app and unsigned Apple-silicon DMG."

    dependsOn(
        "archiveVexLifeProdServiceImage",
    )

    inputs.file(
        serviceImageSourceIdentityFile,
    )
    inputs.dir(
        serviceImageOutputRoot,
    )
    inputs.file(
        setupWindowSource,
    )
    inputs.file(
        macApplicationReleaseContractSource,
    )
    inputs.file(
        macApplicationBrandMarkSource,
    )
    inputs.file(
        macApplicationIconArtworkSource,
    )
    inputs.property(
        "vexlifeAppVersion",
        macApplicationVersionProperty.orElse(
            "0.0.0",
        ),
    )
    outputs.dir(
        macApplicationOutputRoot,
    )
    outputs.upToDateWhen {
        false
    }

    doLast {
        val sourceSha =
            Files.readString(
                serviceImageSourceIdentityFile
                    .get()
                    .asFile
                    .toPath(),
                StandardCharsets.UTF_8,
            )
                .trim()
        require(
            sourceSha.matches(
                Regex(
                    "[0-9a-f]{40}",
                ),
            ),
        ) {
            "macOS application requires one admitted exact source SHA"
        }

        val applicationVersion =
            macApplicationVersionProperty
                .orNull
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }
                ?: "0.0.0"
        require(
            applicationVersion.matches(
                Regex(
                    "[0-9]+\\.[0-9]+\\.[0-9]+",
                ),
            ),
        ) {
            "VexLife macOS application version must be major.minor.patch"
        }

        val repositoryRoot =
            rootDir
                .toPath()
                .toAbsolutePath()
                .normalize()
        val buildVersion =
            runExactCommand(
                listOf(
                    "git",
                    "-C",
                    repositoryRoot.toString(),
                    "rev-list",
                    "--count",
                    sourceSha,
                ),
            )
        require(
            buildVersion.matches(
                Regex(
                    "[1-9][0-9]*",
                ),
            ),
        ) {
            "VexLife macOS application build version must be one positive Git revision count"
        }

        require(
            System.getProperty(
                "os.name",
            ) ==
                "Mac OS X" &&
                System.getProperty(
                    "os.arch",
                ) in
                    setOf(
                        "aarch64",
                        "arm64",
                    ),
        ) {
            "VexLife macOS application must be formed on macOS arm64"
        }

        val imageRoot =
            serviceImageOutputRoot
                .get()
                .asFile
                .toPath()
                .resolve(
                    sourceSha,
                )
                .toAbsolutePath()
                .normalize()
        val imageManifest =
            Files.readString(
                imageRoot.resolve(
                    "service-image.json",
                ),
                StandardCharsets.UTF_8,
            )
        require(
            Regex(
                "\\\"sourceSha\\\"\\s*:\\s*\\\"" +
                    sourceSha +
                    "\\\"",
            )
                .containsMatchIn(
                    imageManifest,
                ) &&
                !Regex(
                    "\\\"releaseDisposition\\\"\\s*:\\s*\\\"LOCAL_ONLY_NO_RELEASE\\\"",
                )
                    .containsMatchIn(
                        imageManifest,
                    ),
        ) {
            "public VexLife.app cannot embed a mismatched image or a local-only Vision runtime"
        }

        val outputRoot =
            macApplicationOutputRoot
                .get()
                .asFile
                .toPath()
                .resolve(
                    sourceSha,
                )
                .toAbsolutePath()
                .normalize()
        if (
            Files.exists(
                outputRoot,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            deleteTreeNoFollow(
                outputRoot,
            )
        }
        Files.createDirectories(
            outputRoot,
        )

        val applicationBundle =
            outputRoot.resolve(
                macApplicationName,
            )
        runExactCommand(
            listOf(
                "/usr/bin/osacompile",
                "-o",
                applicationBundle.toString(),
                setupWindowSource
                    .asFile
                    .toPath()
                    .toString(),
            ),
        )

        val resources =
            applicationBundle.resolve(
                "Contents/Resources",
            )
        val embeddedImage =
            resources.resolve(
                macApplicationEmbeddedImageRelativePath,
            )
        Files.createDirectories(
            resources,
        )

        // VexLife reuses the current Vextreme mark, but the app icon needs a real icon canvas.
        // The source-owned 1024x1024 artwork centers/scales the mark intentionally so Quick Look
        // cannot turn a tiny 120x120 logo into a "logo printed on a blank page" icon.
        val iconWork =
            outputRoot.resolve(
                "icon-work",
            )
        Files.createDirectories(
            iconWork,
        )
        runExactCommand(
            listOf(
                "/usr/bin/qlmanage",
                "-t",
                "-s",
                "1024",
                "-o",
                iconWork.toString(),
                macApplicationIconArtworkSource
                    .asFile
                    .toPath()
                    .toString(),
            ),
        )
        val renderedAppIcon =
            Files.list(
                iconWork,
            )
                .use {
                    stream ->
                    stream
                        .filter {
                            Files.isRegularFile(
                                it,
                                LinkOption.NOFOLLOW_LINKS,
                            ) &&
                                it.fileName
                                    .toString()
                                    .endsWith(
                                        ".png",
                                    )
                        }
                        .toList()
                        .singleOrNull()
                }
                ?: error(
                    "macOS did not render exactly one Vextreme brand-mark PNG",
                )
        require(
            Files.size(
                renderedAppIcon,
            ) >
                0,
        ) {
            "rendered Vextreme brand mark is empty"
        }

        val iconset =
            iconWork.resolve(
                "VexLife.iconset",
            )
        Files.createDirectories(
            iconset,
        )
        listOf(
            16,
            32,
            128,
            256,
            512,
        )
            .forEach {
                points ->
                listOf(
                    points to
                        "icon_${points}x${points}.png",
                    points * 2 to
                        "icon_${points}x${points}@2x.png",
                )
                    .forEach {
                        (pixels, filename) ->
                        runExactCommand(
                            listOf(
                                "/usr/bin/sips",
                                "-z",
                                pixels.toString(),
                                pixels.toString(),
                                renderedAppIcon.toString(),
                                "--out",
                                iconset.resolve(
                                    filename,
                                )
                                    .toString(),
                            ),
                        )
                    }
            }

        // osacompile forms an AppleScript applet whose native icon resource is applet.icns.
        // Replace that exact resource instead of asking Launch Services to switch filenames.
        val applicationIcon =
            resources.resolve(
                "applet.icns",
            )
        Files.deleteIfExists(
            applicationIcon,
        )
        runExactCommand(
            listOf(
                "/usr/bin/iconutil",
                "-c",
                "icns",
                iconset.toString(),
                "-o",
                applicationIcon.toString(),
            ),
        )
        require(
            Files.isRegularFile(
                applicationIcon,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                Files.size(
                    applicationIcon,
                ) >
                0,
        ) {
            "VexLife application icon was not formed"
        }

        // Current osacompile applets also ship an asset catalog and CFBundleIconName=applet.
        // Apple DTS documents that this catalog can keep the generic applet artwork active even
        // after applet.icns is replaced. Remove the generated catalog so Finder uses our icns.
        Files.deleteIfExists(
            resources.resolve(
                "Assets.car",
            ),
        )
        deleteTreeNoFollow(
            iconWork,
        )
        runExactCommand(
            listOf(
                "/usr/bin/ditto",
                imageRoot.toString(),
                embeddedImage.toString(),
            ),
        )
        Files.copy(
            macApplicationReleaseContractSource
                .asFile
                .toPath(),
            resources.resolve(
                "vexlife-macos-application-release.json",
            ),
            StandardCopyOption.REPLACE_EXISTING,
        )

        val infoPlist =
            applicationBundle.resolve(
                "Contents/Info.plist",
            )
        require(
            Files.isRegularFile(
                infoPlist,
                LinkOption.NOFOLLOW_LINKS,
            ),
        ) {
            "VexLife.app is missing Info.plist"
        }

        runExactCommand(
            listOf(
                "/usr/bin/plutil",
                "-replace",
                "CFBundleIdentifier",
                "-string",
                macApplicationBundleIdentifier,
                infoPlist.toString(),
            ),
        )
        runExactCommand(
            listOf(
                "/usr/bin/plutil",
                "-replace",
                "CFBundleShortVersionString",
                "-string",
                applicationVersion,
                infoPlist.toString(),
            ),
        )
        runExactCommand(
            listOf(
                "/usr/bin/plutil",
                "-replace",
                "CFBundleVersion",
                "-string",
                buildVersion,
                infoPlist.toString(),
            ),
        )
        runExactCommand(
            listOf(
                "/usr/bin/plutil",
                "-insert",
                "LSMultipleInstancesProhibited",
                "-bool",
                "YES",
                infoPlist.toString(),
            ),
        )
        runExactCommand(
            listOf(
                "/usr/bin/plutil",
                "-insert",
                "VexLifeProdExpectedSourceSha",
                "-string",
                sourceSha,
                infoPlist.toString(),
            ),
        )
        runExactCommand(
            listOf(
                "/usr/bin/plutil",
                "-insert",
                "VexLifeProdEmbeddedSetupLauncherRelativePath",
                "-string",
                macApplicationEmbeddedSetupLauncherRelativePath,
                infoPlist.toString(),
            ),
        )
        runExactCommand(
            listOf(
                "/usr/bin/plutil",
                "-insert",
                "VexLifeProdDistributionTrust",
                "-string",
                macApplicationDistributionTrust,
                infoPlist.toString(),
            ),
        )
        runExactCommand(
            listOf(
                "/usr/bin/plutil",
                "-remove",
                "CFBundleIconName",
                infoPlist.toString(),
            ),
        )
        runExactCommand(
            listOf(
                "/usr/bin/plutil",
                "-replace",
                "CFBundleIconFile",
                "-string",
                "applet",
                infoPlist.toString(),
            ),
        )

        runExactCommand(
            listOf(
                "/usr/bin/plutil",
                "-insert",
                "LSMinimumSystemVersion",
                "-string",
                macApplicationMinimumMacOSVersion,
                infoPlist.toString(),
            ),
        )

        // There is intentionally no Developer ID here. Ad-hoc signing restores the bundle seal after
        // Product-owned metadata/resources are bound; it does not claim Apple identity or notarization.
        runExactCommand(
            listOf(
                "/usr/bin/codesign",
                "--force",
                "--sign",
                "-",
                "--timestamp=none",
                applicationBundle.toString(),
            ),
        )
        runExactCommand(
            listOf(
                "/usr/bin/codesign",
                "--verify",
                "--strict",
                applicationBundle.toString(),
            ),
        )

        val applicationPayloadSha256 =
            payloadFingerprint(
                applicationBundle,
            )

        val diskImageStage =
            outputRoot.resolve(
                "dmg-stage",
            )
        Files.createDirectories(
            diskImageStage,
        )
        runExactCommand(
            listOf(
                "/usr/bin/ditto",
                applicationBundle.toString(),
                diskImageStage
                    .resolve(
                        macApplicationName,
                    )
                    .toString(),
            ),
        )
        Files.createSymbolicLink(
            diskImageStage.resolve(
                "Applications",
            ),
            Path.of(
                "/Applications",
            ),
        )

        val writableDiskImage =
            outputRoot.resolve(
                "VexLife-macOS-AppleSilicon.writable.dmg",
            )
        val diskImageMount =
            outputRoot.resolve(
                "dmg-mount",
            )
        if (
            Files.exists(
                diskImageMount,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            deleteTreeNoFollow(
                diskImageMount,
            )
        }
        Files.createDirectories(
            diskImageMount,
        )

        runExactCommand(
            listOf(
                "/usr/bin/hdiutil",
                "create",
                "-volname",
                macApplicationHumanName,
                "-srcfolder",
                diskImageStage.toString(),
                "-ov",
                "-format",
                "UDRW",
                writableDiskImage.toString(),
            ),
        )
        runExactCommand(
            listOf(
                "/usr/bin/hdiutil",
                "attach",
                writableDiskImage.toString(),
                "-readwrite",
                "-nobrowse",
                "-mountpoint",
                diskImageMount.toString(),
            ),
        )
        try {
            val diskImageVolumeIcon =
                diskImageMount.resolve(
                    macApplicationDiskImageVolumeIconName,
                )
            Files.copy(
                applicationIcon,
                diskImageVolumeIcon,
                StandardCopyOption.REPLACE_EXISTING,
            )
            require(
                Files.isRegularFile(
                    diskImageVolumeIcon,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    Files.size(
                        diskImageVolumeIcon,
                    ) >
                    0,
            ) {
                "VexLife DMG volume icon was not formed"
            }

            val setFile =
                runExactCommand(
                    listOf(
                        "/usr/bin/xcrun",
                        "--find",
                        "SetFile",
                    ),
                )
            val setFilePath =
                Path.of(
                    setFile,
                )
                    .toAbsolutePath()
                    .normalize()
            require(
                Files.isRegularFile(
                    setFilePath,
                    LinkOption.NOFOLLOW_LINKS,
                ) &&
                    Files.isExecutable(
                        setFilePath,
                    ),
            ) {
                "SetFile is unavailable for VexLife DMG volume icon formation"
            }

            runExactCommand(
                listOf(
                    setFilePath.toString(),
                    "-c",
                    "icnC",
                    diskImageVolumeIcon.toString(),
                ),
            )
            runExactCommand(
                listOf(
                    setFilePath.toString(),
                    "-a",
                    "V",
                    diskImageVolumeIcon.toString(),
                ),
            )
            runExactCommand(
                listOf(
                    setFilePath.toString(),
                    "-a",
                    "C",
                    diskImageMount.toString(),
                ),
            )

            // These are macOS mount-time bookkeeping directories, not VexLife payload.
            // Remove any that were created while the writable image was mounted so the
            // sealed DMG stays visually minimal when Finder hidden files are visible.
            listOf(
                ".fseventsd",
                ".Spotlight-V100",
                ".Trashes",
            )
                .forEach {
                    bookkeepingName ->
                    deleteTreeNoFollow(
                        diskImageMount.resolve(
                            bookkeepingName,
                        ),
                    )
                }
        } finally {
            runExactCommand(
                listOf(
                    "/usr/bin/hdiutil",
                    "detach",
                    diskImageMount.toString(),
                ),
            )
        }

        val diskImage =
            outputRoot.resolve(
                macApplicationDiskImageName,
            )
        runExactCommand(
            listOf(
                "/usr/bin/hdiutil",
                "convert",
                writableDiskImage.toString(),
                "-format",
                "UDZO",
                "-o",
                diskImage.toString(),
            ),
        )
        runExactCommand(
            listOf(
                "/usr/bin/hdiutil",
                "verify",
                diskImage.toString(),
            ),
        )

        deleteTreeNoFollow(
            diskImageStage,
        )
        deleteTreeNoFollow(
            diskImageMount,
        )
        Files.deleteIfExists(
            writableDiskImage,
        )

        val diskImageSha256 =
            sha256(
                diskImage,
            )
        val diskImageBytes =
            Files.size(
                diskImage,
            )
        Files.writeString(
            outputRoot.resolve(
                macApplicationDiskImageName +
                    ".sha256",
            ),
            diskImageSha256 +
                "  " +
                macApplicationDiskImageName +
                "\n",
            StandardCharsets.UTF_8,
        )

        val receipt =
            buildString {
                appendLine("{")
                appendLine("  \"schemaVersion\": \"vexlife-prod.macos-application-archive-receipt/v1\",")
                appendLine("  \"truthClass\": \"SOURCE_BOUND_OPEN_SOURCE_MAC_APPLICATION\",")
                appendLine("  \"sourceSha\": \"" + sourceSha + "\",")
                appendLine("  \"platform\": \"" + macApplicationPlatform + "\",")
                appendLine("  \"bundleIdentifier\": \"" + macApplicationBundleIdentifier + "\",")
                appendLine("  \"applicationVersion\": \"" + applicationVersion + "\",")
                appendLine("  \"buildVersion\": \"" + buildVersion + "\",")
                appendLine("  \"applicationName\": \"" + macApplicationName + "\",")
                appendLine("  \"applicationPayloadSha256\": \"" + applicationPayloadSha256 + "\",")
                appendLine("  \"diskImageName\": \"" + macApplicationDiskImageName + "\",")
                appendLine("  \"diskImageSha256\": \"" + diskImageSha256 + "\",")
                appendLine("  \"diskImageBytes\": " + diskImageBytes + ",")
                appendLine("  \"distributionTrust\": \"" + macApplicationDistributionTrust + "\",")
                appendLine("  \"notarized\": false")
                appendLine("}")
            }
        Files.writeString(
            outputRoot.resolve(
                "VexLife-macOS-AppleSilicon.receipt.json",
            ),
            receipt,
            StandardCharsets.UTF_8,
        )

        println(
            "VEXLIFE_PROD_MAC_APPLICATION\t" +
                sourceSha +
                "\t" +
                applicationBundle +
                "\t" +
                applicationPayloadSha256 +
                "\t" +
                diskImage +
                "\t" +
                diskImageSha256,
        )
    }
}

val vexLifeWebJvmMain =
    kotlin.targets
        .getByName("jvm")
        .compilations
        .getByName("main")

val vexLifeWebP1Output =
    layout.buildDirectory.dir(
        "generated/vexweb/vexlife-workspace",
    )

tasks.register<JavaExec>(
    "generateVexLifeWebP1",
) {
    group =
        "vexlife"
    description =
        "Materialize the deterministic VexLife P1 browser artifacts from accepted VexWeb source."

    dependsOn(
        vexLifeWebJvmMain.compileTaskProvider,
    )

    classpath =
        files(
            vexLifeWebJvmMain.output.allOutputs,
            vexLifeWebJvmMain.runtimeDependencyFiles,
        )

    mainClass.set(
        "vexlife.web.VexLifeWebP1ArtifactMaterializerKt",
    )

    args(
        "write",
        vexLifeWebP1Output
            .get()
            .asFile
            .absolutePath,
    )

    outputs.dir(
        vexLifeWebP1Output,
    )
}

tasks.register<JavaExec>(
    "verifyVexLifeWebP1",
) {
    group =
        "verification"
    description =
        "Verify an existing generated VexLife P1 browser artifact set against current VexWeb source."

    dependsOn(
        vexLifeWebJvmMain.compileTaskProvider,
    )

    classpath =
        files(
            vexLifeWebJvmMain.output.allOutputs,
            vexLifeWebJvmMain.runtimeDependencyFiles,
        )

    mainClass.set(
        "vexlife.web.VexLifeWebP1ArtifactMaterializerKt",
    )

    args(
        "verify",
        vexLifeWebP1Output
            .get()
            .asFile
            .absolutePath,
    )
}

tasks.register<JavaExec>("generateVexLifeProdScreenLens") {
    group = "vexlife"
    description = "Write docs/lens/vexlife-prod.screen-lens.json for VexBoard from the Product Blueprint."
    dependsOn(vexLifeWebJvmMain.compileTaskProvider)
    classpath = files(vexLifeWebJvmMain.output.allOutputs, vexLifeWebJvmMain.runtimeDependencyFiles)
    mainClass.set("vexlife.lens.VexLifeProdScreenLensKt")
    args("write", layout.projectDirectory.asFile.absolutePath)
}

tasks.register<JavaExec>("generateVexLifeSiteData") {
    group = "vexlife"
    description = "Write the home page's site data (models, admitted Mac) from the Product's owners."
    dependsOn(vexLifeWebJvmMain.compileTaskProvider)
    classpath = files(vexLifeWebJvmMain.output.allOutputs, vexLifeWebJvmMain.runtimeDependencyFiles)
    mainClass.set("vexlife.site.VexLifeSiteDataKt")
    args(
        "write",
        (project.findProperty("vexlifeSiteDataDir") as String?)
            ?: layout.buildDirectory.dir("site/data").get().asFile.absolutePath,
        (project.findProperty("vexlifeSourceSha") as String?) ?: "",
    )
}

tasks.register<JavaExec>("checkVexLifeStrings") {
    group = "verification"
    description = "Check the keyed string source: English present, placeholders match, manifest current."
    dependsOn(vexLifeWebJvmMain.compileTaskProvider)
    classpath = files(vexLifeWebJvmMain.output.allOutputs, vexLifeWebJvmMain.runtimeDependencyFiles)
    mainClass.set("vexlife.text.VexLifeStringsSourceKt")
    args("check", layout.projectDirectory.asFile.absolutePath)
}

tasks.register<JavaExec>("compileVexLifeStrings") {
    group = "vexlife"
    description = "Mark translations of changed English stale and rewrite localization/strings/manifest.json."
    dependsOn(vexLifeWebJvmMain.compileTaskProvider)
    classpath = files(vexLifeWebJvmMain.output.allOutputs, vexLifeWebJvmMain.runtimeDependencyFiles)
    mainClass.set("vexlife.text.VexLifeStringsSourceKt")
    args("compile", layout.projectDirectory.asFile.absolutePath)
}

// [VXG RealForever]

// Experiment-only: the shared VexWork continuity-pressure harness adapter (#310). It runs from the
// JVM test compilation so nothing it uses ships in the Product, against a synthetic scratch Home
// it deletes after forming its result. Invoked by tools/continuity-pressure/run.mjs.
tasks.register<JavaExec>("continuityPressure") {
    group = "verification"
    description = "Run the continuity-pressure harness adapter (experiment-only, scratch Home)."
    val test = kotlin.jvm().compilations.getByName("test")
    dependsOn("jvmTestClasses")
    classpath = files(test.output.allOutputs, test.runtimeDependencyFiles)
    mainClass.set("vexlife.harness.ContinuityPressureAdapterKt")
    workingDir = projectDir
}
