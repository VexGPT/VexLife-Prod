package vexlife.setup

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdLocalSetup
import vexlife.local.VexLifeProdSetupProgress
import vexlife.packaging.VexLifeProdPrivateServiceImage
import vexlife.packaging.VexLifeProdPrivateServiceImageInstaller
import vexlife.service.VexLifeProdMacServiceLifetime

/**
 * First-run/repair setup process executed from the private service image.
 *
 * It verifies its own source-bound image, carries explicit human acquisition authorization into
 * LocalSetup, installs the verified image into the established Home, delegates exact service
 * start/reuse to the macOS service-lifetime owner, and only then reports machine-readable
 * completion. It does not open presentation or own the long-lived service process.
 */
object VexLifeProdSetupProcess {
    private val GitSha = Regex("[0-9a-f]{40}")

    data class Arguments(
        val productHome: Path,
        val expectedSourceSha: String,
        val allowAcquisition: Boolean,
        val allowRuntimeStart: Boolean = allowAcquisition,
    ) {
        init {
            require(!allowAcquisition || allowRuntimeStart) {
                "acquisition authority without runtime-start authority is not lawful"
            }
        }
    }

    data class HomeArguments(val productHome: Path, val expectedSourceSha: String)

    internal data class Hooks(
        val progress: VexLifeProdSetupProgress = VexLifeProdSetupProgress.Silent,
        val locateImageRoot: () -> Path = ::currentImageRoot,
        val observeImage:
            (
                imageRoot: Path,
                expectedSourceSha: String,
            ) -> VexLifeProdPrivateServiceImage.Observation =
            { imageRoot, expectedSourceSha ->
                VexLifeProdPrivateServiceImage.observe(
                    imageRoot = imageRoot,
                    expectedSourceSha = expectedSourceSha,
                )
            },
        val formFreshHome: (productHome: Path) -> VexLifeProdHomeFiles.Result = { productHome ->
            VexLifeProdLocalSetup.formFreshHome(productHome)
        },
        val establishSession:
            (
                productHome: Path,
                allowAcquisition: Boolean,
                allowRuntimeStart: Boolean,
            ) -> VexLifeProdLocalSetup.Session =
            { productHome, allowAcquisition, allowRuntimeStart ->
                VexLifeProdLocalSetup.establishSession(
                    productHome = productHome,
                    allowAcquisition = allowAcquisition,
                    allowRuntimeStart = allowRuntimeStart,
                    progress = progress,
                )
            },
        val installImage:
            (
                sourceImageRoot: Path,
                productHome: Path,
                expectedSourceSha: String,
            ) -> VexLifeProdPrivateServiceImageInstaller.Result =
            { sourceImageRoot, productHome, expectedSourceSha ->
                VexLifeProdPrivateServiceImageInstaller.install(
                    sourceImageRoot = sourceImageRoot,
                    productHome = productHome,
                    expectedSourceSha = expectedSourceSha,
                )
            },
        val startService:
            (productHome: Path, expectedSourceSha: String) -> VexLifeProdMacServiceLifetime.Result =
            { productHome, expectedSourceSha ->
                VexLifeProdMacServiceLifetime.startOrReuse(
                    productHome = productHome,
                    desiredSourceSha = expectedSourceSha,
                )
            },
        val reportImageVerified: (VexLifeProdPrivateServiceImage.Observation) -> Unit =
            { observation ->
                println(
                    "VEXLIFE_PROD_SETUP_IMAGE_VERIFIED\t" +
                        observation.sourceSha +
                        "\t" +
                        observation.payloadSha256
                )
                System.out.flush()
            },
        val reportHomeReady: (HomeArguments, VexLifeProdHomeFiles.Result) -> Unit =
            { arguments, home ->
                println(
                    "VEXLIFE_PROD_HOME_READY\t" +
                        arguments.expectedSourceSha +
                        "\t" +
                        home.productHome +
                        "\t" +
                        home.manifest.homeRef +
                        "\t" +
                        home.installation.deviceRef +
                        "\t" +
                        home.installation.companionLineageRef
                )
                System.out.flush()
            },
        val reportReady:
            (
                arguments: Arguments,
                session: VexLifeProdLocalSetup.Session,
                installed: VexLifeProdPrivateServiceImageInstaller.Result,
                service: VexLifeProdMacServiceLifetime.Result,
            ) -> Unit =
            { arguments, session, installed, service ->
                println(
                    "VEXLIFE_PROD_SETUP_READY\t" +
                        arguments.expectedSourceSha +
                        "\t" +
                        session.result.productHome +
                        "\t" +
                        installed.imagePath +
                        "\t" +
                        session.result.qualifiedRuntimeEvidence.runtimeReceiptRef +
                        "\t" +
                        service.pid +
                        "\t" +
                        service.serviceOrigin
                )
                System.out.flush()
            },
    )

    private fun selectedHome(value: String): Path {
        val path = Path.of(value)
        require(path.isAbsolute) {
            "selected Vex Home path must be absolute"
        }
        return path.normalize()
    }

