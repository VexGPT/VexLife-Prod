package vexlife.model.heretic

import kotlinx.serialization.Serializable

private val ArchitectureRefPattern =
    Regex(
        "[A-Za-z0-9][A-Za-z0-9._:/-]*",
    )

private fun requireArchitectureRef(
    value: String,
    label: String,
) {
    require(
        ArchitectureRefPattern.matches(
            value,
        ),
    ) {
        "$label must be one stable architecture ref"
    }
}

private fun requireArchitectureRefs(
    values: List<String>,
    label: String,
) {
    require(
        values.isNotEmpty() &&
            values.distinct().size ==
            values.size,
    ) {
        "$label must contain unique source refs"
    }
    values.forEach {
        requireArchitectureRef(
            it,
            label,
        )
    }
}

/**
 * Read-only, source-owned architecture projection for the first New Beginnings VexArchitecture
 * patient zero.
 *
 * This projection is deliberately Family-specific. It makes the already-accepted
 * HereticRuntimeAvailability relationship consumable without requiring VexBoard (or another reader)
 * to infer semantics from Kotlin proximity. It does not own runtime state, execute Effects, or define
 * a general architecture-export framework.
 */
object HereticRuntimeAvailabilityArchitecture {
    const val Schema =
        "vexlife-prod.heretic-runtime-availability-architecture/v1"

    const val ProjectionRef =
        "architecture.vexlife-prod.heretic-runtime-availability"

    const val FamilyRef =
        "family.vexlife-prod.heretic-runtime-availability"

    const val SourceOwnerRepository =
        "VexGPT/VexLife-Prod"

    const val FamilySource =
        "src/commonMain/kotlin/vexlife/model/heretic/HereticRuntimeAvailability.kt"

    const val StableCompatibilitySource =
        "src/jvmMain/kotlin/vexlife/model/heretic/runtime/StableVexLifeHereticRuntimeCompatibility.kt"

    const val ProductRuntimeSource =
        "src/jvmMain/kotlin/vexlife/model/heretic/runtime/HereticPersistentRuntime.kt"

    const val LocalSetupSource =
        "src/jvmMain/kotlin/vexlife/local/VexLifeProdLocalSetup.kt"

    @Serializable
    enum class Role {
        FAMILY,
        STATE,
        CONTROL,
        EFFECT,
        OWNER,
        OBSERVATION,
    }

    @Serializable
    enum class RelationshipClass {
        RESOLVED_BY,
        CURRENT_TRUTH_FOR,
        FORMS_OBSERVATION,
        ESTABLISHES_STATE,
        ADMITS_WITHOUT_PRODUCT_EFFECT,
        REQUESTS_IF_ACQUISITION_ALLOWED,
        REQUESTS_IF_OBSERVATION_ONLY,
        REALIZED_BY,
        DELEGATES_TO_OWNER,
        INTERPRETED_BY,
        ADMITS_PRODUCT_QUALIFIED,
        CONSUMES,
    }

    @Serializable
    data class Node(
        val nodeRef: String,
        val role: Role,
        val label: String,
        val sourceRefs: List<String>,
    ) {
        init {
            requireArchitectureRef(
                nodeRef,
                "nodeRef",
            )
            require(
                label.isNotBlank(),
            ) {
                "architecture node label must not be blank"
            }
            requireArchitectureRefs(
                sourceRefs,
                "node sourceRefs",
            )
        }
    }

    @Serializable
    data class Relationship(
        val relationshipRef: String,
        val relationshipClass: RelationshipClass,
        val fromRef: String,
        val toRef: String,
        val label: String,
        val sourceRefs: List<String>,
    ) {
        init {
            requireArchitectureRef(
                relationshipRef,
                "relationshipRef",
            )
            requireArchitectureRef(
                fromRef,
                "relationship fromRef",
            )
            requireArchitectureRef(
                toRef,
                "relationship toRef",
            )
            require(
                label.isNotBlank(),
            ) {
                "architecture relationship label must not be blank"
            }
            requireArchitectureRefs(
                sourceRefs,
                "relationship sourceRefs",
            )
        }
    }

