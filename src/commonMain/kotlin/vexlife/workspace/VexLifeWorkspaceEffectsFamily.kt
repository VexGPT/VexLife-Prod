package vexlife.workspace

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.sources.VexLifeLocalSourcesFamily

/**
 * Semantic owner for the person's explicit grant that VexLife may perform bounded workspace
 * effects inside one already-granted Local Source.
 *
 * Local Sources remains read-only. This Family owns only the stronger effect grant.
 *
 * Permanent:
 *
 * ```text
 * READABLE != WRITABLE
 * LOCAL SOURCE GRANT != WORKSPACE EFFECT GRANT
 * WORKSPACE EFFECT GRANT != GENERIC FILESYSTEM AUTHORITY
 * WORKSPACE EFFECT GRANT != GENERIC SHELL AUTHORITY
 * ```
 */
object VexLifeWorkspaceEffectsFamily :
    VexFamilyContract<
        VexLifeWorkspaceEffectsFamily,
        VexLifeWorkspaceEffectsFamily.State,
        VexLifeWorkspaceEffectsFamily.Control,
        VexLifeWorkspaceEffectsFamily.Effect,
    >() {
    const val MaximumGrants = VexLifeLocalSourcesFamily.MaximumGrants
    const val GrantRefPrefix = "grant.vexlife-prod.workspace.sha256."

    private val Sha256 = Regex("[0-9a-f]{64}")

    @Serializable
    data class Grant(
        val grantRef: String,
        val sourceRef: String,
        /** Exact Local Source grant generation this stronger authority is bound to. */
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
                "workspace effect grant is incomplete"
            }
        }
    }

    sealed interface State : VexFamilyContract.State<VexLifeWorkspaceEffectsFamily> {
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
                "workspace effect grants must be unique and within the Product limit"
            }
        }
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeWorkspaceEffectsFamily> {
        val controlRef: String
    }

    data class Allow(
        val sourceRef: String,
    ) : Control {
        init {
            require(VexLifeLocalSourcesFamily.isSourceRef(sourceRef)) {
                "workspace Allow requires one exact Local Source ref"
            }
        }

        override val controlRef = ControlAllow
    }

    data class Revoke(
        val sourceRef: String,
    ) : Control {
        init {
            require(VexLifeLocalSourcesFamily.isSourceRef(sourceRef)) {
                "workspace Revoke requires one exact Local Source ref"
            }
        }

        override val controlRef = ControlRevoke
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeWorkspaceEffectsFamily> {
        data class GrantWorkspace(val sourceRef: String) : Effect
        data class RevokeWorkspace(val sourceRef: String) : Effect
    }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeWorkspaceEffectsFamily, State, Effect>? =
        when (control) {
            is Allow ->
                if (
                    currentState.grants.none { it.sourceRef == control.sourceRef } &&
                    currentState.grants.size < MaximumGrants
                ) {
                    stay(Effect.GrantWorkspace(control.sourceRef))
                } else {
                    null
                }

            is Revoke ->
                currentState.grants
                    .singleOrNull { it.sourceRef == control.sourceRef }
                    ?.let { stay(Effect.RevokeWorkspace(control.sourceRef)) }
        }

    fun state(grants: List<Grant>): State =
        if (grants.isEmpty()) Empty else Granted(grants.toList())

    fun isGrantRef(value: String): Boolean =
        value.startsWith(GrantRefPrefix) &&
            Sha256.matches(value.removePrefix(GrantRefPrefix))

    const val StateEmpty = "EMPTY"
    const val StateGranted = "GRANTED"
    const val ControlAllow = "ALLOW_WORKSPACE_EFFECTS"
    const val ControlRevoke = "REVOKE_WORKSPACE_EFFECTS"

    val ControlRefs: Set<String> = setOf(ControlAllow, ControlRevoke)
}

// [VXG RealForever]
