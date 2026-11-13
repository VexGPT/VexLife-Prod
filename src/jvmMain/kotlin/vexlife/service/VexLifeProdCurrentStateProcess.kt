package vexlife.service

import vexlife.contract.VexContractJson

/**
 * No-effect host probe for current VexLife-Prod awareness.
 *
 * This entry exists so lifecycle/front-door proof can observe current machine/Home/model state
 * before setup, repair, runtime recovery or model-selection effects are admitted.
 */
object VexLifeProdCurrentStateProcess {
    fun run() {
        println(
            VexContractJson.encode(
                VexLifeProdCurrentState.observeCurrentMac(),
            ),
        )
    }
}

fun main() {
    VexLifeProdCurrentStateProcess.run()
}

// [VXG RealForever]
