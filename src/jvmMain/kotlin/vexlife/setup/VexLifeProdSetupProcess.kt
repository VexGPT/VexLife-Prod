package vexlife.setup

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import vexlife.home.VexLifeProdHomeFiles
import vexlife.local.VexLifeProdLocalSetup
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
    private val GitSha =
        Regex(
            "[0-9a-f]{40}",
        )

    data class Arguments(
        val expectedSourceSha: String,
        val allowAcquisition: Boolean,
    )

    data class HomeArguments(
        val expectedSourceSha: String,
    )

    internal data class Hooks(
        val locateImageRoot: () -> Path =
            ::currentImageRoot,
        val observeImage: (
            imageRoot: Path,
            expectedSourceSha: String,
        ) -> VexLifeProdPrivateServiceImage.Observation = {
            imageRoot,
            expectedSourceSha,
            ->
            VexLifeProdPrivateServiceImage.observe(
                imageRoot = imageRoot,
                expectedSourceSha = expectedSourceSha,
            )
        },
        val formFreshHome: () -> VexLifeProdHomeFiles.Result = {
            VexLifeProdLocalSetup.formFreshHome()
        },
        val establishSession: (
            allowAcquisition: Boolean,
        ) -> VexLifeProdLocalSetup.Session = {
            allowAcquisition ->
            VexLifeProdLocalSetup.establishSession(
                allowAcquisition,
            )
        },
        val installImage: (
            sourceImageRoot: Path,
            productHome: Path,
            expectedSourceSha: String,
        ) -> VexLifeProdPrivateServiceImageInstaller.Result = {
            sourceImageRoot,
            productHome,
            expectedSourceSha,
            ->
            VexLifeProdPrivateServiceImageInstaller.install(
                sourceImageRoot = sourceImageRoot,
                productHome = productHome,
                expectedSourceSha = expectedSourceSha,
            )
        },
        val startService: (
            productHome: Path,
            expectedSourceSha: String,
        ) -> VexLifeProdMacServiceLifetime.Result = {
            productHome,
            expectedSourceSha,
            ->
            VexLifeProdMacServiceLifetime.startOrReuse(
                productHome = productHome,
                desiredSourceSha = expectedSourceSha,
            )
        },
        val reportImageVerified: (
            VexLifeProdPrivateServiceImage.Observation,
        ) -> Unit = {
            observation ->
            println(
                "VEXLIFE_PROD_SETUP_IMAGE_VERIFIED\t" +
                    observation.sourceSha +
                    "\t" +
                    observation.payloadSha256,
            )
            System.out.flush()
        },
        val reportHomeReady: (
            HomeArguments,
            VexLifeProdHomeFiles.Result,
        ) -> Unit = {
            arguments,
            home,
            ->
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
                    home.installation.companionLineageRef,
            )
            System.out.flush()
        },
        val reportReady: (
            arguments: Arguments,
            session: VexLifeProdLocalSetup.Session,
            installed: VexLifeProdPrivateServiceImageInstaller.Result,
            service: VexLifeProdMacServiceLifetime.Result,
        ) -> Unit = {
            arguments,
            session,
            installed,
            service,
            ->
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
                    service.serviceOrigin,
            )
            System.out.flush()
        },
    )

    fun parse(
        args: Array<String>,
    ): Arguments {
        require(
            args.size ==
                3 &&
                args[0] ==
                "--expected-source-sha" &&
                args[2] in
                setOf(
                    "--allow-acquisition",
                    "--reuse-existing",
                ),
        ) {
            "VexLife-Prod setup requires exactly --expected-source-sha <git-sha> (--allow-acquisition|--reuse-existing)"
        }

        val sourceSha =
            args[1]
        require(
            GitSha.matches(
                sourceSha,
            ),
        ) {
            "VexLife-Prod setup expected source SHA must be one exact lowercase Git commit"
        }

        return Arguments(
            expectedSourceSha = sourceSha,
            allowAcquisition =
                args[2] ==
                    "--allow-acquisition",
        )
    }

    fun parseHome(
        args: Array<String>,
    ): HomeArguments {
        require(
            args.size ==
                3 &&
                args[0] ==
                "--form-home" &&
                args[1] ==
                "--expected-source-sha"
        ) {
            "VexLife-Prod fresh Home formation requires exactly --form-home --expected-source-sha <git-sha>"
        }

        val sourceSha =
            args[2]
        require(
            GitSha.matches(
                sourceSha,
            ),
        ) {
            "VexLife-Prod fresh Home formation expected source SHA must be one exact lowercase Git commit"
        }

        return HomeArguments(
            expectedSourceSha = sourceSha,
        )
    }

    fun formHome(
        args: Array<String>,
    ) {
        formHomeWithHooks(
            arguments =
                parseHome(
                    args,
                ),
            hooks = Hooks(),
        )
    }

    internal fun formHomeWithHooks(
        arguments: HomeArguments,
        hooks: Hooks,
    ) {
        val imageRoot =
            hooks.locateImageRoot()
                .toAbsolutePath()
                .normalize()
        val observed =
            hooks.observeImage(
                imageRoot,
                arguments.expectedSourceSha,
            )

        hooks.reportImageVerified(
            observed,
        )

        val home =
            hooks.formFreshHome()

        hooks.reportHomeReady(
            arguments,
            home,
        )
    }

    internal fun realizeHomeFormation(
        desiredSourceSha: String,
    ) {
        formHomeWithHooks(
            arguments =
                HomeArguments(
                    expectedSourceSha = desiredSourceSha,
                ),
            hooks =
                Hooks(
                    reportImageVerified = { _ -> },
                    reportHomeReady = { _, _ -> },
                ),
        )
    }

    internal fun realizeProductConvergence(
        desiredSourceSha: String,
        allowAcquisition: Boolean,
    ) {
        runWithHooks(
            arguments =
                Arguments(
                    expectedSourceSha = desiredSourceSha,
                    allowAcquisition = allowAcquisition,
                ),
            hooks =
                Hooks(
                    reportImageVerified = { _ -> },
                    reportReady = { _, _, _, _ -> },
                ),
        )
    }

    fun run(
        args: Array<String>,
    ) {
        runWithHooks(
            arguments =
                parse(
                    args,
                ),
            hooks = Hooks(),
        )
    }

    internal fun runWithHooks(
        arguments: Arguments,
        hooks: Hooks,
    ) {
        val imageRoot =
            hooks.locateImageRoot()
                .toAbsolutePath()
                .normalize()
        val observed =
            hooks.observeImage(
                imageRoot,
                arguments.expectedSourceSha,
            )

        hooks.reportImageVerified(
            observed,
        )

        val session =
            hooks.establishSession(
                arguments.allowAcquisition,
            )

        val installed =
            hooks.installImage(
                imageRoot,
                Path.of(
                    session.result.productHome,
                ),
                arguments.expectedSourceSha,
            )

        require(
            installed.sourceSha ==
                observed.sourceSha &&
                installed.payloadSha256 ==
                observed.payloadSha256,
        ) {
            "installed VexLife-Prod private image differs from verified setup image"
        }

        val service =
            hooks.startService(
                Path.of(
                    session.result.productHome,
                ),
                arguments.expectedSourceSha,
            )

        require(
            service.sourceSha ==
                installed.sourceSha &&
                service.imagePayloadSha256 ==
                installed.payloadSha256,
        ) {
            "started VexLife-Prod service differs from the verified installed image"
        }

        hooks.reportReady(
            arguments,
            session,
            installed,
            service,
        )
    }

    private fun currentImageRoot(): Path {
        val codeSource =
            Path.of(
                VexLifeProdSetupProcess::class
                    .java
                    .protectionDomain
                    .codeSource
                    .location
                    .toURI(),
            )
                .toAbsolutePath()
                .normalize()

        require(
            Files.isRegularFile(
                codeSource,
                LinkOption.NOFOLLOW_LINKS,
            ) &&
                !Files.isSymbolicLink(
                    codeSource,
                ) &&
                codeSource.fileName
                    .toString() ==
                "vexlife-prod.jar",
        ) {
            "VexLife-Prod setup must execute from the packaged vexlife-prod.jar"
        }

        val libRoot =
            requireNotNull(
                codeSource.parent,
            )
        require(
            libRoot.fileName
                .toString() ==
                "lib",
        ) {
            "VexLife-Prod setup JAR is not inside the private image lib directory"
        }

        return requireNotNull(
            libRoot.parent,
        )
    }
}

fun main(
    args: Array<String>,
) {
    if (
        args.size ==
            3 &&
        args[0] ==
            "--form-home" &&
        args[1] ==
            "--expected-source-sha"
    ) {
        VexLifeProdSetupProcess.formHome(
            args,
        )
        return
    }

    if (
        args.size ==
            3 &&
        args[0] ==
            "--inspect-front-door" &&
        args[1] ==
            "--expected-source-sha"
    ) {
        VexLifeProdMacFrontDoorProcess.inspect(
            args[2],
        )
        return
    }

    if (
        args.size ==
            4 &&
        args[0] ==
            "--activate-front-door-control" &&
        args[2] ==
            "--expected-source-sha"
    ) {
        VexLifeProdMacFrontDoorProcess.activate(
            controlRef = args[1],
            desiredSourceSha = args[3],
        )
        return
    }

    VexLifeProdSetupProcess.run(
        args,
    )
}

// [VXG RealForever]
