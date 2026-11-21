package vexlife.context

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

/**
 * Product semantic owner for the person's chosen Context Profile.
 *
 * The Family owns only the relationship "which configuration should construct the next Vex turn".
 * It does not own any context content: Foundation, Conversation, Memory, Working Continuity, browser
 * context, capability reach and model reasoning keep their existing owners.
 */
object VexLifeContextProfileFamily :
    VexFamilyContract<
        VexLifeContextProfileFamily,
        VexLifeContextProfileFamily.State,
        VexLifeContextProfileFamily.Control,
        VexLifeContextProfileFamily.Effect,
    >() {
    sealed interface State : VexFamilyContract.State<VexLifeContextProfileFamily> {
        val stateRef: String
    }

    data class ProductDefault(
        val foundation: VexLifeContextProfile.ProductFoundationIdentity,
    ) : State {
        override val stateRef = StateProductDefault
    }

    data class Saved(
        val profile: VexLifeContextProfile.Effective,
    ) : State {
        override val stateRef = StateSaved
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeContextProfileFamily> {
        val controlRef: String
    }

    data class Use(
        val profileRef: String,
    ) : Control {
        override val controlRef = ControlUse
    }

    data class Save(
        val profileRef: String?,
        val definition: VexLifeContextProfile.Definition,
    ) : Control {
        override val controlRef = ControlSave
    }

    data class SaveAs(
        val definition: VexLifeContextProfile.Definition,
    ) : Control {
        override val controlRef = ControlSaveAs
    }

    data object RestoreProductDefault : Control {
        override val controlRef = ControlRestoreDefault
    }

    data class Import(
        val profile: VexLifeContextProfile.Portable,
    ) : Control {
        override val controlRef = ControlImport
    }


    sealed interface Effect : VexFamilyContract.Effect<VexLifeContextProfileFamily> {
        data class SelectSaved(
            val profileRef: String,
        ) : Effect

        data class Persist(
            val profileRef: String?,
            val definition: VexLifeContextProfile.Definition,
        ) : Effect

        data class PersistAsNew(
            val definition: VexLifeContextProfile.Definition,
        ) : Effect

        data object SelectProductDefault : Effect

        data class ImportAsNew(
            val profile: VexLifeContextProfile.Portable,
        ) : Effect

    }

    @Serializable
    data class DisplayState(
        /** Active-profile badge words; saved profile names remain literal arguments, never keys. */
        val label: VexLifeText,
        /** Active-profile summary beneath the Context Profile heading. */
        val detail: VexLifeText,
        val productDefault: Boolean,
        val controls: List<String>,
    ) : VexFamilyContract.DisplayState<VexLifeContextProfileFamily, State>

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeContextProfileFamily, State, Effect>? =
        when (control) {
            is Use ->
                if (
                    currentState is Saved &&
                        currentState.profile.profileRef == control.profileRef
                ) {
                    null
                } else {
                    stay(Effect.SelectSaved(control.profileRef))
                }

            is Save ->
                if (
                    currentState is Saved &&
                        (
                            control.profileRef ==
                                null ||
                                control.profileRef ==
                                currentState.profile.profileRef
                            )
                ) {
                    stay(
                        Effect.Persist(
                            profileRef = currentState.profile.profileRef,
                            definition = control.definition,
                        ),
                    )
                } else {
                    null
                }

            is SaveAs ->
                stay(
                    Effect.PersistAsNew(
                        definition = control.definition,
                    ),
                )

            RestoreProductDefault ->
                if (currentState is ProductDefault) null else stay(Effect.SelectProductDefault)

            is Import ->
                stay(
                    Effect.ImportAsNew(
                        profile = control.profile,
                    ),
                )

        }

    override fun displayStateFor(state: State): DisplayState =
        when (state) {
            is ProductDefault ->
                DisplayState(
                    label =
                        VexLifeText(
                            "workspace.label.context-profile-product-default",
                        ),
                    detail =
                        state.foundation.version
                            ?.let {
                                VexLifeText.text(
                                    "workspace.description.context-profile-product-foundation-version",
                                    "version" to it,
                                )
                            }
                            ?: VexLifeText(
                                "workspace.description.context-profile-product-foundation",
                            ),
                    productDefault = true,
                    controls =
                        listOf(
                            ControlUse,
                            ControlSaveAs,
                            ControlImport,
                        ),
                )

            is Saved ->
                DisplayState(
                    label =
                        VexLifeText.text(
                            "workspace.label.context-profile-saved",
                            "name" to state.profile.definition.name,
                        ),
                    detail =
                        VexLifeText.text(
                            if (
                                state.profile.productFoundationChangedSinceSave
                            ) {
                                "workspace.description.context-profile-saved-foundation-changed"
                            } else {
                                "workspace.description.context-profile-saved"
                            },
                            "revision" to state.profile.revision.toString(),
                        ),
                    productDefault = false,
                    controls =
                        listOf(
                            ControlUse,
                            ControlSave,
                            ControlSaveAs,
                            ControlRestoreDefault,
                            ControlImport,
                        ),
                )
        }

    fun stateFor(
        effective: VexLifeContextProfile.Effective,
    ): State =
        if (effective.productDefault) {
            ProductDefault(effective.basedOnProductFoundation)
        } else {
            Saved(effective)
        }

    const val StateProductDefault = "PRODUCT_DEFAULT"
    const val StateSaved = "SAVED"

    const val ControlUse = "USE"
    const val ControlSave = "SAVE"
    const val ControlSaveAs = "SAVE_AS"
    const val ControlRestoreDefault = "RESTORE_PRODUCT_DEFAULT"
    const val ControlImport = "IMPORT"

    val StateRefs: Set<String> =
        setOf(
            StateProductDefault,
            StateSaved,
        )

    val ControlRefs: Set<String> =
        setOf(
            ControlUse,
            ControlSave,
            ControlSaveAs,
            ControlRestoreDefault,
            ControlImport,
        )
}

// [VXG RealForever]
