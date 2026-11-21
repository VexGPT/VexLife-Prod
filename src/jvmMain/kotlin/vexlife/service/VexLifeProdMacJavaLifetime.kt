package vexlife.service

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.Serializable
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.packaging.VexLifeProdPrivateServiceImage
import vexlife.packaging.VexLifeProdPrivateServiceImageInstaller

/**
 * Mac-user lifetime owner for the Java execution engine used by VexLife Product setup and service.
 *
 * Product source/image identity may rotate while one compatible Java 21 engine remains stable.
 * This owner never clears quarantine, changes Gatekeeper policy, downloads Java, or owns Product
 * JARs. It materializes only a runtime that already belongs to a verified installed Product image.
 */
object VexLifeProdMacJavaLifetime {
    const val ReceiptSchema =
        "vexlife-prod.macos-java-lifetime/v1"

    const val ReceiptFileName =
        "java-lifetime.json"

    const val SetupProviderSourceProperty =
        "vexlife.setupJavaProviderSourceSha"

    private const val JavaRootName =
        "java"

    private const val RuntimesName =
        "runtimes"

    private val GitSha =
        Regex(
            "[0-9a-f]{40}",
        )

    private val Sha256 =
        Regex(
            "[0-9a-f]{64}",
        )

    @Serializable
    data class Receipt(
        val schemaVersion: String = ReceiptSchema,
        val providerSourceSha: String,
        val providerImagePayloadSha256: String,
        val runtimeFingerprintSha256: String,
        val javaRuntimeMajor: Int,
        val runtimeModules: List<String>,
        val runtimePath: String,
        val javaPath: String,
    )

    data class Binding(
        val receipt: Receipt,
        val javaPath: String,
        val providerSourceSha: String,
        val effectsPerformed: Boolean,
    )

    fun observeCompatible(
        requiredImage: VexLifeProdPrivateServiceImage.Observation,
        lifetimeRoot: Path = VexLifeProdMacServiceLifetime.defaultLifetimeRoot(),
    ): Binding? {
        val root =
            requireLifetimeRootIfPresent(
                lifetimeRoot,
            )
                ?: return null
        val receipt =
            VexDurableContractFile.read<Receipt>(
                ownedRoot = root,
                path =
                    VexOwnedPath.resolveInside(
                        root,
                        ReceiptFileName,
                    ),
            )
                ?: return null

        validateReceipt(
            root = root,
            receipt = receipt,
        )
        if (
            !isCompatible(
                requiredImage = requiredImage,
                receipt = receipt,
            )
        ) {
            return null
        }

        return Binding(
            receipt = receipt,
            javaPath = receipt.javaPath,
            providerSourceSha = receipt.providerSourceSha,
            effectsPerformed = false,
        )
    }

    fun requireCompatible(
        requiredImage: VexLifeProdPrivateServiceImage.Observation,
        lifetimeRoot: Path = VexLifeProdMacServiceLifetime.defaultLifetimeRoot(),
    ): Binding =
        requireNotNull(
            observeCompatible(
                requiredImage = requiredImage,
                lifetimeRoot = lifetimeRoot,
            ),
        ) {
            "VexLife stable Java runtime is absent or incompatible with the current Product image"
        }

