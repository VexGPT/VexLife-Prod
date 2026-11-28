package vexlife.capability

import java.nio.file.Path
import kotlin.system.exitProcess
import vexlife.contract.VexContractJson

/**
 * Child-process entry for the one registered workspace text-check command.
 *
 * The parent chooses the executable, classpath, working scope and timeout. This process accepts one
 * typed JSON request only and never evaluates shell or project code.
 */
fun main(
    args: Array<String>,
) {
    val status =
        try {
            require(
                args.size ==
                    1,
            ) {
                "workspace text-check process requires one typed request"
            }

            val request =
                VexContractJson.decode<VexLifeWorkspaceTextCheck.Request>(
                    args.single(),
                )
            val result =
                VexLifeWorkspaceTextCheck.evaluate(
                    root =
                        Path.of(
                            "",
                        )
                            .toAbsolutePath()
                            .normalize(),
                    request =
                        request,
                )
            print(
                VexContractJson.canonicalEncode(
                    result,
                ),
            )
            0
        } catch (
            error: Throwable,
        ) {
            System.err.print(
                "VEXLIFE_WORKSPACE_TEXT_CHECK_PROCESS_FAILED\t" +
                    error.javaClass.simpleName,
            )
            2
        }

    if (
        status !=
            0
    ) {
        exitProcess(
            status,
        )
    }
}

// [VXG RealForever]
