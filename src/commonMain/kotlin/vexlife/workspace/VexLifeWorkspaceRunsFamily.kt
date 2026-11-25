package vexlife.workspace

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.sources.VexLifeLocalSourcesFamily

/**
 * Semantic owner for the person's explicit grant that VexLife may compile and execute bounded
 * project tests from one already-granted Local Source.
 *
 * This relationship is intentionally distinct from both readable Local Sources and workspace write
 * authority.
 *
 * Permanent:
 *
 * ```text
 * READABLE != WRITABLE != PROJECT_RUN_AUTHORITY
 * WORKSPACE RUN GRANT != GENERIC SHELL AUTHORITY
 * WORKSPACE RUN GRANT != PACKAGE MANAGER / PUBLICATION AUTHORITY
 * WORKSPACE RUN GRANT != SOURCE-WRITE AUTHORITY
 * ```
 */
object VexLifeWorkspaceRunsFamily :
    VexFamilyContract<
        VexLifeWorkspaceRunsFamily,
        VexLifeWorkspaceRunsFamily.State,
        VexLifeWorkspaceRunsFamily.Control,
        VexLifeWorkspaceRunsFamily.Effect,
    >() {
    const val MaximumGrants = VexLifeLocalSourcesFamily.MaximumGrants
    const val GrantRefPrefix = "grant.vexlife-prod.workspace-run.sha256."

    private val Sha256 = Regex("[0-9a-f]{64}")

    @Serializable
    data class Grant(
        val grantRef: String,
        val sourceRef: String,
        /** Exact Local Source grant generation this process-check authority is bound to. */
        val sourceGrantedAt: String,
        val grantedAt: String,
    ) {
        init {
            require(
                isGrantRef(grantRef) &&
                    VexLifeLocalSourcesFamily.isSourceRef(sourceRef) &&
                    sourceGrantedAt.isNotBlank() &&
                    grantedAt.isNotBlank(),
            ) {
                "workspace run grant is incomplete"
            }
        }
    }

    sealed interface State : VexFamilyContract.State<VexLifeWorkspaceRunsFamily> {
        val stateRef: String
        val grants: List<Grant>
    }

    data object Empty : State {
        override val stateRef = StateEmpty
        override val grants: List<Grant> = emptyList()
    }

    data class Granted(
        override val grants: List<Grant>,
    ) : State {
        override val stateRef = StateGranted

        init {
            require(
                grants.isNotEmpty() &&
                    grants.size <= MaximumGrants &&
                    grants.map(Grant::grantRef).distinct().size == grants.size &&
                    grants.map(Grant::sourceRef).distinct().size == grants.size,
            ) {
                "workspace run grants must be unique and within the Product limit"
            }
        }
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeWorkspaceRunsFamily> {
        val controlRef: String
    }

    data class Allow(
        val sourceRef: String,
    ) : Control {
        init {
            require(VexLifeLocalSourcesFamily.isSourceRef(sourceRef)) {
                "workspace run Allow requires one exact Local Source ref"
            }
        }

        override val controlRef = ControlAllow
    }

    data class Revoke(
        val sourceRef: String,
    ) : Control {
        init {
            require(VexLifeLocalSourcesFamily.isSourceRef(sourceRef)) {
                "workspace run Revoke requires one exact Local Source ref"
            }
        }

        override val controlRef = ControlRevoke
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeWorkspaceRunsFamily> {
        data class GrantRuns(val sourceRef: String) : Effect
        data class RevokeRuns(val sourceRef: String) : Effect
    }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeWorkspaceRunsFamily, State, Effect>? =
        when (control) {
            is Allow ->
                if (
                    currentState.grants.none { it.sourceRef == control.sourceRef } &&
                    currentState.grants.size < MaximumGrants
                ) {
                    stay(Effect.GrantRuns(control.sourceRef))
                } else {
                    null
                }

            is Revoke ->
                currentState.grants
                    .singleOrNull { it.sourceRef == control.sourceRef }
                    ?.let { stay(Effect.RevokeRuns(control.sourceRef)) }
        }

    fun state(grants: List<Grant>): State =
        if (grants.isEmpty()) Empty else Granted(grants.toList())

    fun isGrantRef(value: String): Boolean =
        value.startsWith(GrantRefPrefix) &&
            Sha256.matches(value.removePrefix(GrantRefPrefix))

    const val StateEmpty = "EMPTY"
    const val StateGranted = "GRANTED"
    const val ControlAllow = "ALLOW_WORKSPACE_RUNS"
    const val ControlRevoke = "REVOKE_WORKSPACE_RUNS"

    val ControlRefs: Set<String> =
        setOf(
            ControlAllow,
            ControlRevoke,
        )
}

// [VXG RealForever]
