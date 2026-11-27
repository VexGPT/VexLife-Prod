package vexlife.continuity

import java.nio.file.Path
import vexlife.model.heretic.HereticRuntimeRealization
import vexlife.platform.VexLifeJvmPlatformTarget

/**
 * Service-lifetime construction for Continuity semantic acceleration.
 *
 * Product semantics remain owned by [VexLifeContinuitySourceLocator] and ContextRevisit. This
 * owner decides only whether the current service process can safely realize the already-selected
 * BGE-M3 dense Encoder or must stay on the existing lexical/ref/typed-edge backend.
 *
 * Missing/held supporting prerequisites never mean historical source is absent. Windows remains
 * lexical until a separately-qualified BGE realization exists there.
 */
internal object VexLifeContinuitySemanticServiceSession {
    const val DenseCurrentState =
        "BGE_M3_DENSE_CURRENT"

    const val LexicalFallbackState =
        "LEXICAL_FALLBACK"

    data class Session internal constructor(
        val state: String,
        val backend: VexLifeContinuitySourceLocator.Backend,
        val representationBindingRef: String?,
        private val closeSemanticRuntime: (() -> Unit)?,
    ) :
        AutoCloseable {
        init {
            require(
                state in
                    setOf(
                        DenseCurrentState,
                        LexicalFallbackState,
                    )
            ) {
                "Continuity semantic service Session state is invalid"
            }
            require(
                (
                    state ==
                        DenseCurrentState
                ) ==
                    (
                        backend is
                            VexLifeContinuitySourceLocator.SemanticIndexedBackend
                    )
            ) {
                "Continuity semantic service Session state/backend relationship is invalid"
            }
            require(
                if (
                    state ==
                    DenseCurrentState
                ) {
                    representationBindingRef ==
                        (
                            backend as
                                VexLifeContinuitySourceLocator.SemanticIndexedBackend
                        )
                            .semanticRepresentationBindingRef &&
                        closeSemanticRuntime !=
                        null
                } else {
                    representationBindingRef ==
                        null &&
                        closeSemanticRuntime ==
                        null
                }
            ) {
                "Continuity semantic service Session realization binding is invalid"
            }
        }

        override fun close() {
            closeSemanticRuntime
                ?.invoke()
        }
    }

    internal data class Hooks(
        val currentTarget: () -> vexlife.platform.VexLifePlatformRealization.Target = {
            VexLifeJvmPlatformTarget.requireCurrent()
        },
        val observeArtifacts:
            (Path) -> VexLifeBgeM3ArtifactMaterialization.Observation = {
                productHome ->
                VexLifeBgeM3ArtifactMaterialization.observe(
                    productHome,
                )
            },
        val observeMacEnvironment:
            (Path) -> VexLifeBgeM3MacPythonEnvironment.Result? = {
                productHome ->
                VexLifeBgeM3MacPythonEnvironment.observeCurrent(
                    productHome,
                )
            },
        val startMacRuntime:
            (
                Path,
                VexLifeBgeM3ArtifactMaterialization.Observation,
                VexLifeBgeM3MacPythonEnvironment.Result,
            ) -> VexLifeBgeM3MacRuntime.Session = {
                productHome,
                artifacts,
                environment,
                ->
                VexLifeBgeM3MacRuntime.start(
                    VexLifeBgeM3MacRuntime.bind(
                        productHome =
                            productHome,
                        artifacts =
                            artifacts,
                        environment =
                            environment,
                    ),
                )
            },
        val lexicalBackend: () -> VexLifeContinuitySourceLocator.Backend = {
            VexLifeContinuitySourceLocator.InMemoryBackend()
        },
    )

    /**
     * Reuse-only service construction.
     *
     * No acquisition/environment creation is admitted here. If a prerequisite is absent, partial,
     * damaged, invalid, unsupported on this platform, or the supporting worker cannot start, the
     * exact semantic realization is held and Product continues with the existing lexical backend.
     */
    fun establish(
        productHome: Path,
        hooks: Hooks =
            Hooks(),
    ): Session {
        require(
            productHome.isAbsolute,
        ) {
            "Continuity semantic service Session requires one absolute Product Home"
        }

        if (
            hooks.currentTarget() !=
            HereticRuntimeRealization.MacArm64
        ) {
            return lexical(
                hooks,
            )
        }

        val artifacts =
            runCatching {
                hooks.observeArtifacts(
                    productHome,
                )
            }
                .getOrNull()
                ?: return lexical(
                    hooks,
                )

        if (
            artifacts.state !=
            VexLifeBgeM3ArtifactMaterialization.PresenceState.PRESENT
        ) {
            return lexical(
                hooks,
            )
        }

        val environment =
            runCatching {
                hooks.observeMacEnvironment(
                    productHome,
                )
            }
                .getOrNull()
                ?: return lexical(
                    hooks,
                )

        val runtime =
            runCatching {
                hooks.startMacRuntime(
                    productHome,
                    artifacts,
                    environment,
                )
            }
                .getOrNull()
                ?: return lexical(
                    hooks,
                )

        return try {
            val backend =
                VexLifeVolatileDenseSemanticBackend(
                    encoder =
                        runtime,
                )
            Session(
                state =
                    DenseCurrentState,
                backend =
                    backend,
                representationBindingRef =
                    backend.semanticRepresentationBindingRef,
                closeSemanticRuntime =
                    runtime::close,
            )
        } catch (
            error: Throwable,
        ) {
            runCatching {
                runtime.close()
            }
            throw error
        }
    }

    private fun lexical(
        hooks: Hooks,
    ): Session =
        Session(
            state =
                LexicalFallbackState,
            backend =
                hooks.lexicalBackend(),
            representationBindingRef =
                null,
            closeSemanticRuntime =
                null,
        )
}

// [VXG RealForever]