    @Serializable
    data class Snapshot(
        val schemaVersion: String,
        val projectionRef: String,
        val familyRef: String,
        val sourceOwnerRepository: String,
        val nodes: List<Node>,
        val relationships: List<Relationship>,
        val sourceRefs: List<String>,
        val externalEffectsPerformed: Boolean,
    ) {
        init {
            require(
                schemaVersion ==
                    Schema,
            ) {
                "runtime availability architecture schema drift"
            }
            require(
                projectionRef ==
                    ProjectionRef &&
                    familyRef ==
                    FamilyRef &&
                    sourceOwnerRepository ==
                    SourceOwnerRepository,
            ) {
                "runtime availability architecture identity drift"
            }
            require(
                !externalEffectsPerformed,
            ) {
                "architecture projection must remain read-only"
            }
            requireArchitectureRefs(
                sourceRefs,
                "snapshot sourceRefs",
            )
            require(
                nodes.isNotEmpty() &&
                    nodes.map(
                        Node::nodeRef,
                    )
                        .distinct()
                        .size ==
                    nodes.size,
            ) {
                "architecture projection node refs must be unique"
            }
            require(
                relationships.map(
                    Relationship::relationshipRef,
                )
                    .distinct()
                    .size ==
                relationships.size,
            ) {
                "architecture projection relationship refs must be unique"
            }
            val nodeRefs =
                nodes
                    .map(
                        Node::nodeRef,
                    )
                    .toSet()
            relationships.forEach {
                relationship ->
                require(
                    relationship.fromRef in
                        nodeRefs &&
                        relationship.toRef in
                        nodeRefs,
                ) {
                    "architecture relationship endpoints must exist in the projection"
                }
            }
        }
    }