    fun parse(args: Array<String>): Arguments {
        require(
            args.size == 5 &&
                args[0] == "--home" &&
                args[2] == "--expected-source-sha" &&
                args[4] in setOf("--allow-acquisition", "--reuse-existing")
        ) {
            "VexLife-Prod setup requires exactly --home <absolute-path> --expected-source-sha <git-sha> (--allow-acquisition|--reuse-existing)"
        }
        val sourceSha = args[3]
        require(GitSha.matches(sourceSha)) {
            "VexLife-Prod setup expected source SHA must be one exact lowercase Git commit"
        }
        return Arguments(
            productHome = selectedHome(args[1]),
            expectedSourceSha = sourceSha,
            allowAcquisition = args[4] == "--allow-acquisition",
        )
    }

    fun parseHome(args: Array<String>): HomeArguments {
        require(
            args.size == 5 &&
                args[0] == "--form-home" &&
                args[1] == "--home" &&
                args[3] == "--expected-source-sha"
        ) {
            "VexLife-Prod fresh Home formation requires exactly --form-home --home <absolute-path> --expected-source-sha <git-sha>"
        }
        val sourceSha = args[4]
        require(GitSha.matches(sourceSha)) {
            "VexLife-Prod fresh Home formation expected source SHA must be one exact lowercase Git commit"
        }
        return HomeArguments(productHome = selectedHome(args[2]), expectedSourceSha = sourceSha)
    }

    fun formHome(args: Array<String>) {
        formHomeWithHooks(arguments = parseHome(args), hooks = Hooks())
    }

    internal fun formHomeWithHooks(arguments: HomeArguments, hooks: Hooks) {
        hooks.progress.phase(VexLifeProdSetupProgress.Phase.VerifyingProduct)
        val imageRoot = hooks.locateImageRoot().toAbsolutePath().normalize()
        val observed = hooks.observeImage(imageRoot, arguments.expectedSourceSha)

        hooks.reportImageVerified(observed)

        hooks.progress.phase(VexLifeProdSetupProgress.Phase.FormingHome)
        val home = hooks.formFreshHome(arguments.productHome)

        hooks.reportHomeReady(arguments, home)
    }

    internal fun realizeHomeFormation(
        desiredSourceSha: String,
        productHome: Path,
        progress: VexLifeProdSetupProgress = VexLifeProdSetupProgress.Silent,
    ) {
        formHomeWithHooks(
            arguments =
                HomeArguments(productHome = productHome, expectedSourceSha = desiredSourceSha),
            hooks =
                Hooks(
                    progress = progress,
                    reportImageVerified = { _ -> },
                    reportHomeReady = { _, _ -> },
                ),
        )
    }

    internal fun realizeProductConvergence(
        desiredSourceSha: String,
        allowAcquisition: Boolean,
        allowRuntimeStart: Boolean,
        productHome: Path,
        progress: VexLifeProdSetupProgress = VexLifeProdSetupProgress.Silent,
    ) {
        runWithHooks(
            arguments =
                Arguments(
                    productHome = productHome,
                    expectedSourceSha = desiredSourceSha,
                    allowAcquisition = allowAcquisition,
                    allowRuntimeStart = allowRuntimeStart,
                ),
            hooks =
                Hooks(
                    progress = progress,
                    reportImageVerified = { _ -> },
                    reportReady = { _, _, _, _ -> },
                ),
        )
    }

    fun run(args: Array<String>) {
        runWithHooks(arguments = parse(args), hooks = Hooks())
    }

    internal fun runWithHooks(arguments: Arguments, hooks: Hooks) {
        hooks.progress.phase(VexLifeProdSetupProgress.Phase.VerifyingProduct)
        val imageRoot = hooks.locateImageRoot().toAbsolutePath().normalize()
        val observed = hooks.observeImage(imageRoot, arguments.expectedSourceSha)

        hooks.reportImageVerified(observed)

        val session =
            hooks.establishSession(
                arguments.productHome,
                arguments.allowAcquisition,
                arguments.allowRuntimeStart,
            )

        hooks.progress.phase(VexLifeProdSetupProgress.Phase.InstallingProduct)
        val installed =
            hooks.installImage(
                imageRoot,
                Path.of(session.result.productHome),
                arguments.expectedSourceSha,
            )

        require(
            installed.sourceSha == observed.sourceSha &&
                installed.payloadSha256 == observed.payloadSha256
        ) {
            "installed VexLife-Prod private image differs from verified setup image"
        }

        hooks.progress.phase(VexLifeProdSetupProgress.Phase.StartingVexLife)
        val service =
            hooks.startService(Path.of(session.result.productHome), arguments.expectedSourceSha)

        require(
            service.sourceSha == installed.sourceSha &&
                service.imagePayloadSha256 == installed.payloadSha256
        ) {
            "started VexLife-Prod service differs from the verified installed image"
        }

        hooks.reportReady(arguments, session, installed, service)
    }

