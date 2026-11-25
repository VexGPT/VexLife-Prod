package vexlife.workspace

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlinx.serialization.Serializable
import vexlife.contract.VexDurableContractFile
import vexlife.filesystem.VexOwnedPath
import vexlife.home.VexLifeProdHomeFiles
import vexlife.sources.VexLifeLocalSourceStore

/** Durable custody for the person's separate bounded workspace-check grant. */
class VexLifeWorkspaceCheckStore(
    productHome: Path,
    private val localSources: VexLifeLocalSourceStore =
        VexLifeLocalSourceStore(productHome),
    private val now: () -> Instant = Instant::now,
) {
    @Serializable
    private data class Persisted(
        val schemaVersion: String,
        val grants: List<VexLifeWorkspaceChecksFamily.Grant>,
    ) {
        init {
            require(
                schemaVersion == Schema &&
                    grants.size <= VexLifeWorkspaceChecksFamily.MaximumGrants &&
                    grants.map { it.grantRef }.distinct().size == grants.size &&
                    grants.map { it.sourceRef }.distinct().size == grants.size,
            ) {
                "workspace check persisted grant set is invalid"
            }
        }
    }

    sealed interface Outcome {
        data class Realized(
            val grants: List<VexLifeWorkspaceChecksFamily.Grant>,
        ) : Outcome

        data class Refused(
            val reason: String,
            val grants: List<VexLifeWorkspaceChecksFamily.Grant>,
        ) : Outcome
    }

    private val home =
        VexOwnedPath.requireRoot(
            productHome,
            "canonical VexLife Home",
        )

    init {
        requireNotNull(VexLifeProdHomeFiles.observeExisting(home)) {
            "workspace checks require one canonical VexLife Home"
        }
    }

    fun grants(): List<VexLifeWorkspaceChecksFamily.Grant> =
        VexDurableContractFile.read<Persisted>(
            ownedRoot = home,
            path = file(),
        )?.grants.orEmpty()

    /** Only grants whose exact Local Source generation is still current. */
    fun currentGrants(): List<VexLifeWorkspaceChecksFamily.Grant> {
        val sources =
            localSources
                .grants()
                .associateBy {
                    it.sourceRef
                }
        return grants().filter {
            grant ->
            sources[grant.sourceRef]?.grantedAt ==
                grant.sourceGrantedAt &&
                localSources.root(
                    grant.sourceRef,
                ) !=
                null
        }
    }

    fun currentGrant(
        sourceRef: String,
    ): VexLifeWorkspaceChecksFamily.Grant? =
        currentGrants()
            .singleOrNull {
                it.sourceRef ==
                    sourceRef
            }

    fun state(): VexLifeWorkspaceChecksFamily.State =
        VexLifeWorkspaceChecksFamily.state(
            currentGrants(),
        )

    fun grant(
        sourceRef: String,
    ): Outcome {
        val source =
            localSources
                .grants()
                .singleOrNull {
                    it.sourceRef ==
                        sourceRef
                }
                ?: return Outcome.Refused(
                    RefusedSourceNotCurrent,
                    currentGrants(),
                )
        if (
            localSources.root(
                sourceRef,
            ) ==
            null
        ) {
            return Outcome.Refused(
                RefusedSourceNotCurrent,
                currentGrants(),
            )
        }

        val current =
            grants()
        val exactCurrent =
            current.singleOrNull {
                it.sourceRef ==
                    sourceRef &&
                    it.sourceGrantedAt ==
                    source.grantedAt
            }
        if (
            exactCurrent !=
                null
        ) {
            return Outcome.Refused(
                RefusedAlreadyGranted,
                currentGrants(),
            )
        }

        val grantedAt =
            now()
                .toString()
        val nextGrant =
            VexLifeWorkspaceChecksFamily.Grant(
                grantRef =
                    VexLifeWorkspaceChecksFamily.GrantRefPrefix +
                        sha256(
                            listOf(
                                source.sourceRef,
                                source.grantedAt,
                                grantedAt,
                            )
                                .joinToString(
                                    "\u0000",
                                ),
                        ),
                sourceRef =
                    source.sourceRef,
                sourceGrantedAt =
                    source.grantedAt,
                grantedAt =
                    grantedAt,
            )

        val next =
            current.filterNot {
                it.sourceRef ==
                    sourceRef
            } +
                nextGrant
        if (
            next.size >
                VexLifeWorkspaceChecksFamily.MaximumGrants
        ) {
            return Outcome.Refused(
                RefusedLimitReached,
                currentGrants(),
            )
        }

        write(
            next,
        )
        return Outcome.Realized(
            currentGrants(),
        )
    }

    fun revoke(
        sourceRef: String,
    ): Outcome {
        val current =
            grants()
        if (
            current.none {
                it.sourceRef ==
                    sourceRef
            }
        ) {
            return Outcome.Refused(
                RefusedNotGranted,
                currentGrants(),
            )
        }

        write(
            current.filterNot {
                it.sourceRef ==
                    sourceRef
            },
        )
        return Outcome.Realized(
            currentGrants(),
        )
    }

    private fun write(
        grants: List<VexLifeWorkspaceChecksFamily.Grant>,
    ) {
        VexDurableContractFile.write(
            ownedRoot =
                home,
            path =
                file(),
            value =
                Persisted(
                    Schema,
                    grants,
                ),
        )
        require(
            grants() ==
                grants,
        ) {
            "workspace checks did not reobserve the exact persisted grant set"
        }
    }

    private fun file(): Path =
        VexOwnedPath.resolveInside(
            home,
            RelativePath,
        )

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance(
                "SHA-256",
            )
            .digest(
                value.toByteArray(
                    StandardCharsets.UTF_8,
                ),
            )
            .joinToString(
                "",
            ) {
                "%02x".format(
                    it.toInt() and
                        0xff,
                )
            }

    companion object {
        const val Schema =
            "vexlife-prod.workspace-checks/v1"

        const val RelativePath =
            "runtime/vexlife-prod/settings/workspace-checks.json"

        const val RefusedSourceNotCurrent =
            "WORKSPACE_CHECK_SOURCE_NOT_CURRENT"

        const val RefusedAlreadyGranted =
            "WORKSPACE_CHECKS_ALREADY_GRANTED"

        const val RefusedLimitReached =
            "WORKSPACE_CHECKS_LIMIT_REACHED"

        const val RefusedNotGranted =
            "WORKSPACE_CHECKS_NOT_GRANTED"
    }
}

// [VXG RealForever]
