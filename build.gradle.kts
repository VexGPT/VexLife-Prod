import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest
import org.gradle.api.tasks.bundling.Jar

plugins {
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
    val pattern =
        Regex(
            "\\"" +
                Regex.escape(
                    field,
                ) +
                "\\"\\s*:\\s*\\"([^\\"]+)\\"",
        )

    return requireNotNull(
        pattern.find(
            text,
        )
            ?.groupValues
            ?.get(
                1,
            ),
    ) {
        "required JSON string field is missing: $field"
    }
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

require(
    releaseSchemaVersion ==
        "vexlife-prod.private-service-release/v1" &&
        releaseRepository ==
        "VexGPT/VexLife-Prod" &&
        releasePlatform ==
        "darwin-arm64" &&
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
        require(
            Runtime.version()
                .feature() ==
                21,
        ) {
            "VexLife-Prod service image must be assembled with JDK 21"
        }

        val javaHome =
            Path.of(
                System.getProperty(
                    "java.home",
                ),
            )
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
            jvmMainCompilation
                .runtimeDependencyFiles
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
                    java.io.File.pathSeparator,
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

        val moduleSet =
            modules
                .split(
                    ",",
                )
                .toSet()
        require(
            "jdk.httpserver" in
                moduleSet &&
                "java.net.http" in
                moduleSet,
        ) {
            "service runtime image must include jdk.httpserver + java.net.http"
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
                modules,
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
            modules
                .split(
                    ",",
                )
                .sorted()
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
                    "  \"schemaVersion\": \"vexlife-prod.private-service-image/v3\",",
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
                    "  ]",
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
                    "  \"schemaVersion\": \"vexlife-prod.private-service-image-archive-receipt/v2\",",
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
                bootstrapSha256,
        )
    }
}

// [VXG RealForever]
