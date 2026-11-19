package vexlife.release

import kotlinx.serialization.Serializable
import vexlife.semantic.VexFamilyContract
import vexlife.text.VexLifeText

/**
 * Product semantic owner for intentionally published VexLife release communication.
 *
 * Upstream release availability decides whether a release applies to this installation. This Family
 * does not fetch GitHub, compare versions, install software, or own Spiral geometry. It owns only
 * the person's acknowledgement relationship to already-applicable release communication.
 *
 * Dismissed communication is omitted from State after acknowledgement truth is re-observed. A View
 * or Dismiss Control emits an Effect and does not predict that acknowledgement was persisted.
 */
object VexLifeReleaseInboxFamily :
    VexFamilyContract<
        VexLifeReleaseInboxFamily,
        VexLifeReleaseInboxFamily.State,
        VexLifeReleaseInboxFamily.Control,
        VexLifeReleaseInboxFamily.Effect,
    >() {
    enum class Standing {
        Unread,
        Viewed,
    }

    @Serializable
    enum class AcknowledgementStanding {
        Viewed,
        Dismissed,
    }

    enum class SourceCurrentness {
        Current,
        Unavailable,
    }

    data class Notice(
        val release: VexLifeReleaseIdentity.Published,
    ) {
        val identity: VexLifeReleaseIdentity.Value =
            release.identity
    }

    data class Acknowledgement(
        val identity: VexLifeReleaseIdentity.Value,
        val standing: AcknowledgementStanding,
    )

    data class Communication(
        val notice: Notice,
        val standing: Standing,
    )

    data class State(
        val sourceCurrentness: SourceCurrentness,
        val communications: List<Communication>,
    ) : VexFamilyContract.State<VexLifeReleaseInboxFamily> {
        init {
            require(
                communications
                    .map { it.notice.identity }
                    .distinct()
                    .size ==
                    communications.size,
            ) {
                "release inbox State cannot contain duplicate release identities"
            }
        }
    }

    sealed interface Control : VexFamilyContract.Control<VexLifeReleaseInboxFamily> {
        val identity: VexLifeReleaseIdentity.Value
        val controlRef: String

        data class View(
            override val identity: VexLifeReleaseIdentity.Value,
        ) : Control {
            override val controlRef: String = ControlView
        }

        data class Dismiss(
            override val identity: VexLifeReleaseIdentity.Value,
        ) : Control {
            override val controlRef: String = ControlDismiss
        }
    }

    sealed interface Effect : VexFamilyContract.Effect<VexLifeReleaseInboxFamily> {
        data class ViewRelease(
            val notice: Notice,
        ) : Effect

        data class DismissRelease(
            val identity: VexLifeReleaseIdentity.Value,
        ) : Effect
    }

    @Serializable
    data class ItemDisplay(
        val release: VexLifeReleaseIdentity.Coordinate,
        val releaseIdentifier: String,
        val publishedAtEpochMillis: Long,
        val page: String,
        val unread: Boolean,
        val status: VexLifeText,
        val viewControlRef: String,
        val viewLabel: VexLifeText,
        val dismissControlRef: String,
        val dismissLabel: VexLifeText,
    )

    @Serializable
    data class DisplayState(
        val quickActionLabel: VexLifeText,
        val heading: VexLifeText,
        val empty: VexLifeText,
        val unreadCount: Int,
        val items: List<ItemDisplay>,
    ) : VexFamilyContract.DisplayState<VexLifeReleaseInboxFamily, State>

    fun observe(
        notices: List<Notice>,
        acknowledgements: List<Acknowledgement>,
        sourceCurrentness: SourceCurrentness =
            SourceCurrentness.Current,
    ): State {
        require(
            notices.map(Notice::identity).distinct().size == notices.size,
        ) {
            "release inbox observation cannot contain duplicate release notices"
        }
        require(
            acknowledgements.map(Acknowledgement::identity).distinct().size ==
                acknowledgements.size,
        ) {
            "release inbox observation cannot contain duplicate acknowledgements"
        }

        val acknowledgementByIdentity =
            acknowledgements.associateBy(Acknowledgement::identity)

        return State(
            sourceCurrentness = sourceCurrentness,
            communications =
                notices
                    .sortedByDescending {
                        it.release.publishedAtEpochMillis
                    }
                    .mapNotNull { notice ->
                        when (acknowledgementByIdentity[notice.identity]?.standing) {
                            AcknowledgementStanding.Dismissed -> null
                            AcknowledgementStanding.Viewed ->
                                Communication(
                                    notice = notice,
                                    standing = Standing.Viewed,
                                )

                            null ->
                                Communication(
                                    notice = notice,
                                    standing = Standing.Unread,
                                )
                        }
                    },
        )
    }

    override fun act(
        currentState: State,
        control: Control,
    ): Act<VexLifeReleaseInboxFamily, State, Effect>? {
        val communication =
            currentState.communications.singleOrNull {
                it.notice.identity ==
                    control.identity
            } ?: return null

        return when (control) {
            is Control.View ->
                stay(
                    Effect.ViewRelease(
                        communication.notice,
                    ),
                )

            is Control.Dismiss ->
                stay(
                    Effect.DismissRelease(
                        communication.notice.identity,
                    ),
                )
        }
    }

    override fun displayStateFor(state: State): DisplayState =
        DisplayState(
            quickActionLabel =
                VexLifeText(
                    KeyQuickAction,
                ),
            heading =
                VexLifeText(
                    KeyHeading,
                ),
            empty =
                VexLifeText(
                    when (state.sourceCurrentness) {
                        SourceCurrentness.Current -> KeyEmpty
                        SourceCurrentness.Unavailable -> KeyUnavailable
                    },
                ),
            unreadCount =
                state.communications.count {
                    it.standing ==
                        Standing.Unread
                },
            items =
                state.communications.map { communication ->
                    val identity =
                        communication.notice.identity
                    ItemDisplay(
                        release =
                            VexLifeReleaseIdentity.coordinate(
                                identity,
                            ),
                        releaseIdentifier =
                            VexLifeReleaseIdentity.identifier(
                                identity,
                            ),
                        publishedAtEpochMillis =
                            communication.notice.release.publishedAtEpochMillis,
                        page =
                            communication.notice.release.page.value,
                        unread =
                            communication.standing ==
                                Standing.Unread,
                        status =
                            VexLifeText(
                                when (communication.standing) {
                                    Standing.Viewed -> KeyViewed
                                    Standing.Unread ->
                                        when (identity) {
                                            is VexLifeReleaseIdentity.Value.Stable ->
                                                KeyUpdateAvailable

                                            is VexLifeReleaseIdentity.Value.Preview ->
                                                KeyPreviewUpdateAvailable
                                        }
                                },
                            ),
                        viewControlRef = ControlView,
                        viewLabel =
                            VexLifeText(
                                KeyView,
                            ),
                        dismissControlRef = ControlDismiss,
                        dismissLabel =
                            VexLifeText(
                                KeyDismiss,
                            ),
                    )
                },
        )

    fun controlForRef(
        controlRef: String,
        identity: VexLifeReleaseIdentity.Value,
    ): Control? =
        when (controlRef) {
            ControlView ->
                Control.View(
                    identity,
                )

            ControlDismiss ->
                Control.Dismiss(
                    identity,
                )

            else -> null
        }

    /**
     * The acknowledgement status one release item shows, as its `release-alerts:state:` dataKey
     * names it: unread (stable or preview) or viewed. Display refs, not separate Family States.
     */
    const val StatusUnread = "UNREAD"
    const val StatusUnreadPreview = "UNREAD_PREVIEW"
    const val StatusViewed = "VIEWED"
    val StatusRefs: Set<String> = setOf(StatusUnread, StatusUnreadPreview, StatusViewed)

    const val ControlView = "VIEW_RELEASE"
    const val ControlDismiss = "DISMISS_RELEASE"

    const val KeyQuickAction = "release-alerts.label.quick-action"
    const val KeyHeading = "release-alerts.heading.panel"
    const val KeyEmpty = "release-alerts.description.empty"
    const val KeyUnavailable = "release-alerts.description.unavailable"
    const val KeyUpdateAvailable = "release-alerts.status.update-available"
    const val KeyPreviewUpdateAvailable = "release-alerts.status.preview-update-available"
    const val KeyViewed = "release-alerts.status.viewed"
    const val KeyView = "release-alerts.button.view"
    const val KeyDismiss = "release-alerts.button.dismiss"
}

// [VXG RealForever]
