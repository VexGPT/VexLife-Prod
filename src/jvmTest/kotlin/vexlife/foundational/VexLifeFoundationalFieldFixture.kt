package vexlife.foundational

import vexlife.contract.VexContractJson
import vexlife.foundational.VexLifeFoundationalField.Reading

/** Readings a test can only obtain the way the Product does: from exact body and revision text. */
object VexLifeFoundationalFieldFixture {
    val shipped: Reading.Current
        get() = VexLifeFoundationalField.readCurrent() as Reading.Current

    /** The whole kernel exactly as a prompt with room receives it. */
    val wholeKernel: VexLifeFoundationalField.Projection
        get() =
            VexLifeFoundationalField.project(shipped, VexLifeFoundationalField.KernelTokenBudget)

    /** A later recorded revision whose first module says one more sentence. */
    fun revised(version: String = "0.6.0"): Reading.Current {
        val body = shipped.body
        val first = body.modules.first()
        val changed =
            body.copy(
                version = version,
                modules =
                    listOf(
                        first.copy(kernel = first.kernel + "A sentence added by revision $version.")
                    ) + body.modules.drop(1),
            )
        return read(changed, shipped.revisions) as Reading.Current
    }

    fun read(
        body: VexLifeFoundationalField.Body,
        earlier: List<VexLifeFoundationalField.Revision>,
        recorded: Boolean = true,
    ): Reading =
        VexLifeFoundationalField.read(
            bodyText = VexContractJson.encode(body),
            revisionsText =
                VexContractJson.encode(
                    VexLifeFoundationalField.Revisions(
                        schemaVersion = VexLifeFoundationalField.RevisionsSchema,
                        revisions =
                            earlier +
                                if (recorded) {
                                    listOf(
                                        VexLifeFoundationalField.Revision(
                                            version = body.version,
                                            bodySha256 = VexLifeFoundationalField.fingerprint(body),
                                            recordedOn = "2026-11-15",
                                            recordedBy = "test",
                                            summary = "test revision",
                                            why = "test",
                                        )
                                    )
                                } else {
                                    emptyList()
                                },
                    )
                ),
        )

    val missing: Reading
        get() = VexLifeFoundationalField.read(bodyText = null, revisionsText = null)
}

// [VXG RealForever]
