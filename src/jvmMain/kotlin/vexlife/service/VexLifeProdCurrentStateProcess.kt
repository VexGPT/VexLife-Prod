package vexlife.service

import java.nio.file.Path
import vexlife.contract.VexContractJson
import vexlife.home.VexLifeProdHomeFiles

/**
 * No-effect host probe for current VexLife-Prod awareness.
 *
 * This entry exists so lifecycle/front-door proof can observe current machine/Home/model state
 * before setup, repair, runtime recovery or model-selection effects are admitted.
 *
 * Usage: `[--home <absolute Vex Home>]`. Without `--home` the probe observes the default
 * suggestion `~/.vexlife`, named explicitly here at the command-line edge; the current-state owner
 * itself has no default Home.
 */
object VexLifeProdCurrentStateProcess {
    fun run(args: Array<String>) {
        val home =
            when {
                args.isEmpty() -> VexLifeProdHomeFiles.defaultMacRoot(Path.of(System.getProperty("user.home")))
                args.size == 2 && args[0] == "--home" -> Path.of(args[1])
                else -> throw IllegalArgumentException("usage: [--home <absolute Vex Home>]")
            }
        println(VexContractJson.encode(VexLifeProdCurrentState.observeCurrentMac(home)))
    }
}

fun main(args: Array<String>) {
    VexLifeProdCurrentStateProcess.run(args)
}

// [VXG RealForever]
