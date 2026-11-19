package vexlife.sources

import kotlinx.serialization.Serializable

/**
 * Folders on this computer the person has granted Vex to read.
 *
 * Meaning owned here:
 * - a grant is one exact folder the person explicitly added; only [Control.ADD] creates one and
 *   only [Control.REMOVE] ends one;
 * - which folders are never grantable ([Refusal]) — the filesystem root, the person's whole home
 *   folder, and anything overlapping the Vex Home, which has its own reach;
 * - reading a granted folder never changes it.
 *
 * Permanent distinctions:
 *
 * ```text
 * CHOSEN / PICKED FOLDER != GRANTED FOLDER   (a picker result is a proposal; ADD admits it)
 * LOCAL SOURCE           != VEX HOME         (Home is Vex's own; sources are the person's)
 * LOCAL SOURCE           != TURN ATTACHMENT  (a grant persists across turns; an attachment is bytes)
 * GRANTED                != EVERYTHING BELOW IS READ (Vex reads what a turn asks for, bounded)
 * ```
 */
object VexLifeLocalSources {
    const val Schema = "vexlife-prod.local-sources/v1"
    const val RefPrefix = "source.vexlife-prod.folder."
    const val MaximumGrants = 16

    private val RefPattern = Regex(Regex.escape(RefPrefix) + "[0-9a-f]{16}")

    enum class Control {
        ADD,
        REMOVE,
    }

    enum class Refusal {
        NOT_ABSOLUTE,
        NOT_A_FOLDER,
        LINK,
        TOO_BROAD,
        OVERLAPS_VEX_HOME,
        ALREADY_GRANTED,
        LIMIT_REACHED,
        NOT_GRANTED,
    }

    @Serializable
    data class Grant(
        val ref: String,
        val name: String,
        val path: String,
        val grantedAt: String,
    ) {
        init {
            require(isRef(ref)) { "local source ref is invalid" }
            require(name.isNotBlank() && path.isNotBlank()) { "local source grant is incomplete" }
        }
    }

    @Serializable
    data class Grants(
        val schemaVersion: String,
        val grants: List<Grant>,
    ) {
        init {
            require(schemaVersion == Schema) { "local sources schema is not current" }
            require(grants.size <= MaximumGrants) { "too many local sources" }
            require(grants.map { it.ref }.distinct().size == grants.size) { "local source refs repeat" }
        }
    }

    fun isRef(value: String): Boolean = RefPattern.matches(value)
}

// [VXG RealForever]
