package vexlife.model.heretic.runtime

import java.nio.file.Path
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
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
    private val productHome: Path =
        VexOwnedPath.requireRoot(
            productHome,
            "VexLife-Prod Home",
        )

    private val receiptPath: Path =
        VexOwnedPath.resolveInside(
            this.productHome,
            HereticProductIdentity.RuntimeReceiptRelativePath,
        )

    override fun read(): HereticPersistentRuntime.Receipt? =
        VexDurableContractFile.read(
            ownedRoot = productHome,
            path = receiptPath,
        )

    override fun write(
        receipt: HereticPersistentRuntime.Receipt,
    ) {
        VexDurableContractFile.write(
            ownedRoot = productHome,
            path = receiptPath,
            value = receipt,
        )
    }

    override fun delete() {
        VexDurableContractFile.delete(
            ownedRoot = productHome,
            path = receiptPath,
        )
    }

    fun path(): Path =
        receiptPath
}

// [VXG RealForever]
