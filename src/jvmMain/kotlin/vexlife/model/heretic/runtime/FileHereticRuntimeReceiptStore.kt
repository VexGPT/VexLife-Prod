package vexlife.model.heretic.runtime

import java.nio.file.Path
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.model.heretic.HereticProductIdentity

/**
 * Durable receipt store for a VexLife-Prod-owned Heretic runtime.
 *
 * Storage mechanics are delegated to [VexDurableContractFile]. This store writes only the
 * product-owned coordinate beneath runtime/vexlife-prod; the stable VexLife predecessor receipt
 * remains separate and read-only to this owner.
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
            HereticProductIdentity.ProductRuntimeReceiptRelativePath,
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