    private fun currentImageRoot(): Path {
        val codeSource =
            Path.of(
                    VexLifeProdSetupProcess::class.java.protectionDomain.codeSource.location.toURI()
                )
                .toAbsolutePath()
                .normalize()

        require(
            Files.isRegularFile(codeSource, LinkOption.NOFOLLOW_LINKS) &&
                !Files.isSymbolicLink(codeSource) &&
                codeSource.fileName.toString() == "vexlife-prod.jar"
        ) {
            "VexLife-Prod setup must execute from the packaged vexlife-prod.jar"
        }

        val libRoot = requireNotNull(codeSource.parent)
        require(libRoot.fileName.toString() == "lib") {
            "VexLife-Prod setup JAR is not inside the private image lib directory"
        }

        return requireNotNull(libRoot.parent)
    }
}

/**
 * Exact flag pairs after a command word; every required flag once, optional flags at most once,
 * nothing else. A malformed shape is refused rather than guessed.
 */
internal fun setupFlags(
    args: List<String>,
    required: Set<String>,
    optional: Set<String> = emptySet(),
): Map<String, String>? {
    if (args.size % 2 != 0) return null
    val pairs = args.chunked(2).map { it[0] to it[1] }
    val names = pairs.map { it.first }
    if (names.toSet().size != names.size) return null
    if (!names.containsAll(required) || !(required + optional).containsAll(names)) return null
    if (pairs.any { it.second.startsWith("--") }) return null
    return pairs.toMap()
}

fun main(args: Array<String>) {
    if (
        args.size == 5 &&
            args[0] == "--form-home" &&
            args[1] == "--home" &&
            args[3] == "--expected-source-sha"
    ) {
        VexLifeProdSetupProcess.formHome(args)
        return
    }

    val command = args.firstOrNull()
    val rest = args.drop(1)

    if (command == "--inspect-front-door") {
        val flags =
            requireNotNull(
                setupFlags(
                    rest,
                    required = setOf("--home", "--expected-source-sha"),
                    optional = setOf("--language", "--system-language", "--model"),
                )
            ) {
                "--inspect-front-door requires --home <path> --expected-source-sha <sha> " +
                    "[--language <code>] [--system-language <code>] [--model <handle>]"
            }
        VexLifeProdMacFrontDoorProcess.inspect(
            desiredSourceSha = flags.getValue("--expected-source-sha"),
            productHome = Path.of(flags.getValue("--home")),
            language = flags["--language"],
            systemLanguage = flags["--system-language"],
            model = flags["--model"],
        )
        return
    }

    if (command == "--activate-front-door-control" && rest.isNotEmpty()) {
        val flags =
            requireNotNull(
                setupFlags(
                    rest.drop(1),
                    required = setOf("--home", "--expected-source-sha"),
                    optional = setOf("--language", "--model"),
                )
            ) {
                "--activate-front-door-control <ref> requires --home <path> --expected-source-sha <sha> " +
                    "[--language <code>] [--model <handle>]"
            }
        VexLifeProdMacFrontDoorProcess.activate(
            controlRef = rest[0],
            desiredSourceSha = flags.getValue("--expected-source-sha"),
            productHome = Path.of(flags.getValue("--home")),
            language = flags["--language"],
            model = flags["--model"],
        )
        return
    }

    if (command == "--setup-language") {
        val flags =
            requireNotNull(
                setupFlags(
                    rest,
                    required = setOf("--home"),
                    optional = setOf("--language", "--system-language"),
                )
            ) {
                "--setup-language requires --home <path> [--language <code>] [--system-language <code>]"
            }
        VexLifeProdMacFrontDoorProcess.observeLanguage(
            productHome = Path.of(flags.getValue("--home")),
            language = flags["--language"],
            systemLanguage = flags["--system-language"],
        )
        return
    }

    if (command == "--choose-language" && rest.isNotEmpty()) {
        val flags =
            requireNotNull(setupFlags(rest.drop(1), required = setOf("--home"))) {
                "--choose-language <code> requires --home <path>"
            }
        VexLifeProdMacFrontDoorProcess.chooseLanguage(
            productHome = Path.of(flags.getValue("--home")),
            language = rest[0],
        )
        return
    }

    if (command == "--setup-model") {
        val flags =
            requireNotNull(setupFlags(rest, required = setOf("--home"), optional = setOf("--model", "--language"))) {
                "--setup-model requires --home <path> [--model <handle>] [--language <code>]"
            }
        VexLifeProdMacFrontDoorProcess.observeModel(
            productHome = Path.of(flags.getValue("--home")),
            model = flags["--model"],
            language = flags["--language"],
        )
        return
    }

    if (command == "--choose-model" && rest.isNotEmpty()) {
        val flags =
            requireNotNull(setupFlags(rest.drop(1), required = setOf("--home"), optional = setOf("--language"))) {
                "--choose-model <handle> requires --home <path> [--language <code>]"
            }
        VexLifeProdMacFrontDoorProcess.chooseModel(
            productHome = Path.of(flags.getValue("--home")),
            model = rest[0],
            language = flags["--language"],
        )
        return
    }

    VexLifeProdSetupProcess.run(args)
}

// [VXG RealForever]