    fun current(): Snapshot {
        val family =
            Node(
                nodeRef = FamilyRef,
                role = Role.FAMILY,
                label = "Heretic runtime availability",
                sourceRefs =
                    listOf(
                        FamilySource,
                    ),
            )
        val control =
            Node(
                nodeRef =
                    "control.vexlife-prod.heretic-runtime-availability.ensure-available",
                role = Role.CONTROL,
                label = "Ensure available",
                sourceRefs =
                    listOf(
                        FamilySource,
                    ),
            )
        val stableBorrowable =
            Node(
                nodeRef =
                    "state.vexlife-prod.heretic-runtime-availability.stable-borrowable",
                role = Role.STATE,
                label = "Stable predecessor borrowable",
                sourceRefs =
                    listOf(
                        FamilySource,
                        StableCompatibilitySource,
                    ),
            )
        val stableUnavailable =
            Node(
                nodeRef =
                    "state.vexlife-prod.heretic-runtime-availability.stable-predecessor-unavailable",
                role = Role.STATE,
                label = "Stable predecessor unavailable",
                sourceRefs =
                    listOf(
                        FamilySource,
                        StableCompatibilitySource,
                    ),
            )
        val qualified =
            Node(
                nodeRef =
                    "state.vexlife-prod.heretic-runtime-availability.qualified",
                role = Role.STATE,
                label = "Qualified Heretic runtime available",
                sourceRefs =
                    listOf(
                        FamilySource,
                    ),
            )
        val ensureProduct =
            Node(
                nodeRef =
                    "effect.vexlife-prod.heretic-runtime-availability.ensure-product-owned-runtime",
                role = Role.EFFECT,
                label = "Ensure product-owned runtime",
                sourceRefs =
                    listOf(
                        FamilySource,
                    ),
            )
        val observeProduct =
            Node(
                nodeRef =
                    "effect.vexlife-prod.heretic-runtime-availability.observe-current-product-owned-runtime",
                role = Role.EFFECT,
                label = "Observe current product-owned runtime",
                sourceRefs =
                    listOf(
                        FamilySource,
                    ),
            )
        val stableOwner =
            Node(
                nodeRef =
                    "owner.vexlife-prod.stable-vexlife-heretic-runtime-compatibility",
                role = Role.OWNER,
                label = "Stable VexLife runtime compatibility",
                sourceRefs =
                    listOf(
                        StableCompatibilitySource,
                    ),
            )
        val productOwner =
            Node(
                nodeRef =
                    "owner.vexlife-prod.heretic-persistent-runtime",
                role = Role.OWNER,
                label = "VexLife-Prod Heretic persistent runtime",
                sourceRefs =
                    listOf(
                        ProductRuntimeSource,
                    ),
            )
        val setupOwner =
            Node(
                nodeRef =
                    "owner.vexlife-prod.local-setup",
                role = Role.OWNER,
                label = "VexLife-Prod local setup composition",
                sourceRefs =
                    listOf(
                        LocalSetupSource,
                    ),
            )
        val stableObservation =
            Node(
                nodeRef =
                    "observation.vexlife-prod.heretic-runtime-availability.stable-borrowable",
                role = Role.OBSERVATION,
                label = "Exact stable predecessor observation",
                sourceRefs =
                    listOf(
                        StableCompatibilitySource,
                    ),
            )
        val productObservation =
            Node(
                nodeRef =
                    "observation.vexlife-prod.heretic-runtime-availability.product-qualified",
                role = Role.OBSERVATION,
                label = "Qualified product-owned runtime evidence",
                sourceRefs =
                    listOf(
                        ProductRuntimeSource,
                    ),
            )

        val nodes =
            listOf(
                family,
                control,
                stableBorrowable,
                stableUnavailable,
                qualified,
                ensureProduct,
                observeProduct,
                stableOwner,
                productOwner,
                setupOwner,
                stableObservation,
                productObservation,
            )
                .sortedBy(
                    Node::nodeRef,
                )

        fun relationship(
            suffix: String,
            relationshipClass: RelationshipClass,
            from: Node,
            to: Node,
            label: String,
            sourceRefs: List<String>,
        ): Relationship =
            Relationship(
                relationshipRef =
                    "relationship.vexlife-prod.heretic-runtime-availability.$suffix",
                relationshipClass = relationshipClass,
                fromRef = from.nodeRef,
                toRef = to.nodeRef,
                label = label,
                sourceRefs = sourceRefs,
            )

        val relationships =
            listOf(
                relationship(
                    suffix = "ensure-available.resolved-by.family",
                    relationshipClass = RelationshipClass.RESOLVED_BY,
                    from = control,
                    to = family,
                    label = "EnsureAvailable is interpreted by the Family",
                    sourceRefs =
                        listOf(
                            FamilySource,
                        ),
                ),
                relationship(
                    suffix = "stable-borrowable.current-truth-for.family",
                    relationshipClass = RelationshipClass.CURRENT_TRUTH_FOR,
                    from = stableBorrowable,
                    to = family,
                    label = "Borrowable predecessor truth changes what EnsureAvailable means",
                    sourceRefs =
                        listOf(
                            FamilySource,
                            StableCompatibilitySource,
                        ),
                ),
                relationship(
                    suffix = "stable-unavailable.current-truth-for.family",
                    relationshipClass = RelationshipClass.CURRENT_TRUTH_FOR,
                    from = stableUnavailable,
                    to = family,
                    label = "Unavailable predecessor truth releases the product-owned path",
                    sourceRefs =
                        listOf(
                            FamilySource,
                            StableCompatibilitySource,
                        ),
                ),
                relationship(
                    suffix = "stable-owner.forms.stable-observation",
                    relationshipClass = RelationshipClass.FORMS_OBSERVATION,
                    from = stableOwner,
                    to = stableObservation,
                    label = "Observes exact predecessor receipt and process without ownership transfer",
                    sourceRefs =
                        listOf(
                            StableCompatibilitySource,
                        ),
                ),
                relationship(
                    suffix = "stable-observation.establishes.stable-borrowable",
                    relationshipClass = RelationshipClass.ESTABLISHES_STATE,
                    from = stableObservation,
                    to = stableBorrowable,
                    label = "Exact live predecessor evidence establishes borrowable truth",
                    sourceRefs =
                        listOf(
                            StableCompatibilitySource,
                            FamilySource,
                        ),
                ),
                relationship(
                    suffix = "family.admits.borrowed-qualified",
                    relationshipClass = RelationshipClass.ADMITS_WITHOUT_PRODUCT_EFFECT,
                    from = family,
                    to = qualified,
                    label = "Borrowable predecessor satisfies availability without a product-owned Effect",
                    sourceRefs =
                        listOf(
                            FamilySource,
                        ),
                ),
                relationship(
                    suffix = "family.requests.ensure-product-owned",
                    relationshipClass = RelationshipClass.REQUESTS_IF_ACQUISITION_ALLOWED,
                    from = family,
                    to = ensureProduct,
                    label = "Unavailable predecessor plus acquisition authority requests product-owned establishment",
                    sourceRefs =
                        listOf(
                            FamilySource,
                        ),
                ),
                relationship(
                    suffix = "family.requests.observe-product-owned",
                    relationshipClass = RelationshipClass.REQUESTS_IF_OBSERVATION_ONLY,
                    from = family,
                    to = observeProduct,
                    label = "Unavailable predecessor without acquisition authority requests observation only",
                    sourceRefs =
                        listOf(
                            FamilySource,
                        ),
                ),
                relationship(
                    suffix = "ensure-product-owned.realized-by.setup",
                    relationshipClass = RelationshipClass.REALIZED_BY,
                    from = ensureProduct,
                    to = setupOwner,
                    label = "Local setup realizes the Family-authored establishment request",
                    sourceRefs =
                        listOf(
                            FamilySource,
                            LocalSetupSource,
                        ),
                ),
                relationship(
                    suffix = "observe-product-owned.realized-by.setup",
                    relationshipClass = RelationshipClass.REALIZED_BY,
                    from = observeProduct,
                    to = setupOwner,
                    label = "Local setup realizes the Family-authored observation request",
                    sourceRefs =
                        listOf(
                            FamilySource,
                            LocalSetupSource,
                        ),
                ),
                relationship(
                    suffix = "setup.delegates-to.product-runtime-owner",
                    relationshipClass = RelationshipClass.DELEGATES_TO_OWNER,
                    from = setupOwner,
                    to = productOwner,
                    label = "Process lifecycle and qualification remain with HereticPersistentRuntime",
                    sourceRefs =
                        listOf(
                            LocalSetupSource,
                            ProductRuntimeSource,
                        ),
                ),
                relationship(
                    suffix = "product-runtime.forms.product-qualified-observation",
                    relationshipClass = RelationshipClass.FORMS_OBSERVATION,
                    from = productOwner,
                    to = productObservation,
                    label = "Product runtime owner returns exact qualified evidence",
                    sourceRefs =
                        listOf(
                            ProductRuntimeSource,
                        ),
                ),
                relationship(
                    suffix = "product-qualified-observation.interpreted-by.family",
                    relationshipClass = RelationshipClass.INTERPRETED_BY,
                    from = productObservation,
                    to = family,
                    label = "The Family interprets product-owned qualified evidence",
                    sourceRefs =
                        listOf(
                            FamilySource,
                            ProductRuntimeSource,
                        ),
                ),
                relationship(
                    suffix = "family.admits.product-qualified",
                    relationshipClass = RelationshipClass.ADMITS_PRODUCT_QUALIFIED,
                    from = family,
                    to = qualified,
                    label = "Product-owned qualified evidence establishes the same availability meaning",
                    sourceRefs =
                        listOf(
                            FamilySource,
                        ),
                ),
                relationship(
                    suffix = "setup.consumes.family",
                    relationshipClass = RelationshipClass.CONSUMES,
                    from = setupOwner,
                    to = family,
                    label = "Local setup composes the semantic decision without owning it",
                    sourceRefs =
                        listOf(
                            LocalSetupSource,
                            FamilySource,
                        ),
                ),
            )
                .sortedBy(
                    Relationship::relationshipRef,
                )

        return Snapshot(
            schemaVersion = Schema,
            projectionRef = ProjectionRef,
            familyRef = FamilyRef,
            sourceOwnerRepository = SourceOwnerRepository,
            nodes = nodes,
            relationships = relationships,
            sourceRefs =
                listOf(
                    FamilySource,
                    StableCompatibilitySource,
                    ProductRuntimeSource,
                    LocalSetupSource,
                )
                    .sorted(),
            externalEffectsPerformed = false,
        )
    }
}

// [VXG RealForever]
