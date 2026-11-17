package vexlife.model.heretic

import kotlinx.serialization.Serializable

/**
 * Read-only architecture projection of the runtime-realization configuration topology.
 *
 * This is attached to [HereticRuntimeAvailabilityArchitecture.FamilyRef]: the realization map
 * declares static dependency relationships; the Family owns dynamic State / Control / Effect
 * meaning against whichever realization was resolved.
 *
 * Readers must not infer platform requirements from Kotlin class names or source proximity.
 */
object HereticRuntimeRealizationArchitecture {
    const val Schema =
        "vexlife-prod.heretic-runtime-realization-architecture/v1"

    const val ProjectionRef =
        "architecture.vexlife-prod.heretic-runtime-realization"

    const val FamilyRef =
        HereticRuntimeAvailabilityArchitecture.FamilyRef

    const val RealizationSource =
        "src/commonMain/kotlin/vexlife/model/heretic/HereticRuntimeRealization.kt"

    const val FamilySource =
        "src/commonMain/kotlin/vexlife/model/heretic/HereticRuntimeAvailability.kt"

    @Serializable
    enum class Role {
        FAMILY,
        MODEL,
        TARGET,
        REALIZATION,
        ARTIFACT,
        ENVIRONMENT,
        RUNTIME_BINARY,
        RUNTIME,
        PROCESS,
        QUALIFICATION,
        RECEIPT,
        NOT_REQUIRED,
    }

    @Serializable
    enum class RelationshipClass {
        FAMILY_CONSUMES_REALIZATION,
        MODEL_REALIZED_BY,
        REALIZATION_TARGETS,
        REQUIRES,
        EXPLICITLY_NOT_REQUIRED,
        RECEIPT_COORDINATE,
    }

    @Serializable
    data class Node(
        val nodeRef: String,
        val role: Role,
        val label: String,
        val sourceRefs: List<String>,
    )

    @Serializable
    data class Relationship(
        val relationshipRef: String,
        val relationshipClass: RelationshipClass,
        val fromRef: String,
        val toRef: String,
        val label: String,
    )

    @Serializable
    data class Snapshot(
        val schemaVersion: String,
        val projectionRef: String,
        val familyRef: String,
        val nodes: List<Node>,
        val relationships: List<Relationship>,
        val externalEffectsPerformed: Boolean,
    ) {
        init {
            require(schemaVersion == Schema)
            require(projectionRef == ProjectionRef)
            require(familyRef == FamilyRef)
            require(!externalEffectsPerformed)
            require(nodes.map(Node::nodeRef).distinct().size == nodes.size)
            require(relationships.map(Relationship::relationshipRef).distinct().size == relationships.size)
            val nodeRefs = nodes.map(Node::nodeRef).toSet()
            require(
                relationships.all {
                    it.fromRef in nodeRefs && it.toRef in nodeRefs
                },
            ) {
                "runtime realization architecture contains a relationship to an absent node"
            }
        }
    }