    fun convergeFromInstalledProvider(
        productHome: Path,
        providerSourceSha: String,
        requiredImage: VexLifeProdPrivateServiceImage.Observation,
        lifetimeRoot: Path = VexLifeProdMacServiceLifetime.defaultLifetimeRoot(),
        copier: RuntimeCopier = SystemRuntimeCopier,
    ): Binding {
        require(
            GitSha.matches(
                providerSourceSha,
            ),
        ) {
            "stable Java provider source must be one exact Git commit"
        }

        observeCompatible(
            requiredImage = requiredImage,
            lifetimeRoot = lifetimeRoot,
        )
            ?.let {
                return it
            }

        val providerImagePath =
            VexLifeProdPrivateServiceImageInstaller.installedImagePath(
                productHome = productHome,
                sourceSha = providerSourceSha,
            )
        val provider =
            VexLifeProdPrivateServiceImage.observe(
                imageRoot = providerImagePath,
                expectedSourceSha = providerSourceSha,
            )
        require(
            requiredImage.runtimeModules
                .toSet()
                .all {
                    it in
                        provider.runtimeModules
                },
        ) {
            "installed Java provider does not contain every module required by the current Product image"
        }

        val sourceRuntime =
            requireNotNull(
                Path.of(
                    provider.javaPath,
                )
                    .parent
                    ?.parent,
            )
                .toAbsolutePath()
                .normalize()
        require(
            sourceRuntime ==
                providerImagePath
                    .resolve(
                        "runtime",
                    )
                    .toAbsolutePath()
                    .normalize(),
        ) {
            "installed Java provider runtime escaped its verified Product image"
        }

        val runtimeFingerprint =
            runtimeFingerprint(
                sourceRuntime,
            )
        val root =
            ensureLifetimeRoot(
                lifetimeRoot,
            )
        val javaRoot =
            VexOwnedPath.ensureDirectory(
                root,
                VexOwnedPath.resolveInside(
                    root,
                    JavaRootName,
                ),
            )
        val runtimesRoot =
            VexOwnedPath.ensureDirectory(
                root,
                VexOwnedPath.resolveInside(
                    javaRoot,
                    RuntimesName,
                ),
            )
        val runtimeTarget =
            VexOwnedPath.resolveInside(
                runtimesRoot,
                runtimeFingerprint,
            )

        var effectsPerformed =
            false
        if (
            !Files.exists(
                runtimeTarget,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            val staging =
                VexOwnedPath.resolveInside(
                    runtimesRoot,
                    ".staging-" +
                        UUID.randomUUID()
                            .toString(),
                )
            try {
                copier.copy(
                    sourceRuntime = sourceRuntime,
                    targetRuntime = staging,
                )
                require(
                    runtimeFingerprint(
                        staging,
                    ) ==
                        runtimeFingerprint,
                ) {
                    "stable Java staged runtime differs from its verified provider"
                }
                Files.move(
                    staging,
                    runtimeTarget,
                    StandardCopyOption.ATOMIC_MOVE,
                )
                effectsPerformed =
                    true
            } finally {
                if (
                    Files.exists(
                        staging,
                        LinkOption.NOFOLLOW_LINKS,
                    )
                ) {
                    staging
                        .toFile()
                        .deleteRecursively()
                }
            }
        } else {
            require(
                runtimeFingerprint(
                    runtimeTarget,
                ) ==
                    runtimeFingerprint,
            ) {
                "existing stable Java runtime coordinate does not match its fingerprint"
            }
        }

        val javaPath =
            runtimeTarget
                .resolve(
                    "bin/java",
                )
                .toAbsolutePath()
                .normalize()
        require(
            Files.isRegularFile(
                javaPath,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                !Files.isSymbolicLink(
                    javaPath,
                ) &&
                Files.isExecutable(
                    javaPath,
                ) &&
                Files.isRegularFile(
                    runtimeTarget.resolve(
                        "lib/modules",
                    ),
                    LinkOption.NOFOLLOW_LINKS,
                ),
        ) {
            "stable Java runtime is incomplete after materialization"
        }

        val receipt =
            Receipt(
                providerSourceSha = provider.sourceSha,
                providerImagePayloadSha256 = provider.payloadSha256,
                runtimeFingerprintSha256 = runtimeFingerprint,
                javaRuntimeMajor = VexLifeProdPrivateServiceImage.JavaRuntimeMajor,
                runtimeModules = provider.runtimeModules,
                runtimePath = runtimeTarget.toString(),
                javaPath = javaPath.toString(),
            )
        VexDurableContractFile.write(
            ownedRoot = root,
            path =
                VexOwnedPath.resolveInside(
                    root,
                    ReceiptFileName,
                ),
            value = receipt,
        )
        effectsPerformed =
            true

        val rebound =
            requireNotNull(
                observeCompatible(
                    requiredImage = requiredImage,
                    lifetimeRoot = root,
                ),
            ) {
                "stable Java lifetime receipt did not read back after convergence"
            }

        return rebound.copy(
            effectsPerformed = effectsPerformed,
        )
    }

    fun interface RuntimeCopier {
        fun copy(
            sourceRuntime: Path,
            targetRuntime: Path,
        )
    }

    object SystemRuntimeCopier : RuntimeCopier {
        override fun copy(
            sourceRuntime: Path,
            targetRuntime: Path,
        ) {
            require(
                !Files.exists(
                    targetRuntime,
                    LinkOption.NOFOLLOW_LINKS,
                ),
            ) {
                "stable Java staging coordinate already exists"
            }
            val process =
                ProcessBuilder(
                        "/usr/bin/ditto",
                        sourceRuntime.toString(),
                        targetRuntime.toString(),
                    )
                    .redirectErrorStream(
                        true,
                    )
                    .start()
            val output =
                process.inputStream
                    .readBytes()
                    .toString(
                        StandardCharsets.UTF_8,
                    )
            val status =
                process.waitFor()
            require(
                status ==
                    0,
            ) {
                "stable Java runtime copy failed: " +
                    output.trim()
            }
        }
    }

    private fun validateReceipt(
        root: Path,
        receipt: Receipt,
    ) {
        require(
            receipt.schemaVersion ==
                ReceiptSchema &&
                GitSha.matches(
                    receipt.providerSourceSha,
                ) &&
                Sha256.matches(
                    receipt.providerImagePayloadSha256,
                ) &&
                Sha256.matches(
                    receipt.runtimeFingerprintSha256,
                ) &&
                receipt.javaRuntimeMajor ==
                VexLifeProdPrivateServiceImage.JavaRuntimeMajor &&
                receipt.runtimeModules.isNotEmpty() &&
                receipt.runtimeModules ==
                receipt.runtimeModules
                    .distinct()
                    .sorted(),
        ) {
            "stable Java lifetime receipt identity drift"
        }

        val runtime =
            Path.of(
                receipt.runtimePath,
            )
                .toAbsolutePath()
                .normalize()
        val expected =
            VexOwnedPath.resolveInside(
                VexOwnedPath.resolveInside(
                    VexOwnedPath.resolveInside(
                        root,
                        JavaRootName,
                    ),
                    RuntimesName,
                ),
                receipt.runtimeFingerprintSha256,
            )
        require(
            runtime ==
                expected &&
                Path.of(
                    receipt.javaPath,
                )
                    .toAbsolutePath()
                    .normalize() ==
                    runtime.resolve(
                        "bin/java",
                    )
                        .normalize(),
        ) {
            "stable Java lifetime receipt coordinate drift"
        }
        require(
            Files.isRegularFile(
                Path.of(
                    receipt.javaPath,
                ),
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                Files.isExecutable(
                    Path.of(
                        receipt.javaPath,
                    ),
                ) &&
                runtimeFingerprint(
                    runtime,
                ) ==
                    receipt.runtimeFingerprintSha256,
        ) {
            "stable Java lifetime runtime no longer matches its receipt"
        }
    }

    private fun isCompatible(
        requiredImage: VexLifeProdPrivateServiceImage.Observation,
        receipt: Receipt,
    ): Boolean =
        requiredImage.runtimeModules
            .toSet()
            .all {
                it in
                    receipt.runtimeModules
            }

    private fun ensureLifetimeRoot(
        lifetimeRoot: Path,
    ): Path {
        require(
            lifetimeRoot.isAbsolute,
        ) {
            "stable Java lifetime root must be absolute"
        }
        val root =
            lifetimeRoot
                .toAbsolutePath()
                .normalize()
        Files.createDirectories(
            root,
        )
        return VexOwnedPath.requireRoot(
            root,
            "VexLife stable Java lifetime root",
        )
    }

    private fun requireLifetimeRootIfPresent(
        lifetimeRoot: Path,
    ): Path? {
        require(
            lifetimeRoot.isAbsolute,
        ) {
            "stable Java lifetime root must be absolute"
        }
        val root =
            lifetimeRoot
                .toAbsolutePath()
                .normalize()
        if (
            !Files.exists(
                root,
                LinkOption.NOFOLLOW_LINKS,
            )
        ) {
            return null
        }
        return VexOwnedPath.requireRoot(
            root,
            "VexLife stable Java lifetime root",
        )
    }

    internal fun runtimeFingerprint(
        runtimeRoot: Path,
    ): String {
        val root =
            VexOwnedPath.requireRoot(
                runtimeRoot,
                "VexLife stable Java runtime",
            )
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
                            root.relativize(
                                path,
                            )
                                .toString()
                        val descriptor =
                            when {
                                path ==
                                    root ->
                                    "D\u0000"

                                Files.isSymbolicLink(
                                    path,
                                ) -> {
                                    val target =
                                        Files.readSymbolicLink(
                                            path,
                                        )
                                    require(
                                        !target.isAbsolute &&
                                            path.parent
                                                .resolve(
                                                    target,
                                                )
                                                .normalize()
                                                .startsWith(
                                                    root,
                                                ),
                                    ) {
                                        "stable Java runtime symlink escaped runtime root"
                                    }
                                    "L\u0000" +
                                        relative +
                                        "\u0000" +
                                        target
                                            .toString()
                                }

                                Files.isDirectory(
                                    path,
                                    LinkOption.NOFOLLOW_LINKS,
                                ) ->
                                    "D\u0000" +
                                        relative

                                Files.isRegularFile(
                                    path,
                                    LinkOption.NOFOLLOW_LINKS,
                                ) ->
                                    "F\u0000" +
                                        relative +
                                        "\u0000" +
                                        Files.size(
                                            path,
                                        )

                                else ->
                                    error(
                                        "stable Java runtime contains unsupported filesystem entry: $relative",
                                    )
                            }

                        digest.update(
                            descriptor.toByteArray(
                                StandardCharsets.UTF_8,
                            ),
                        )
                        digest.update(
                            0.toByte(),
                        )
                        if (
                            Files.isRegularFile(
                                path,
                                LinkOption.NOFOLLOW_LINKS,
                            )
                        ) {
                            Files.newInputStream(
                                path,
                            )
                                .use {
                                    input ->
                                    val buffer =
                                        ByteArray(
                                            64 * 1024,
                                        )
                                    while (
                                        true
                                    ) {
                                        val count =
                                            input.read(
                                                buffer,
                                            )
                                        if (
                                            count <
                                            0
                                        ) {
                                            break
                                        }
                                        digest.update(
                                            buffer,
                                            0,
                                            count,
                                        )
                                    }
                                }
                        }
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
}

// [VXG RealForever]
