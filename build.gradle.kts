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

val visionRuntimeContractSource =
    layout.projectDirectory.file(
        "packaging/private-service-image/vision-runtime-contract.json",
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
        "vision-runtime/helper/VexLifeVisionSystemPickerAgent.app/Contents/MacOS/VexLifeVisionSystemPickerAgent" &&
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
                        (
                            if (
                                visionRuntimeArtifactInput ==
                                null
                            ) {
                                "vexlife-prod.private-service-image/v3"
                            } else {
                                "vexlife-prod.private-service-image/v4"
                            }
                        ) +
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

// [VXG RealForever]
