package vexlife.sources

import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.sources.VexLifeLocalSources.Grant
import vexlife.sources.VexLifeLocalSources.Grants
import vexlife.sources.VexLifeLocalSources.Refusal

/**
 * The person's granted folders, kept in their Vex Home. Only [add] and [remove] change the set;
 * observing a folder (or a native picker returning one) never does.
 */
class VexLifeLocalSourceStore(
    private val productHome: Path,
    private val personHome: Path? = System.getProperty("user.home")?.let { Path.of(it) },
    private val now: () -> Instant = Instant::now,
) {
    sealed interface Outcome {
        data class Realized(val grants: List<Grant>) : Outcome

        data class Refused(val refusal: Refusal, val grants: List<Grant>) : Outcome
    }

    fun grants(): List<Grant> =
        VexDurableContractFile.read<Grants>(ownedRoot = home(), path = file())?.grants.orEmpty()

    /** The current real folder for [ref], or null when it is not granted or no longer a real folder. */
    fun root(ref: String): Path? =
        grants()
            .firstOrNull { it.ref == ref }
            ?.let { Path.of(it.path) }
            ?.takeIf { Files.isDirectory(it, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(it) }

    fun add(rawPath: String): Outcome {
        val current = grants()
        fun refused(refusal: Refusal) = Outcome.Refused(refusal, current)

        val requested = runCatching { Path.of(rawPath.trim()) }.getOrNull() ?: return refused(Refusal.NOT_ABSOLUTE)
        if (!requested.isAbsolute) return refused(Refusal.NOT_ABSOLUTE)
        val folder = requested.normalize()
        if (Files.isSymbolicLink(folder)) return refused(Refusal.LINK)
        if (!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) return refused(Refusal.NOT_A_FOLDER)
        val real = folder.toRealPath()
        if (real != folder.toRealPath(LinkOption.NOFOLLOW_LINKS)) return refused(Refusal.LINK)
        if (real.parent == null || real == personHome?.toRealPathOrNull()) return refused(Refusal.TOO_BROAD)
        val vexHome = home().toRealPath()
        if (real.startsWith(vexHome) || vexHome.startsWith(real)) return refused(Refusal.OVERLAPS_VEX_HOME)

        val ref = refFor(real)
        if (current.any { it.ref == ref }) return refused(Refusal.ALREADY_GRANTED)
        if (current.size >= VexLifeLocalSources.MaximumGrants) return refused(Refusal.LIMIT_REACHED)

        val next =
            current +
                Grant(
                    ref = ref,
                    name = real.fileName?.toString() ?: real.toString(),
                    path = real.toString(),
                    grantedAt = now().toString(),
                )
        write(next)
        return Outcome.Realized(next)
    }

    fun remove(ref: String): Outcome {
        val current = grants()
        if (current.none { it.ref == ref }) return Outcome.Refused(Refusal.NOT_GRANTED, current)
        val next = current.filterNot { it.ref == ref }
        write(next)
        return Outcome.Realized(next)
    }

    private fun write(grants: List<Grant>) {
        VexDurableContractFile.write(
            ownedRoot = home(),
            path = file(),
            value = Grants(VexLifeLocalSources.Schema, grants),
        )
    }

    private fun home(): Path = VexOwnedPath.requireRoot(productHome, "canonical VexLife Home")

    private fun file(): Path = VexOwnedPath.resolveInside(home(), RelativePath)

    private fun Path.toRealPathOrNull(): Path? = runCatching { toRealPath() }.getOrNull()

    private fun refFor(real: Path): String =
        VexLifeLocalSources.RefPrefix +
            MessageDigest.getInstance("SHA-256")
                .digest(real.toString().toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                .take(16)

    companion object {
        const val RelativePath = "runtime/vexlife-prod/settings/local-sources.json"
    }
}

// [VXG RealForever]