    fun current(): Snapshot {
        val nodes = linkedMapOf<String, Node>()
        val relationships = mutableListOf<Relationship>()

        fun node(
            nodeRef: String,
            role: Role,
            label: String,
        ): Node =
            nodes.getOrPut(nodeRef) {
                Node(
                    nodeRef = nodeRef,
                    role = role,
                    label = label,
                    sourceRefs = listOf(RealizationSource),
                )
            }

        fun relationship(
            realization: HereticRuntimeRealization.Value,
            suffix: String,
            relationshipClass: RelationshipClass,
            fromRef: String,
            toRef: String,
            label: String,
        ) {
            relationships +=
                Relationship(
                    relationshipRef =
                        "relationship.vexlife-prod.runtime-realization." +
                            realization.realizationRef.substringAfter("realization.vexlife-prod.") +
                            "." +
                            suffix,
                    relationshipClass = relationshipClass,
                    fromRef = fromRef,
                    toRef = toRef,
                    label = label,
                )
        }

        val family =
            nodes.getOrPut(FamilyRef) {
                Node(
                    nodeRef = FamilyRef,
                    role = Role.FAMILY,
                    label = "Heretic runtime availability",
                    sourceRefs = listOf(FamilySource),
                )
            }

        HereticRuntimeRealization.Entries.forEach { realization ->
            val model =
                node(
                    nodeRef = realization.modelRef,
                    role = Role.MODEL,
                    label = realization.modelRef,
                )
            val target =
                node(
                    nodeRef = realization.target.targetRef,
                    role = Role.TARGET,
                    label = realization.target.targetRef,
                )
            val realizationNode =
                node(
                    nodeRef = realization.realizationRef,
                    role = Role.REALIZATION,
                    label = realization.realizationRef,
                )

            relationship(
                realization,
                "family-consumes",
                RelationshipClass.FAMILY_CONSUMES_REALIZATION,
                family.nodeRef,
                realizationNode.nodeRef,
                "Runtime Availability Family consumes this declared realization",
            )
            relationship(
                realization,
                "model-realized-by",
                RelationshipClass.MODEL_REALIZED_BY,
                model.nodeRef,
                realizationNode.nodeRef,
                "Product model is realized by this exact platform relationship",
            )
            relationship(
                realization,
                "targets",
                RelationshipClass.REALIZATION_TARGETS,
                realizationNode.nodeRef,
                target.nodeRef,
                "Realization targets one exact platform/architecture coordinate",
            )

            fun dependency(
                suffix: String,
                role: Role,
                requirement: HereticRuntimeRealization.Requirement<*>,
                requiredRef: (Any) -> String,
                requiredLabel: (Any) -> String,
            ) {
                when (requirement) {
                    is HereticRuntimeRealization.Requirement.Required<*> -> {
                        val value = requireNotNull(requirement.value)
                        val dep =
                            node(
                                nodeRef = requiredRef(value),
                                role = role,
                                label = requiredLabel(value),
                            )
                        relationship(
                            realization,
                            suffix,
                            RelationshipClass.REQUIRES,
                            realizationNode.nodeRef,
                            dep.nodeRef,
                            "Realization requires this dependency",
                        )
                    }

                    is HereticRuntimeRealization.Requirement.NotRequired -> {
                        val absent =
                            node(
                                nodeRef = requirement.reasonRef,
                                role = Role.NOT_REQUIRED,
                                label = requirement.reasonRef,
                            )
                        relationship(
                            realization,
                            suffix + "-not-required",
                            RelationshipClass.EXPLICITLY_NOT_REQUIRED,
                            realizationNode.nodeRef,
                            absent.nodeRef,
                            "This dependency class is explicitly not required",
                        )
                    }
                }
            }

            dependency(
                "artifact",
                Role.ARTIFACT,
                realization.dependencies.artifact,
                { (it as HereticRuntimeRealization.ArtifactProfile).artifactRef },
                { (it as HereticRuntimeRealization.ArtifactProfile).providerRepository },
            )
            dependency(
                "environment",
                Role.ENVIRONMENT,
                realization.dependencies.environment,
                { (it as HereticRuntimeRealization.EnvironmentProfile).environmentRef },
                { (it as HereticRuntimeRealization.EnvironmentProfile).environmentRef },
            )
            dependency(
                "runtime-binary",
                Role.RUNTIME_BINARY,
                realization.dependencies.runtimeBinary,
                { (it as HereticRuntimeRealization.RuntimeBinaryProfile).binaryRef },
                { (it as HereticRuntimeRealization.RuntimeBinaryProfile).filename },
            )
            dependency(
                "runtime",
                Role.RUNTIME,
                realization.dependencies.runtime,
                { (it as HereticRuntimeRealization.RuntimeProfile).runtimeAdapterRef },
                { (it as HereticRuntimeRealization.RuntimeProfile).runtimeClass },
            )
            dependency(
                "process",
                Role.PROCESS,
                realization.dependencies.process,
                { (it as HereticRuntimeRealization.ProcessProfile).processSubstrateRef },
                { (it as HereticRuntimeRealization.ProcessProfile).lifetimeOwnerRef },
            )
            dependency(
                "qualification",
                Role.QUALIFICATION,
                realization.dependencies.qualification,
                { (it as HereticRuntimeRealization.QualificationProfile).qualificationRef },
                { (it as HereticRuntimeRealization.QualificationProfile).qualificationRef },
            )

            val receipt =
                node(
                    nodeRef =
                        "receipt-coordinate." +
                            realization.realizationRef.substringAfter("realization."),
                    role = Role.RECEIPT,
                    label = realization.runtimeReceiptRelativePath,
                )
            relationship(
                realization,
                "receipt",
                RelationshipClass.RECEIPT_COORDINATE,
                realizationNode.nodeRef,
                receipt.nodeRef,
                "Qualified runtime truth persists at this realization-owned coordinate",
            )
        }

        return Snapshot(
            schemaVersion = Schema,
            projectionRef = ProjectionRef,
            familyRef = FamilyRef,
            nodes = nodes.values.sortedBy(Node::nodeRef),
            relationships = relationships.sortedBy(Relationship::relationshipRef),
            externalEffectsPerformed = false,
        )
    }
}

// [VXG RealForever]
