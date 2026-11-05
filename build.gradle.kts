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

val serviceImageRoot =
    layout.buildDirectory.dir(
        "vexlife-prod-service-image",
    )

tasks.register(
    "assembleVexLifeProdServiceImage",
) {
    group =
        "distribution"
    description =
        "Build the private self-contained macOS arm64 VexLife-Prod service image."

    dependsOn(
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
    outputs.dir(
        serviceImageRoot,
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
            "VexLife-Prod service image requires a clean exact-source checkout:\n" +
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
            serviceImageRoot
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
            output
                .toFile()
                .deleteRecursively()
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

        Files.createDirectories(
            output,
        )
        Files.createDirectories(
            libRoot,
        )
        Files.createDirectories(
            binRoot,
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
                    "  \"schemaVersion\": \"vexlife-prod.private-service-image/v1\",",
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

// [VXG RealForever]
