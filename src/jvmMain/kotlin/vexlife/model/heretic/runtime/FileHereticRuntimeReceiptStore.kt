package vexlife.model.heretic.runtime

import java.nio.file.Path
import vexlife.contract.VexDurableContractFile
import vexlife.model.heretic.HereticProductIdentity

/**
 * Durable receipt store for the isolated VexLife-Prod Heretic runtime.
 *
 * Storage mechanics are delegated to [VexDurableContractFile]. This class contributes only the
 * rightful Heretic receipt coordinate inside the Prod Home.
 */
class FileHereticRuntimeReceiptStore(
    productHome: Path,
) : HereticPersistentRuntime.ReceiptStore {
    private val receiptPath: Path =
        productHome
            .also {
                require(
                    it.isAbsolute,
                ) {
                    "VexLife-Prod Home must be absolute"
                }
            }
            .normalize()
            .resolve(
                HereticProductIdentity.RuntimeReceiptRelativePath,
            )
            .normalize()
            .also {
                require(
                    it.startsWith(
                        productHome.normalize(),
                    ),
                ) {
                    "Heretic runtime receipt path escaped VexLife-Prod Home"
                }
            }

    override fun read(): HereticPersistentRuntime.Receipt? =
        VexDurableContractFile.read(
            receiptPath,
        )

    override fun write(
        receipt: HereticPersistentRuntime.Receipt,
    ) {
        VexDurableContractFile.write(
            receiptPath,
            receipt,
        )
    }

    override fun delete() {
        VexDurableContractFile.delete(
            receiptPath,
        )
    }

    fun path(): Path =
        receiptPath
}

// [VXG RealForever]
