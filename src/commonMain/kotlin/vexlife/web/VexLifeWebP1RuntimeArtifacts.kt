package vexlife.web

import vexlife.browser.VexLifeProdBrowserContract
import vexlife.context.VexLifeContextProfile
import vexlife.continuity.VexLifePrivateModeFamily
import vexlife.diagnostics.VexLifeDiagnosticsFamily
import vexlife.localasr.VexLifeLocalAsrModelsFamily
import vexlife.model.VexLifeModelsFamily
import vexlife.release.VexLifeReleaseInboxFamily
import vexlife.person.VexLifePersonSettings
import vexlife.conversation.VexLifeConversationTurnFamily
import vexlife.service.VexLifeProdServiceContract
import vexlife.voiceinput.VexLifeVoiceInputFamily
import vexlife.workspace.VexLifeSourceTemplate
import vexlife.workspace.VexLifeWorkspaceApp

/**
 * Current VexLife-Prod sequencing beneath the canonical A003 presentation contract.
 *
 * Presentation contract:
 * - ordinary Enter sends;
 * - Shift+Enter inserts a newline;
 * - the human turn appears immediately as in-flight presentation state;
 * - durable Conversation remains authoritative and replaces the in-flight projection on commit;
 * - browser Context inclusion is explicit;
 * - current Send idempotency / Memory / working-continuity / runtime owners remain untouched.
 */
object VexLifeWebP1RuntimeArtifacts {
    fun generate(
        presentationSourceSha: String? = null,
    ): VexWebArtifacts.Bundle {
        val semantic =
            VexWebArtifacts.generate(
                VexLifeWebP1.Surface,
            )

        return semantic.copy(
            html =
                VexLifeWebP1ExperienceMarkup.render(),
            css =
                VexLifeWebP1ExperienceStyle.render(),
            javascript =
                runtimeJavascript(
                    presentationSourceSha,
                ).trimIndent() +
                    "\n",
        )
    }

    private fun runtimeJavascript(
        presentationSourceSha: String?,
    ): String {
        val browserCompanionSeam =
            VexLifeWebP1.BodyFrame.seams
                .filterIsInstance<VexWeb.Seam.Vertical>()
                .single {
                    it.ref ==
                        VexLifeWebP1.Refs.BrowserCompanionSeam
                }
        val allocation =
            browserCompanionSeam.allocation as
                VexWeb.Parameter.Allocation.Ratio
        val authoredBrowserShare =
            allocation.beforeParts.toDouble() /
                allocation.totalParts.toDouble()
        fun jsList(
            values: List<String>,
        ): String =
            values.joinToString(
                separator = ",",
                prefix = "[",
                postfix = "]",
            ) {
                jsString(
                    it,
                )
            }

        val humanPageSchemes =
            jsList(
                VexLifeProdBrowserContract.HumanPageSchemes,
            )
        val rejectedHumanPageHosts =
            jsList(
                VexLifeProdBrowserContract.RejectedHumanPageHosts,
            )
        val admittedHosts =
            jsList(
                VexLifeProdBrowserContract.AdmittedHosts,
            )
        val workspaceApps = jsList(VexLifeWorkspaceApp.entries.map { it.key })
        val sourceTemplateScopes = jsList(VexLifeSourceTemplate.Members.map { it.scope })

        fun jsNullable(value: String?): String = value?.let(::jsString) ?: "null"

        fun voiceDisplay(state: VexLifeVoiceInputFamily.State): String {
            val display = VexLifeVoiceInputFamily.displayStateFor(state)
            return buildString {
                append("Object.freeze({statusKey:")
                append(jsString(display.status.key))
                append(",primaryControlRef:")
                append(jsNullable(display.primaryControlRef))
                append(",primaryLabelKey:")
                append(jsNullable(display.primaryLabel?.key))
                append(",active:")
                append(display.active)
                append(",attention:")
                append(jsString(display.attention.name))
                append("})")
            }
        }

        val voiceStateDisplays =
            listOf(
                VexLifeVoiceInputFamily.StateReady to
                    voiceDisplay(VexLifeVoiceInputFamily.Ready),
                VexLifeVoiceInputFamily.StateRequestingMicrophone to
                    voiceDisplay(VexLifeVoiceInputFamily.RequestingMicrophone),
                VexLifeVoiceInputFamily.StateListening to
                    voiceDisplay(VexLifeVoiceInputFamily.Listening),
                VexLifeVoiceInputFamily.StateStopping to
                    voiceDisplay(VexLifeVoiceInputFamily.Stopping),
            ).joinToString(separator = ",", prefix = "{", postfix = "}") { (stateRef, display) ->
                jsString(stateRef) + ":" + display
            }
        val voiceHoldDisplays =
            VexLifeVoiceInputFamily.HoldReason.entries.joinToString(
                separator = ",",
                prefix = "{",
                postfix = "}",
            ) {
                jsString(it.reasonRef) + ":" +
                    voiceDisplay(VexLifeVoiceInputFamily.Held(it))
            }
        val voiceFailureDisplays =
            VexLifeVoiceInputFamily.FailureReason.entries.joinToString(
                separator = ",",
                prefix = "{",
                postfix = "}",
            ) {
                jsString(it.reasonRef) + ":" +
                    voiceDisplay(VexLifeVoiceInputFamily.Failed(it))
            }

        fun voiceSemanticState(
            state: VexLifeVoiceInputFamily.State?,
        ): String {
            if (state == null) return "null"
            val holdReason =
                (state as? VexLifeVoiceInputFamily.Held)
                    ?.reason
                    ?.reasonRef
            val failureReason =
                (state as? VexLifeVoiceInputFamily.Failed)
                    ?.reason
                    ?.reasonRef
            return buildString {
                append("Object.freeze({stateRef:")
                append(jsString(state.stateRef))
                append(",holdReasonRef:")
                append(jsNullable(holdReason))
                append(",failureReasonRef:")
                append(jsNullable(failureReason))
                append("})")
            }
        }

        fun voiceAct(
            state: VexLifeVoiceInputFamily.State,
            control: VexLifeVoiceInputFamily.Control,
        ): String {
            val act =
                VexLifeVoiceInputFamily.resolve(
                    currentState = state,
                    control = control,
                )
                    ?: return "null"
            return buildString {
                append("Object.freeze({admittedState:")
                append(voiceSemanticState(act.admittedState))
                append(",effectRef:")
                append(jsNullable(act.effect?.effectRef))
                append("})")
            }
        }

        val voiceExampleStates =
            listOf(
                VexLifeVoiceInputFamily.Ready,
                VexLifeVoiceInputFamily.RequestingMicrophone,
                VexLifeVoiceInputFamily.Listening,
                VexLifeVoiceInputFamily.Stopping,
                VexLifeVoiceInputFamily.Held(
                    VexLifeVoiceInputFamily.HoldReason.LocalRecognitionUnavailable
                ),
                VexLifeVoiceInputFamily.Failed(
                    VexLifeVoiceInputFamily.FailureReason.Unknown,
                ),
            )
        val voiceActByState =
            voiceExampleStates.joinToString(
                separator = ",",
                prefix = "{",
                postfix = "}",
            ) { state ->
                jsString(state.stateRef) +
                    ":Object.freeze({" +
                    jsString(VexLifeVoiceInputFamily.ControlStart) +
                    ":" +
                    voiceAct(state, VexLifeVoiceInputFamily.Start) +
                    "," +
                    jsString(VexLifeVoiceInputFamily.ControlStop) +
                    ":" +
                    voiceAct(state, VexLifeVoiceInputFamily.Stop) +
                    "})"
            }

        val voiceCapabilityStateByFacts =
            buildList {
                for (localRecognition in listOf(false, true)) {
                    for (microphoneApi in listOf(false, true)) {
                        for (audioFeedback in listOf(false, true)) {
                            val key =
                                listOf(
                                    localRecognition,
                                    microphoneApi,
                                    audioFeedback,
                                )
                                    .joinToString(":") {
                                        if (it) "1" else "0"
                                    }
                            add(
                                jsString(key) +
                                    ":" +
                                    voiceSemanticState(
                                        VexLifeVoiceInputFamily.observeCapability(
                                            localRecognitionAvailable = localRecognition,
                                            microphoneApiAvailable = microphoneApi,
                                            audioFeedbackAvailable = audioFeedback,
                                        )
                                    )
                            )
                        }
                    }
                }
            }
                .joinToString(
                    separator = ",",
                    prefix = "{",
                    postfix = "}",
                )

        val voiceFailureStateByReason =
            VexLifeVoiceInputFamily.FailureReason.entries.joinToString(
                separator = ",",
                prefix = "{",
                postfix = "}",
            ) { reason ->
                jsString(reason.reasonRef) +
                    ":" +
                    voiceSemanticState(
                        VexLifeVoiceInputFamily.observeFailure(
                            reason,
                        )
                    )
            }

        val voiceRecognitionStartedState =
            voiceSemanticState(
                VexLifeVoiceInputFamily.observeRecognitionStarted()
            )

        return """
            // Generated VexLife-Prod A003-faithful presentation over current New-Beginnings owners.
            (() => {
              const root = document.getElementById('workspace');
              if (!root) return;

              const service = Object.freeze({
                presentationSourceSha: ${jsNullable(presentationSourceSha)},
                presentationSourceHeader: ${jsString(VexLifeProdServiceContract.PresentationSourceHeader)},
                stalePresentationCode: ${jsString(VexLifeProdServiceContract.StalePresentationCode)},
                sourceCurrentnessPath: ${jsString(VexLifeProdServiceContract.SourceCurrentnessPath)},
                currentStatePath: ${jsString(VexLifeProdServiceContract.CurrentStatePath)},
                conversationPath: ${jsString(VexLifeProdServiceContract.ConversationPath)},
                conversationTurnPath: ${jsString(VexLifeProdServiceContract.ConversationTurnPath)},
                contextPath: ${jsString(VexLifeProdServiceContract.ContextPath)},
                contextHeldUnsupportedState: ${jsString(VexLifeProdServiceContract.ContextStateHeldUnsupportedDurablePromptReceipt)},
                contextProfilesPath: ${jsString(VexLifeProdServiceContract.ContextProfilesPath)},
                contextProfilesControlPath: ${jsString(VexLifeProdServiceContract.ContextProfilesControlPath)},
                contextProfileHomeRevealPath: ${jsString(VexLifeProdServiceContract.ContextProfileHomeRevealPath)},
                contextProfileExportPath: ${jsString(VexLifeProdServiceContract.ContextProfileExportPath)},
                vexLabsPath: ${jsString(VexLifeProdServiceContract.VexLabsPath)},
                vexLabsControlPath: ${jsString(VexLifeProdServiceContract.VexLabsControlPath)},
                vexLabsSendPath: ${jsString(VexLifeProdServiceContract.VexLabsSendPath)},
                foundationalFieldPath: ${jsString(VexLifeProdServiceContract.FoundationalFieldPath)},
                memoryPath: ${jsString(VexLifeProdServiceContract.MemoryPath)},
                releaseAlertsPath: ${jsString(VexLifeProdServiceContract.ReleaseAlertsPath)},
                releaseAlertsControlPath: ${jsString(VexLifeProdServiceContract.ReleaseAlertsControlPath)},
                releaseAlertsStringsPath: ${jsString(VexLifeProdServiceContract.ReleaseAlertsStringsPath)},
                settingsPath: ${jsString(VexLifeProdServiceContract.SettingsPath)},
                localAsrPath: ${jsString(VexLifeProdServiceContract.LocalAsrPath)},
                localAsrSpokenLanguagePath: ${jsString(VexLifeProdServiceContract.LocalAsrSpokenLanguagePath)},
                localAsrComposerOutputPath: ${jsString(VexLifeProdServiceContract.LocalAsrComposerOutputPath)},
                localAsrModelControlPath: ${jsString(VexLifeProdServiceContract.LocalAsrModelControlPath)},
                localAsrPreparePath: ${jsString(VexLifeProdServiceContract.LocalAsrPreparePath)},
                localAsrTranscribePath: ${jsString(VexLifeProdServiceContract.LocalAsrTranscribePath)},
                privatePath: ${jsString(VexLifeProdServiceContract.PrivatePath)},
                privateControlPath: ${jsString(VexLifeProdServiceContract.PrivateControlPath)},
                privateSendPath: ${jsString(VexLifeProdServiceContract.PrivateSendPath)},
                privateNoteProposePath: ${jsString(VexLifeProdServiceContract.PrivateNoteProposePath)},
                privateNoteKeepPath: ${jsString(VexLifeProdServiceContract.PrivateNoteKeepPath)},
                privateNoteDeclinePath: ${jsString(VexLifeProdServiceContract.PrivateNoteDeclinePath)},
                visionPath: ${jsString(VexLifeProdServiceContract.VisionPath)},
                visionControlPath: ${jsString(VexLifeProdServiceContract.VisionControlPath)},
                diagnosticsPath: ${jsString(VexLifeProdServiceContract.DiagnosticsPath)},
                diagnosticsControlPath: ${jsString(VexLifeProdServiceContract.DiagnosticsControlPath)},
                modelsPath: ${jsString(VexLifeProdServiceContract.ModelsPath)},
                modelsControlPath: ${jsString(VexLifeProdServiceContract.ModelsControlPath)},
                modelsImportCheckPath: ${jsString(VexLifeProdServiceContract.ModelsImportCheckPath)},
                modelsImportAddPath: ${jsString(VexLifeProdServiceContract.ModelsImportAddPath)},
                chronologyPath: ${jsString(VexLifeProdServiceContract.ChronologyPath)},
                workspaceStringsPath: ${jsString(VexLifeProdServiceContract.WorkspaceStringsPath)},
                sendPath: ${jsString(VexLifeProdServiceContract.ConversationSendPath)},
                cancelPath: ${jsString(VexLifeProdServiceContract.ConversationCancelPath)}
              });
              const apps = Object.freeze({
                keys: $workspaceApps,
                sourceTemplate: ${jsString(VexLifeWorkspaceApp.SourceTemplate.key)},
                archives: ${jsString(VexLifeWorkspaceApp.Archives.key)},
                vexContext: ${jsString(VexLifeWorkspaceApp.VexContext.key)},
                browser: ${jsString(VexLifeWorkspaceApp.Browser.key)},
                sourceTemplateScopes: $sourceTemplateScopes,
                storageKey: 'vexlife-prod.presentation.app'
              });
              const browser = Object.freeze({
                initialPageUrl: ${jsString(VexLifeProdBrowserContract.InitialPageUrl)},
                humanPageSchemes: $humanPageSchemes,
                rejectedHumanPageHosts: $rejectedHumanPageHosts,
                rejectedHumanPageHostSuffix: ${jsString(VexLifeProdBrowserContract.RejectedHumanPageHostSuffix)},
                admittedHosts: $admittedHosts,
                frameHostSuffix: ${jsString(VexLifeProdBrowserContract.FrameHostSuffix)},
                frameHostPort: ${VexLifeProdBrowserContract.FrameHostPort},
                contextCharacterLimit: ${VexLifeProdBrowserContract.ContextCharacterLimit},
                maxTitleCharacters: ${VexLifeProdBrowserContract.MaxTitleCharacters},
                captureRendered: ${jsString(VexLifeProdBrowserContract.CaptureRendered)},
                messageType: 'vexlife.browser-context/v1',
                authoredBrowserShare: $authoredBrowserShare
              });
              // Kotlin ConversationTurn Family vocabulary crossing into presentation. The current
              // State/DisplayState itself is observed from the service, not invented in JavaScript.
              const conversationTurn = Object.freeze({
                control: Object.freeze({
                  send: ${jsString(VexLifeConversationTurnFamily.ControlSend)},
                  stop: ${jsString(VexLifeConversationTurnFamily.ControlStop)}
                }),
                outcome: Object.freeze({
                  realized: ${jsString(VexLifeProdServiceContract.ConversationControlOutcome.REALIZED.name)}
                })
              });
              // Serialization boundary for the Kotlin VoiceInput Family. Browser code realizes
              // Effects but does not invent Product State, Control, reason refs or display keys.
              const voiceInput = Object.freeze({
                state: Object.freeze({
                  ready: ${jsString(VexLifeVoiceInputFamily.StateReady)},
                  requestingMicrophone: ${jsString(VexLifeVoiceInputFamily.StateRequestingMicrophone)},
                  listening: ${jsString(VexLifeVoiceInputFamily.StateListening)},
                  stopping: ${jsString(VexLifeVoiceInputFamily.StateStopping)},
                  held: ${jsString(VexLifeVoiceInputFamily.StateHeld)},
                  failed: ${jsString(VexLifeVoiceInputFamily.StateFailed)}
                }),
                control: Object.freeze({
                  start: ${jsString(VexLifeVoiceInputFamily.ControlStart)},
                  stop: ${jsString(VexLifeVoiceInputFamily.ControlStop)}
                }),
                effect: Object.freeze({
                  beginLocalDictation: ${jsString(VexLifeVoiceInputFamily.EffectBeginLocalDictation)},
                  endLocalDictation: ${jsString(VexLifeVoiceInputFamily.EffectEndLocalDictation)}
                }),
                holdReason: Object.freeze({
                  localRecognitionUnavailable: ${jsString(VexLifeVoiceInputFamily.HoldReason.LocalRecognitionUnavailable.reasonRef)},
                  microphoneApiUnavailable: ${jsString(VexLifeVoiceInputFamily.HoldReason.MicrophoneApiUnavailable.reasonRef)},
                  audioFeedbackUnavailable: ${jsString(VexLifeVoiceInputFamily.HoldReason.AudioFeedbackUnavailable.reasonRef)}
                }),
                failureReason: Object.freeze({
                  microphonePermissionRequired: ${jsString(VexLifeVoiceInputFamily.FailureReason.MicrophonePermissionRequired.reasonRef)},
                  microphoneUnavailable: ${jsString(VexLifeVoiceInputFamily.FailureReason.MicrophoneUnavailable.reasonRef)},
                  recognitionStopped: ${jsString(VexLifeVoiceInputFamily.FailureReason.RecognitionStopped.reasonRef)},
                  unknown: ${jsString(VexLifeVoiceInputFamily.FailureReason.Unknown.reasonRef)}
                }),
                attentionOverlay: ${jsString(VexLifeVoiceInputFamily.Attention.Overlay.name)},
                displayByState: Object.freeze($voiceStateDisplays),
                holdDisplayByReason: Object.freeze($voiceHoldDisplays),
                failureDisplayByReason: Object.freeze($voiceFailureDisplays),
                actByState: Object.freeze($voiceActByState),
                capabilityStateByFacts: Object.freeze($voiceCapabilityStateByFacts),
                failureStateByReason: Object.freeze($voiceFailureStateByReason),
                recognitionStartedState: $voiceRecognitionStartedState
              });

              const byId = (id) => document.getElementById(id);
              // Elements that carry a typed identity are found by it, not by a DOM id string: the
              // Blueprint ref is the element at every layer (docs/CULTURE.md).
              const byRef = (ref) => document.querySelector('[data-vex-ref="' + ref + '"]');
              const ui = Object.freeze({
                frame: byId('frame'),
                seam: byId('seam'),
                collapseBefore: byId('collapse-before'),
                collapseAfter: byId('collapse-after'),
                query: byRef(${jsString(VexLifeWebP1.BrowserQuery.identity.ref)}),
                addressForm: byId('address-form'),
                addressError: byId('address-error'),
                reload: byId('reload'),
                appSelector: byRef(${jsString(VexLifeWebP1.AppSelector.identity.ref)}),
                addressForm: byId('address-form'),
                family: byRef(${jsString(VexLifeWebP1.SourceTemplateFamily.identity.ref)}),
                viewport: root.querySelector('.viewport'),
                archives: byRef(${jsString(VexLifeWebP1.Archives.identity.ref)}),
                archivesSummary: byId('archives-summary'),
                archivesTimeline: byRef(${jsString(VexLifeWebP1.ArchivesTimeline.identity.ref)}),
                vexContext: byRef(${jsString(VexLifeWebP1.VexContext.identity.ref)}),
                contextProfileBadge: byId('context-profile-badge'),
                contextProfileSummary: byId('context-profile-summary'),
                contextProfileChoice: byId('context-profile-choice'),
                contextProfileName: byId('context-profile-name'),
                contextProfileFoundationMode: byId('context-profile-foundation-mode'),
                contextProfileFoundationField: byId('context-profile-foundation-field'),
                contextProfileFoundationText: byId('context-profile-foundation-text'),
                contextProcessRetentionSelect: byId('context-process-retention'),
                contextProfileSave: byId('context-profile-save'),
                contextProfileSaveAs: byId('context-profile-save-as'),
                contextProfileRestore: byRef(${jsString(VexLifeWebP1.VexContextProfileRestore.identity.ref)}),
                contextProfileExport: byRef(${jsString(VexLifeWebP1.VexContextProfilePortability.identity.ref)}),
                contextProfileImport: byId('context-profile-import'),
                contextProfileHome: byRef(${jsString(VexLifeWebP1.VexContextProfileHome.identity.ref)}),
                contextProfileNote: byId('context-profile-note'),
                contextChangesList: byId('context-changes-list'),
                vexLabsStatus: byId('vex-labs-status'),
                vexLabsTest: byId('vex-labs-test'),
                vexLabsSaveSupported: byId('vex-labs-save-supported'),
                vexLabsRestore: byId('vex-labs-restore'),
                vexLabsReload: byId('vex-labs-reload'),
                vexLabsClear: byId('vex-labs-clear'),
                vexLabsInputBudget: byId('vex-labs-input-budget'),
                vexLabsMemoryMode: byId('vex-labs-memory-mode'),
                vexLabsMemoryBudget: byId('vex-labs-memory-budget'),
                vexLabsNote: byId('vex-labs-note'),
                vexLabsFields: byId('vex-labs-fields'),
                vexLabsRequest: byId('vex-labs-request'),
                vexLabsRequestSha: byId('vex-labs-request-sha'),
                vexLabsRequestBody: byId('vex-labs-request-body'),
                vexContextTurnSummary: byId('vex-context-turn-summary'),
                vexContextBudget: byId('vex-context-budget'),
                contextFoundationStatus: byId('context-foundation-status'),
                contextFoundationBody: byId('context-foundation-body'),
                contextMemoryStatus: byId('context-memory-status'),
                contextMemoryBody: byId('context-memory-body'),
                contextContinuityStatus: byId('context-continuity-status'),
                contextContinuityBody: byId('context-continuity-body'),
                contextConversationStatus: byId('context-conversation-status'),
                contextConversationBody: byId('context-conversation-body'),
                contextBrowserStatus: byId('context-browser-status'),
                contextBrowserBody: byId('context-browser-body'),
                contextRequestStatus: byId('context-request-status'),
                contextRequestBody: byId('context-request-body'),
                contextComponentList: byId('context-component-list'),
                contextProcessStatus: byId('context-process-status'),
                contextProcessBody: byId('context-process-body'),
                contextOutputBody: byId('context-output-body'),
                contextAfterBody: byId('context-after-body'),
                quickActions: byRef(${jsString(VexLifeWebP1.QuickActions.identity.ref)}),
                releaseAlertsPanel: byRef(${jsString(VexLifeWebP1.ReleaseAlertsPanel.identity.ref)}),
                releaseAlertsTitle: byId('release-alerts-title'),
                releaseAlertsEmpty: byId('release-alerts-empty'),
                releaseAlertsList: byRef(${jsString(VexLifeWebP1.ReleaseAlertsList.identity.ref)}),
                languagePanel: byRef(${jsString(VexLifeWebP1.LanguagePanel.identity.ref)}),
                // The typed choice is the labelled field; the control the runtime fills is its select.
                replyLanguage: byRef(${jsString(VexLifeWebP1.ReplyLanguageChoice.identity.ref)})?.querySelector('select'),
                screenLanguage: byRef(${jsString(VexLifeWebP1.ScreenLanguageChoice.identity.ref)})?.querySelector('select'),
                speech: byId('speech'),
                speechSwitch: byId('speech-switch'),
                speechStatus: byId('speech-status'),
                speechVoice: byId('speech-voice'),
                speechThoughts: byId('speech-thoughts'),
                speechRate: byId('speech-rate'),
                speechRateValue: byId('speech-rate-value'),
                speechPlay: byId('speech-play'),
                speechStop: byId('speech-stop'),
                speechPrevious: byId('speech-previous'),
                speechNext: byId('speech-next'),
                speechChoose: byId('speech-choose'),
                pageFrame: byId('page-frame'),
                contextFrame: byId('context-frame'),
                viewportEmpty: byId('viewport-empty'),
                frameStatus: byRef(${jsString(VexLifeWebP1.BrowserFrameStatus.identity.ref)}),
                companionTitle: byId('companion-title'),
                companionMode: byId('companion-mode'),
                companionStatus: byRef(${jsString(VexLifeWebP1.CompanionStatus.identity.ref)}),
                companionStatusText: byId('companion-status-text'),
                companionStatusModel: byId('companion-status-model'),
                visionControl: byRef(${jsString(VexLifeWebP1.Vision.identity.ref)}),
                visionControlStatus: byId('vision-control-status'),
                viewSwitch: byRef(${jsString(VexLifeWebP1.CompanionViewSwitch.identity.ref)}),
                conversation: byRef(${jsString(VexLifeWebP1.Conversation.identity.ref)}),
                settings: byRef(${jsString(VexLifeWebP1.Settings.identity.ref)}),
                timeline: byRef(${jsString(VexLifeWebP1.ConversationTimeline.identity.ref)}),
                jumpLatest: byRef(${jsString(VexLifeWebP1.ConversationJumpLatest.identity.ref)}),
                responseProgress: byRef(${jsString(VexLifeWebP1.ResponseProgress.identity.ref)}),
                contextShare: byRef(${jsString(VexLifeWebP1.ContextShare.identity.ref)}),
                contextShareLabel: byId('context-share-label'),
                ticket: byRef(${jsString(VexLifeWebP1.ContextReceipt.identity.ref)}),
                ticketTitle: byId('ticket-title'),
                ticketLine: byId('ticket-line'),
                ticketUrl: byId('ticket-url'),
                ticketHash: byId('ticket-hash'),
                ticketView: byId('ticket-view'),
                composerHold: byId('composer-hold'),
                composeForm: byId('compose-form'),
                draft: byId('draft'),
                voiceInput: byRef(${jsString(VexLifeWebP1.VoiceInput.identity.ref)}),
                voiceInputOverlay: byId('voice-input-overlay'),
                voiceInputStatus: byId('voice-input-status'),
                voiceWaveform: byId('voice-waveform'),
                send: byRef(${jsString(VexLifeWebP1.Send.identity.ref)}),
                stop: byRef(${jsString(VexLifeWebP1.Stop.identity.ref)}),
                contextWindow: byRef(${jsString(VexLifeWebP1.ContextWindow.identity.ref)}),
                contextCore: byId('context-core'),
                contextUsed: byId('context-used'),
                contextRemaining: byId('context-remaining'),
                contextText: byId('context-text'),
                contextNote: byId('context-note'),
                memory: byRef(${jsString(VexLifeWebP1.MemoryResource.identity.ref)}),
                memoryUsed: byId('memory-used'),
                memoryRemaining: byId('memory-remaining'),
                memoryText: byId('memory-text'),
                memoryNote: byId('memory-note'),
                vexMemoryLabel: byId('vex-memory-label'),
                modelsState: byRef(${jsString(VexLifeWebP1.ModelsState.identity.ref)}),
                modelsCurrentState: byId('models-current-state'),
                modelChoice: byId('model-choice'),
                modelChoiceFacts: byId('model-choice-facts'),
                modelChoiceNotes: byId('model-choice-notes'),
                modelsList: byRef(${jsString(VexLifeWebP1.ModelsList.identity.ref)}),
                modelImportUrl: byId('model-import-url'),
                modelImportCheck: byId('model-import-check'),
                modelImportAdd: byId('model-import-add'),
                modelImportResult: byId('model-import-result'),
                modelImportFacts: byId('model-import-facts'),
                modelConfiguration: byRef(${jsString(VexLifeWebP1.ModelConfiguration.identity.ref)}),
                modelConfigurationRecommendation: byId('model-configuration-recommendation'),
                modelPromptCacheEntries: byId('model-prompt-cache-entries'),
                modelUseRecommended: byRef(${jsString(VexLifeWebP1.ModelUseRecommended.identity.ref)}),
                modelStart: byRef(${jsString(VexLifeWebP1.ModelStart.identity.ref)}),
                modelsNote: byId('models-note'),
                localAsrSelection: byRef(${jsString(VexLifeWebP1.LocalAsrSelection.identity.ref)}),
                localAsrModelsList: byRef(${jsString(VexLifeWebP1.LocalAsrModelsList.identity.ref)}),
                localAsrSpokenLanguage: byRef(${jsString(VexLifeWebP1.LocalAsrSpokenLanguage.identity.ref)}),
                localAsrComposerOutput: byRef(${jsString(VexLifeWebP1.LocalAsrComposerOutput.identity.ref)}),
                localAsrStatus: byRef(${jsString(VexLifeWebP1.LocalAsrStatus.identity.ref)}),
                localAsrNote: byId('local-asr-note'),
                localAsrStorageNote: byRef(${jsString(VexLifeWebP1.LocalAsrStorageNote.identity.ref)}),
                diagnosticsStatus: byId('diagnostics-status'),
                diagnosticsCapture: byId('diagnostics-capture'),
                diagnosticsRecords: byId('diagnostics-records'),
                diagnosticsEnhanced: byRef(${jsString(VexLifeWebP1.DiagnosticsEnhancedCapture.identity.ref)}),
                diagnosticsClear: byRef(${jsString(VexLifeWebP1.DiagnosticsClearHistory.identity.ref)}),
                vexMemoryDetail: byId('vex-memory-detail'),
                resourceNote: byId('resource-note'),
                homeCurrentState: byId('home-current-state'),
                thoughtVisibility: byRef(${jsString(VexLifeWebP1.ThoughtVisibility.identity.ref)}),
                lowMemoryAlert: byRef(${jsString(VexLifeWebP1.LowMemoryAlert.identity.ref)}),
                alertThreshold: byId('alert-threshold'),
                privateMode: byRef(${jsString(VexLifeWebP1.PrivateMode.identity.ref)}),
                privateModeLabel: byId('private-mode-label'),
                privateBanner: byRef(${jsString(VexLifeWebP1.PrivateBanner.identity.ref)}),
                privateBannerText: byId('private-banner-text'),
                privateNotes: byId('private-notes'),
                privateProposal: byId('private-proposal'),
                privateProposalText: byId('private-proposal-text'),
                privateKeep: byId('private-keep'),
                privateDecline: byId('private-decline'),
                privatePropose: byId('private-propose'),
                inspector: byRef(${jsString(VexLifeWebP1.ContextInspector.identity.ref)}),
                inspectorTitle: byId('inspector-title'),
                inspectorFacts: byId('inspector-facts'),
                inspectorText: byId('inspector-text'),
                inspectorClose: byId('inspector-close'),
                snackbar: byId('snackbar')
              });

              const state = {
                conversation: null,
                conversationTurn: null,
                context: null,
                currentness: null,
                currentState: null,
                vision: null,
                visionInFlight: false,
                privateMode: null,
                privateInFlight: false,
                settings: null,
                releaseAlerts: null,
                releaseAlertsOpen: false,
                quickActionsOpen: false,
                languageOpen: false,
                privatePending: null,
                app: (() => {
                  try {
                    const saved = localStorage.getItem(apps.storageKey);
                    if (saved && apps.keys.includes(saved)) return saved;
                  } catch {}
                  return apps.sourceTemplate;
                })(),
                memory: null,
                contextProfiles: null,
                foundationalField: null,
                vexLabs: null,
                vexLabsDraft: null,
                vexLabsLastResult: null,
                vexLabsInFlight: false,
                vexLabsPending: null,
                companionMode: 'CONVERSATION',
                contextProfileDirty: false,
                contextProfileInFlight: false,
                // Browser-session presentation preference only. Never part of a portable Context Profile.
                contextProcessRetention: 'SESSION_ONLY',
                // Browser-session presentation state only. Conversation remains the exact owner;
                // this map only remembers which long projected message cards the person collapsed.
                contextConversationTurnRef: null,
                contextConversationCollapsedByEventRef: new Map(),
                diagnostics: null,
                diagnosticsInFlight: false,
                // Settings → Models projection (VexLifeModels); refreshed while a download runs.
                models: null,
                modelsInFlight: false,
                modelImportInFlight: false,
                // Local microphone recognition is independent from the serving reasoning model.
                localAsr: null,
                localAsrInFlight: false,
                workspaceStrings: null,
                browserContext: null,
                includePage: false,
                humanPageUrl: null,
                humanFrameLoadExpected: false,
                contextTargetPageUrl: null,
                contextLoading: false,
                view: 'conversation',
                allocation: null,
                collapsed: 'none',
                pending: null,
                sendInFlight: false,
                sendCancelInFlight: false,
                sendCancelRequested: false,
                streamPhase: null,
                streamedContent: '',
                streamedReasoning: '',
                thoughtsVisible: true,
                humanReasoningByResponse: new Map(),
                // Exact provider requests returned only by the live Send. Never persisted or re-formed.
                providerRequestsByResponse: new Map(),
                // Human-readable browser-session projection of facts already emitted by the Send
                // stream. This is deliberately not a turn/stage owner: it may open/freeze visual
                // containers, but it never feeds DirectCompanionTurn, capability execution, or the
                // raw streamedReasoning/streamedContent buffers consumed by Speech/Kokoro.
                liveProcessEntries: [],
                processEntriesByResponse: new Map(),
                // Session-only presentation of terminal failed Sends. Conversation/recovery remains
                // the semantic owner; this only preserves what the human already saw in this browser
                // so terminal failure does not erase the process before the next lawful Control.
                failedAttempts: [],
                liveProcessExpanded: true,
                liveProcessManuallyToggled: false,
                // What Vex reached during the live turn (bridge uses: handle/action/target/state), and
                // per completed response for this browser session. Reach, never the content.
                liveReaches: [],
                reachesByResponse: new Map(),
                expandedThoughts: new Set(),
                timelineFollowTail: true,
                timelineScrollTop: 0,
                reasoningViewports: new Map(),
                theme: null
              };

              const vexLabsProfileBudgetFieldRef = 'field.vexlife-prod.context-profile.input-token-budget';
              const vexLabsMemoryBudgetFieldRef = 'field.vexlife-prod.continuity.maximum-memory-tokens';
              const isVexLabs = () => state.companionMode === 'LAB';
              const ensureVexLabsDraft = () => {
                if (state.vexLabsDraft) return state.vexLabsDraft;
                const defaults = state.vexLabs?.defaults;
                state.vexLabsDraft = {
                  inputTokenBudget: Number(defaults?.inputTokenBudget ?? 192000),
                  maximumMemoryTokens: Number(defaults?.maximumMemoryTokens ?? 100000),
                  memoryMode: String(defaults?.memoryMode ?? 'CURRENT')
                };
                return state.vexLabsDraft;
              };

              const vexLabsDraftMatchesLastTest = () => {
                const draft = ensureVexLabsDraft();
                const tested = state.vexLabsLastResult;
                return Boolean(
                  tested?.state === 'LAB_TURN_COMPLETED' &&
                  Number(tested.promptInputTokenBudget) === Number(draft.inputTokenBudget) &&
                  Number(tested.maximumMemoryTokens) === Number(draft.maximumMemoryTokens) &&
                  String(tested.memoryMode) === String(draft.memoryMode)
                );
              };

              const vexLabsAdmissionText = (admission) => {
                if (!admission) return '';
                const requested = Number(admission.requestedInputTokenBudget ?? 0).toLocaleString();
                const minimum = Number(admission.minimumInputTokenBudget ?? 0).toLocaleString();
                if (admission.reason === 'IMMEDIATE_PRIOR_REQUIRED') {
                  return requested + ' VexLife-estimated tokens is too small for this turn. This attempt observed a floor of ' + minimum +
                    ' to keep the required current state plus the exact immediate prior Lab turn. Raise the Lab budget and retest; the observed floor can move as more context becomes eligible.';
                }
                return requested + ' VexLife-estimated tokens is too small for this turn. This attempt observed a mandatory-current-state floor of ' + minimum +
                  ' before older Conversation or Memory can fit. Raise the Lab budget and retest; the observed floor can move as more context becomes eligible.';
              };

              // VoiceInput execution lives at the browser boundary. Semantic meaning comes from
              // VexLifeVoiceInputFamily above; browser API strings are converted here and do not leak
              // into Product State or human-facing copy.
              const dictation = {
                stateRef: null,
                holdReasonRef: null,
                failureReasonRef: null,
                language: null,
                desiredActive: false,
                session: 0,
                stream: null,
                audioContext: null,
                audioSource: null,
                analyser: null,
                processor: null,
                silentGain: null,
                animationFrame: null,
                progressiveTimer: null,
                progressivePromise: null,
                progressiveInFlight: false,
                progressiveCooldownUntil: 0,
                progressiveLastAt: 0,
                progressiveLastSampleCount: 0,
                lastSpeechAt: 0,
                speechActive: false,
                speechSinceLastCommit: false,
                noiseFloor: 0.004,
                lastHypothesis: '',
                stableHypothesisPasses: 0,
                sampleRate: 0,
                pcmChunks: [],
                sampleCount: 0,
                draftPrefix: '',
                finalTranscript: '',
                interimTranscript: ''
              };

              const voiceInputLanguage = () => {
                const code = state.localAsr?.resolvedSpokenLanguageCode ?? null;
                return code ? { code } : null;
              };

              // While one Begin/End realization is active, capability refreshes may observe new
              // provider facts but may not overwrite the Family's in-flight session State.
              const voiceInputSessionOwnsState = () =>
                dictation.stateRef === voiceInput.state.requestingMicrophone ||
                dictation.stateRef === voiceInput.state.listening ||
                dictation.stateRef === voiceInput.state.stopping;

              // Browser keeps only a local mirror of Family-owned semantic output. It may not
              // manufacture State/reason refs from provider facts.
              const applyVoiceInputSemanticState = (semanticState) => {
                if (!semanticState) return;
                dictation.stateRef = semanticState.stateRef;
                dictation.holdReasonRef = semanticState.holdReasonRef ?? null;
                dictation.failureReasonRef = semanticState.failureReasonRef ?? null;
              };

              const voiceInputCapabilityKey = (
                localRecognitionAvailable,
                microphoneApiAvailable,
                audioFeedbackAvailable
              ) =>
                [localRecognitionAvailable, microphoneApiAvailable, audioFeedbackAvailable]
                  .map((value) => value ? '1' : '0')
                  .join(':');

              const selectedLocalAsrOption = () => {
                const projection = state.localAsr;
                if (!projection) return null;
                return (projection.options ?? []).find((option) =>
                  option.providerRef === projection.selectedProviderRef &&
                  option.modelRef === projection.selectedModelRef
                ) ?? null;
              };

              const localAsrAvailableForLanguage = (language) => {
                const option = selectedLocalAsrOption();
                const supportedCodes = option?.languageCodes ?? [];
                return Boolean(
                  language &&
                  option &&
                  option.currentState === 'READY' &&
                  (
                    supportedCodes.includes(language.code) ||
                    (language.code === 'auto' && supportedCodes.length > 1)
                  )
                );
              };

              const observeVoiceInputCapability = (force = false) => {
                if (!force && voiceInputSessionOwnsState()) return;
                const language = voiceInputLanguage();
                const semanticState =
                  voiceInput.capabilityStateByFacts[
                    voiceInputCapabilityKey(
                      localAsrAvailableForLanguage(language),
                      Boolean(navigator.mediaDevices?.getUserMedia),
                      Boolean(window.AudioContext || window.webkitAudioContext)
                    )
                  ];
                if (!semanticState) return;
                applyVoiceInputSemanticState(semanticState);
              };

              const voiceInputActFor = (controlRef) =>
                voiceInput.actByState[dictation.stateRef]?.[controlRef] ?? null;

              const voiceInputDisplay = () => {
                if (dictation.stateRef === voiceInput.state.held) {
                  return voiceInput.holdDisplayByReason[dictation.holdReasonRef] ?? null;
                }
                if (dictation.stateRef === voiceInput.state.failed) {
                  return voiceInput.failureDisplayByReason[dictation.failureReasonRef] ??
                    voiceInput.failureDisplayByReason[voiceInput.failureReason.unknown];
                }
                return voiceInput.displayByState[dictation.stateRef] ?? null;
              };

              const renderVoiceInput = () => {
                if (!ui.voiceInput) return;
                const display = voiceInputDisplay();
                if (!display) return;
                ui.voiceInput.disabled =
                  isVexLabs() || !display.primaryControlRef || state.sendInFlight || state.privateInFlight;
                ui.voiceInput.dataset.state = dictation.stateRef ?? '';
                ui.voiceInput.dataset.holdReason = dictation.holdReasonRef ?? '';
                ui.voiceInput.dataset.failureReason = dictation.failureReasonRef ?? '';
                ui.voiceInput.setAttribute('aria-pressed', String(Boolean(display.active)));
                const overlay = display.attention === voiceInput.attentionOverlay;
                ui.voiceInput.setAttribute('aria-expanded', String(overlay));
                const humanLabel = display.primaryLabelKey
                  ? t(display.primaryLabelKey)
                  : t(display.statusKey);
                ui.voiceInput.setAttribute('aria-label', humanLabel);
                ui.voiceInput.title = humanLabel;
                if (ui.voiceInputOverlay) ui.voiceInputOverlay.hidden = !overlay;
                if (ui.voiceInputStatus) ui.voiceInputStatus.textContent = t(display.statusKey);
              };

              const stopVoiceCapture = async () => {
                if (dictation.progressiveTimer !== null) {
                  window.clearInterval(dictation.progressiveTimer);
                  dictation.progressiveTimer = null;
                }
                if (dictation.animationFrame !== null) {
                  cancelAnimationFrame(dictation.animationFrame);
                  dictation.animationFrame = null;
                }
                if (dictation.processor) {
                  dictation.processor.onaudioprocess = null;
                  try { dictation.processor.disconnect(); } catch {}
                  dictation.processor = null;
                }
                if (dictation.silentGain) {
                  try { dictation.silentGain.disconnect(); } catch {}
                  dictation.silentGain = null;
                }
                if (dictation.audioSource) {
                  try { dictation.audioSource.disconnect(); } catch {}
                  dictation.audioSource = null;
                }
                if (dictation.stream) {
                  for (const track of dictation.stream.getTracks()) track.stop();
                  dictation.stream = null;
                }
                const context = dictation.audioContext;
                dictation.audioContext = null;
                dictation.analyser = null;
                if (context && context.state !== 'closed') {
                  await context.close().catch(() => {});
                }
                if (ui.voiceWaveform) {
                  const context2d = ui.voiceWaveform.getContext('2d');
                  context2d?.clearRect(0, 0, ui.voiceWaveform.width, ui.voiceWaveform.height);
                }
              };

              const drawVoiceWaveform = () => {
                const analyser = dictation.analyser;
                const canvas = ui.voiceWaveform;
                if (!dictation.desiredActive || !analyser || !canvas) return;
                const context2d = canvas.getContext('2d');
                if (!context2d) return;
                const values = new Uint8Array(analyser.frequencyBinCount);
                analyser.getByteFrequencyData(values);
                context2d.clearRect(0, 0, canvas.width, canvas.height);
                context2d.fillStyle = getComputedStyle(ui.voiceInput).color;
                const bars = 14;
                const gap = 3;
                const width = (canvas.width - gap * (bars - 1)) / bars;
                for (let index = 0; index < bars; index += 1) {
                  const bucket = Math.floor((index / bars) * values.length);
                  const normalized = values[bucket] / 255;
                  const height = Math.max(3, normalized * (canvas.height - 8));
                  const x = index * (width + gap);
                  const y = (canvas.height - height) / 2;
                  context2d.fillRect(x, y, width, height);
                }
                dictation.animationFrame = requestAnimationFrame(drawVoiceWaveform);
              };

              const startVoiceCapture = async (stream) => {
                const AudioContextClass = window.AudioContext || window.webkitAudioContext;
                const context = new AudioContextClass();
                await context.resume();
                const source = context.createMediaStreamSource(stream);
                const analyser = context.createAnalyser();
                analyser.fftSize = 128;
                analyser.smoothingTimeConstant = 0.78;
                source.connect(analyser);

                // ScriptProcessor is deliberately used only for the first lived local-ASR circuit:
                // it exposes raw PCM without another browser recognition provider. Once the circuit
                // is proven, AudioWorklet can replace this concrete capture mechanism without
                // changing VoiceInput or local-ASR ownership.
                const processor = context.createScriptProcessor(4096, 1, 1);
                const silentGain = context.createGain();
                silentGain.gain.value = 0;
                source.connect(processor);
                processor.connect(silentGain);
                silentGain.connect(context.destination);

                dictation.sampleRate = context.sampleRate;
                dictation.pcmChunks = [];
                dictation.sampleCount = 0;
                processor.onaudioprocess = (event) => {
                  if (!dictation.desiredActive) return;
                  const input = event.inputBuffer.getChannelData(0);
                  const copy = new Float32Array(input.length);
                  copy.set(input);
                  dictation.pcmChunks.push(copy);
                  dictation.sampleCount += copy.length;

                  let power = 0;
                  for (let index = 0; index < input.length; index += 1) {
                    power += input[index] * input[index];
                  }
                  const rms = Math.sqrt(power / Math.max(1, input.length));
                  const threshold = Math.max(0.008, dictation.noiseFloor * 3.0);
                  const now = performance.now();
                  if (rms >= threshold) {
                    dictation.speechActive = true;
                    dictation.speechSinceLastCommit = true;
                    dictation.lastSpeechAt = now;
                  } else {
                    if (rms < threshold * 0.7) {
                      dictation.noiseFloor =
                        Math.max(0.001, Math.min(0.03, dictation.noiseFloor * 0.97 + rms * 0.03));
                    }
                    if (
                      dictation.speechActive &&
                      dictation.lastSpeechAt > 0 &&
                      now - dictation.lastSpeechAt >= 220
                    ) {
                      dictation.speechActive = false;
                    }
                  }
                };

                dictation.progressiveTimer = window.setInterval(() => {
                  void maybeProgressiveTranscription();
                }, 250);

                dictation.audioContext = context;
                dictation.audioSource = source;
                dictation.analyser = analyser;
                dictation.processor = processor;
                dictation.silentGain = silentGain;
                drawVoiceWaveform();
              };

              const appendTranscriptSegment = (existing, next) => {
                const segment = String(next ?? '').trim();
                if (!segment) return existing;
                return existing ? existing + ' ' + segment : segment;
              };

              const projectDictationIntoDraft = () => {
                const spoken = [dictation.finalTranscript, dictation.interimTranscript]
                  .map((value) => String(value ?? '').trim())
                  .filter(Boolean)
                  .join(' ');
                const prefix = dictation.draftPrefix;
                ui.draft.value =
                  prefix +
                  (prefix && spoken && !/\s$/u.test(prefix) ? ' ' : '') +
                  spoken;
                sizeDraft();
              };

              const encodeCapturedPcmWav = () => {
                const sampleRate = Number(dictation.sampleRate);
                const sampleCount = Number(dictation.sampleCount);
                if (!Number.isFinite(sampleRate) || sampleRate <= 0 || sampleCount <= 0) return null;

                const bytes = new ArrayBuffer(44 + sampleCount * 2);
                const view = new DataView(bytes);
                const writeText = (offset, text) => {
                  for (let index = 0; index < text.length; index += 1) {
                    view.setUint8(offset + index, text.charCodeAt(index));
                  }
                };

                writeText(0, 'RIFF');
                view.setUint32(4, 36 + sampleCount * 2, true);
                writeText(8, 'WAVE');
                writeText(12, 'fmt ');
                view.setUint32(16, 16, true);
                view.setUint16(20, 1, true);
                view.setUint16(22, 1, true);
                view.setUint32(24, sampleRate, true);
                view.setUint32(28, sampleRate * 2, true);
                view.setUint16(32, 2, true);
                view.setUint16(34, 16, true);
                writeText(36, 'data');
                view.setUint32(40, sampleCount * 2, true);

                let sampleIndex = 0;
                for (const chunk of dictation.pcmChunks) {
                  for (let index = 0; index < chunk.length; index += 1) {
                    const sample = Math.max(-1, Math.min(1, chunk[index]));
                    view.setInt16(
                      44 + sampleIndex * 2,
                      sample < 0 ? sample * 0x8000 : sample * 0x7fff,
                      true
                    );
                    sampleIndex += 1;
                  }
                }
                return new Uint8Array(bytes);
              };

              const clearCapturedPcm = (preserveSampleRate = false) => {
                dictation.pcmChunks = [];
                dictation.sampleCount = 0;
                if (!preserveSampleRate) dictation.sampleRate = 0;
                dictation.progressiveLastSampleCount = 0;
              };

              const voiceInputFailureReason = (error, stage) => {
                const code = String(error ?? '');
                if (
                  stage === 'microphone' &&
                  (
                    code === 'not-allowed' ||
                    code === 'service-not-allowed' ||
                    code === 'NotAllowedError' ||
                    code === 'Permission denied'
                  )
                ) {
                  return voiceInput.failureReason.microphonePermissionRequired;
                }
                if (
                  stage === 'microphone' &&
                  (code === 'audio-capture' || code === 'NotFoundError')
                ) {
                  return voiceInput.failureReason.microphoneUnavailable;
                }
                if (
                  stage === 'local-asr' ||
                  stage === 'local-asr-prepare' ||
                  code === 'AbortError' ||
                  code === 'InvalidStateError'
                ) {
                  return voiceInput.failureReason.recognitionStopped;
                }
                return voiceInput.failureReason.unknown;
              };

              const settleVoiceInputFailure = async (semanticState) => {
                dictation.desiredActive = false;
                applyVoiceInputSemanticState(semanticState);
                await stopVoiceCapture();
                clearCapturedPcm();
                dictation.language = null;
                renderVoiceInput();
              };

              const failVoiceInput = async (failureReasonRef, options = {}) => {
                const semanticState =
                  voiceInput.failureStateByReason[failureReasonRef] ??
                  voiceInput.failureStateByReason[voiceInput.failureReason.unknown];
                if (!semanticState) return;
                reportFact('VOICE_INPUT_FAILURE_OBSERVED', {
                  stage: String(options.stage ?? 'unknown'),
                  code: String(options.code ?? 'unknown'),
                  failureReasonRef,
                  stateRef: dictation.stateRef ?? ''
                });
                await settleVoiceInputFailure(semanticState);
              };

              const prepareLocalAsr = async () => {
                const response = await effectFetch(service.localAsrPreparePath, {
                  method: 'POST',
                  headers: { accept: 'application/json' }
                });
                if (!response.ok) {
                  await readJson(response);
                }
              };

              const transcribeCapturedAudio = async (
                language,
                dictationPass = ${jsString(VexLifeProdServiceContract.LocalAsrDictationPass.Final.wire)}
              ) => {
                const wav = encodeCapturedPcmWav();
                if (!wav) return null;
                const response = await effectFetch(service.localAsrTranscribePath, {
                  method: 'POST',
                  headers: {
                    accept: 'application/json',
                    'content-type': 'audio/wav',
                    'x-vexlife-language': language.code,
                    ${jsString(VexLifeProdServiceContract.LocalAsrDictationPassHeader)}: dictationPass
                  },
                  body: wav
                });
                const result = await readJson(response);
                const projection = state.localAsr;
                if (
                  !projection ||
                  result.providerRef !== projection.selectedProviderRef ||
                  result.modelRef !== projection.selectedModelRef
                ) {
                  throw new Error('Local speech recognition returned from a provider other than the selected one.');
                }
                return String(result.transcript ?? '').trim();
              };

              const normalizedHypothesis = (value) =>
                String(value ?? '')
                  .trim()
                  .replace(/\s+/gu, ' ');

              const endsAtStrongSentenceBoundary = (value) =>
                /[.!?。！？؟।॥][\s"'”’）)\]]*$/u.test(String(value ?? '').trim());

              const endsWithEnglishFiller = (value, language) =>
                language?.code === 'en' &&
                /(?:^|[\s,])(?:um+|uh+|erm+|hmm+)[.!?…]*$/iu.test(String(value ?? '').trim());

              const maybeProgressiveTranscription = () => {
                if (
                  !dictation.desiredActive ||
                  dictation.progressiveInFlight ||
                  !dictation.speechSinceLastCommit ||
                  !dictation.language ||
                  dictation.sampleRate <= 0 ||
                  performance.now() < dictation.progressiveCooldownUntil
                ) {
                  return;
                }

                const now = performance.now();
                const minimumSamples = dictation.sampleRate * 0.8;
                const newSamples =
                  dictation.sampleCount - dictation.progressiveLastSampleCount;
                if (dictation.sampleCount < minimumSamples || newSamples < dictation.sampleRate * 0.35) {
                  return;
                }

                const silenceMs =
                  dictation.lastSpeechAt > 0
                    ? Math.max(0, now - dictation.lastSpeechAt)
                    : 0;
                const periodicWhileSpeaking =
                  dictation.speechActive &&
                  now - dictation.progressiveLastAt >= 1400;
                const refineThinkingPause =
                  !dictation.speechActive &&
                  silenceMs >= 550 &&
                  now - dictation.progressiveLastAt >= 500;

                if (!periodicWhileSpeaking && !refineThinkingPause) return;

                const requestSession = dictation.session;
                const requestedSamples = dictation.sampleCount;
                dictation.progressiveLastAt = now;
                dictation.progressiveLastSampleCount = requestedSamples;
                dictation.progressiveInFlight = true;

                const run = (async () => {
                  try {
                    const transcript =
                      await transcribeCapturedAudio(
                        dictation.language,
                        ${jsString(VexLifeProdServiceContract.LocalAsrDictationPass.Partial.wire)}
                      );
                    if (
                      !transcript ||
                      requestSession !== dictation.session ||
                      !dictation.desiredActive
                    ) {
                      return;
                    }

                    const normalized = normalizedHypothesis(transcript);
                    if (normalized === normalizedHypothesis(dictation.lastHypothesis)) {
                      dictation.stableHypothesisPasses += 1;
                    } else {
                      dictation.lastHypothesis = transcript;
                      dictation.stableHypothesisPasses = 1;
                    }

                    dictation.interimTranscript = transcript;
                    projectDictationIntoDraft();

                    const completedAt = performance.now();
                    const silenceAfterResult =
                      dictation.lastSpeechAt > 0
                        ? Math.max(0, completedAt - dictation.lastSpeechAt)
                        : 0;
                    const noNewAudio = dictation.sampleCount === requestedSamples;
                    const stable = dictation.stableHypothesisPasses >= 2;
                    const sentenceBoundary = endsAtStrongSentenceBoundary(transcript);
                    const fillerTail = endsWithEnglishFiller(transcript, dictation.language);

                    const sentenceSettled =
                      noNewAudio &&
                      !dictation.speechActive &&
                      silenceAfterResult >= 900 &&
                      sentenceBoundary &&
                      stable &&
                      !fillerTail;
                    const longPauseSettled =
                      noNewAudio &&
                      !dictation.speechActive &&
                      silenceAfterResult >= 2600 &&
                      stable &&
                      !fillerTail;

                    if (sentenceSettled || longPauseSettled) {
                      dictation.finalTranscript =
                        appendTranscriptSegment(dictation.finalTranscript, transcript);
                      dictation.interimTranscript = '';
                      dictation.lastHypothesis = '';
                      dictation.stableHypothesisPasses = 0;
                      dictation.speechSinceLastCommit = false;
                      clearCapturedPcm(true);
                      projectDictationIntoDraft();
                    }
                  } catch {
                    // Progressive recognition is an enhancement. A partial-pass failure must never
                    // kill the microphone session or the proven final Stop -> transcript fallback.
                    dictation.progressiveCooldownUntil = performance.now() + 3000;
                  } finally {
                    if (requestSession === dictation.session) {
                      dictation.progressiveInFlight = false;
                    }
                  }
                })();

                dictation.progressivePromise = run;
              };

              const realizeEndLocalDictation = async (abort = false) => {
                const language = dictation.language ?? voiceInputLanguage();
                dictation.desiredActive = false;
                dictation.session += 1;
                const progressive = dictation.progressivePromise;
                await stopVoiceCapture();
                if (progressive) await progressive.catch(() => {});

                if (abort) {
                  clearCapturedPcm();
                  dictation.language = null;
                  observeVoiceInputCapability(true);
                  renderVoiceInput();
                  return;
                }

                try {
                  const transcript =
                    dictation.speechSinceLastCommit
                      ? await transcribeCapturedAudio(language)
                      : null;
                  if (transcript) {
                    dictation.finalTranscript =
                      appendTranscriptSegment(dictation.finalTranscript, transcript);
                    dictation.interimTranscript = '';
                    projectDictationIntoDraft();
                  }
                  clearCapturedPcm();
                  dictation.language = null;
                  await loadLocalAsr().catch(() => {});
                  observeVoiceInputCapability(true);
                  renderVoiceInput();
                } catch (error) {
                  const code =
                    error instanceof Error
                      ? error.message
                      : String(error);
                  await failVoiceInput(
                    voiceInputFailureReason(code, 'local-asr'),
                    { stage: 'local-asr', code }
                  );
                }
              };

              const realizeBeginLocalDictation = async (language) => {
                if (
                  !language ||
                  !localAsrAvailableForLanguage(language) ||
                  !navigator.mediaDevices?.getUserMedia ||
                  !(window.AudioContext || window.webkitAudioContext)
                ) {
                  dictation.desiredActive = false;
                  observeVoiceInputCapability(true);
                  renderVoiceInput();
                  return;
                }

                const session = dictation.session + 1;
                dictation.session = session;
                dictation.desiredActive = true;
                dictation.language = language;
                dictation.draftPrefix = ui.draft.value;
                dictation.finalTranscript = '';
                dictation.interimTranscript = '';
                dictation.progressivePromise = null;
                dictation.progressiveInFlight = false;
                dictation.progressiveCooldownUntil = 0;
                dictation.progressiveLastAt = 0;
                dictation.progressiveLastSampleCount = 0;
                dictation.lastSpeechAt = 0;
                dictation.speechActive = false;
                dictation.speechSinceLastCommit = false;
                dictation.noiseFloor = 0.004;
                dictation.lastHypothesis = '';
                dictation.stableHypothesisPasses = 0;
                clearCapturedPcm();

                reportFact('VOICE_INPUT_MICROPHONE_REQUESTED', {
                  language: language.code
                });

                const providerReady = prepareLocalAsr();

                let stream;
                try {
                  stream = await navigator.mediaDevices.getUserMedia({
                    audio: {
                      echoCancellation: true,
                      noiseSuppression: true,
                      autoGainControl: true
                    },
                    video: false
                  });
                } catch (error) {
                  void providerReady.catch(() => {});
                  if (session !== dictation.session) return;
                  const code =
                    error instanceof DOMException
                      ? error.name
                      : error instanceof Error
                        ? error.message
                        : error;
                  await failVoiceInput(
                    voiceInputFailureReason(code, 'microphone'),
                    { stage: 'microphone', code }
                  );
                  return;
                }

                if (!dictation.desiredActive || session !== dictation.session) {
                  for (const track of stream.getTracks()) track.stop();
                  return;
                }

                reportFact('VOICE_INPUT_MICROPHONE_GRANTED', {
                  language: language.code
                });
                dictation.stream = stream;

                try {
                  await providerReady;
                } catch (error) {
                  if (session !== dictation.session) return;
                  const code =
                    error instanceof Error
                      ? error.message
                      : String(error);
                  await failVoiceInput(
                    voiceInputFailureReason(code, 'local-asr-prepare'),
                    { stage: 'local-asr-prepare', code }
                  );
                  return;
                }

                if (!dictation.desiredActive || session !== dictation.session) return;

                try {
                  await startVoiceCapture(stream);
                } catch (error) {
                  const code =
                    error instanceof DOMException
                      ? error.name
                      : error instanceof Error
                        ? error.message
                        : error;
                  await failVoiceInput(
                    voiceInput.failureReason.unknown,
                    { stage: 'audio-capture', code }
                  );
                  return;
                }

                if (!dictation.desiredActive || session !== dictation.session) return;

                reportFact('VOICE_INPUT_AUDIO_CAPTURE_STARTED', {
                  language: language.code,
                  sampleRate: String(dictation.sampleRate),
                  providerRef: String(state.localAsr?.selectedProviderRef ?? ''),
                  modelRef: String(state.localAsr?.selectedModelRef ?? '')
                });
                applyVoiceInputSemanticState(voiceInput.recognitionStartedState);
                renderVoiceInput();
              };

              const applyVoiceInputControl = async (options = {}) => {
                if (state.sendInFlight || state.privateInFlight) return;

                if (
                  !dictation.desiredActive &&
                  (dictation.stateRef === null ||
                    dictation.stateRef === voiceInput.state.ready ||
                    dictation.stateRef === voiceInput.state.held)
                ) {
                  observeVoiceInputCapability();
                }

                const display = voiceInputDisplay();
                const controlRef = options.controlRef ?? display?.primaryControlRef ?? null;
                if (!controlRef) {
                  renderVoiceInput();
                  return;
                }

                const act = voiceInputActFor(controlRef);
                if (!act) {
                  renderVoiceInput();
                  return;
                }

                reportFact('VOICE_INPUT_CONTROL_EMITTED', {
                  controlRef,
                  currentStateRef: dictation.stateRef ?? '',
                  admittedStateRef: act.admittedState?.stateRef ?? '',
                  effectRef: act.effectRef ?? ''
                });
                if (act.admittedState) {
                  applyVoiceInputSemanticState(act.admittedState);
                }
                renderVoiceInput();

                if (act.effectRef === voiceInput.effect.beginLocalDictation) {
                  const language = voiceInputLanguage();
                  if (!language) {
                    observeVoiceInputCapability();
                    renderVoiceInput();
                    return;
                  }
                  await realizeBeginLocalDictation(language);
                } else if (act.effectRef === voiceInput.effect.endLocalDictation) {
                  await realizeEndLocalDictation(Boolean(options.abort));
                }
              };

              const quickActionSvg = (paths) =>
                '<svg viewBox="0 0 24 24" aria-hidden="true">' + paths + '</svg>';

              const configureSourceQuickAction = (id, action, label, icon) => {
                const button = byId(id);
                const item = button?.closest('li');
                if (!button || !item) return null;
                item.hidden = false;
                item.className = 'quick-action-item';
                button.hidden = false;
                button.type = 'button';
                button.className = 'quick-action';
                button.dataset.action = action;
                button.dataset.bound = 'true';
                button.setAttribute('aria-label', label);
                button.title = label;
                button.innerHTML = quickActionSvg(icon);
                return { item, button };
              };

              const createQuickActionButton = (id, action, label, glyph) => {
                const item = document.createElement('li');
                item.className = 'quick-action-item';
                const button = document.createElement('button');
                button.id = id;
                button.type = 'button';
                button.className = 'quick-action quick-action-glyph';
                button.dataset.action = action;
                button.dataset.bound = 'true';
                button.setAttribute('aria-label', label);
                button.title = label;
                button.textContent = glyph;
                item.append(button);
                return { item, button };
              };

              const initializeQuickActions = () => {
                if (!ui.quickActions || byId('quick-actions-trigger')) return;

                const sourceItems = [...ui.quickActions.children];
                const speechAction = configureSourceQuickAction(
                  'speech-source',
                  'speech',
                  'Speech',
                  '<path d="M4 9.5h3.2L11.5 6v12l-4.3-3.5H4z"/><path d="M15 9.2a4 4 0 0 1 0 5.6M17.6 6.8a7.3 7.3 0 0 1 0 10.4"/>'
                );
                const languageAction = configureSourceQuickAction(
                  'language-source',
                  'language',
                  'Language',
                  '<path d="M5 20.5V4M5 4.5h12.5l-2 3.5 2 3.5H5"/>'
                );
                const visionAction = configureSourceQuickAction(
                  'vision-quick-access-source',
                  'vision',
                  'Vision',
                  '<path d="M3 12s3.6-5.2 9-5.2 9 5.2 9 5.2-3.6 5.2-9 5.2S3 12 3 12Z"/><circle cx="12" cy="12" r="2.8"/>'
                );
                const releaseAlertsAction = configureSourceQuickAction(
                  'release-alerts-source',
                  'release-alerts',
                  ${jsString(VexLifeReleaseInboxFamily.KeyQuickAction)},
                  '<path d="M7 10a5 5 0 0 1 10 0v3l2 3H5l2-3z"/><path d="M10 19h4"/>'
                );
                const themeAction = createQuickActionButton(
                  'theme-quick-action',
                  'theme',
                  'Toggle dark / light theme',
                  currentTheme() === 'dark' ? '🌙' : '☀️'
                );

                const supportItem = document.createElement('li');
                supportItem.className = 'quick-action-item';
                const support = document.createElement('a');
                support.id = 'support-quick-action';
                support.className = 'quick-action quick-action-glyph';
                support.dataset.action = 'support';
                support.dataset.bound = 'true';
                support.href = 'https://vgong24.github.io/Vextreme/pages/vex-support.html';
                support.target = '_blank';
                support.rel = 'noopener noreferrer';
                support.setAttribute('aria-label', 'Support Vextreme and VexLife');
                support.title = 'Support the creator';
                support.textContent = '🫴';
                supportItem.append(support);

                const triggerItem = document.createElement('li');
                triggerItem.className = 'quick-action-trigger-item';
                const trigger = document.createElement('button');
                trigger.id = 'quick-actions-trigger';
                trigger.type = 'button';
                trigger.className = 'quick-action quick-action-trigger quick-action-glyph';
                trigger.setAttribute('aria-label', 'Open quick actions');
                trigger.setAttribute('aria-expanded', 'false');
                trigger.textContent = '🌀';
                triggerItem.append(trigger);

                const visibleSourceItems = new Set(
                  [
                    speechAction?.item,
                    languageAction?.item,
                    visionAction?.item,
                    releaseAlertsAction?.item
                  ].filter(Boolean)
                );
                const preservedSourceItems = sourceItems.filter((item) => !visibleSourceItems.has(item));
                ui.quickActions.replaceChildren(
                  ...preservedSourceItems,
                  ...(speechAction ? [speechAction.item] : []),
                  ...(languageAction ? [languageAction.item] : []),
                  ...(visionAction ? [visionAction.item] : []),
                  themeAction.item,
                  ...(releaseAlertsAction ? [releaseAlertsAction.item] : []),
                  supportItem,
                  triggerItem
                );
                ui.quickActions.dataset.open = 'false';

                trigger.addEventListener('click', (event) => {
                  event.stopPropagation();
                  state.quickActionsOpen = !state.quickActionsOpen;
                  if (!state.quickActionsOpen) {
                    state.languageOpen = false;
                    state.releaseAlertsOpen = false;
                    speech.open = false;
                    speech.choosing = false;
                    renderReleaseAlerts();
                  }
                  renderQuickActions();
                });

                themeAction.button.addEventListener('click', (event) => {
                  event.stopPropagation();
                  toggleTheme();
                  renderQuickActions();
                });

                speechAction?.button.addEventListener('click', (event) => {
                  event.stopPropagation();
                  state.languageOpen = false;
                  state.releaseAlertsOpen = false;
                  speech.open = !speech.open;
                  if (!speech.open) speech.choosing = false;
                  renderReleaseAlerts();
                  renderSpeechVoices();
                  renderQuickActions();
                });

                languageAction?.button.addEventListener('click', (event) => {
                  event.stopPropagation();
                  state.languageOpen = !state.languageOpen;
                  if (state.languageOpen) {
                    speech.open = false;
                    speech.choosing = false;
                    state.releaseAlertsOpen = false;
                  }
                  renderReleaseAlerts();
                  renderQuickActions();
                });

                visionAction?.button.addEventListener('click', (event) => {
                  event.stopPropagation();
                  ui.visionControl?.click();
                });

                releaseAlertsAction?.button.addEventListener('click', (event) => {
                  event.stopPropagation();
                  state.releaseAlertsOpen = !state.releaseAlertsOpen;
                  if (state.releaseAlertsOpen) {
                    state.languageOpen = false;
                    speech.open = false;
                    speech.choosing = false;
                  }
                  renderReleaseAlerts();
                  renderQuickActions();
                });

                document.addEventListener('click', (event) => {
                  if (!state.quickActionsOpen) return;
                  // Authored LocalInteractions may activate an existing Product Control with a
                  // synthetic click. Only a real human click outside the Spiral dismisses it.
                  if (!event.isTrusted) return;
                  if (
                    ui.quickActions.contains(event.target) ||
                    ui.speech?.contains(event.target) ||
                    ui.languagePanel?.contains(event.target) ||
                    ui.releaseAlertsPanel?.contains(event.target)
                  ) return;
                  state.quickActionsOpen = false;
                  state.languageOpen = false;
                  state.releaseAlertsOpen = false;
                  speech.open = false;
                  speech.choosing = false;
                  renderReleaseAlerts();
                  renderQuickActions();
                });
              };

              const readJson = async (response) => {
                const value = await response.json();
                if (!response.ok) {
                  const error = new Error(String(value?.message ?? ('HTTP ' + response.status)));
                  error.vexStatus = response.status;
                  error.vexResponse = value;
                  throw error;
                }
                return value;
              };

              const effectFetch = async (path, init = {}) => {
                const headers = new Headers(init.headers || {});
                if (service.presentationSourceSha) {
                  headers.set(service.presentationSourceHeader, service.presentationSourceSha);
                }
                const response = await fetch(path, { ...init, headers });
                if (response.status === 409) {
                  let value = null;
                  try { value = await response.clone().json(); } catch {}
                  if (value?.code === service.stalePresentationCode) {
                    notify(String(value?.message ?? 'VexLife updated while this page was open.') + ' Reloading automatically…');
                    window.setTimeout(() => window.location.reload(), 2500);
                  }
                }
                return response;
              };

              const readEventStream = async (response, onEvent) => {
                if (!response.ok) return readJson(response);
                if (!response.body) throw new Error('Vex stream did not expose a readable response body.');
                const reader = response.body.getReader();
                const decoder = new TextDecoder();
                let buffer = '';

                const consume = (raw) => {
                  const data = raw
                    .split(/\r?\n/gu)
                    .filter((line) => line.startsWith('data:'))
                    .map((line) => line.slice(5).trimStart())
                    .join('\n');
                  if (!data || data === '[DONE]') return;
                  let event;
                  try { event = JSON.parse(data); }
                  catch { throw new Error('Vex stream emitted invalid JSON.'); }
                  onEvent(event);
                };

                for (;;) {
                  const next = await reader.read();
                  if (next.done) break;
                  buffer += decoder.decode(next.value, { stream: true });
                  for (;;) {
                    const match = buffer.match(/\r?\n\r?\n/u);
                    if (!match || match.index === undefined) break;
                    const raw = buffer.slice(0, match.index);
                    buffer = buffer.slice(match.index + match[0].length);
                    consume(raw);
                  }
                }
                buffer += decoder.decode();
                if (buffer.trim()) consume(buffer);
              };

              const notify = (message) => {
                if (!ui.snackbar) return;
                ui.snackbar.textContent = message;
                ui.snackbar.hidden = false;
                clearTimeout(notify.timer);
                notify.timer = setTimeout(() => { ui.snackbar.hidden = true; }, 4800);
              };

              const THOUGHT_VISIBILITY_KEY = 'vexlife-prod.presentation.thoughts-visible';

              const loadThoughtVisibility = () => {
                try {
                  const stored = localStorage.getItem(THOUGHT_VISIBILITY_KEY);
                  if (stored === 'false') state.thoughtsVisible = false;
                  else if (stored === 'true') state.thoughtsVisible = true;
                } catch {}
              };

              const saveThoughtVisibility = () => {
                try {
                  localStorage.setItem(THOUGHT_VISIBILITY_KEY, String(state.thoughtsVisible));
                } catch {}
              };

              const currentTheme = () =>
                document.documentElement.dataset.theme ||
                (matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');

              const applyTheme = () => {
                try {
                  state.theme = localStorage.getItem('vexlife-prod.presentation.theme');
                } catch {
                  state.theme = null;
                }
                if (state.theme === 'light' || state.theme === 'dark') {
                  document.documentElement.dataset.theme = state.theme;
                } else {
                  delete document.documentElement.dataset.theme;
                }
              };

              const toggleTheme = () => {
                state.theme = currentTheme() === 'dark' ? 'light' : 'dark';
                document.documentElement.dataset.theme = state.theme;
                try {
                  localStorage.setItem('vexlife-prod.presentation.theme', state.theme);
                } catch {}
              };

              // Speech: the Vextreme Spiral FAB's Read Aloud, ported and extended. When Speech is on, Vex
              // is read aloud as it streams, and its thoughts too if the person chooses. It reads the
              // Conversation/raw stream projections, never the DOM. The visual process projection is
              // a sibling observer of that same stream: freezing/reordering visual containers must
              // never become Speech input, drop speech deltas, or feed execution back upstream.
              // Kokoro voices run on this machine after a one-time model download; system voices are
              // limited to those that run on this machine. Either way the words are never sent to a
              // speech service.
              const SPEECH_SETTINGS = 'vexlife-prod.presentation.speech/v1';
              const KOKORO_MODULE_URL = 'https://cdn.jsdelivr.net/npm/kokoro-js@1.2.1/+esm';
              const KOKORO_MODEL_ID = 'onnx-community/Kokoro-82M-v1.0-ONNX';
              const KOKORO_VOICES = [
                { id: 'af_heart', name: 'Heart' },
                { id: 'af_bella', name: 'Bella' },
                { id: 'af_nicole', name: 'Nicole' },
                { id: 'bf_emma', name: 'Emma (UK)' },
                { id: 'am_michael', name: 'Michael' },
                { id: 'am_fenrir', name: 'Fenrir' },
                { id: 'am_puck', name: 'Puck' },
                { id: 'bm_george', name: 'George (UK)' }
              ];
              const speech = {
                systemSupported: 'speechSynthesis' in window && 'SpeechSynthesisUtterance' in window,
                open: false,
                choosing: false,
                queue: [],
                current: null,
                paused: false,
                generation: 0,
                stream: { reply: 0, thought: 0 },
                kokoro: { worker: null, state: 'idle', jobs: new Map(), nextId: 1, cache: new Map(), context: null, source: null },
                settings: { enabled: false, voice: 'kokoro:af_heart', readThoughts: false, rate: 1 }
              };

              const loadSpeechSettings = () => {
                try {
                  const saved = JSON.parse(localStorage.getItem(SPEECH_SETTINGS) || '{}');
                  speech.settings.enabled = saved.enabled === true;
                  speech.settings.readThoughts = saved.readThoughts === true;
                  if (typeof saved.voice === 'string' && saved.voice) speech.settings.voice = saved.voice;
                  const rate = Number(saved.rate);
                  if (Number.isFinite(rate)) speech.settings.rate = Math.min(1.6, Math.max(0.7, rate));
                } catch {}
              };

              const saveSpeechSettings = () => {
                try {
                  localStorage.setItem(SPEECH_SETTINGS, JSON.stringify(speech.settings));
                } catch {}
              };

              const speechLanguage = () =>
                String(state.settings?.replyLanguage ?? document.documentElement.lang ?? 'en');

              const speechVoicePrefixes = () => {
                const language = speechLanguage();
                if (language === 'zh-Hans') return ['zh-cn', 'zh-sg', 'zh-hans', 'zh'];
                if (language === 'zh-Hant') return ['zh-tw', 'zh-hk', 'zh-mo', 'zh-hant', 'zh'];
                const normalized = language.toLowerCase();
                return [normalized, normalized.slice(0, 2)];
              };

              const localVoices = () => {
                if (!speech.systemSupported) return [];
                const prefixes = speechVoicePrefixes();
                const candidates = window.speechSynthesis.getVoices()
                  .filter((voice) => voice.localService)
                  .map((voice) => {
                    const language = String(voice.lang || '').toLowerCase();
                    const preference = prefixes.findIndex((prefix) => language.startsWith(prefix));
                    return { voice, preference };
                  })
                  .filter((entry) => entry.preference >= 0);
                const bestPreference = Math.min(...candidates.map((entry) => entry.preference));
                return Number.isFinite(bestPreference)
                  ? candidates
                      .filter((entry) => entry.preference === bestPreference)
                      .map((entry) => entry.voice)
                  : [];
              };

              // Kokoro voices are English; other languages use this machine's own voices.
              const voiceChoices = () => {
                const natural = speechLanguage() === 'en'
                  ? KOKORO_VOICES.map((voice) => ({ value: 'kokoro:' + voice.id, label: voice.name, group: 'natural' }))
                  : [];
                const system = localVoices().map((voice) => ({ value: 'system:' + voice.voiceURI, label: voice.name, group: 'system' }));
                return natural.concat(system);
              };

              const chosenVoice = () => {
                const choices = voiceChoices();
                return choices.find((choice) => choice.value === speech.settings.voice) ?? choices[0] ?? null;
              };

              const speechText = (text) => String(text ?? '')
                .replace(/[*_#>~|]+/gu, ' ')
                .replace(/\[([^\]]+)\]\([^)]*\)/gu, (match, label) => label)
                .replace(/\s+/gu, ' ')
                .trim();

              const speechChunks = (text) => {
                const sentences = String(text).match(/[^.!?]+[.!?]*\s*/gu) || [];
                const chunks = [];
                let current = '';
                for (const sentence of sentences) {
                  if (current && (current + sentence).length > 300) {
                    chunks.push(current.trim());
                    current = '';
                  }
                  current += sentence;
                }
                if (current.trim()) chunks.push(current.trim());
                return chunks;
              };

              // Runs inside a module Worker so the page stays responsive while Kokoro loads and speaks.
              function kokoroWorkerMain(moduleUrl, modelId) {
                let engine = null;
                let preparing = null;
                let generation = 0;
                let active = false;
                const queue = [];
                const options = async () => {
                  try {
                    if (self.navigator && self.navigator.gpu) {
                      const adapter = await self.navigator.gpu.requestAdapter();
                      if (adapter) return { device: 'webgpu', dtype: 'fp32' };
                    }
                  } catch (error) {}
                  return { device: 'wasm', dtype: 'q8' };
                };
                const ready = () => {
                  if (engine) return Promise.resolve(engine);
                  if (preparing) return preparing;
                  self.postMessage({ type: 'preparing' });
                  preparing = import(moduleUrl).then(async (module) => {
                    const chosen = await options();
                    try {
                      engine = await module.KokoroTTS.from_pretrained(modelId, chosen);
                    } catch (error) {
                      if (chosen.device !== 'webgpu') throw error;
                      engine = await module.KokoroTTS.from_pretrained(modelId, { device: 'wasm', dtype: 'q8' });
                    }
                    self.postMessage({ type: 'ready' });
                    return engine;
                  }).catch((error) => {
                    preparing = null;
                    throw error;
                  });
                  return preparing;
                };
                const drain = async () => {
                  if (active) return;
                  active = true;
                  try {
                    while (queue.length) {
                      const job = queue.shift();
                      if (job.generation !== generation) continue;
                      try {
                        const tts = await ready();
                        if (job.generation !== generation) continue;
                        const raw = await tts.generate(job.text, { voice: job.voice, speed: 1 });
                        const samples = raw && raw.audio;
                        if (!samples || !samples.length) throw new Error('KOKORO_AUDIO_EMPTY');
                        self.postMessage({ type: 'audio', id: job.id, sampleRate: raw.sampling_rate, samples: samples.buffer }, [samples.buffer]);
                      } catch (error) {
                        self.postMessage({ type: 'error', id: job.id, message: String((error && error.message) || error) });
                      }
                    }
                  } finally {
                    active = false;
                  }
                };
                self.onmessage = (event) => {
                  const value = event.data || {};
                  if (value.type === 'cancel') {
                    generation = value.generation;
                    queue.length = 0;
                    return;
                  }
                  if (value.type === 'prepare') {
                    ready().catch((error) => self.postMessage({ type: 'error', id: null, message: String((error && error.message) || error) }));
                    return;
                  }
                  if (value.type === 'generate') {
                    generation = value.generation;
                    queue.push(value);
                    void drain();
                  }
                };
              }

              const kokoroWorker = () => {
                const kokoro = speech.kokoro;
                if (kokoro.worker) return kokoro.worker;
                const source = '(' + kokoroWorkerMain.toString() + ')(' + JSON.stringify(KOKORO_MODULE_URL) + ', ' + JSON.stringify(KOKORO_MODEL_ID) + ');';
                const url = URL.createObjectURL(new Blob([source], { type: 'text/javascript' }));
                kokoro.worker = new Worker(url, { type: 'module' });
                kokoro.state = 'loading';
                kokoro.worker.onmessage = (event) => {
                  const value = event.data || {};
                  if (value.type === 'preparing' || value.type === 'ready') {
                    kokoro.state = value.type === 'ready' ? 'ready' : 'loading';
                    renderSpeech();
                    return;
                  }
                  const job = kokoro.jobs.get(value.id);
                  if (value.type === 'audio' && job) {
                    kokoro.jobs.delete(value.id);
                    job.resolve({ samples: new Float32Array(value.samples), sampleRate: value.sampleRate });
                  } else if (value.type === 'error') {
                    if (job) {
                      kokoro.jobs.delete(value.id);
                      job.reject(new Error(value.message));
                    }
                    if (kokoro.state !== 'ready') kokoro.state = 'failed';
                    renderSpeech();
                  }
                };
                kokoro.worker.onerror = () => {
                  kokoro.state = 'failed';
                  for (const job of kokoro.jobs.values()) job.reject(new Error('The natural voice stopped.'));
                  kokoro.jobs.clear();
                  renderSpeech();
                };
                return kokoro.worker;
              };

              const kokoroAudio = (text, voiceId) => {
                const kokoro = speech.kokoro;
                const key = voiceId + '|' + text;
                if (kokoro.cache.has(key)) return kokoro.cache.get(key);
                const id = kokoro.nextId++;
                const promise = new Promise((resolve, reject) => kokoro.jobs.set(id, { resolve, reject }));
                kokoroWorker().postMessage({ type: 'generate', id, text, voice: voiceId, generation: speech.generation });
                kokoro.cache.set(key, promise);
                if (kokoro.cache.size > 24) kokoro.cache.delete(kokoro.cache.keys().next().value);
                promise.catch(() => kokoro.cache.delete(key));
                return promise;
              };

              const audioContext = () => {
                const kokoro = speech.kokoro;
                if (!kokoro.context) {
                  const Context = window.AudioContext || window.webkitAudioContext;
                  if (!Context) return null;
                  kokoro.context = new Context();
                }
                if (kokoro.context.state === 'suspended' && !speech.paused) kokoro.context.resume().catch(() => {});
                return kokoro.context;
              };

              const speakItem = (item, generation) => new Promise((resolve, reject) => {
                const voice = chosenVoice();
                if (!voice) {
                  reject(new Error('No voice on this Mac can read aloud.'));
                  return;
                }
                if (voice.value.startsWith('kokoro:')) {
                  const voiceId = voice.value.slice('kokoro:'.length);
                  const next = speech.queue[1];
                  kokoroAudio(item.text, voiceId).then(({ samples, sampleRate }) => {
                    if (generation !== speech.generation) {
                      resolve();
                      return;
                    }
                    const context = audioContext();
                    if (!context) {
                      reject(new Error('This browser cannot play the natural voice.'));
                      return;
                    }
                    const buffer = context.createBuffer(1, samples.length, sampleRate);
                    buffer.copyToChannel(samples, 0);
                    const source = context.createBufferSource();
                    source.buffer = buffer;
                    source.playbackRate.value = speech.settings.rate;
                    source.connect(context.destination);
                    speech.kokoro.source = source;
                    source.onended = () => {
                      if (speech.kokoro.source === source) speech.kokoro.source = null;
                      resolve();
                    };
                    source.start();
                    if (next) kokoroAudio(next.text, voiceId).catch(() => {});
                  }, reject);
                  return;
                }
                const system = localVoices().find((candidate) => candidate.voiceURI === voice.value.slice('system:'.length));
                const utterance = new SpeechSynthesisUtterance(item.text);
                if (system) {
                  utterance.voice = system;
                  utterance.lang = system.lang;
                }
                utterance.rate = speech.settings.rate;
                utterance.onend = () => resolve();
                utterance.onerror = (event) => (event.error === 'interrupted' || event.error === 'canceled'
                  ? resolve()
                  : reject(new Error(String(event.error))));
                window.speechSynthesis.speak(utterance);
              });

              const pumpSpeech = () => {
                if (speech.current || speech.paused || !speech.queue.length) {
                  renderSpeech();
                  return;
                }
                const generation = speech.generation;
                speech.current = speech.queue[0];
                renderSpeech();
                speakItem(speech.current, generation).then(() => {
                  if (generation !== speech.generation) return;
                  speech.queue.shift();
                  speech.current = null;
                  pumpSpeech();
                }, () => {
                  if (generation !== speech.generation) return;
                  speech.current = null;
                  const fallback = localVoices()[0];
                  if (chosenVoice()?.group === 'natural' && fallback) {
                    speech.settings.voice = 'system:' + fallback.voiceURI;
                    saveSpeechSettings();
                    renderSpeechVoices();
                    notify("The natural voice couldn't load, so this Mac's voice is reading instead.");
                    pumpSpeech();
                    return;
                  }
                  stopSpeech();
                  notify("Speech stopped because the voice couldn't continue.");
                });
              };

              const enqueueSpeech = (text, eventRef) => {
                for (const chunk of speechChunks(speechText(text))) speech.queue.push({ text: chunk, eventRef });
                pumpSpeech();
              };

              const stopSpeech = () => {
                speech.generation += 1;
                speech.queue = [];
                speech.current = null;
                speech.paused = false;
                speech.choosing = false;
                if (speech.systemSupported) window.speechSynthesis.cancel();
                const kokoro = speech.kokoro;
                if (kokoro.source) {
                  try {
                    kokoro.source.onended = null;
                    kokoro.source.stop();
                  } catch {}
                  kokoro.source = null;
                }
                if (kokoro.context?.state === 'suspended') kokoro.context.resume().catch(() => {});
                if (kokoro.worker) kokoro.worker.postMessage({ type: 'cancel', generation: speech.generation });
                for (const job of kokoro.jobs.values()) job.reject(new Error('cancelled'));
                kokoro.jobs.clear();
                kokoro.cache.clear();
                renderSpeech();
              };

              const pauseOrResumeSpeech = () => {
                if (!speech.current && !speech.queue.length) {
                  startReading(null);
                  return;
                }
                speech.paused = !speech.paused;
                if (speech.systemSupported) {
                  if (speech.paused) window.speechSynthesis.pause();
                  else window.speechSynthesis.resume();
                }
                const context = speech.kokoro.context;
                if (context) {
                  if (speech.paused) context.suspend().catch(() => {});
                  else context.resume().catch(() => {});
                }
                if (!speech.paused) pumpSpeech();
                renderSpeech();
              };

              // Live speech follows the stream sentence by sentence; the tail is spoken on completion.
              const speakFresh = (text, offset, final) => {
                const pending = String(text ?? '').slice(offset);
                if (!pending.trim()) return offset + pending.length;
                const match = final ? null : pending.match(/^[\s\S]*[.!?:;](?=\s)|^[\s\S]*\n/u);
                const end = final ? pending.length : (match ? match[0].length : 0);
                if (end <= 0) return offset;
                enqueueSpeech(pending.slice(0, end), 'live');
                return offset + end;
              };

              const followStreamSpeech = (final) => {
                if (!speech.settings.enabled) return;
                if (speech.settings.readThoughts) {
                  speech.stream.thought = speakFresh(state.streamedReasoning, speech.stream.thought, final || Boolean(state.streamedContent));
                }
                speech.stream.reply = speakFresh(state.streamedContent, speech.stream.reply, final);
              };

              const readablePlan = () => (isPrivate()
                ? (state.privateMode?.exchanges ?? []).flatMap((exchange, index) => [
                    { role: 'human', content: exchange.human, eventRef: 'private.' + (index * 2 + 1) },
                    { role: 'vex', content: exchange.vex, eventRef: 'private.' + (index * 2 + 2) }
                  ])
                : (state.conversation?.messages ?? []))
                .filter((message) => String(message.content ?? '').trim())
                .map((message) => ({ eventRef: String(message.eventRef ?? ''), vex: message.role !== 'human', text: message.content }));

              const startReading = (eventRef) => {
                const plan = readablePlan();
                stopSpeech();
                if (!plan.length) {
                  notify('There is nothing to read yet.');
                  return;
                }
                let index = eventRef ? plan.findIndex((message) => message.eventRef === eventRef) : -1;
                if (index < 0) {
                  const vex = plan.map((message, at) => (message.vex ? at : -1)).filter((at) => at >= 0);
                  index = vex.length ? vex[vex.length - 1] : plan.length - 1;
                }
                audioContext();
                for (const message of plan.slice(index)) enqueueSpeech(message.text, message.eventRef);
              };

              const stepReading = (direction) => {
                const plan = readablePlan();
                const at = plan.findIndex((message) => message.eventRef === speech.current?.eventRef);
                const target = plan[at + direction];
                if (at < 0 || !target) return;
                startReading(target.eventRef);
              };

              const markReading = () => {
                if (!ui.timeline) return;
                const current = speech.current?.eventRef;
                for (const item of ui.timeline.querySelectorAll('.message')) {
                  item.classList.toggle('reading', Boolean(current) && current !== 'live' && item.dataset.eventRef === current);
                }
                ui.timeline.dataset.choosing = String(speech.choosing);
              };

              const renderSpeechVoices = () => {
                if (!ui.speechVoice) return;
                const choices = voiceChoices();
                const groups = [
                  ['natural', 'Natural voices · downloaded once, made on this Mac'],
                  ['system', "This Mac's voices"]
                ].map(([group, label]) => {
                  const members = choices.filter((choice) => choice.group === group);
                  if (!members.length) return null;
                  const node = document.createElement('optgroup');
                  node.label = label;
                  for (const choice of members) {
                    const option = document.createElement('option');
                    option.value = choice.value;
                    option.textContent = choice.label;
                    node.append(option);
                  }
                  return node;
                }).filter(Boolean);
                ui.speechVoice.replaceChildren(...groups);
                const chosen = chosenVoice();
                if (chosen) ui.speechVoice.value = chosen.value;
                ui.speechVoice.disabled = !choices.length;
              };

              const renderSpeech = () => {
                if (!ui.speech) return;
                ui.speech.hidden = !speech.open;
                const playing = Boolean(speech.current) && !speech.paused;
                const busy = Boolean(speech.current) || speech.queue.length > 0;
                ui.speech.dataset.mode = playing ? 'playing' : busy ? 'paused' : 'idle';
                const action = ui.quickActions?.querySelector('.quick-action[data-action="speech"]');
                if (action) {
                  action.setAttribute('aria-expanded', String(speech.open));
                  action.setAttribute('aria-pressed', String(speech.settings.enabled));
                  action.dataset.active = String(speech.settings.enabled);
                }
                ui.speechSwitch.setAttribute('aria-checked', String(speech.settings.enabled));
                ui.speechThoughts.checked = speech.settings.readThoughts;
                ui.speechRate.value = String(speech.settings.rate);
                ui.speechRateValue.textContent = speech.settings.rate.toFixed(1) + '×';
                ui.speechPlay.setAttribute('aria-label', playing ? 'Pause' : 'Play');
                ui.speechPlay.title = playing ? 'Pause' : 'Play';
                ui.speechChoose.setAttribute('aria-pressed', String(speech.choosing));
                const usable = voiceChoices().length > 0;
                ui.speechSwitch.disabled = !usable;
                ui.speechPlay.disabled = !usable;
                ui.speechChoose.disabled = !usable;
                ui.speechStop.disabled = !busy;
                ui.speechPrevious.disabled = !busy || !speech.current || speech.current.eventRef === 'live';
                ui.speechNext.disabled = ui.speechPrevious.disabled;
                const natural = chosenVoice()?.group === 'natural';
                const kokoroState = speech.kokoro.state;
                ui.speechStatus.textContent = !usable
                  ? 'No voice on this Mac is available for speech.'
                  : speech.choosing
                    ? 'Choose a message to start from. Esc cancels.'
                    : natural && kokoroState === 'loading'
                      ? 'Getting the natural voice ready. The first time, it downloads once (about 90 MB); speech is made on this Mac.'
                      : natural && kokoroState === 'failed'
                        ? "The natural voice couldn't load; this Mac's voices still work."
                        : busy
                          ? (playing ? 'Reading Vex…' : 'Paused.')
                          : speech.settings.enabled
                            ? (speech.settings.readThoughts ? "Vex's thoughts and replies will be read aloud." : "Vex's replies will be read aloud.")
                            : 'Speech is off. You can still choose where to start reading.';
                markReading();
              };

              const LANGUAGE_FLAGS = Object.freeze({
                en: '🇺🇸',
                ja: '🇯🇵',
                es: '🇪🇸',
                fr: '🇫🇷',
                de: '🇩🇪',
                pt: '🇧🇷',
                zh: '🇨🇳',
                ko: '🇰🇷',
                ar: '🇸🇦',
                hi: '🇮🇳'
              });

              const presentationLanguageCode = (code) => {
                const value = String(code ?? '').trim();
                if (!value) return '';
                if (value === 'zh-Hans' || value === 'zh-Hant') return 'zh';
                return value.split('-')[0].toLowerCase();
              };

              const languageFlagFor = (code) =>
                LANGUAGE_FLAGS[presentationLanguageCode(code)] ?? '🏳';

              // This is intentionally a projection seam, not selection ownership. The application-
              // language owner is now current (VexLifeLanguageFamily), so the Swirl shows the
              // language VexLife's screens are shown in; the title names both choices.
              const currentSwirlLanguage = () => {
                const code = state.settings?.screenLanguageShown ?? state.settings?.replyLanguage ?? 'en';
                const name =
                  (state.settings?.screenLanguages ?? [])
                    .find((language) => language.code === code)
                    ?.name ?? code;
                return Object.freeze({
                  code,
                  name,
                  meaning: 'VexLife screen language'
                });
              };

              // Two Home settings, each with its own responsibility (VexLifePersonSettings): the
              // language Vex replies in (told on every message; Speech and dictation follow it) and
              // the language VexLife's own screens are written in. Choosing one never moves the other.
              const choiceName = (languages, code) =>
                (languages ?? []).find((language) => language.code === code)?.name ?? code;
              const fillChoice = (select, options, current) => {
                if (!select) return;
                select.replaceChildren(...options.map(([value, label]) => {
                  const option = document.createElement('option');
                  option.value = value;
                  option.textContent = label;
                  option.selected = value === current;
                  return option;
                }));
              };
              const renderLanguage = () => {
                if (!ui.languagePanel) return;
                ui.languagePanel.hidden = !state.languageOpen;
                const settings = state.settings ?? {};
                const reply = settings.replyLanguage ?? 'en';
                const replyName = choiceName(settings.languages, reply);
                const shownName = choiceName(settings.screenLanguages, settings.screenLanguageShown ?? 'en');
                const action = ui.quickActions?.querySelector('.quick-action[data-action="language"]');
                if (action) {
                  const swirlLanguage = currentSwirlLanguage();
                  action.classList.add('quick-action-glyph');
                  action.textContent = languageFlagFor(swirlLanguage.code);
                  action.setAttribute('aria-expanded', String(state.languageOpen));
                  action.title =
                    t('workspace.label.reply-language') + ' ' + replyName + ' · ' +
                    t('workspace.label.screen-language') + ' ' + shownName;
                  action.setAttribute('aria-label', action.title);
                }
                fillChoice(ui.replyLanguage, (settings.languages ?? []).map((language) => [language.code, language.name]), reply);
                const follow = ${jsString(VexLifePersonSettings.FollowReplyLanguage)};
                fillChoice(
                  ui.screenLanguage,
                  [
                    [follow, t('workspace.label.screen-language-follow').replace('{language}', shownName)],
                    ...(settings.screenLanguages ?? []).map((language) => [language.code, language.name])
                  ],
                  settings.screenLanguage ?? follow
                );
              };

              const loadSettings = async () => {
                const response = await fetch(service.settingsPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.settings = await readJson(response);
                renderLanguage();
                renderSpeechVoices();
                renderSpeech();
                observeVoiceInputCapability();
                renderVoiceInput();
                return state.settings;
              };

              // kind is 'replyLanguage' or 'screenLanguage': the update names exactly one choice.
              const chooseLanguage = async (kind, code) => {
                try {
                  const response = await effectFetch(service.settingsPath, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json', accept: 'application/json' },
                    body: JSON.stringify({ [kind]: code })
                  });
                  state.settings = await readJson(response);
                  // Screens follow the reply language until one is chosen, so either change may move them.
                  await loadWorkspaceStrings().catch(() => applyStaticText());
                  renderLanguage();
                  if (kind === 'replyLanguage') {
                    renderSpeechVoices();
                    renderSpeech();
                    await loadLocalAsr().catch(() => {
                      observeVoiceInputCapability();
                      renderVoiceInput();
                    });
                    renderVexContext();
                    notify(t('workspace.status.reply-language-changed').replace('{language}', choiceName(state.settings.languages, code)));
                  } else {
                    notify(t('workspace.status.screen-language-changed').replace('{language}', choiceName(state.settings.screenLanguages, state.settings.screenLanguageShown)));
                  }
                } catch (error) {
                  renderLanguage();
                  notify(error instanceof Error ? error.message : t('workspace.status.language-change-failed'));
                }
              };

              const releaseAlertControl = async (entry, controlRef) => {
                try {
                  const response = await effectFetch(service.releaseAlertsControlPath, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json', accept: 'application/json' },
                    body: JSON.stringify({
                      controlRef,
                      release: entry.release
                    })
                  });
                  const result = await readJson(response);
                  state.releaseAlerts = result.releaseAlerts;
                  if (result.pageToOpenOrNull) {
                    state.releaseAlertsOpen = false;
                    renderReleaseAlerts();
                    renderQuickActions();
                    navigate(result.pageToOpenOrNull);
                    return;
                  }
                  renderReleaseAlerts();
                  renderQuickActions();
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'The release action could not be completed.');
                }
              };

              const renderReleaseAlerts = () => {
                if (!ui.releaseAlertsPanel) return;
                const display = state.releaseAlerts?.display;
                ui.releaseAlertsPanel.hidden = !state.releaseAlertsOpen;
                if (!display) {
                  ui.releaseAlertsTitle.textContent = t(${jsString(VexLifeReleaseInboxFamily.KeyHeading)});
                  ui.releaseAlertsEmpty.textContent = t(${jsString(VexLifeReleaseInboxFamily.KeyUnavailable)});
                  ui.releaseAlertsEmpty.hidden = false;
                  ui.releaseAlertsList.replaceChildren();
                  return;
                }
                ui.releaseAlertsTitle.textContent = resolveText(display.heading);
                ui.releaseAlertsEmpty.textContent = resolveText(display.empty);
                const items = (display.items ?? []).map((entry) => {
                  const item = document.createElement('li');
                  item.className = 'release-alerts-item';
                  item.dataset.unread = String(entry.unread === true);

                  const header = document.createElement('div');
                  header.className = 'release-alerts-item-head';

                  const release = document.createElement('strong');
                  release.className = 'release-alerts-version';
                  release.textContent = String(entry.releaseIdentifier ?? '');

                  const status = document.createElement('span');
                  status.className = 'release-alerts-status';
                  status.textContent = resolveText(entry.status);

                  const date = document.createElement('time');
                  date.className = 'release-alerts-date';
                  const instant = Number(entry.publishedAtEpochMillis);
                  if (Number.isFinite(instant) && instant >= 0) {
                    date.dateTime = new Date(instant).toISOString();
                    date.textContent = new Date(instant).toLocaleDateString(
                      document.documentElement.lang || undefined
                    );
                  }

                  header.append(release, status, date);

                  const controls = document.createElement('div');
                  controls.className = 'release-alerts-controls';

                  const view = document.createElement('button');
                  view.type = 'button';
                  view.className = 'release-alerts-view';
                  view.textContent = resolveText(entry.viewLabel);
                  view.addEventListener('click', () => {
                    void releaseAlertControl(entry, entry.viewControlRef);
                  });

                  const dismiss = document.createElement('button');
                  dismiss.type = 'button';
                  dismiss.className = 'release-alerts-dismiss';
                  dismiss.textContent = resolveText(entry.dismissLabel);
                  dismiss.addEventListener('click', () => {
                    void releaseAlertControl(entry, entry.dismissControlRef);
                  });

                  controls.append(view, dismiss);
                  item.append(header, controls);
                  return item;
                });
                ui.releaseAlertsEmpty.hidden = items.length > 0;
                ui.releaseAlertsList.replaceChildren(...items);
              };

              const loadReleaseAlerts = async () => {
                const response = await fetch(service.releaseAlertsPath, {
                  headers: { accept: 'application/json' },
                  cache: 'no-store'
                });
                state.releaseAlerts = await readJson(response);
                renderReleaseAlerts();
                renderQuickActions();
                return state.releaseAlerts;
              };

              const renderQuickActions = () => {
                if (!ui.quickActions) return;

                const unreadReleaseAlerts =
                  Number(state.releaseAlerts?.display?.unreadCount ?? 0) > 0;

                const trigger = byId('quick-actions-trigger');
                if (trigger) {
                  trigger.setAttribute('aria-expanded', String(state.quickActionsOpen));
                  trigger.setAttribute(
                    'aria-label',
                    state.quickActionsOpen ? 'Close quick actions' : 'Open quick actions'
                  );
                  trigger.dataset.attention =
                    String(unreadReleaseAlerts && !state.quickActionsOpen);
                }
                ui.quickActions.dataset.open = String(state.quickActionsOpen);

                const theme = byId('theme-quick-action');
                if (theme) {
                  theme.textContent = currentTheme() === 'dark' ? '🌙' : '☀️';
                  theme.title = 'Toggle theme';
                  theme.setAttribute('aria-label', 'Toggle dark / light theme');
                }

                const speechButton = byId('speech-source');
                if (speechButton) {
                  speechButton.dataset.bound = 'true';
                  speechButton.setAttribute('aria-expanded', String(speech.open));
                  speechButton.setAttribute('aria-pressed', String(speech.settings.enabled));
                  speechButton.dataset.active = String(speech.settings.enabled);
                }

                const languageButton = byId('language-source');
                if (languageButton) {
                  languageButton.dataset.bound = 'true';
                  languageButton.setAttribute('aria-expanded', String(state.languageOpen));
                }

                const visionButton = byId('vision-quick-access-source');
                if (visionButton) {
                  const display = state.vision?.display;
                  const bound = Boolean(display?.primaryControlRef);
                  const label = String(display?.label ?? 'Vision · Held');
                  visionButton.dataset.bound = String(bound);
                  visionButton.dataset.indicator = String(display?.indicatorRef ?? 'None');
                  visionButton.disabled = !bound || state.visionInFlight;
                  visionButton.setAttribute('aria-label', label);
                  visionButton.title = state.visionInFlight
                    ? 'Vision · Working'
                    : label + (display?.primaryLabel ? ' — ' + display.primaryLabel : '');
                }

                const releaseAlertsButton = byId('release-alerts-source');
                if (releaseAlertsButton) {
                  const label = t(${jsString(VexLifeReleaseInboxFamily.KeyQuickAction)});
                  releaseAlertsButton.dataset.bound = 'true';
                  releaseAlertsButton.dataset.attention =
                    String(unreadReleaseAlerts && state.quickActionsOpen);
                  releaseAlertsButton.setAttribute(
                    'aria-expanded',
                    String(state.releaseAlertsOpen)
                  );
                  releaseAlertsButton.setAttribute('aria-label', label);
                  releaseAlertsButton.title = label;
                }

                renderSpeech();
                renderLanguage();
              };

              // Vision copy, state and the one lawful Control come from the Product (VexLifeVisionFamily);
              // this renderer only shows them and sends the Control back for fresh resolution.
              const renderMemory = () => {
                const display = state.memory?.display;
                ui.vexMemoryLabel.textContent = String(display?.label ?? 'Memory · Checking');
                ui.vexMemoryDetail.textContent = String(display?.detail ?? "VexLife hasn't read Vex's Memory yet.");
                root.dataset.vexMemoryState = String(state.memory?.state ?? '');
              };

              const renderVision = () => {
                if (!ui.visionControl) return;
                const display = state.vision?.display;
                const label = String(display?.label ?? 'Vision · Held');
                const stateNode = ui.visionControl.querySelector('.vision-state');
                if (stateNode) stateNode.textContent = state.visionInFlight ? 'Working' : label.replace(/^Vision · /, '');
                ui.visionControl.dataset.indicator = String(display?.indicatorRef ?? 'None');
                ui.visionControl.disabled = !display?.primaryControlRef || state.visionInFlight;
                ui.visionControl.setAttribute('aria-disabled', String(ui.visionControl.disabled));
                ui.visionControl.title = state.visionInFlight
                  ? 'Vision · Working'
                  : label + (display?.primaryLabel ? ' — ' + display.primaryLabel : '');
                if (ui.visionControlStatus) {
                  ui.visionControlStatus.textContent = String(display?.detail ?? 'Vision availability is checking.');
                }
                renderQuickActions();
              };

              const setCompanionStatus = (stateName, text, model = '') => {
                if (!ui.companionStatus) return;
                ui.companionStatus.dataset.state = stateName;
                ui.companionStatusText.textContent = text;
                ui.companionStatusModel.textContent = model;
              };

              const contextFrameUrlFor = (pageUrl) => {
                const page = new URL(pageUrl);
                if (page.protocol !== 'https:' || page.port || !browser.admittedHosts.includes(page.hostname)) return null;
                return 'http://' + page.hostname + browser.frameHostSuffix + ':' + browser.frameHostPort + page.pathname + page.search + page.hash;
              };

              const admitPageAddress = (value) => {
                let text = String(value ?? '').trim();
                if (!text) throw new Error('Type a web address.');
                if (!/^[a-z][a-z0-9+.-]*:/iu.test(text)) text = 'https://' + text;
                const page = new URL(text);
                if (!browser.humanPageSchemes.includes(page.protocol)) {
                  throw new Error('Only http and https web pages open here.');
                }
                if (page.username || page.password) {
                  throw new Error('Addresses with embedded credentials do not open here.');
                }
                const host = page.hostname.toLowerCase();
                if (
                  browser.rejectedHumanPageHosts.includes(host) ||
                  host.endsWith(browser.rejectedHumanPageHostSuffix)
                ) {
                  throw new Error('The embedded Browser does not navigate to the local Product service.');
                }
                return page.href;
              };

              const normalizeContext = (value) => {
                if (!value || value.type !== browser.messageType || typeof value.pageUrl !== 'string' || typeof value.text !== 'string') return null;
                try {
                  const page = new URL(value.pageUrl);
                  if (page.protocol !== 'https:' || page.port || !browser.admittedHosts.includes(page.hostname)) return null;
                } catch {
                  return null;
                }
                const text = value.text.slice(0, browser.contextCharacterLimit).replace(/\r\n?/gu, '\n').trim();
                if (!text) return null;
                const title = typeof value.title === 'string' && value.title.length > 0
                  ? value.title.slice(0, browser.maxTitleCharacters)
                  : null;
                return Object.freeze({ pageUrl: value.pageUrl, title, text, capture: browser.captureRendered });
              };

              const renderFrameStatus = (text = '', stateName = '') => {
                if (!ui.frameStatus) return;
                ui.frameStatus.textContent = text;
                ui.frameStatus.dataset.frame = stateName;
                ui.frameStatus.hidden = !text;
              };

              // Product-owned navigation is the only place today where the exact identity of the
              // visible page is *earned*: the Product chose the address, so it knows it. Any context
              // captured for the previous page is invalidated because it described another page.
              const navigate = (value) => {
                const pageUrl = admitPageAddress(value);
                const contextFrameUrl = contextFrameUrlFor(pageUrl);

                if (state.browserContext) reportFact('BROWSER_CONTEXT_INVALIDATED', { by: 'navigation' });
                reportFact('BROWSER_PAGE_IDENTITY_OBSERVED', { ...pageRefs(pageUrl), via: 'product-navigation', eligible: String(Boolean(contextFrameUrl)) });
                state.browserContext = null;
                state.includePage = false;
                state.humanPageUrl = pageUrl;
                state.humanFrameLoadExpected = true;
                state.contextTargetPageUrl = contextFrameUrl ? pageUrl : null;
                state.contextLoading = false;

                ui.query.value = pageUrl;
                ui.query.placeholder = 'Type a web address';
                ui.pageFrame.src = pageUrl;
                ui.contextFrame.removeAttribute('src');
                ui.viewportEmpty.hidden = true;
                for (const member of ui.family.querySelectorAll('.family-member')) {
                  if (pageUrl.startsWith(member.dataset.familyScope)) member.setAttribute('aria-current', 'page');
                  else member.removeAttribute('aria-current');
                }
                renderFrameStatus(
                  contextFrameUrl
                    ? 'Opening page directly · Vex page context is available on demand.'
                    : 'Opening page directly · Vex page context is not available for this site yet.',
                  'Loading'
                );
                renderComposer();
              };

              const SCROLL_END_EPSILON = 2;

              const isElementAtBottom = (element) =>
                element.scrollHeight - element.scrollTop - element.clientHeight <= SCROLL_END_EPSILON;

              const reasoningViewport = (key, initialFollowTail = false) => {
                let viewport = state.reasoningViewports.get(key);
                if (!viewport) {
                  viewport = { followTail: initialFollowTail, scrollTop: 0 };
                  state.reasoningViewports.set(key, viewport);
                }
                return viewport;
              };

              const bindReasoningViewport = (element, key, initialFollowTail = false) => {
                element.dataset.scrollKey = key;
                reasoningViewport(key, initialFollowTail);
                element.addEventListener('scroll', () => {
                  const viewport = reasoningViewport(key, initialFollowTail);
                  viewport.scrollTop = element.scrollTop;
                  viewport.followTail = isElementAtBottom(element);
                });
              };

              const applyReasoningViewport = (
                element,
                key,
                initialFollowTail = false,
                restoreReadingPosition = true
              ) => {
                const viewport = reasoningViewport(key, initialFollowTail);
                if (viewport.followTail) {
                  element.scrollTop = element.scrollHeight;
                } else if (restoreReadingPosition) {
                  const maxScrollTop = Math.max(0, element.scrollHeight - element.clientHeight);
                  element.scrollTop = Math.min(viewport.scrollTop, maxScrollTop);
                }
                viewport.scrollTop = element.scrollTop;
              };

              const restoreReasoningViewports = () => {
                ui.timeline.querySelectorAll('.reasoning[data-scroll-key]').forEach((element) => {
                  const key = String(element.dataset.scrollKey ?? '');
                  const liveTail = element.dataset.liveTail === 'true';
                  applyReasoningViewport(element, key, liveTail);
                });
              };

              const resetLiveProcessViewports = () => {
                for (const key of state.reasoningViewports.keys()) {
                  if (key === 'live' || key.startsWith('live.process.')) {
                    state.reasoningViewports.delete(key);
                  }
                }
              };

              const applyTimelineViewport = (restoreReadingPosition = true) => {
                if (state.timelineFollowTail) {
                  ui.timeline.scrollTop = ui.timeline.scrollHeight;
                } else if (restoreReadingPosition) {
                  const maxScrollTop = Math.max(0, ui.timeline.scrollHeight - ui.timeline.clientHeight);
                  ui.timeline.scrollTop = Math.min(state.timelineScrollTop, maxScrollTop);
                }
                state.timelineScrollTop = ui.timeline.scrollTop;
                ui.jumpLatest.hidden = state.timelineFollowTail;
              };

              const nextSequence = () => {
                const messages = state.conversation?.messages ?? [];
                return messages.reduce((max, message) => Math.max(max, Number(message.sequence) || 0), -1) + 1;
              };

              const thoughtMark = () => {
                const mark = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                mark.setAttribute('class', 'thought-mark');
                mark.setAttribute('viewBox', '0 0 16 16');
                mark.innerHTML = '<path d="M8 1.8 14.2 8 8 14.2 1.8 8z"/><path d="M8 5 11 8 8 11 5 8z"/>';
                return mark;
              };

              // Reach in the person's words: the bridge names handle/action/target, the words are keyed.
              const reachText = (use) => {
                const target = String(use?.target ?? '')
                  .replace(/^home:/u, '')
                  .replace(/^search: /u, '')
                  .replace(/^memory: /u, '');
                const key =
                  use?.handle === 'web' && use?.action === 'search'
                    ? 'workspace.status.reach-web-search'
                    : use?.handle === 'web'
                      ? 'workspace.status.reach-web-fetch'
                      : use?.handle === 'github'
                        ? 'workspace.status.reach-github'
                        : use?.handle === 'home'
                          ? 'workspace.status.reach-home'
                          : use?.handle === 'memory'
                            ? 'workspace.status.reach-memory'
                            : 'workspace.status.reach-atlas';
                const words = t(key).replace('{target}', target);
                if (!use?.state || use.state === 'RETURNED') return words;
                const settled = String(use.state).toLowerCase().replace(/_/gu, ' ');
                return words + ' ' + t('workspace.label.reach-state').replace('{state}', settled);
              };

              const reachList = (uses) => {
                const list = document.createElement('ul');
                list.className = 'reaches live-reaches';
                for (const use of uses) {
                  const line = document.createElement('li');
                  line.textContent = reachText(use);
                  list.append(line);
                }
                return list;
              };

              const cloneReach = (use) => ({
                handle: String(use?.handle ?? ''),
                action: String(use?.action ?? ''),
                target: use?.target == null ? null : String(use.target),
                state: use?.state == null ? null : String(use.state)
              });

              const freezeActiveProcessThought = () => {
                const entry = state.liveProcessEntries[state.liveProcessEntries.length - 1];
                if (entry?.kind === 'thought' && entry.active) entry.active = false;
              };

              const appendProcessReasoning = (delta) => {
                let entry = state.liveProcessEntries[state.liveProcessEntries.length - 1];
                if (entry?.kind !== 'thought' || !entry.active) {
                  entry = { kind: 'thought', text: '', active: true };
                  state.liveProcessEntries.push(entry);
                }
                entry.text += String(delta ?? '');
              };

              const projectProcessReach = (use) => {
                freezeActiveProcessThought();
                const next = cloneReach(use);
                if (next.state) {
                  for (let index = state.liveProcessEntries.length - 1; index >= 0; index -= 1) {
                    const entry = state.liveProcessEntries[index];
                    if (
                      entry?.kind === 'reach' &&
                      !entry.use.state &&
                      entry.use.handle === next.handle &&
                      entry.use.action === next.action &&
                      entry.use.target === next.target
                    ) {
                      entry.use = next;
                      return;
                    }
                  }
                }
                state.liveProcessEntries.push({ kind: 'reach', use: next });
              };

              const freezeLiveProcess = () => {
                freezeActiveProcessThought();
              };

              const snapshotLiveProcess = () =>
                state.liveProcessEntries.map((entry) =>
                  entry.kind === 'thought'
                    ? { kind: 'thought', text: entry.text, active: false }
                    : { kind: 'reach', use: cloneReach(entry.use) });

              const clearLiveProcessState = () => {
                state.streamPhase = null;
                state.streamedContent = '';
                state.streamedReasoning = '';
                state.liveReaches = [];
                state.liveProcessEntries = [];
                state.liveProcessExpanded = true;
                state.liveProcessManuallyToggled = false;
                resetLiveProcessViewports();
              };

              const failureRouteLabel = (route) =>
                route === 'RESUME_LAST_VALID_HEAD_THEN_FORM_NEW_SEND_ATTEMPT'
                  ? 'Try again from the last completed turn with a new Send.'
                  : route === 'FORM_NEW_SEND_ATTEMPT_AND_RETRY'
                    ? 'Try again as a new Send.'
                    : route
                      ? 'Next: ' + String(route).replaceAll('_', ' ').toLowerCase() + '.'
                      : 'This Send stopped before Vex completed a reply.';

              const processEntryNode = (entry, viewportKey, live) => {
                if (entry.kind === 'thought') {
                  if (!state.thoughtsVisible || !String(entry.text ?? '').trim()) return null;
                  const box = document.createElement('div');
                  box.className = 'reasoning process-thought' + (live && entry.active ? ' live-reasoning' : '');
                  box.dataset.liveTail = String(Boolean(live && entry.active));
                  bindReasoningViewport(box, viewportKey, live && entry.active);
                  // The reasoning viewport remains the scroll owner. TextPresentation lives inside
                  // that mounted viewport so Markdown readability can evolve without replacing the
                  // scrollbar/pointer surface or changing the raw reasoning owned by the stream.
                  box.append(textPresentationBody(entry.text));
                  return box;
                }

                const use = entry.use;
                const line = document.createElement('div');
                line.className = 'process-action';
                line.dataset.state = use?.state ? String(use.state) : 'ACTIVE';
                const mark = document.createElement('span');
                mark.className = 'process-action-mark';
                mark.setAttribute('aria-hidden', 'true');
                mark.textContent = !use?.state ? '↳' : use.state === 'RETURNED' ? '✓' : '!';
                const text = document.createElement('span');
                text.className = 'process-action-text';
                text.textContent = reachText(use);
                line.append(mark, text);
                return line;
              };

              const processHistoryNode = (entries, keyPrefix, live) => {
                const history = document.createElement('div');
                history.className = 'process-history';
                entries.forEach((entry, index) => {
                  const node = processEntryNode(entry, keyPrefix + '.' + String(index), live);
                  if (node) history.append(node);
                });
                return history.childElementCount ? history : null;
              };

              const syncLiveProcessEntry = (node, entry, viewportKey) => {
                if (entry.kind === 'thought') {
                  const active = Boolean(entry.active);
                  node.classList.toggle('live-reasoning', active);
                  node.dataset.liveTail = String(active);
                  let body = node.querySelector('.text-presentation');
                  if (!body) {
                    body = textPresentationBody(entry.text);
                    node.append(body);
                  }
                  // Regression boundary: T004 established that human scrolling owns follow state.
                  // Keep this exact reasoning viewport mounted. Only its inner TextPresentation
                  // changes. If the human is following the tail, restore the tail after geometry
                  // changes; if the human scrolled away, leave scrollTop untouched.
                  if (active) {
                    scheduleLiveTextPresentation(
                      body,
                      entry.text,
                      () => {
                        applyReasoningViewport(node, viewportKey, true, false);
                        applyTimelineViewport(false);
                      }
                    );
                  } else {
                    presentText(body, entry.text);
                  }
                  return;
                }

                const use = entry.use;
                node.dataset.state = use?.state ? String(use.state) : 'ACTIVE';
                const mark = node.querySelector('.process-action-mark');
                if (mark) mark.textContent = !use?.state ? '↳' : use.state === 'RETURNED' ? '✓' : '!';
                const text = node.querySelector('.process-action-text');
                if (text) text.textContent = reachText(use);
              };

              const syncLiveProcessHistory = (history) => {
                const visible = new Set();
                state.liveProcessEntries.forEach((entry, index) => {
                  if (entry.kind === 'thought' && (!state.thoughtsVisible || !String(entry.text ?? '').trim())) {
                    return;
                  }
                  const key = String(index);
                  const viewportKey = 'live.process.' + key;
                  let node = history.querySelector('[data-process-entry="' + key + '"]');
                  if (!node) {
                    node = processEntryNode(entry, viewportKey, true);
                    if (!node) return;
                    node.dataset.processEntry = key;
                    history.append(node);
                  }
                  syncLiveProcessEntry(node, entry, viewportKey);
                  visible.add(key);
                });

                // Removal is only for a human visibility-control change. Normal streaming is
                // append/update-only, so older process containers retain stable DOM identity.
                history.querySelectorAll('[data-process-entry]').forEach((node) => {
                  if (!visible.has(String(node.dataset.processEntry ?? ''))) node.remove();
                });
              };

              const createLiveProcessPanel = () => {
                const panel = document.createElement('div');
                panel.className = 'live-process-panel';

                const toggle = document.createElement('button');
                toggle.type = 'button';
                toggle.className = 'thought-toggle live-process-toggle';
                toggle.dataset.liveProcess = 'true';
                toggle.hidden = true;
                toggle.append(thoughtMark(), document.createElement('span'));

                const history = document.createElement('div');
                history.className = 'process-history live-process-history';

                panel.append(toggle, history);
                return panel;
              };

              const syncLiveProcessPanel = (thought) => {
                let panel = thought.querySelector('.live-process-panel');
                if (!panel) {
                  panel = createLiveProcessPanel();
                  thought.append(panel);
                }

                const hasEntries = state.liveProcessEntries.length > 0;
                panel.hidden = !hasEntries;
                if (!hasEntries) return;

                const collapseWithAnswer = Boolean(state.streamedContent);
                const expanded = collapseWithAnswer ? state.liveProcessExpanded : true;
                const toggle = panel.querySelector('.live-process-toggle');
                if (toggle) {
                  toggle.hidden = !collapseWithAnswer;
                  toggle.setAttribute('aria-expanded', String(expanded));
                  const label = toggle.querySelector('span');
                  if (label) label.textContent = expanded ? 'hide process' : 'view process';
                }

                const history = panel.querySelector('.live-process-history');
                if (!history) return;

                // Collapse is visual only. Keep the process DOM mounted so opening it again does
                // not recreate scrollable children or cut sibling observers such as Speech/Kokoro.
                history.hidden = !expanded;
                if (expanded) syncLiveProcessHistory(history);
              };

              const reachedLine = (message) => {
                if (message.role !== 'vex') return null;
                const key = String(message.messageRef ?? '');
                if (state.processEntriesByResponse.has(key)) return null;
                const uses = state.reachesByResponse.get(key);
                if (!uses || !uses.length) return null;
                const line = document.createElement('p');
                line.className = 'receipt-line reached-line';
                line.textContent = t('workspace.label.reached') + ': ' + uses.map(reachText).join(' · ');
                return line;
              };

              const completedThoughtNode = (message) => {
                if (message.role !== 'vex') return null;
                // Durable Conversation responses use messageRef. Private responses intentionally do
                // not become Conversation messages, so their existing eventRef is enough as a
                // presentation-local key for the same ephemeral thought UI.
                const key = String(message.messageRef ?? message.eventRef ?? '');
                const entries = state.processEntriesByResponse.get(key) ?? [];
                const reasoning = state.thoughtsVisible ? state.humanReasoningByResponse.get(key) : null;
                const hasVisibleProcess =
                  entries.some((entry) => entry.kind === 'reach' || (state.thoughtsVisible && String(entry.text ?? '').trim()));
                if (!hasVisibleProcess && !reasoning) return null;

                const holder = document.createElement('div');
                holder.className = 'thought';
                const expanded = state.expandedThoughts.has(key);
                const toggle = document.createElement('button');
                toggle.type = 'button';
                toggle.className = 'thought-toggle';
                toggle.dataset.thoughtKey = key;
                toggle.setAttribute('aria-expanded', String(expanded));
                const label = document.createElement('span');
                label.textContent = expanded ? 'hide process' : 'view process';
                toggle.append(thoughtMark(), label);
                holder.append(toggle);

                if (expanded) {
                  const process = entries.length
                    ? processHistoryNode(entries, 'response.' + key + '.process', false)
                    : null;
                  if (process) {
                    holder.append(process);
                  } else if (reasoning) {
                    // Non-streaming/idempotent recovery may have only response-local reasoning.
                    // Keep that truthful fallback without inventing missing process boundaries.
                    const box = document.createElement('div');
                    box.className = 'reasoning';
                    bindReasoningViewport(box, 'response.' + key, false);
                    box.append(textPresentationBody(reasoning));
                    holder.append(box);
                  }
                }
                return holder;
              };

              // Current TextPresentation interpretation grammar: Markdown.
              // Pattern recognition produces detached DOM nodes using createElement/textContent only;
              // it never creates HTML markup strings. The same grammar can serve reply and reasoning
              // text because custody stays upstream and each host surface keeps its own scroll owner.
              const markdownElement = (tag, className, children) => {
                const node = document.createElement(tag);
                if (className) node.className = className;
                for (const child of children || []) node.append(child);
                return node;
              };

              const markdownSafeHref = (url) => /^(https?:\/\/|mailto:)/i.test(url) ? url : null;

              const markdownWordChar = (character) => Boolean(character) && /[\p{L}\p{N}]/u.test(character);

              const markdownInlinePatterns = [
                { kind: 'code', re: /``([^\n]+?)``/ },
                { kind: 'code', re: /`([^`\n]+)`/ },
                { kind: 'link', re: /\[([^\]\n]+)\]\(([^()\s]+(?:\([^()\s]*\))?[^()\s]*)\)/ },
                { kind: 'url', re: /https?:\/\/[^\s<>()\[\]{}"']+/ },
                { kind: 'strong', re: /\*\*(?=\S)([^\n]*?\S)\*\*/ },
                { kind: 'strong', re: /__(?=\S)([^\n]*?\S)__/, word: true },
                { kind: 'del', re: /~~(?=\S)([^\n]*?\S)~~/ },
                { kind: 'em', re: /\*(?=[^\s*])([^*\n]*?[^\s*])\*/ },
                { kind: 'em', re: /_(?=[^\s_])([^_\n]*?[^\s_])_/, word: true }
              ];

              const markdownInlineNodes = (text, depth) => {
                const nodes = [];
                let rest = String(text);
                let guard = 0;
                while (rest && guard < 10000) {
                  guard += 1;
                  let best = null;
                  for (const rule of markdownInlinePatterns) {
                    const match = rule.re.exec(rest);
                    if (!match) continue;
                    if (rule.word) {
                      // snake_case and __init__ stay text: an underscore delimiter needs a boundary.
                      const before = rest.charAt(match.index - 1);
                      const after = rest.charAt(match.index + match[0].length);
                      if (markdownWordChar(before) || markdownWordChar(after)) continue;
                    }
                    if (!best || match.index < best.match.index) best = { rule, match };
                  }
                  if (!best || (depth || 0) > 8) {
                    nodes.push(document.createTextNode(rest));
                    break;
                  }
                  const { rule, match } = best;
                  if (match.index > 0) nodes.push(document.createTextNode(rest.slice(0, match.index)));
                  if (rule.kind === 'code') {
                    const code = document.createElement('code');
                    code.textContent = match[1];
                    nodes.push(code);
                  } else if (rule.kind === 'link') {
                    const href = markdownSafeHref(match[2]);
                    if (href) {
                      const link = markdownElement('a', null, markdownInlineNodes(match[1], (depth || 0) + 1));
                      link.href = href;
                      link.target = '_blank';
                      link.rel = 'noopener noreferrer';
                      nodes.push(link);
                    } else {
                      nodes.push(document.createTextNode(match[0]));
                    }
                  } else if (rule.kind === 'url') {
                    let url = match[0];
                    while (/[.,;:!?)]/.test(url.charAt(url.length - 1))) url = url.slice(0, -1);
                    const link = document.createElement('a');
                    link.href = url;
                    link.target = '_blank';
                    link.rel = 'noopener noreferrer';
                    link.textContent = url;
                    nodes.push(link);
                    rest = rest.slice(match.index + url.length);
                    continue;
                  } else {
                    nodes.push(markdownElement(rule.kind, null, markdownInlineNodes(match[1], (depth || 0) + 1)));
                  }
                  rest = rest.slice(match.index + match[0].length);
                }
                return nodes;
              };

              const markdownSplitRow = (line) => {
                let text = line.trim();
                if (text.startsWith('|')) text = text.slice(1);
                if (text.endsWith('|') && !text.endsWith('\\|')) text = text.slice(0, -1);
                const cells = [];
                let current = '';
                for (let index = 0; index < text.length; index += 1) {
                  const character = text.charAt(index);
                  if (character === '\\' && text.charAt(index + 1) === '|') {
                    current += '|';
                    index += 1;
                  } else if (character === '|') {
                    cells.push(current.trim());
                    current = '';
                  } else {
                    current += character;
                  }
                }
                cells.push(current.trim());
                return cells;
              };

              const markdownFullMatch = (re, line) => {
                const match = line.match(re);
                return match && match[0].length === line.length ? match : null;
              };

              const markdownTableSeparator = (line) =>
                Boolean(line) && line.includes('-') && Boolean(markdownFullMatch(/\s*\|?(?:\s*:?-+:?\s*\|)*\s*:?-+:?\s*\|?\s*/, line));

              const markdownFence = /^\s{0,3}(`{3,}|~{3,})\s*([\w+#.-]*)/;
              const markdownHeading = /^\s{0,3}(#{1,6})\s+(.*)/;
              const markdownRule = /\s{0,3}([-*_])(?:\s*\1){2,}\s*/;
              const markdownItem = /^(\s*)([-*+]|\d{1,9}[.)])\s+(.*)/;
              const markdownQuote = /^\s{0,3}>\s?(.*)/;

              const markdownStartsBlock = (line, next) =>
                markdownFence.test(line) || markdownHeading.test(line) || Boolean(markdownFullMatch(markdownRule, line)) ||
                markdownQuote.test(line) || markdownItem.test(line) ||
                (line.includes('|') && markdownTableSeparator(next));

              const markdownBlockNodes = (source, depth) => {
                const lines = String(source).replace(/\r\n?/g, '\n').split('\n');
                const out = [];
                let index = 0;
                const blank = (line) => !line || !line.trim();
                if (depth > 6) return [markdownElement('p', null, [document.createTextNode(String(source))])];
                while (index < lines.length) {
                  const line = lines[index];
                  if (blank(line)) {
                    index += 1;
                    continue;
                  }
                  let match = line.match(markdownFence);
                  if (match) {
                    const marker = match[1];
                    const body = [];
                    index += 1;
                    while (index < lines.length && !lines[index].trim().startsWith(marker)) {
                      body.push(lines[index]);
                      index += 1;
                    }
                    index += 1;
                    const code = document.createElement('code');
                    if (match[2]) code.dataset.language = match[2];
                    code.textContent = body.join('\n');
                    out.push(markdownElement('pre', null, [code]));
                    continue;
                  }
                  match = line.match(markdownHeading);
                  if (match) {
                    const level = match[1].length;
                    const heading = markdownElement('p', 'prose-heading md-h' + level, markdownInlineNodes(match[2].replace(/\s+#+\s*(?![\s\S])/, ''), 0));
                    heading.setAttribute('role', 'heading');
                    heading.setAttribute('aria-level', String(Math.min(6, level + 2)));
                    out.push(heading);
                    index += 1;
                    continue;
                  }
                  if (markdownFullMatch(markdownRule, line)) {
                    out.push(document.createElement('hr'));
                    index += 1;
                    continue;
                  }
                  if (line.includes('|') && markdownTableSeparator(lines[index + 1])) {
                    const header = markdownSplitRow(line);
                    const aligns = markdownSplitRow(lines[index + 1]).map((cell) =>
                      cell.startsWith(':') && cell.endsWith(':') ? 'center' : cell.endsWith(':') ? 'right' : cell.startsWith(':') ? 'left' : ''
                    );
                    index += 2;
                    const rows = [];
                    while (index < lines.length && !blank(lines[index]) && lines[index].includes('|')) {
                      rows.push(markdownSplitRow(lines[index]));
                      index += 1;
                    }
                    const cell = (tag, text, column) => {
                      const node = markdownElement(tag, null, markdownInlineNodes(text, 0));
                      if (aligns[column]) node.style.textAlign = aligns[column];
                      return node;
                    };
                    const head = markdownElement('thead', null, [markdownElement('tr', null, header.map((text, column) => cell('th', text, column)))]);
                    const body = markdownElement('tbody', null, rows.map((row) =>
                      markdownElement('tr', null, header.map((_, column) => cell('td', row[column] ?? '', column)))
                    ));
                    out.push(markdownElement('table', 'md-table', [head, body]));
                    continue;
                  }
                  match = line.match(markdownQuote);
                  if (match) {
                    const quoted = [];
                    while (index < lines.length && (match = lines[index].match(markdownQuote))) {
                      quoted.push(match[1]);
                      index += 1;
                    }
                    out.push(markdownElement('blockquote', null, markdownBlockNodes(quoted.join('\n'), depth + 1)));
                    continue;
                  }
                  match = line.match(markdownItem);
                  if (match) {
                    const indent = match[1].length;
                    const ordered = /\d/.test(match[2]);
                    const list = document.createElement(ordered ? 'ol' : 'ul');
                    if (ordered) {
                      const start = parseInt(match[2], 10);
                      if (start !== 1) list.start = start;
                    }
                    const items = [];
                    while (index < lines.length) {
                      const current = lines[index];
                      const item = current.match(markdownItem);
                      if (item && item[1].length === indent && /\d/.test(item[2]) === ordered) {
                        items.push([item[3]]);
                        index += 1;
                      } else if (!blank(current) && items.length && current.match(/^\s*/)[0].length > indent) {
                        items[items.length - 1].push(current.slice(Math.min(current.match(/^\s*/)[0].length, indent + 2)));
                        index += 1;
                      } else if (blank(current) && items.length && index + 1 < lines.length) {
                        const following = lines[index + 1];
                        const nextItem = following.match(markdownItem);
                        const continues =
                          (nextItem && nextItem[1].length === indent && /\d/.test(nextItem[2]) === ordered) ||
                          (!blank(following) && following.match(/^\s*/)[0].length > indent);
                        if (!continues) break;
                        items[items.length - 1].push('');
                        index += 1;
                      } else {
                        break;
                      }
                    }
                    for (const itemLines of items) {
                      const content = markdownBlockNodes(itemLines.join('\n'), depth + 1);
                      const li = document.createElement('li');
                      // A one-paragraph item is written inline, as people expect from a short list.
                      if (content.length && content[0].tagName === 'P' && !content[0].classList.contains('prose-heading')) {
                        li.append(...content[0].childNodes);
                        li.append(...content.slice(1));
                      } else {
                        li.append(...content);
                      }
                      list.append(li);
                    }
                    out.push(list);
                    continue;
                  }
                  const paragraph = [];
                  while (index < lines.length && !blank(lines[index]) &&
                    (!paragraph.length || !markdownStartsBlock(lines[index], lines[index + 1]))) {
                    paragraph.push(lines[index]);
                    index += 1;
                  }
                  const node = document.createElement('p');
                  paragraph.forEach((text, position) => {
                    if (position > 0) node.append(document.createElement('br'));
                    node.append(...markdownInlineNodes(text.trim(), 0));
                  });
                  out.push(node);
                }
                return out;
              };

              // VexArchitecture TextPresentation boundary:
              // - exact text remains owned by the relationship that produced it: canonical/live
              //   response text stays with Conversation/Send, ephemeral reasoning stays session-only;
              // - TextPresentation only asks how that exact text should become human-readable;
              // - one interpretation grammar recognizes presentation patterns in the source text;
              // - current Web realization uses Markdown patterns to form visible text structure;
              // - applying that presentation keeps compatible mounted nodes and changes only the
              //   smallest incompatible visible subtree;
              // - each host surface keeps its own interaction owner: timeline scroll for replies,
              //   reasoningViewport for thinking/process text.
              //
              // TextPresentation names the reusable relationship, not a new truth owner.
              // Markdown is one interpretation grammar; DOM is one platform manifestation.
              // RESPONSE TEXT != REASONING TEXT, even when both use the same TextPresentation.
              //
              // EVOLUTION / REUSE NOTE FOR FUTURE IMPLEMENTERS:
              // Do not duplicate this Web DOM machinery into Android, iOS, another template system,
              // or a non-Markdown presentation just because those realizations do not exist yet.
              // Start from TextPresentation: exact owner-provided text -> recognized presentation
              // meaning -> platform-specific manifestation. Reuse that relationship and keep each
              // platform's concrete realization local. If two or more real consumers need the same
              // presentation structure, that repeated pressure may earn a shared typed
              // TextPresentation structure above the platform renderers. Until then, do not invent
              // a portable AST or make this Web implementation the cross-platform authority.
              //
              // This is not Relay/convergence: no second independently-owned truth is composed here.
              // DOM identity and presentedTextByBody are presentation-local realization/currentness only.
              const sameTextPresentationKind = (current, projected) =>
                current.nodeType === projected.nodeType &&
                (current.nodeType !== 1 || current.tagName === projected.tagName);

              const applyTextPresentationAttributes = (current, projected) => {
                const next = new Map(
                  Array.from(projected.attributes).map((attribute) => [attribute.name, attribute.value])
                );
                for (const attribute of Array.from(current.attributes)) {
                  if (!next.has(attribute.name)) current.removeAttribute(attribute.name);
                }
                for (const [name, value] of next) {
                  if (current.getAttribute(name) !== value) current.setAttribute(name, value);
                }
              };

              const applyTextPresentationNode = (parent, current, projected) => {
                if (!sameTextPresentationKind(current, projected)) {
                  parent.insertBefore(projected, current);
                  current.remove();
                  return projected;
                }
                if (current.nodeType === 3) {
                  if (current.nodeValue !== projected.nodeValue) current.nodeValue = projected.nodeValue;
                  return current;
                }
                applyTextPresentationAttributes(current, projected);
                applyTextPresentationChildren(current, Array.from(projected.childNodes));
                return current;
              };

              const applyTextPresentationChildren = (parent, projectedChildren) => {
                let index = 0;
                for (const projected of projectedChildren) {
                  const current = parent.childNodes[index] || null;
                  if (current) applyTextPresentationNode(parent, current, projected);
                  else parent.append(projected);
                  index += 1;
                }
                while (parent.childNodes.length > projectedChildren.length) {
                  parent.childNodes[parent.childNodes.length - 1].remove();
                }
              };

              const presentedTextByBody = new WeakMap();

              const presentText = (container, source) => {
                const text = String(source ?? '');
                if (presentedTextByBody.get(container) === text) return;
                try {
                  // Reconsider presentation from the exact current source while mutating only the
                  // nested DOM subtrees whose projected structure or content actually changed.
                  applyTextPresentationChildren(container, markdownBlockNodes(text, 0));
                  container.removeAttribute('data-markdown-fallback');
                } catch {
                  // Presentation failure must never erase answer text that already exists.
                  container.replaceChildren(document.createTextNode(text));
                  container.setAttribute('data-markdown-fallback', 'true');
                }
                presentedTextByBody.set(container, text);
              };

              const textPresentationBody = (source) => {
                const body = document.createElement('div');
                body.className = 'text-presentation';
                presentText(body, source);
                return body;
              };

              // Live TextPresentation is frame-batched across all mounted surfaces. The queue is
              // presentation-local only: it does not own source currentness or scroll intent.
              // Each queued surface supplies its own afterPresent continuation so the rightful
              // interaction owner can respond to changed geometry without being collapsed together.
              let liveTextPresentationFrame = 0;
              const liveTextPresentationQueue = new Map();
              const scheduleLiveTextPresentation = (body, source, afterPresent = null) => {
                liveTextPresentationQueue.set(body, { source: String(source ?? ''), afterPresent });
                if (liveTextPresentationFrame) return;
                liveTextPresentationFrame = requestAnimationFrame(() => {
                  liveTextPresentationFrame = 0;
                  const pending = Array.from(liveTextPresentationQueue.entries());
                  liveTextPresentationQueue.clear();
                  for (const [target, presentation] of pending) {
                    if (!target?.isConnected) continue;
                    presentText(target, presentation.source);
                    if (typeof presentation.afterPresent === 'function') presentation.afterPresent();
                  }
                });
              };
              const messageNode = (message) => {
                const item = document.createElement('li');
                const human = message.role === 'human';
                item.className = 'message ' + (human ? 'message-you' : 'message-vex');
                item.dataset.eventRef = String(message.eventRef ?? '');

                const index = document.createElement('p');
                index.className = 'message-index';
                index.textContent = '[' + String(Number(message.sequence) || 0).padStart(2, '0') + ']';

                const bubble = document.createElement('div');
                bubble.className = 'bubble';
                if (human) {
                  // What the person wrote is shown exactly as written.
                  const paragraph = document.createElement('p');
                  paragraph.textContent = String(message.content ?? '');
                  bubble.append(paragraph);
                } else {
                  bubble.append(textPresentationBody(message.content));
                }

                const speaker = document.createElement('p');
                speaker.className = 'speaker';
                speaker.textContent = human ? 'You' : 'Vex';

                const thought = completedThoughtNode(message);
                if (thought) item.append(index, thought, bubble, speaker);
                else item.append(index, bubble, speaker);
                const reached = reachedLine(message);
                if (reached) item.append(reached);
                return item;
              };

              const waitingNode = (sequence) => {
                const item = document.createElement('li');
                item.className = 'message message-vex waiting';
                item.dataset.hasContent = String(Boolean(state.streamedContent));

                const index = document.createElement('p');
                index.className = 'message-index';
                index.textContent = '[' + String(sequence).padStart(2, '0') + ']';

                const thought = document.createElement('div');
                thought.className = 'thought thinking-now';
                const who = document.createElement('span');
                who.className = 'speaker-inline';
                who.textContent = 'Vex';
                const mark = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                mark.setAttribute('class', 'thought-mark');
                mark.setAttribute('viewBox', '0 0 16 16');
                mark.innerHTML = '<path d="M8 1.8 14.2 8 8 14.2 1.8 8z"/><path d="M8 5 11 8 8 11 5 8z"/>';
                const label = document.createElement('span');
                label.className = 'thinking-label';
                label.textContent =
                  state.streamPhase === 'CheckingRuntime'
                    ? 'checking Vex…'
                    : state.streamPhase === 'Reconnecting'
                      ? 'reconnecting Vex…'
                      : state.streamPhase === 'Generating' || state.streamPhase === 'Reasoning'
                        ? 'thinking…'
                        : state.streamPhase === 'Responding'
                          ? 'responding…'
                          : 'preparing…';
                thought.append(who, mark, label);
                // Live process DOM is mounted once for this waiting message. Streaming updates
                // mutate/append inside it; they do not replace the panel or existing containers.
                thought.append(createLiveProcessPanel());
                syncLiveProcessPanel(thought);

                const bubble = document.createElement('div');
                bubble.className = 'bubble';
                if (state.streamedContent) bubble.append(textPresentationBody(state.streamedContent));
                const speaker = document.createElement('p');
                speaker.className = 'speaker';
                speaker.textContent = 'Vex';

                item.append(index, thought, bubble, speaker);
                return item;
              };

              const failedAttemptNode = (attempt, sequence) => {
                const item = document.createElement('li');
                item.className = 'message message-vex failed-attempt';
                item.dataset.failedAttemptKey = String(attempt.idempotencyKey ?? '');

                const index = document.createElement('p');
                index.className = 'message-index';
                index.textContent = '[' + String(sequence).padStart(2, '0') + ']';

                const thought = document.createElement('div');
                thought.className = 'thought';
                const who = document.createElement('span');
                who.className = 'speaker-inline';
                who.textContent = 'Vex';
                const mark = thoughtMark();
                const label = document.createElement('span');
                label.className = 'thinking-label';
                label.textContent = 'stopped before finishing';
                thought.append(who, mark, label);

                const history =
                  processHistoryNode(
                    attempt.processEntries ?? [],
                    'failed.' + String(attempt.idempotencyKey ?? ''),
                    false
                  );
                if (history) thought.append(history);

                const bubble = document.createElement('div');
                bubble.className = 'bubble';
                const detail = document.createElement('p');
                detail.textContent =
                  attempt.cancelledByPerson
                    ? t('workspace.status.conversation-stopped')
                    : String(attempt.failureCode ?? 'TURN_FAILED') +
                      ' · ' +
                      failureRouteLabel(attempt.exactNextSafeRoute);
                bubble.append(detail);

                const retry = document.createElement('button');
                retry.type = 'button';
                retry.className = 'thought-toggle failed-attempt-retry';
                retry.dataset.failedAttemptKey = String(attempt.idempotencyKey ?? '');
                retry.textContent = 'Try Again';
                retry.disabled = state.sendInFlight;
                bubble.append(retry);

                const speaker = document.createElement('p');
                speaker.className = 'speaker';
                speaker.textContent = 'Vex';

                item.append(index, thought, bubble, speaker);
                return item;
              };

              const ephemeralConversationSurface = ({
                kind,
                exchanges,
                pending,
                emptyHeading,
                emptyNote
              }) => {
                const prefix = kind + '.';
                const messages = (exchanges ?? []).flatMap((exchange, index) => {
                  const humanKey = prefix + String(index * 2 + 1);
                  const vexKey = prefix + String(index * 2 + 2);
                  const reasoning = String(exchange.sealedHumanReasoning ?? '').trim();
                  if (reasoning) state.humanReasoningByResponse.set(vexKey, reasoning);
                  else {
                    state.humanReasoningByResponse.delete(vexKey);
                    state.expandedThoughts.delete(vexKey);
                    state.reasoningViewports.delete('response.' + vexKey);
                  }
                  return [
                    { role: 'human', content: exchange.human, sequence: index * 2 + 1, eventRef: humanKey },
                    { role: 'vex', content: exchange.vex, sequence: index * 2 + 2, eventRef: vexKey }
                  ];
                });
                return {
                  kind,
                  reasoningPrefix: prefix,
                  messages,
                  pending: pending
                    ? {
                        content: pending,
                        sequence: messages.length + 1,
                        eventRef: prefix + 'pending'
                      }
                    : null,
                  failedAttempts: [],
                  emptyHeading,
                  emptyNote
                };
              };

              const activeConversationSurface = () => {
                if (isVexLabs()) {
                  return ephemeralConversationSurface({
                    kind: 'vex-labs',
                    exchanges: state.vexLabs?.exchanges ?? [],
                    pending: state.vexLabsPending,
                    emptyHeading: 'Vex Labs Test',
                    emptyNote: 'This is a scratch conversation against the real native model. Lab turns do not enter your real archives or rewrite Memory.'
                  });
                }
                if (isPrivate()) {
                  return ephemeralConversationSurface({
                    kind: 'private',
                    exchanges: state.privateMode?.exchanges ?? [],
                    pending: state.privatePending,
                    emptyHeading: 'Private conversation',
                    emptyNote: 'Nothing you say here is kept. Vex still knows what it knew.'
                  });
                }
                return {
                  kind: 'canonical',
                  reasoningPrefix: null,
                  messages: state.conversation?.messages ?? [],
                  pending: state.pending
                    ? {
                        content: state.pending.content,
                        sequence: nextSequence(),
                        eventRef: 'presentation.pending.' + state.pending.idempotencyKey
                      }
                    : null,
                  failedAttempts: state.failedAttempts,
                  emptyHeading: 'Converse with Vex',
                  emptyNote: 'Include the page to hand Vex its text. Enter sends; Shift+Enter starts a new line.'
                };
              };

              const pruneInactiveEphemeralReasoning = (activePrefix) => {
                for (const key of Array.from(state.humanReasoningByResponse.keys())) {
                  const ephemeral = key.startsWith('private.') || key.startsWith('vex-labs.');
                  if (!ephemeral || (activePrefix && key.startsWith(activePrefix))) continue;
                  state.humanReasoningByResponse.delete(key);
                  state.expandedThoughts.delete(key);
                  state.reasoningViewports.delete('response.' + key);
                }
              };

              const renderTimeline = () => {
                if (!ui.timeline) return;
                const surface = activeConversationSurface();
                ui.timeline.dataset.private = String(surface.kind === 'private');
                ui.timeline.dataset.vexLabs = String(surface.kind === 'vex-labs');
                pruneInactiveEphemeralReasoning(surface.reasoningPrefix);

                const nodes = surface.messages.map(messageNode);
                let sequence =
                  surface.kind === 'canonical'
                    ? nextSequence()
                    : surface.messages.length + 1;

                for (const attempt of surface.failedAttempts) {
                  nodes.push(messageNode({
                    role: 'human',
                    content: attempt.content,
                    sequence,
                    eventRef: 'presentation.failed.' + attempt.idempotencyKey
                  }));
                  sequence += 1;
                  nodes.push(failedAttemptNode(attempt, sequence));
                  sequence += 1;
                }

                if (surface.pending) {
                  const pendingSequence =
                    surface.kind === 'canonical'
                      ? sequence
                      : surface.pending.sequence;
                  nodes.push(messageNode({
                    role: 'human',
                    content: surface.pending.content,
                    sequence: pendingSequence,
                    eventRef: surface.pending.eventRef
                  }));
                  nodes.push(waitingNode(pendingSequence + 1));
                }

                if (!nodes.length) {
                  const empty = document.createElement('li');
                  empty.className = 'timeline-empty';
                  const heading = document.createElement('p');
                  heading.className = 'timeline-empty-heading';
                  heading.textContent = surface.emptyHeading;
                  const note = document.createElement('p');
                  note.textContent = surface.emptyNote;
                  empty.append(heading, note);
                  nodes.push(empty);
                }

                ui.timeline.replaceChildren(...nodes);
                restoreReasoningViewports();
                applyTimelineViewport();
                markReading();
              };

              const updateLiveTurn = () => {
                const item = ui.timeline.querySelector('.message.waiting');
                if (!item) {
                  renderTimeline();
                  return;
                }

                item.dataset.hasContent = String(Boolean(state.streamedContent));
                const label = item.querySelector('.thinking-label');
                if (label) {
                  label.textContent =
                    state.streamPhase === 'CheckingRuntime'
                      ? 'checking Vex…'
                      : state.streamPhase === 'Reconnecting'
                        ? 'reconnecting Vex…'
                        : state.streamPhase === 'Generating' || state.streamPhase === 'Reasoning'
                          ? 'thinking…'
                          : state.streamPhase === 'Responding'
                            ? 'responding…'
                            : 'preparing…';
                }

                const thought = item.querySelector('.thinking-now');
                // Preserve live DOM identity while Vex streams. This is a continuation of the
                // T004 scroll-ownership fix: state restoration cannot compensate for destroying
                // the element a human is actively scrolling/dragging.
                if (thought) syncLiveProcessPanel(thought);

                const bubble = item.querySelector('.bubble');
                let body = bubble?.querySelector('.text-presentation');
                if (state.streamedContent) {
                  if (!body && bubble) {
                    body = textPresentationBody('');
                    bubble.append(body);
                  }
                  if (body) scheduleLiveTextPresentation(body, state.streamedContent, () => applyTimelineViewport(false));
                } else if (body) {
                  body.remove();
                }

                applyTimelineViewport(false);
              };

              const renderTicket = () => {
                const context = state.browserContext;
                const visible = state.includePage && context;
                ui.ticket.hidden = !visible;
                if (!visible) return;
                ui.ticket.dataset.state = 'Ready';
                ui.ticketTitle.textContent = context.title || 'Included page';
                ui.ticketLine.textContent = 'A separately rendered snapshot of this entered URL will be given to Vex on the next turn.';
                ui.ticketUrl.textContent = context.pageUrl;
                ui.ticketHash.textContent = '';
                ui.ticketView.dataset.contextReady = 'true';
              };

              // Private mode: semantics and copy come from the service (VexLifePrivateModeFamily). The
              // private transcript exists only in the running service; this renderer only shows it.
              const isPrivate = () => state.privateMode?.state === ${jsString(VexLifePrivateModeFamily.StatePrivate)};

              const renderPrivate = () => {
                const display = state.privateMode?.display;
                const privateNow = isPrivate();
                ui.privateMode.setAttribute('aria-pressed', String(privateNow));
                ui.privateMode.dataset.private = String(privateNow);
                ui.privateModeLabel.textContent = String(display?.label ?? 'Kept in your archives');
                ui.privateMode.title = String(display?.detail ?? '') + (display?.primaryLabel ? ' (' + display.primaryLabel + ')' : '');
                ui.privateMode.disabled = !display || state.privateInFlight;
                root.dataset.vexPrivate = String(privateNow);
                ui.privateBanner.hidden = !privateNow;
                if (!privateNow) return;
                ui.privateBannerText.textContent = String(display?.detail ?? '');
                const notes = (state.privateMode?.keptNotes ?? []).map((note) => {
                  const item = document.createElement('li');
                  item.textContent = 'Kept: ' + note;
                  return item;
                });
                ui.privateNotes.replaceChildren(...notes);
                const proposal = state.privateMode?.proposedNoteOrNull;
                ui.privateProposal.hidden = !proposal;
                ui.privateProposalText.textContent = proposal ? 'Vex would keep: “' + proposal + '”' : '';
                ui.privatePropose.hidden = Boolean(proposal);
                ui.privatePropose.disabled = state.privateInFlight || !(state.privateMode?.exchanges ?? []).length;
                ui.privateKeep.disabled = state.privateInFlight;
                ui.privateDecline.disabled = state.privateInFlight;
              };

              const loadPrivate = async () => {
                const response = await fetch(service.privatePath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                const wasPrivate = isPrivate();
                state.privateMode = await readJson(response);
                renderPrivate();
                if (wasPrivate !== isPrivate()) renderTimeline();
                if (state.app === apps.archives) renderArchives();
                return state.privateMode;
              };

              const privatePost = async (path, body) => {
                state.privateInFlight = true;
                renderPrivate();
                try {
                  const response = await effectFetch(path, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json', accept: 'application/json' },
                    body: JSON.stringify(body ?? {})
                  });
                  const value = await readJson(response);
                  state.privateMode = value?.private ?? value;
                } finally {
                  state.privateInFlight = false;
                }
                renderPrivate();
                renderTimeline();
                renderComposer();
                if (state.app === apps.archives) renderArchives();
              };

              const togglePrivate = async () => {
                const controlRef = state.privateMode?.display?.primaryControlRef;
                if (!controlRef || state.privateInFlight || state.sendInFlight) return;
                try {
                  await privatePost(service.privateControlPath, {
                    controlRef,
                    requestRef: 'request.vexlife-prod.private.' + crypto.randomUUID().toLowerCase()
                  });
                  notify(isPrivate()
                    ? 'Private mode is on. Nothing here is kept unless you keep a note.'
                    : 'Private mode is off. Vex will know a private conversation happened.');
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'Private mode could not change.');
                  await loadPrivate().catch(() => {});
                }
              };

              const sendPrivate = async () => {
                const content = ui.draft.value.trim();
                if (!content || state.privateInFlight) return;
                state.privatePending = content;
                ui.draft.value = '';
                sizeDraft();
                renderTimeline();
                try {
                  if (speech.settings.enabled) audioContext();
                  await privatePost(service.privateSendPath, { content });
                  const exchanges = state.privateMode?.exchanges ?? [];
                  if (speech.settings.enabled && exchanges.length) {
                    enqueueSpeech(exchanges[exchanges.length - 1].vex, 'private.' + exchanges.length * 2);
                  }
                } catch (error) {
                  if (!ui.draft.value) ui.draft.value = content;
                  sizeDraft();
                  notify(error instanceof Error ? error.message : 'Vex could not reply. Nothing was kept.');
                } finally {
                  state.privatePending = null;
                  renderTimeline();
                  renderComposer();
                }
              };

              // The left pane shows one app at a time. Choosing an app is presentation state only.
              const inSourceTemplate = (pageUrl) =>
                Boolean(pageUrl) && apps.sourceTemplateScopes.some((scope) => String(pageUrl).startsWith(scope));

              const renderApp = () => {
                const app = state.app;
                ui.appSelector.value = app;
                ui.family.hidden = app !== apps.sourceTemplate;
                ui.archives.hidden = app !== apps.archives;
                ui.vexContext.hidden = app !== apps.vexContext;
                ui.viewport.hidden = app === apps.archives || app === apps.vexContext;
                ui.addressForm.dataset.app = app;
                ui.query.disabled = app === apps.archives || app === apps.vexContext;
                if (app === apps.archives || app === apps.vexContext) ui.frameStatus.hidden = true;
                for (const member of ui.family.querySelectorAll('.family-member')) {
                  const current = String(state.humanPageUrl ?? '').startsWith(member.dataset.familyScope);
                  if (current) member.setAttribute('aria-current', 'page');
                  else member.removeAttribute('aria-current');
                }
                root.dataset.vexApp = app;
                if (app === apps.archives) renderArchives();
                if (app === apps.vexContext) renderVexContext();
              };

              const archiveDayKey = (date) =>
                date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0') + '-' + String(date.getDate()).padStart(2, '0');

              const renderArchives = () => {
                if (!ui.archivesTimeline) return;
                const messages = (state.conversation?.messages ?? []).filter((message) => !Number.isNaN(Date.parse(message.formedAt)));
                if (!messages.length) {
                  ui.archivesSummary.textContent = 'Everything you and Vex say is kept here, in your Vex Home, by day.';
                  const empty = document.createElement('li');
                  empty.className = 'archives-empty';
                  empty.textContent = 'Nothing here yet.';
                  ui.archivesTimeline.replaceChildren(empty);
                  return;
                }
                const days = new Map();
                const place = (at, entry) => {
                  const key = archiveDayKey(at);
                  if (!days.has(key)) days.set(key, { date: at, messages: [] });
                  days.get(key).messages.push(entry);
                };
                for (const message of messages) place(new Date(message.formedAt), message);
                for (const interval of state.privateMode?.intervals ?? []) {
                  if (!Number.isNaN(Date.parse(interval.startedAt))) {
                    place(new Date(interval.startedAt), { privateInterval: interval, formedAt: interval.startedAt });
                  }
                }
                for (const day of days.values()) day.messages.sort((a, b) => Date.parse(a.formedAt) - Date.parse(b.formedAt));
                const first = new Date(messages[0].formedAt);
                ui.archivesSummary.textContent =
                  messages.length + ' message' + (messages.length === 1 ? '' : 's') + ' over ' + days.size + ' day' + (days.size === 1 ? '' : 's') +
                  ' since ' + first.toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' }) +
                  ' · kept in your Vex Home';
                const items = [...days.entries()].reverse().map(([key, day]) => {
                  const item = document.createElement('li');
                  item.className = 'archives-day';
                  item.dataset.day = key;
                  const heading = document.createElement('h3');
                  heading.textContent = day.date.toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
                  const count = document.createElement('span');
                  const spoken = day.messages.filter((entry) => !entry.privateInterval).length;
                  count.textContent = spoken + ' message' + (spoken === 1 ? '' : 's');
                  heading.append(count);
                  const entries = document.createElement('ol');
                  entries.className = 'archives-entries';
                  for (const message of day.messages) {
                    const entry = document.createElement('li');
                    if (message.privateInterval) {
                      const interval = message.privateInterval;
                      const from = new Date(interval.startedAt);
                      const to = new Date(interval.endedAt);
                      entry.className = 'archives-entry archives-private';
                      const time = document.createElement('time');
                      time.dateTime = String(interval.startedAt);
                      time.textContent = from.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
                      const who = document.createElement('span');
                      who.className = 'who';
                      who.textContent = 'Private';
                      const text = document.createElement('p');
                      text.textContent =
                        'Private conversation until ' + to.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' }) +
                        ' · ' + interval.exchangeCount + ' exchange' + (interval.exchangeCount === 1 ? '' : 's') + ' · not kept' +
                        (interval.keptNotes.length ? '\nKept: ' + interval.keptNotes.join('\nKept: ') : '');
                      entry.append(time, who, text);
                      entries.append(entry);
                      continue;
                    }
                    entry.className = 'archives-entry';
                    entry.dataset.eventRef = String(message.eventRef ?? '');
                    const time = document.createElement('time');
                    time.dateTime = String(message.formedAt);
                    time.textContent = new Date(message.formedAt).toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
                    const who = document.createElement('span');
                    who.className = 'who';
                    who.textContent = message.role === 'human' ? 'You' : 'Vex';
                    const text = document.createElement('p');
                    text.textContent = String(message.content ?? '');
                    entry.append(time, who, text);
                    entries.append(entry);
                  }
                  item.append(heading, entries);
                  return item;
                });
                ui.archivesTimeline.replaceChildren(...items);
              };

              const shortIdentity = (value) => {
                const text = String(value ?? '');
                if (!text) return '';
                return text.length > 22 ? text.slice(0, 10) + '…' + text.slice(-8) : text;
              };

              const contextLine = (parent, text, strong = null) => {
                const p = document.createElement('p');
                if (strong) {
                  const bold = document.createElement('strong');
                  bold.textContent = strong;
                  p.append(bold, document.createTextNode(' ' + text));
                } else {
                  p.textContent = text;
                }
                parent.append(p);
                return p;
              };

              const contextExactText = (parent, text) => {
                const pre = document.createElement('pre');
                pre.textContent = String(text ?? '');
                parent.append(pre);
                return pre;
              };

              const contextEvidenceDisclosure = (parent, summaryText, text) => {
                const details = document.createElement('details');
                details.className = 'context-evidence-disclosure';
                const summary = document.createElement('summary');
                summary.textContent = summaryText;
                const pre = document.createElement('pre');
                pre.textContent = String(text ?? '');
                details.append(summary, pre);
                parent.append(details);
                return details;
              };

              const currentContextProfile = () => state.contextProfiles?.active ?? null;

              const profileDefinitionFromForm = () => {
                const mode = String(ui.contextProfileFoundationMode.value || 'PRODUCT_DEFAULT');
                const foundationText =
                  mode === ${jsString(VexLifeContextProfile.FoundationMode.EXTEND_PRODUCT_DEFAULT.name)}
                    ? String(ui.contextProfileFoundationText.value ?? '').trim()
                    : '';
                return {
                  schemaVersion: 'vexlife-prod.context-profile/v1',
                  name: String(ui.contextProfileName.value ?? '').trim() || 'My Vex',
                  description: '',
                  foundationMode: mode,
                  foundationText
                };
              };

              const renderContextProfile = () => {
                const projection = state.contextProfiles;
                if (!projection) {
                  ui.contextProfileSummary.textContent = 'Context profiles are not available yet.';
                  return;
                }
                const active = projection.active;
                const choices = [
                  { profileRef: 'profile.vexlife.context.product-default', definition: { name: 'VexLife Default' }, productDefault: true },
                  ...(projection.saved ?? [])
                ];
                ui.contextProfileChoice.replaceChildren(...choices.map((profile) => {
                  const option = document.createElement('option');
                  option.value = profile.profileRef;
                  option.textContent = profile.definition.name + (profile.productDefault ? '' : ' · r' + profile.revision);
                  return option;
                }));
                ui.contextProfileChoice.value = active.profileRef;
                ui.contextProfileChoice.disabled = state.contextProfileInFlight;
                ui.contextProfileBadge.textContent = active.definition.name;
                ui.contextProfileSummary.textContent =
                  active.productDefault
                    ? 'Using the context relationship that shipped with this VexLife version.'
                    : 'Saved revision ' + active.revision +
                      (active.productFoundationChangedSinceSave
                        ? ' · VexLife has a newer Product foundation than the one this profile was based on.'
                        : ' · applies to each new message until you choose another profile.');
                ui.contextProfileRestore.hidden = active.productDefault;
                ui.contextProfileSave.disabled = active.productDefault || state.contextProfileInFlight;
                ui.contextProfileSaveAs.disabled = state.contextProfileInFlight;
                ui.contextProfileRestore.disabled = state.contextProfileInFlight;
                ui.contextProfileHome.disabled = state.contextProfileInFlight;
                ui.contextProfileFoundationField.hidden =
                  ui.contextProfileFoundationMode.value !== ${jsString(VexLifeContextProfile.FoundationMode.EXTEND_PRODUCT_DEFAULT.name)};
                ui.contextProcessRetentionSelect.value = state.contextProcessRetention;
                if (!state.contextProfileDirty) {
                  ui.contextProfileName.value = active.definition.name;
                  ui.contextProfileFoundationMode.value = active.definition.foundationMode;
                  ui.contextProfileFoundationText.value = active.definition.foundationText || '';
                  ui.contextProfileFoundationField.hidden =
                    active.definition.foundationMode !== ${jsString(VexLifeContextProfile.FoundationMode.EXTEND_PRODUCT_DEFAULT.name)};
                  ui.contextProfileNote.textContent =
                    active.productDefault
                      ? 'You can save a named profile without changing VexLife’s default.'
                      : 'Saved in your Vex Home. Profile files contain configuration, not your conversations or memories.';
                }
                const card = ui.contextProfileName.closest('.context-profile-card');
                if (card) card.dataset.dirty = String(state.contextProfileDirty);
              };

              const renderVexLabs = () => {
                if (!ui.vexLabsFields) return;
                const projection = state.vexLabs;
                if (!projection) {
                  ui.vexLabsStatus.textContent = 'Loading native Lab';
                  ui.vexLabsFields.replaceChildren(
                    Object.assign(document.createElement('p'), {
                      className: 'context-empty',
                      textContent: 'Reading the current model and configuration fields…'
                    })
                  );
                  return;
                }

                const draft = ensureVexLabsDraft();
                const inputFocused = document.activeElement === ui.vexLabsInputBudget;
                const memoryFocused = document.activeElement === ui.vexLabsMemoryBudget;
                if (!inputFocused) ui.vexLabsInputBudget.value = String(draft.inputTokenBudget);
                if (!memoryFocused) ui.vexLabsMemoryBudget.value = String(draft.maximumMemoryTokens);
                ui.vexLabsMemoryMode.value = draft.memoryMode;

                const staleSnapshot =
                  projection.seedConversationHeadSha256 &&
                  projection.currentProductConversationHeadSha256 &&
                  projection.seedConversationHeadSha256 !== projection.currentProductConversationHeadSha256;
                ui.vexLabsStatus.textContent =
                  (projection.conversationPreset === 'EMPTY' ? 'Empty scratch conversation' : 'Current conversation snapshot') +
                  ' · ' + Number(projection.scratchConversationMessageCount ?? 0).toLocaleString() + ' messages';
                const lastAdmission = projection.lastAdmission ?? null;
                ui.vexLabsNote.textContent = state.vexLabsInFlight
                  ? 'Running the real native model through the shared Conversation engine against the scratch Lab…'
                  : lastAdmission
                    ? vexLabsAdmissionText(lastAdmission)
                  : vexLabsDraftMatchesLastTest()
                    ? 'This exact draft has been tested. Supported settings may now be saved.'
                    : staleSnapshot
                    ? 'Your real Conversation moved after this Lab snapshot. Reload current conversation whenever you want the Lab to catch up.'
                    : draft.memoryMode === 'NONE'
                      ? 'Lab Memory is off. Your real Memory is untouched.'
                      : 'Scratch Lab only. Your real Conversation and Memory are not written by Lab turns.';

                const busy = state.vexLabsInFlight;
                for (const button of [ui.vexLabsTest, ui.vexLabsRestore, ui.vexLabsReload, ui.vexLabsClear]) {
                  if (button) button.disabled = busy;
                }
                if (ui.vexLabsSaveSupported) {
                  ui.vexLabsSaveSupported.disabled = busy || !vexLabsDraftMatchesLastTest();
                  ui.vexLabsSaveSupported.title =
                    vexLabsDraftMatchesLastTest()
                      ? 'Save the tested active-context depth through its existing Context Profile owner.'
                      : 'Run a Lab turn with these exact values before saving.';
                }
                ui.vexLabsInputBudget.disabled = busy;
                ui.vexLabsMemoryBudget.disabled = busy;
                ui.vexLabsMemoryMode.disabled = busy;

                const fieldNodes = (projection.fields ?? []).map((field) => {
                  const row = document.createElement('details');
                  row.className = 'vex-labs-field';
                  row.dataset.fieldRef = String(field.fieldRef ?? '');

                  const summary = document.createElement('summary');
                  const title = document.createElement('span');
                  title.className = 'vex-labs-field-title';
                  title.textContent = String(field.label ?? field.fieldRef ?? 'Field');

                  const badge = document.createElement('span');
                  badge.className = 'vex-labs-field-badge';
                  const editable = field.labEditability === 'EDITABLE_LAB_ONLY';
                  const restart = field.labEditability === 'EDITABLE_PRODUCT_CONTROL_RESTART';
                  badge.dataset.state = editable ? 'edit' : field.labEditability === 'HELD' ? 'held' : 'read';
                  badge.textContent = editable ? 'Edit / test' : restart ? 'Restart setting' : field.labEditability === 'HELD' ? 'Held' : 'Read only';

                  const current = document.createElement('span');
                  current.className = 'vex-labs-field-value';
                  const testValue =
                    field.fieldRef === vexLabsProfileBudgetFieldRef
                      ? draft.inputTokenBudget
                      : field.fieldRef === vexLabsMemoryBudgetFieldRef
                        ? draft.maximumMemoryTokens
                        : null;
                  const unit = field.unit ? ' ' + field.unit : '';
                  current.textContent =
                    testValue === null
                      ? 'Current: ' + String(field.currentValue ?? 'not set') + unit
                      : 'Current Product: ' + String(field.currentValue ?? 'not set') + unit +
                        ' · Lab test: ' + Number(testValue).toLocaleString() + unit;
                  summary.append(title, badge, current);

                  const body = document.createElement('div');
                  body.className = 'vex-labs-field-body';
                  const purpose = document.createElement('p');
                  purpose.textContent = String(field.explanation ?? '');
                  body.append(purpose);

                  const editDetail = document.createElement('p');
                  editDetail.textContent = String(field.labEditDetail ?? '');
                  body.append(editDetail);

                  if (editable) {
                    const edit = document.createElement('button');
                    edit.type = 'button';
                    edit.className = 'quiet-button vex-labs-field-edit';
                    edit.dataset.fieldRef = String(field.fieldRef ?? '');
                    edit.textContent = 'Edit test value';
                    body.append(edit);
                  }

                  const facts = document.createElement('div');
                  facts.className = 'vex-labs-field-facts';
                  const factValues = [
                    field.inheritedDefaultValue != null ? 'Default ' + field.inheritedDefaultValue : null,
                    field.recommendedValue != null ? 'Recommended ' + field.recommendedValue : null,
                    field.minimumValue != null || field.maximumValue != null
                      ? 'Technical range ' + String(field.minimumValue ?? '…') + '–' + String(field.maximumValue ?? '…')
                      : null,
                    field.labObservedMinimumValue != null
                      ? 'Last held turn observed floor ' + Number(field.labObservedMinimumValue).toLocaleString()
                      : null,
                    field.ownerRef ? 'Owner ' + field.ownerRef : null,
                    field.userApplyBoundary ? 'Applies ' + String(field.userApplyBoundary).replaceAll('_', ' ').toLowerCase() : null
                  ].filter(Boolean);
                  for (const value of factValues) {
                    const chip = document.createElement('span');
                    chip.textContent = value;
                    facts.append(chip);
                  }
                  if (facts.childElementCount) body.append(facts);

                  if (field.increaseConsequence) {
                    const line = document.createElement('p');
                    line.textContent = 'If increased: ' + field.increaseConsequence;
                    body.append(line);
                  }
                  if (field.decreaseConsequence) {
                    const line = document.createElement('p');
                    line.textContent = 'If decreased: ' + field.decreaseConsequence;
                    body.append(line);
                  }

                  row.append(summary, body);
                  return row;
                });
                ui.vexLabsFields.replaceChildren(...fieldNodes);

                const result = state.vexLabsLastResult;
                ui.vexLabsRequest.hidden = !result?.providerRequestBody;
                if (result?.providerRequestBody) {
                  ui.vexLabsRequestSha.textContent = 'SHA-256 ' + String(result.providerRequestBodySha256 ?? '');
                  ui.vexLabsRequestBody.textContent = String(result.providerRequestBody);
                } else {
                  ui.vexLabsRequestSha.textContent = '';
                  ui.vexLabsRequestBody.textContent = '';
                }
              };

              const renderVexContext = () => {
                if (!ui.vexContext) return;
                renderContextProfile();
                renderVexLabs();
                const prompt = state.context?.promptContext ?? null;
                const active = currentContextProfile();
                const messages = state.conversation?.messages ?? [];
                const latestVex =
                  [...messages].reverse().find((message) =>
                    message.role === 'vex' &&
                    (!state.context?.turnRef || message.turnRef === state.context.turnRef)
                  ) ?? null;
                const latestHuman =
                  latestVex
                    ? [...messages].reverse().find((message) =>
                        message.role === 'human' && message.turnRef === latestVex.turnRef
                      ) ?? null
                    : null;
                const responseKey = String(latestVex?.messageRef ?? '');

                if (!prompt) {
                  const contextHeldUnsupported =
                    state.context?.state === service.contextHeldUnsupportedState;
                  const heldSchema =
                    String(state.context?.durablePromptReceiptSchema ?? 'unknown durable schema');
                  ui.vexContextTurnSummary.textContent =
                    contextHeldUnsupported
                      ? 'Context held for this completed turn.'
                      : 'No completed turn yet.';
                  ui.vexContextBudget.textContent = '';
                  ui.contextChangesList.replaceChildren(
                    Object.assign(document.createElement('p'), {
                      className: 'context-empty',
                      textContent: contextHeldUnsupported
                        ? 'This VexLife build cannot safely project the durable prompt receipt (' + heldSchema + '). The completed Conversation remains available.'
                        : 'No context transitions yet. Vex is still at the beginning of this Home.'
                    })
                  );
                  for (const node of [
                    ui.contextFoundationBody,
                    ui.contextMemoryBody,
                    ui.contextContinuityBody,
                    ui.contextConversationBody,
                    ui.contextBrowserBody,
                    ui.contextRequestBody,
                    ui.contextProcessBody,
                    ui.contextOutputBody,
                    ui.contextAfterBody
                  ]) node.replaceChildren();
                  ui.contextComponentList.replaceChildren();
                  return;
                }

                const exactTurnRef = String(state.context.turnRef ?? '');
                if (state.contextConversationTurnRef !== exactTurnRef) {
                  state.contextConversationTurnRef = exactTurnRef;
                  state.contextConversationCollapsedByEventRef.clear();
                }
                const usedInputTokens = Number(prompt.providerMaterializedInputTokenEstimate ?? 0);
                const inputTokenBudget = Number(prompt.inputTokenBudget ?? 0);
                const priorCompletedExchanges = Number(prompt.priorCompletedExchangeCount ?? 0);
                const usedWorkingContinuity = String(prompt.contextMode ?? '').includes('WORKING_CHECKPOINT');
                ui.vexContextTurnSummary.textContent = 'Last completed reply';
                ui.vexContextTurnSummary.title = exactTurnRef;
                ui.vexContextBudget.textContent =
                  usedWorkingContinuity
                    ? 'Earlier conversation was condensed'
                    : priorCompletedExchanges > 0
                      ? 'Recent conversation fit directly'
                      : 'Conversation started here';
                ui.vexContextBudget.title =
                  usedInputTokens.toLocaleString() + ' / ' + inputTokenBudget.toLocaleString() + ' input tokens';

                const promptMemoryComponents =
                  (prompt.contextComponents ?? []).filter((component) =>
                    component.componentRef === 'component.vexlife-prod.prompt.memory-current-context' ||
                    component.componentRef === 'component.vexlife-prod.prompt.memory-current-accepted-semantic'
                  );
                const dailyMemoryUsed =
                  promptMemoryComponents.some((component) =>
                    component.componentRef === 'component.vexlife-prod.prompt.memory-current-context');
                const acceptedSemanticMemoryUsed =
                  promptMemoryComponents.some((component) =>
                    component.componentRef === 'component.vexlife-prod.prompt.memory-current-accepted-semantic');
                const anyPromptMemoryUsed = promptMemoryComponents.length > 0;

                const changes = [];
                const change = (title, detail, stateName) => {
                  const item = document.createElement('div');
                  item.className = 'context-change';
                  item.dataset.state = stateName;
                  const body = document.createElement('p');
                  const strong = document.createElement('strong');
                  strong.textContent = title;
                  body.append(strong, document.createTextNode(' ' + detail));
                  item.append(body);
                  changes.push(item);
                };

                if (active && prompt.contextProfileFingerprint && active.fingerprint !== prompt.contextProfileFingerprint) {
                  change(
                    'Context profile changed.',
                    active.definition.name + ' will apply to your next message; the reply above used the earlier profile.',
                    'changed'
                  );
                } else if (active) {
                  change('Context profile.', active.definition.name + ' is current for the next turn.', 'current');
                }

                const usedReplyLanguage = String(prompt.replyLanguage ?? '');
                const currentReplyLanguage = String(state.settings?.replyLanguage ?? '');
                const languageName = (code) =>
                  state.settings?.languages?.find((language) => language.code === code)?.name ??
                  (code === usedReplyLanguage ? prompt.replyLanguageName : null) ??
                  code;
                if (usedReplyLanguage) {
                  if (currentReplyLanguage && currentReplyLanguage !== usedReplyLanguage) {
                    change(
                      'Reply language changed.',
                      'This reply was told ' + languageName(usedReplyLanguage) +
                        '. Your next message is currently set to ' + languageName(currentReplyLanguage) +
                        '. VexLife will read that Home setting at Send and provide it automatically before Vex starts thinking.',
                      'changed'
                    );
                  } else {
                    change(
                      'Reply language.',
                      'Vex was told ' + languageName(usedReplyLanguage) +
                        ' for this reply. VexLife read it from your Vex Home at Send and provided it automatically; Vex did not reach for it.',
                      'current'
                    );
                  }
                }

                if (usedWorkingContinuity) {
                  change(
                    'Earlier conversation was condensed.',
                    'Vex carried forward the current position instead of keeping the whole recent conversation in view. Older conversation is still in your archives and can be revisited.',
                    'changed'
                  );
                } else if (priorCompletedExchanges > 0) {
                  change(
                    'Recent conversation stayed available.',
                    'Vex could use the recent conversation directly for this reply; no continuity handoff was needed.',
                    'current'
                  );
                } else {
                  change(
                    'Conversation started here.',
                    'There was no earlier conversation to carry into this reply.',
                    'current'
                  );
                }

                if (acceptedSemanticMemoryUsed) {
                  change(
                    'Memory shaped this reply.',
                    dailyMemoryUsed
                      ? 'Daily Memory and current accepted-semantic Memory were both supplied under one shared Memory budget.'
                      : 'Current accepted-semantic Memory was supplied even though no Daily Memory component entered the prompt.',
                    'current'
                  );
                } else if (dailyMemoryUsed) {
                  const currentMemory = state.memory?.memoryContextFingerprint ?? null;
                  const rememberedCount = Number(state.memory?.rememberedCount ?? 0);
                  if (currentMemory && prompt.memoryContextFingerprint && currentMemory !== prompt.memoryContextFingerprint) {
                    change(
                      'Memory changed since this reply.',
                      'The Daily Memory available now is different from the Daily Memory component that shaped this reply.',
                      'changed'
                    );
                  } else {
                    change(
                      'Memory.',
                      'Vex used ' + rememberedCount + ' current Daily Memory item' + (rememberedCount === 1 ? '' : 's') + ' for this reply.',
                      'current'
                    );
                  }
                } else {
                  change('Memory.', 'No structured Memory component was included in this reply.', 'current');
                }
                ui.contextChangesList.replaceChildren(...changes);

                // Foundation: Product source remains present; a profile may add a personal extension.
                ui.contextFoundationBody.replaceChildren();
                const productField = prompt.foundationalField;
                const profileMode = prompt.contextProfileFoundationMode ?? 'PRODUCT_DEFAULT';
                ui.contextFoundationStatus.textContent =
                  productField?.state === 'CARRIED' || productField?.state === 'CARRIED_PARTIAL'
                    ? 'VexLife foundation ' + (productField.fieldVersion ?? '')
                    : 'Foundation was not available';
                if (productField) {
                  const omittedFoundationParts = Number(productField.omittedModuleRefs?.length ?? 0);
                  contextLine(
                    ui.contextFoundationBody,
                    omittedFoundationParts > 0
                      ? "VexLife's built-in foundation was included, with " + omittedFoundationParts +
                        ' part' + (omittedFoundationParts === 1 ? '' : 's') + ' left out to make room.'
                      : "VexLife's built-in foundation was included.",
                    'Built-in foundation'
                  );
                  const liveField = state.foundationalField;
                  if (
                    liveField?.kernelText &&
                    liveField?.foundationalField?.bodySha256 === productField.bodySha256
                  ) {
                    contextEvidenceDisclosure(ui.contextFoundationBody, 'Show exact foundation text', liveField.kernelText);
                  } else {
                    contextLine(
                      ui.contextFoundationBody,
                      'VexLife kept the exact historical foundation identity for this reply instead of substituting the current text.',
                      'Exact evidence'
                    );
                  }
                }
                if (prompt.contextProfileRef) {
                  contextLine(
                    ui.contextFoundationBody,
                    profileMode === ${jsString(VexLifeContextProfile.FoundationMode.EXTEND_PRODUCT_DEFAULT.name)}
                      ? 'The profile used for this reply added its own foundation after the built-in VexLife foundation.'
                      : 'This reply used the built-in VexLife foundation without a separate profile extension.',
                    'Profile for this reply'
                  );
                }
                if (
                  prompt.profileFoundationState === 'CARRIED' &&
                  active &&
                  active.fingerprint === prompt.contextProfileFingerprint &&
                  active.definition.foundationText
                ) {
                  contextEvidenceDisclosure(
                    ui.contextFoundationBody,
                    'Show exact profile foundation text',
                    active.definition.foundationText
                  );
                } else if (prompt.profileFoundationState === 'CARRIED') {
                  contextLine(
                    ui.contextFoundationBody,
                    'The exact profile revision used for this reply is preserved in Vex Home; this screen will not replace it with the current revision.',
                    'Personal foundation'
                  );
                }

                // Structured Memory.
                ui.contextMemoryBody.replaceChildren();
                const rememberedCount = Number(state.memory?.rememberedCount ?? 0);
                ui.contextMemoryStatus.textContent =
                  acceptedSemanticMemoryUsed
                    ? (dailyMemoryUsed ? 'Daily + accepted semantic Memory used' : 'Accepted semantic Memory used')
                    : dailyMemoryUsed
                      ? rememberedCount + ' Daily Memory item' + (rememberedCount === 1 ? '' : 's') + ' used'
                      : 'Not used';
                if (anyPromptMemoryUsed) {
                  if (dailyMemoryUsed) {
                    contextLine(
                      ui.contextMemoryBody,
                      prompt.memoryContextFingerprint === state.memory?.memoryContextFingerprint
                        ? 'The Daily Memory component that shaped this reply is still the Daily Memory available now.'
                        : 'The Daily Memory component that shaped this reply is historical; VexLife will not relabel current Daily Memory as the old one.',
                      'Daily currentness'
                    );
                    contextEvidenceDisclosure(
                      ui.contextMemoryBody,
                      'Show exact Daily Memory reference',
                      String(prompt.memoryContextRef ?? '')
                    );
                  }
                  if (acceptedSemanticMemoryUsed) {
                    contextLine(
                      ui.contextMemoryBody,
                      dailyMemoryUsed
                        ? 'Current accepted-semantic Memory also shaped this reply under the same Memory budget. Exact statement refs already supplied by Daily were excluded from this second component.'
                        : 'Current accepted-semantic Memory shaped this reply even though no Daily Memory component entered the prompt.',
                      'Accepted semantic Memory'
                    );
                  }
                  contextLine(
                    ui.contextMemoryBody,
                    'This view follows the exact Memory components recorded by the prompt receipt; it does not infer aggregate Memory participation from Daily Memory alone.',
                    'Evidence'
                  );
                } else {
                  contextLine(ui.contextMemoryBody, state.memory?.display?.detail ?? 'No structured Memory component was used for this reply.');
                }

                // Working continuity / rollover.
                ui.contextContinuityBody.replaceChildren();
                ui.contextContinuityStatus.textContent =
                  prompt.workingContinuityIncluded ? 'Current position carried forward' : 'Not needed';
                if (prompt.workingContinuityIncluded) {
                  contextLine(
                    ui.contextContinuityBody,
                    'Vex carried forward the current position instead of keeping the whole recent conversation in view. Older Conversation is still kept in your archives.'
                  );
                  contextEvidenceDisclosure(
                    ui.contextContinuityBody,
                    'Show exact continuity reference',
                    String(prompt.workingContinuityContextRef ?? '')
                  );
                } else {
                  contextLine(ui.contextContinuityBody, 'The recent conversation did not need a separate continuity handoff for this reply.');
                }

                // Exact selected Conversation.
                ui.contextConversationBody.replaceChildren();
                const selectedRefs = new Set(prompt.selectedConversationEventRefs ?? []);
                const selectedMessages = messages.filter((message) => selectedRefs.has(message.eventRef));
                ui.contextConversationStatus.textContent =
                  selectedMessages.length === 0
                    ? 'No earlier messages used'
                    : selectedMessages.length + ' earlier message' + (selectedMessages.length === 1 ? '' : 's') + ' used';
                contextLine(
                  ui.contextConversationBody,
                  priorCompletedExchanges === 0
                    ? 'This reply started a new conversation, so there were no earlier messages to include.'
                    : 'There were ' + priorCompletedExchanges + ' earlier completed exchange' +
                      (priorCompletedExchanges === 1 ? '' : 's') + '; Vex used ' +
                      selectedMessages.length + ' earlier message' + (selectedMessages.length === 1 ? '' : 's') +
                      ' directly for this reply.',
                  'What came from Conversation'
                );
                for (const message of selectedMessages) {
                  const card = document.createElement('div');
                  card.className = 'context-exact-message';
                  card.dataset.role = message.role;

                  const header = document.createElement('div');
                  header.className = 'context-exact-message-head';
                  const meta = document.createElement('small');
                  meta.textContent = message.role === 'human' ? 'Human · exact Conversation' : 'Vex · exact Conversation';
                  header.append(meta);

                  const messageRef = String(message.eventRef ?? '');
                  const content = String(message.content ?? '');
                  const collapsible =
                    content.length > 480 ||
                    content.split('\n').length > 8;
                  const collapsed =
                    collapsible &&
                    state.contextConversationCollapsedByEventRef.get(messageRef) === true;

                  if (collapsible) {
                    const toggle = document.createElement('button');
                    toggle.type = 'button';
                    toggle.className = 'context-message-toggle';
                    toggle.textContent = collapsed ? 'Show message' : 'Collapse message';
                    toggle.setAttribute('aria-expanded', String(!collapsed));
                    toggle.addEventListener('click', () => {
                      state.contextConversationCollapsedByEventRef.set(messageRef, !collapsed);
                      renderVexContext();
                    });
                    header.append(toggle);
                  }

                  const body = document.createElement('p');
                  body.textContent = content;
                  body.hidden = collapsed;
                  card.dataset.collapsed = String(collapsed);
                  card.append(header, body);
                  ui.contextConversationBody.append(card);
                }

                // Browser / reached context: exact historical content is intentionally not reconstructed.
                ui.contextBrowserBody.replaceChildren();
                ui.contextBrowserStatus.textContent = prompt.browserContextIncluded ? 'Used' : 'Not used';
                if (prompt.browserContextIncluded) {
                  contextLine(ui.contextBrowserBody, prompt.browserTitle || prompt.browserPageUrl || 'Page', 'Browser context');
                  contextLine(
                    ui.contextBrowserBody,
                    Number(prompt.browserContextCharacters ?? 0).toLocaleString() +
                      ' characters supplied · ' + (prompt.browserContextCapture ?? 'captured text'),
                    'Amount'
                  );
                  contextLine(
                    ui.contextBrowserBody,
                    'The completed-turn receipt keeps the hash and source identity, not a second raw copy of the page text.',
                    'Afterward'
                  );
                } else {
                  contextLine(ui.contextBrowserBody, 'No browser page text was supplied in this turn.');
                }
                const reaches = latestVex ? (state.reachesByResponse.get(String(latestVex.messageRef)) ?? []) : [];
                for (const reach of reaches) {
                  contextLine(
                    ui.contextBrowserBody,
                    String(reach.handle ?? '') + ':' + String(reach.action ?? '') +
                      (reach.target ? ' · ' + reach.target : '') +
                      ' · ' + String(reach.state ?? ''),
                    'Reached'
                  );
                }

                // The current request is not part of selected prior Conversation; show its exact
                // canonical event separately so the causal stack never skips the human cause.
                ui.contextRequestBody.replaceChildren();
                ui.contextRequestStatus.textContent = latestHuman ? 'Your exact message' : 'Unavailable';
                if (latestHuman) {
                  contextExactText(ui.contextRequestBody, latestHuman.content);
                } else {
                  contextLine(ui.contextRequestBody, 'The current request is not available in the Conversation projection.');
                }

                // Exact provider component order remains available, but human meaning is primary.
                const componentLabels = Object.freeze({
                  'foundational-field': 'VexLife foundation',
                  'memory-current-context': 'Daily Memory',
                  'memory-current-accepted-semantic': 'Accepted semantic Memory',
                  'capability-frame': 'Capability guide',
                  'current-state-envelope': 'Current VexLife state',
                  'capability-frame-current': 'Capabilities available now',
                  'person-context': 'Your settings',
                  'conversation-extent': 'Conversation extent',
                  'working-continuity': 'Conversation continuity',
                  'browser-context': 'Browser context',
                  'source-return-protocol': 'Older Conversation retrieval',
                  'current-request': 'Your message'
                });
                const placementLabels = Object.freeze({
                  SYSTEM_PREFIX: 'standing context',
                  LIVE_TAIL: 'current context',
                  CURRENT_REQUEST: 'your message'
                });
                const componentItems = (prompt.contextComponents ?? []).map((component) => {
                  const item = document.createElement('li');
                  const exactRef = component.componentRef.replace('component.vexlife-prod.prompt.', '');
                  const name = document.createElement('span');
                  name.textContent = componentLabels[exactRef] ?? exactRef.replaceAll('-', ' ');
                  name.title = component.componentRef;
                  const detail = document.createElement('span');
                  const placement =
                    placementLabels[component.placementClass] ??
                    component.placementClass.toLowerCase().replaceAll('_', ' ');
                  const arrival =
                    exactRef === 'person-context'
                      ? ' · read from Vex Home at Send · provided automatically · no reach required'
                      : '';
                  detail.textContent = ' · ' + placement + arrival;
                  detail.title =
                    component.stabilityClass + ' · ' + component.placementClass +
                    ' · provider message ' + component.providerMessageIndex;
                  item.append(name, detail);
                  return item;
                });
                const providerRequests =
                  responseKey
                    ? (state.providerRequestsByResponse.get(responseKey) ?? [])
                    : [];
                const modelViewItem = document.createElement('li');
                modelViewItem.className = 'context-model-view';
                const modelView = document.createElement('details');
                modelView.className = 'context-evidence-disclosure';
                const modelSummary = document.createElement('summary');
                modelSummary.textContent =
                  providerRequests.length
                    ? 'Model view · exact VexLife → runtime request' +
                      (providerRequests.length === 1 ? '' : 's') +
                      ' · this browser session'
                    : 'Model view · exact VexLife → runtime request unavailable after reload/recovery';
                modelView.append(modelSummary);
                if (providerRequests.length) {
                  for (const request of providerRequests) {
                    const stage = document.createElement('div');
                    stage.className = 'context-model-stage';
                    const label = document.createElement('small');
                    label.textContent =
                      'Stage ' + String(request.stageIndex) +
                      ' · SHA-256 ' + String(request.requestBodySha256);
                    const pre = document.createElement('pre');
                    let visible = String(request.requestBody ?? '');
                    try {
                      visible = JSON.stringify(JSON.parse(visible), null, 2);
                    } catch {}
                    pre.textContent = visible;
                    stage.append(label, pre);
                    modelView.append(stage);
                  }
                  const note = document.createElement('p');
                  note.className = 'context-empty';
                  note.textContent =
                    'These are the exact JSON requests VexLife sent to the local model runtime. ' +
                    'They are kept only in this browser session. This is not the post-chat-template/tokenized model input; ' +
                    'the selected runtime may transform messages/tools before inference.';
                  modelView.append(note);
                } else {
                  const note = document.createElement('p');
                  note.className = 'context-empty';
                  note.textContent =
                    state.context?.finalProviderRequestBodySha256
                      ? 'VexLife kept only the final request hash: ' +
                        String(state.context.finalProviderRequestBodySha256) +
                        '. It will not reconstruct old provider text from current sources.'
                      : 'No exact live provider request is available for this completed turn.';
                  modelView.append(note);
                }
                modelViewItem.append(modelView);
                componentItems.push(modelViewItem);
                ui.contextComponentList.replaceChildren(...componentItems);

                // Session process and exact output.
                ui.contextProcessBody.replaceChildren();
                const reasoning = responseKey ? state.humanReasoningByResponse.get(responseKey) : null;
                ui.contextProcessStatus.textContent =
                  reasoning ? 'Visible in this session' : 'Not kept after the reply';
                if (reasoning) {
                  contextExactText(ui.contextProcessBody, reasoning);
                } else {
                  contextLine(
                    ui.contextProcessBody,
                    'Vex’s temporary working process was not saved after the reply. VexLife kept evidence that the turn ran without turning that process into durable Memory.'
                  );
                }
                if (reaches.length) {
                  contextLine(ui.contextProcessBody, reaches.length + ' external reach' + (reaches.length === 1 ? '' : 'es') + ' occurred during this turn.');
                }

                ui.contextOutputBody.replaceChildren();
                if (latestVex) {
                  contextExactText(ui.contextOutputBody, latestVex.content);
                } else {
                  contextLine(ui.contextOutputBody, 'The exact response is not available in the current Conversation projection.');
                }

                ui.contextAfterBody.replaceChildren();
                contextLine(ui.contextAfterBody, 'Your message and Vex’s reply stayed in your archives.', 'Conversation');
                contextLine(ui.contextAfterBody, 'VexLife kept a receipt of what shaped the reply, including order and source identities.', 'Evidence');
                contextLine(ui.contextAfterBody, 'Vex’s temporary working process was not saved as durable Context or Memory.', 'Process');
                contextLine(
                  ui.contextAfterBody,
                  active
                    ? active.definition.name + ' will shape your next message.'
                    : 'VexLife defaults will shape your next message.',
                  'Next turn'
                );
              };

              const loadContextProfiles = async () => {
                const response = await fetch(service.contextProfilesPath, {
                  headers: { accept: 'application/json' },
                  cache: 'no-store'
                });
                state.contextProfiles = await readJson(response);
                state.contextProfileDirty = false;
                renderVexContext();
                return state.contextProfiles;
              };

              const loadFoundationalField = async () => {
                const response = await fetch(service.foundationalFieldPath, {
                  headers: { accept: 'application/json' },
                  cache: 'no-store'
                });
                state.foundationalField = await readJson(response);
                renderVexContext();
                return state.foundationalField;
              };

              const contextProfileControl = async (controlRef, body = {}) => {
                if (state.contextProfileInFlight) return null;
                state.contextProfileInFlight = true;
                renderContextProfile();
                try {
                  const response = await effectFetch(service.contextProfilesControlPath, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json', accept: 'application/json' },
                    body: JSON.stringify({ controlRef, ...body })
                  });
                  const result = await readJson(response);
                  state.contextProfiles = result.profiles;
                  state.contextProfileDirty = false;
                  renderVexContext();
                  return result;
                } finally {
                  state.contextProfileInFlight = false;
                  renderContextProfile();
                }
              };

              const loadVexLabs = async (resetDraft = false) => {
                const response = await fetch(service.vexLabsPath, {
                  headers: { accept: 'application/json' },
                  cache: 'no-store'
                });
                state.vexLabs = await readJson(response);
                if (resetDraft || !state.vexLabsDraft) {
                  const defaults = state.vexLabs.defaults;
                  state.vexLabsDraft = {
                    inputTokenBudget: Number(defaults.inputTokenBudget),
                    maximumMemoryTokens: Number(defaults.maximumMemoryTokens),
                    memoryMode: String(state.vexLabs.memoryMode ?? defaults.memoryMode)
                  };
                }
                renderVexLabs();
                if (isVexLabs()) renderTimeline();
                return state.vexLabs;
              };

              const vexLabsControl = async (controlRef, resetDraft = false) => {
                if (state.vexLabsInFlight) return null;
                state.vexLabsInFlight = true;
                renderVexLabs();
                try {
                  const response = await effectFetch(service.vexLabsControlPath, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json', accept: 'application/json' },
                    body: JSON.stringify({ controlRef })
                  });
                  const result = await readJson(response);
                  state.vexLabs = result.projection;
                  state.vexLabsLastResult = null;
                  if (resetDraft) {
                    const defaults = state.vexLabs.defaults;
                    state.vexLabsDraft = {
                      inputTokenBudget: Number(defaults.inputTokenBudget),
                      maximumMemoryTokens: Number(defaults.maximumMemoryTokens),
                      memoryMode: String(defaults.memoryMode)
                    };
                  }
                  return result;
                } finally {
                  state.vexLabsInFlight = false;
                  renderVexLabs();
                  renderTimeline();
                  renderComposer();
                }
              };

              const saveVexLabsSupportedSettings = async () => {
                const draft = ensureVexLabsDraft();
                if (!vexLabsDraftMatchesLastTest()) {
                  notify('Test this exact Lab configuration before saving supported settings.');
                  return;
                }
                const active = currentContextProfile();
                if (!active) {
                  notify('VexLife has not loaded the current Context Profile yet.');
                  return;
                }
                const definition = {
                  ...active.definition,
                  schemaVersion: 'vexlife-prod.context-profile/v2',
                  inputTokenBudget: Number(draft.inputTokenBudget)
                };
                const saveAs = Boolean(active.productDefault);
                if (saveAs) {
                  const name = window.prompt('Save the tested context depth as a Context Profile', 'Vex Labs ' + Number(draft.inputTokenBudget).toLocaleString());
                  if (!name) return;
                  definition.name = name.trim();
                }
                try {
                  const result = await contextProfileControl(
                    saveAs ? 'SAVE_AS' : 'SAVE',
                    saveAs
                      ? { definition }
                      : { profileRef: active.profileRef, definition }
                  );
                  if (!result) return;
                  await loadVexLabs(true);
                  notify(
                    'Saved the tested active-context depth to “' +
                    result.profiles.active.definition.name +
                    '”. The Memory prompt depth remains Lab-only because Memory has no durable user Control yet.'
                  );
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'The tested context setting could not be saved.');
                }
              };

              const sendVexLabs = async () => {
                if (state.vexLabsInFlight) return;
                const content = ui.draft.value.trim();
                if (!content) return;
                const draft = ensureVexLabsDraft();
                state.vexLabsInFlight = true;
                state.vexLabsPending = content;
                ui.draft.value = '';
                renderTimeline();
                renderComposer();
                renderVexLabs();
                try {
                  const response = await effectFetch(service.vexLabsSendPath, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json', accept: 'application/json' },
                    body: JSON.stringify({
                      content,
                      inputTokenBudget: Number(draft.inputTokenBudget),
                      maximumMemoryTokens: Number(draft.maximumMemoryTokens),
                      memoryMode: String(draft.memoryMode)
                    })
                  });
                  const result = await readJson(response);
                  state.vexLabs = result.projection;
                  if (result.realConversationMutationPerformed || result.realMemoryMutationPerformed) {
                    throw new Error('The Lab reported a real-state mutation and was stopped.');
                  }
                  if (result.state === 'HELD_CONTEXT_BUDGET') {
                    state.vexLabsLastResult = null;
                    ui.draft.value = content;
                    sizeDraft();
                    const detail = vexLabsAdmissionText(result.admission);
                    notify(detail || 'This Lab turn needs more active context before Vex can be called.');
                  } else {
                    state.vexLabsLastResult = result;
                  }
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'The native Lab turn could not complete.');
                } finally {
                  state.vexLabsPending = null;
                  state.vexLabsInFlight = false;
                  renderVexLabs();
                  renderTimeline();
                  renderComposer();
                  ui.draft.focus();
                }
              };

              const saveContextProfile = async (saveAs) => {
                const definition = profileDefinitionFromForm();
                if (
                  definition.foundationMode === ${jsString(VexLifeContextProfile.FoundationMode.EXTEND_PRODUCT_DEFAULT.name)} &&
                  !definition.foundationText
                ) {
                  notify('Add foundation text before saving this profile.');
                  return;
                }
                if (saveAs) {
                  const name = window.prompt('Name this Context Profile', definition.name === 'VexLife Default' ? 'My Vex' : definition.name);
                  if (!name) return;
                  definition.name = name.trim();
                }
                try {
                  const active = currentContextProfile();
                  const result = await contextProfileControl(
                    saveAs ? 'SAVE_AS' : 'SAVE',
                    saveAs
                      ? { definition }
                      : { profileRef: active?.productDefault ? null : active?.profileRef, definition }
                  );
                  if (!result) return;
                  const profile = result.profiles.active;
                  notify(
                    'Saved “' + profile.definition.name +
                    '”. It will shape your next message.' +
                    (definition.foundationMode === ${jsString(VexLifeContextProfile.FoundationMode.EXTEND_PRODUCT_DEFAULT.name)}
                      ? ' Your extension follows the stable VexLife foundation.'
                      : '')
                  );
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'The Context Profile could not be saved.');
                }
              };

              const importContextProfile = async (file) => {
                if (!file) return;
                try {
                  const portable = JSON.parse(await file.text());
                  const name = String(portable.name ?? 'Imported profile');
                  const mode = String(portable.foundationMode ?? '');
                  if (!window.confirm(
                    'Import “' + name + '” as your own Context Profile?\n\nFoundation: ' +
                    mode.replaceAll('_', ' ').toLowerCase() +
                    '\n\nThe portable profile is configuration only; VexLife does not import the creator’s conversations, memories or reasoning.'
                  )) return;
                  const result = await contextProfileControl('IMPORT', { portable });
                  if (result) notify('Imported “' + result.profiles.active.definition.name + '”. It will apply to your next message.');
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'That file is not a valid Context Profile.');
                } finally {
                  ui.contextProfileImport.value = '';
                }
              };

              const chooseApp = (app) => {
                if (!apps.keys.includes(app)) return;
                state.app = app;
                try {
                  localStorage.setItem(apps.storageKey, app);
                } catch {}
                if (app === apps.archives || app === apps.vexContext) {
                  state.includePage = false;
                } else if (app === apps.sourceTemplate && !inSourceTemplate(state.humanPageUrl)) {
                  const home = ui.family.querySelector('.family-member')?.dataset.familyHome;
                  if (home) navigate(home);
                }
                renderApp();
                renderComposer();
              };

              const activeConversationController = () => {
                if (isVexLabs()) {
                  return {
                    kind: 'vex-labs',
                    inFlight: state.vexLabsInFlight,
                    submit: sendVexLabs,
                    pageContextAllowed: false,
                    voiceAllowed: false,
                    holdText: state.vexLabsInFlight
                      ? 'Vex Labs is running the real native model through the shared Conversation engine…'
                      : 'Vex Labs · scratch Conversation only. Your real Conversation and Memory are not written.'
                  };
                }
                if (isPrivate()) {
                  return {
                    kind: 'private',
                    inFlight: state.privateInFlight,
                    submit: sendPrivate,
                    pageContextAllowed: false,
                    voiceAllowed: true,
                    holdText: state.privateInFlight
                      ? 'Vex is replying privately…'
                      : 'Private · nothing here is kept unless you keep a note.'
                  };
                }
                return {
                  kind: 'canonical',
                  inFlight: state.sendInFlight,
                  submit: send,
                  pageContextAllowed: true,
                  voiceAllowed: true,
                  holdText: null
                };
              };

              const submitActiveConversation = () => {
                const controller = activeConversationController();
                void controller.submit();
              };

              const renderComposer = () => {
                const contextEligible = Boolean(state.contextTargetPageUrl);
                const contextReady = Boolean(state.browserContext);
                const archivesOpen = state.app === apps.archives;
                const controller = activeConversationController();
                const privateNow = controller.kind === 'private';
                const labsNow = controller.kind === 'vex-labs';
                const modelSwitching = Boolean(state.models?.switching);
                const modelSwitchHeld =
                  Boolean(state.models?.switchPending) &&
                  state.models?.lastOutcome?.controlRef === ${jsString(VexLifeModelsFamily.ControlUse)} &&
                  state.models?.lastOutcome?.state === 'FAILED';
                // Stop belongs to the canonical ConversationTurn Family only; private and Vex Labs
                // composers keep their own in-flight bookkeeping and never project canonical Stop.
                const canonicalNow = controller.kind === 'canonical';
                const turnDisplay = canonicalNow ? (state.conversationTurn?.display ?? null) : null;
                const turnControlRef = turnDisplay?.primaryControlRef ?? null;
                const turnActive = Boolean(turnDisplay?.active);
                const turnCancelling = Boolean(turnDisplay?.cancelling);
                const sendLawful = turnControlRef === conversationTurn.control.send;
                const stopLawful = turnControlRef === conversationTurn.control.stop;
                const canInclude = !archivesOpen && controller.pageContextAllowed && (contextEligible || contextReady);
                ui.contextShare.disabled = !canInclude || state.contextLoading;
                ui.contextShare.dataset.state = state.contextLoading
                  ? 'Loading'
                  : contextReady
                    ? 'Ready'
                    : contextEligible
                      ? 'Available'
                      : 'Unavailable';
                ui.contextShare.setAttribute('aria-pressed', String(state.includePage && contextReady));
                ui.contextShareLabel.textContent = state.contextLoading
                  ? 'Reading page…'
                  : state.includePage && contextReady
                    ? 'Page included'
                    : 'Include this page';
                ui.composerHold.textContent = turnActive
                  ? resolveText(turnDisplay.status)
                  : modelSwitching
                  ? resolveText(state.models.switching.message)
                  : modelSwitchHeld
                    ? resolveText(state.models.lastOutcome.message)
                    : controller.holdText
                      ? controller.holdText
                    : state.contextLoading
                    ? 'Reading the current page separately for Vex…'
                  : archivesOpen
                    ? "Vex doesn't see your archives yet."
                  : !canInclude
                    ? 'Vex page context is not available for this site yet.'
                    : contextEligible && !contextReady
                      ? 'Page context is available on demand.'
                      : state.pending && !state.sendInFlight
                        ? 'The prior Send outcome is unresolved. Send again to reconcile that exact attempt.'
                        : state.failedAttempts.length
                          ? 'A prior Send stopped. Its process is preserved here; use Try Again to form a new Send.'
                          : '';
                // Family projection owns which Control is lawful. sendInFlight is only transport
                // bookkeeping while the service is being re-observed.
                ui.send.hidden = canonicalNow && (state.sendInFlight || (turnDisplay !== null && !sendLawful));
                ui.stop.hidden = !(turnActive || turnCancelling);
                const sendLabel = sendLawful && turnDisplay?.primaryLabel
                  ? resolveText(turnDisplay.primaryLabel)
                  : t('workspace.button.conversation-send');
                ui.send.textContent = sendLabel;
                ui.send.setAttribute('aria-label', sendLabel);
                ui.send.title = sendLabel;
                const stopLabel = stopLawful && turnDisplay?.primaryLabel
                  ? resolveText(turnDisplay.primaryLabel)
                  : t('workspace.button.conversation-stop');
                ui.stop.textContent = stopLabel;
                ui.stop.setAttribute('aria-label', stopLabel);
                ui.stop.title = stopLabel;
                ui.stop.disabled = !stopLawful || state.sendCancelInFlight;
                ui.send.disabled = modelSwitching || modelSwitchHeld ||
                  controller.inFlight ||
                  (canonicalNow
                    ? (turnDisplay !== null && !sendLawful) || (!state.pending && !ui.draft.value.trim())
                    : !ui.draft.value.trim());
                ui.responseProgress.hidden = true;
                renderTicket();
                ui.contextShare.hidden = !controller.pageContextAllowed;
                ui.voiceInput.hidden = !controller.voiceAllowed;
                renderVoiceInput();
              };

              const setMeter = (meter, parts, text, label, known = true) => {
                if (!meter) return;
                meter.dataset.known = String(known);
                meter.setAttribute('aria-label', label);
                parts.forEach(([node, share]) => { if (node) node.style.width = Math.max(0, Math.min(1, share)) * 100 + '%'; });
                const textNode = meter.querySelector('.meter-text');
                if (textNode) textNode.textContent = text;
              };

              const formatBytes = (value) => {
                if (value === null || value === undefined || value === '') return 'Unknown';
                const bytes = Number(value);
                if (!Number.isFinite(bytes) || bytes < 0) return 'Unknown';
                const units = ['B', 'KB', 'MB', 'GB', 'TB'];
                let scaled = bytes;
                let unit = 0;
                while (scaled >= 1024 && unit < units.length - 1) {
                  scaled /= 1024;
                  unit += 1;
                }
                const digits = unit >= 3 ? 1 : (unit === 0 ? 0 : 1);
                return scaled.toFixed(digits) + ' ' + units[unit];
              };

              const renderSettings = () => {
                const prompt = state.context?.promptContext ?? null;
                if (prompt) {
                  const hard = Number(prompt.hardTokenLimit) || 0;
                  const used = Number(prompt.providerMaterializedInputTokenEstimate) || 0;
                  const reserved = Number(prompt.reservedOutputTokens) || 0;
                  const coreShare = hard > 0 ? Math.min(1, used / hard) : 0;
                  const reservedShare = hard > 0 ? Math.min(1 - coreShare, reserved / hard) : 0;
                  setMeter(
                    ui.contextWindow,
                    [[ui.contextCore, coreShare], [ui.contextUsed, reservedShare]],
                    hard > 0 ? used.toLocaleString() + ' input · ' + reserved.toLocaleString() + ' reserved · ' + Math.max(0, hard - used - reserved).toLocaleString() + ' remaining' : 'Context measured',
                    'Prompt context from the current canonical turn',
                    hard > 0
                  );
                  const priorCount = Array.isArray(prompt.selectedConversationEventRefs) ? prompt.selectedConversationEventRefs.length : 0;
                  const memoryCount = Array.isArray(prompt.memorySourceRefs) ? prompt.memorySourceRefs.length : 0;
                  const gaps = Number(prompt.privateConversationsNotRemembered) || 0;
                  const field = prompt.foundationalField ?? null;
                  const fieldParts = field && Array.isArray(field.carriedModules) ? field.carriedModules.length : 0;
                  const fieldLeftOut = field && Array.isArray(field.omittedModuleRefs) ? field.omittedModuleRefs.length : 0;
                  const fieldNote = !field
                    ? ''
                    : field.state === 'HELD'
                      ? ' The foundational field was not carried (' + String(field.heldReason ?? 'UNKNOWN').replaceAll('_', ' ').toLowerCase() + ').'
                      : ' Foundational field ' + String(field.fieldVersion ?? '') + ' was carried: ' + fieldParts + ' part' + (fieldParts === 1 ? '' : 's') +
                        (fieldLeftOut ? ', ' + fieldLeftOut + ' left out for room' : '') +
                        ', about ' + (Number(field.estimatedSystemMessageTokens) || 0).toLocaleString() + ' tokens.';
                  const recordExchanges = prompt.priorCompletedExchangeCount ?? null;
                  const recordNote = recordExchanges === null
                    ? ''
                    : ' of ' + (Number(recordExchanges) * 2).toLocaleString() + ' on record';
                  ui.contextNote.textContent =
                    'Canonical turn receipt: ' + priorCount + ' prior Conversation event' + (priorCount === 1 ? '' : 's') +
                    ' selected' + recordNote + '. Whole-history enumeration: ' + (prompt.wholeHistoryEventEnumerationPerformed ? 'yes' : 'no') + '.' +
                    (prompt.replyLanguageName ? ' Vex was told to reply in ' + prompt.replyLanguageName + '.' : '') +
                    (gaps ? ' It was told about ' + gaps + ' private conversation' + (gaps === 1 ? '' : 's') + ' it does not remember.' : '') +
                    fieldNote;
                  ui.memoryNote.textContent = prompt.memoryIncluded
                    ? 'VexLife Memory contributed through ' + memoryCount + ' source' + (memoryCount === 1 ? '' : 's') + ' to the current prompt.'
                    : 'No VexLife Memory packet was included in the current prompt.';
                } else {
                  setMeter(ui.contextWindow, [[ui.contextCore, 0], [ui.contextUsed, 0]], 'No turn receipt yet', 'No current prompt receipt', false);
                  ui.contextNote.textContent = 'Send a turn to inspect the current New-Beginnings prompt sequencing.';
                  ui.memoryNote.textContent = 'Memory inclusion is reported per completed canonical turn.';
                }

                const current = state.currentState;
                if (current) {
                  const totalMemory = Number(current.totalMemoryBytes);
                  const minimumMemory = Number(current.modelMinimumMemoryBytes);
                  const memoryKnown = Number.isFinite(totalMemory) && totalMemory > 0;
                  const minimumKnown = Number.isFinite(minimumMemory) && minimumMemory >= 0;
                  const minimumShare = memoryKnown && minimumKnown
                    ? Math.max(0, Math.min(1, minimumMemory / totalMemory))
                    : 0;
                  setMeter(
                    ui.memory,
                    [[ui.memoryUsed, minimumShare], [ui.memoryRemaining, memoryKnown ? 1 - minimumShare : 0]],
                    memoryKnown
                      ? formatBytes(totalMemory) + ' system · ' + (minimumKnown ? formatBytes(minimumMemory) + ' current-model minimum' : 'model minimum unknown')
                      : 'System memory unknown',
                    'Fresh host memory observation and current model requirement',
                    memoryKnown
                  );

                  const freeStorage = formatBytes(current.freeStorageBytes);
                  const modelTotal = formatBytes(current.modelArtifactTotalBytes);
                  const modelPresent = formatBytes(current.modelArtifactPresentBySizeBytes);
                  const modelLabel = current.activeModelRef ?? 'No model selected';
                  const modelStorage =
                    current.modelArtifactTotalBytes === null || current.modelArtifactTotalBytes === undefined
                      ? 'No selected-model artifact requirement.'
                      : 'Model files present by declared size: ' + modelPresent + ' of ' + modelTotal +
                        ' (' + String(current.modelArtifactState ?? 'UNKNOWN').replaceAll('_', ' ').toLowerCase() + '). ' +
                        'Exact hashes are not rechecked in this awareness view.';
                  ui.resourceNote.textContent =
                    'Storage available: ' + freeStorage +
                    '. Model: ' + modelLabel + '. ' + modelStorage + ' ' +
                    'Runtime binding: ' + String(current.runtimeState ?? 'UNKNOWN').replaceAll('_', ' ').toLowerCase() +
                    '; currentness: ' + String(current.runtimeCurrentness ?? 'UNKNOWN').replaceAll('_', ' ').toLowerCase() + '.';
                  ui.homeCurrentState.textContent =
                    'Current Home: ' + String(current.homePath ?? 'Unknown') +
                    ' · ' + String(current.homeDisposition ?? 'UNKNOWN').replaceAll('_', ' ').toLowerCase() +
                    ' · ' + String(current.homeRef ?? 'Unknown') + '.';
                } else {
                  setMeter(ui.memory, [[ui.memoryUsed, 0], [ui.memoryRemaining, 0]], 'Not measured', 'Current machine state is unavailable', false);
                  ui.resourceNote.textContent = 'Current machine and model resource truth is unavailable.';
                  ui.homeCurrentState.textContent = 'Current Home has not been observed yet.';
                }

                ui.thoughtVisibility.setAttribute('aria-checked', String(state.thoughtsVisible));
                ui.lowMemoryAlert.setAttribute('aria-checked', 'false');
                ui.lowMemoryAlert.disabled = true;
                ui.alertThreshold.textContent = 'not connected';
              };

              const renderCompanionHead = () => {
                const settings = state.view === 'settings';
                const labs = isVexLabs();
                ui.companionTitle.textContent = settings ? 'Settings' : labs ? 'Vex Labs Test' : 'Converse with Vex';
                ui.companionMode.hidden = settings;
                ui.companionMode.value = state.companionMode;
                ui.companionStatus.hidden = settings;
                ui.viewSwitch.textContent = settings ? 'Return' : 'Settings';
                ui.viewSwitch.setAttribute('aria-expanded', String(settings));
                ui.conversation.hidden = settings;
                ui.settings.hidden = !settings;
                ui.privateMode.hidden = labs;
                ui.visionControl.hidden = labs;
                const region = ui.companionTitle.closest('.region-companion');
                if (region) region.dataset.companionMode = state.companionMode;
              };

              const render = () => {
                renderCompanionHead();
                renderReleaseAlerts();
                renderVision();
                renderTimeline();
                renderComposer();
                renderVoiceInput();
                renderSettings();
                renderLocalAsr();
                renderMemory();
              };

              const loadSourceCurrentness = async () => {
                const response = await fetch(service.sourceCurrentnessPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.currentness = await readJson(response);
                root.dataset.vexServiceSourceSha = String(state.currentness.sourceSha ?? '');
                root.dataset.vexServicePid = String(state.currentness.processPid ?? '');
                setCompanionStatus('Ready', 'Ready', ui.companionStatusModel.textContent);
              };

              const loadCurrentState = async () => {
                const response = await fetch(service.currentStatePath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.currentState = await readJson(response);
                root.dataset.vexCurrentStateObservedAt = String(state.currentState?.observedAt ?? '');
                renderSettings();
                return state.currentState;
              };

              // Keyed words of the workspace scope, in the person's language. Until they arrive an
              // element shows its key, never an English string written here (localization law).
              const t = (key) => {
                const words = state.workspaceStrings?.[key];
                return typeof words === 'string' ? words : key;
              };

              // Resolves a Family-authored VexLifeText {key, args, gib, parts} in the person's language.
              const resolveText = (text) => {
                if (!text || typeof text.key !== 'string') return '';
                const args = text.args || {};
                const gib = text.gib || {};
                const parts = text.parts || {};
                return t(text.key).replace(/\{([a-zA-Z0-9]+)\}/gu, (whole, name) => {
                  if (name in args) {
                    // Formatting is presentation: an ISO instant is shown in the person's locale.
                    const value = String(args[name]);
                    return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/u.test(value)
                      ? new Date(value).toLocaleString(document.documentElement.lang || undefined)
                      : value;
                  }
                  if (name in parts) return resolveText(parts[name]);
                  if (name in gib) return (Number(gib[name]) / 1073741824).toFixed(1);
                  return whole;
                });
              };

              const applyStaticText = () => {
                for (const element of root.querySelectorAll('[data-vex-text]')) {
                  element.textContent = t(element.getAttribute('data-vex-text'));
                }
              };

              const loadWorkspaceStrings = async () => {
                const [workspaceResponse, releaseAlertsResponse] = await Promise.all([
                  fetch(service.workspaceStringsPath, { headers: { accept: 'application/json' }, cache: 'no-store' }),
                  fetch(service.releaseAlertsStringsPath, { headers: { accept: 'application/json' }, cache: 'no-store' })
                ]);
                const workspace = await readJson(workspaceResponse);
                const releaseAlerts = await readJson(releaseAlertsResponse);
                state.workspaceStrings = {
                  ...(workspace?.strings ?? {}),
                  ...(releaseAlerts?.strings ?? {})
                };
                // The page is written in the screen language (not the reply language): it names the
                // page's lang for assistive technology and for locale formatting.
                if (workspace?.language) document.documentElement.lang = workspace.language;
                applyStaticText();
                renderLanguage();
                renderDiagnostics();
                renderReleaseAlerts();
                renderQuickActions();
                renderLocalAsr();
                renderVoiceInput();
              };

              // The presentation reports only what it observed itself (what it showed, what it could
              // prove about the visible page). It never reports a Conversation or runtime outcome:
              // those facts belong to their owners. A failed report changes nothing.
              const reportFact = (fact, refs = {}) => {
                try {
                  void effectFetch(service.chronologyPath, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json' },
                    body: JSON.stringify({ fact, refs })
                  }).catch(() => {});
                } catch {}
              };

              const pageRefs = (pageUrl) => {
                try {
                  const page = new URL(pageUrl);
                  return { host: page.hostname, path: page.pathname };
                } catch {
                  return {};
                }
              };

              const renderDiagnostics = () => {
                if (!ui.diagnosticsStatus) return;
                const projection = state.diagnostics;
                if (!projection) {
                  ui.diagnosticsStatus.textContent = t('workspace.status.diagnostics-unavailable');
                  ui.diagnosticsCapture.textContent = '';
                  ui.diagnosticsRecords.textContent = '';
                  ui.diagnosticsEnhanced.disabled = true;
                  ui.diagnosticsClear.disabled = true;
                  return;
                }
                const display = projection.display;
                ui.diagnosticsStatus.textContent = resolveText(display.status);
                ui.diagnosticsCapture.textContent = resolveText(display.capture);
                ui.diagnosticsRecords.textContent = projection.chronologyRecords > 0 && projection.oldestAt
                  ? t('workspace.label.diagnostics-records')
                      .replace('{count}', String(projection.chronologyRecords))
                      .replace('{since}', new Date(projection.oldestAt).toLocaleString(document.documentElement.lang || undefined))
                  : t('workspace.label.diagnostics-no-records');
                // Buttons carry the Family's Control refs; their words come from the Control's key.
                const enhanced = display.controls.find((control) => control.controlRef === ${jsString(VexLifeDiagnosticsFamily.ControlStartEnhanced)} || control.controlRef === ${jsString(VexLifeDiagnosticsFamily.ControlStopEnhanced)});
                const clear = display.controls.find((control) => control.controlRef === ${jsString(VexLifeDiagnosticsFamily.ControlClearHistory)});
                ui.diagnosticsEnhanced.hidden = !enhanced;
                if (enhanced) {
                  ui.diagnosticsEnhanced.dataset.controlRef = enhanced.controlRef;
                  ui.diagnosticsEnhanced.textContent = resolveText(enhanced.label);
                }
                ui.diagnosticsEnhanced.disabled = !enhanced || state.diagnosticsInFlight;
                ui.diagnosticsClear.hidden = !clear;
                if (clear) ui.diagnosticsClear.textContent = resolveText(clear.label);
                ui.diagnosticsClear.disabled = !clear || state.diagnosticsInFlight;
              };

              const loadDiagnostics = async () => {
                const response = await fetch(service.diagnosticsPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.diagnostics = await readJson(response);
                renderDiagnostics();
                return state.diagnostics;
              };

              const diagnosticsControl = async (controlRef) => {
                if (state.diagnosticsInFlight) return;
                state.diagnosticsInFlight = true;
                renderDiagnostics();
                try {
                  const response = await effectFetch(service.diagnosticsControlPath, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json', accept: 'application/json' },
                    body: JSON.stringify({ controlRef })
                  });
                  state.diagnostics = await readJson(response);
                } catch (error) {
                  notify(error instanceof Error ? error.message : t('workspace.status.diagnostics-unavailable'));
                } finally {
                  state.diagnosticsInFlight = false;
                  renderDiagnostics();
                }
              };

              const gibText = (bytes) => (Number(bytes) / 1073741824).toFixed(1);

              const servingModelLabel = () => {
                const serving = (state.models?.models || []).find((model) => model.serving);
                return serving?.label ?? '';
              };

              const selectedModel = () =>
                (state.models?.models || []).find((model) => model.targeted) ?? null;

              const modelHasControl = (model, controlRef) =>
                (model?.display?.controls || []).some((control) => control.controlRef === controlRef);

              const modelResourceFacts = (model) =>
                model.recommendedMemoryBytes == null
                  ? t('workspace.label.model-facts-memory-unestablished').replace('{size}', gibText(model.totalBytes))
                  : t('workspace.label.model-facts')
                      .replace('{size}', gibText(model.totalBytes))
                      .replace('{memory}', gibText(model.recommendedMemoryBytes))
                      .replace('{fit}', t(model.fitsThisMac === true ? 'workspace.label.model-fits' : model.fitsThisMac === false ? 'workspace.label.model-tight' : 'workspace.label.model-memory-unestablished'));

              // Settings -> Models. Selection, launch configuration and activation remain separate
              // Controls. The renderer never turns a selection/config edit into a runtime effect.
              const renderModels = () => {
                if (!ui.modelsList) return;
                ui.modelsList.textContent = '';
                const projection = state.models;
                if (!projection) return;

                const models = projection.models || [];
                const selected = selectedModel();
                const serving = models.find((model) => model.serving) ?? null;
                const modelImport = projection.modelImport ?? null;

                if (ui.modelImportCheck) ui.modelImportCheck.disabled = state.modelImportInFlight;
                if (ui.modelImportUrl) ui.modelImportUrl.disabled = state.modelImportInFlight;
                if (ui.modelImportResult) {
                  ui.modelImportResult.textContent = modelImport?.message ? resolveText(modelImport.message) : '';
                }
                if (ui.modelImportFacts) {
                  ui.modelImportFacts.textContent =
                    modelImport && modelImport.state !== 'HELD' && modelImport.totalBytes != null
                      ? t('workspace.label.model-import-facts')
                          .replace('{size}', gibText(modelImport.totalBytes))
                          .replace('{license}', modelImport.providerLicense ?? '')
                          .replace('{revision}', String(modelImport.providerRevision ?? '').slice(0, 12))
                      : '';
                }
                if (ui.modelImportAdd) {
                  ui.modelImportAdd.hidden = modelImport?.state !== 'READY';
                  ui.modelImportAdd.disabled = state.modelImportInFlight || modelImport?.state !== 'READY';
                  ui.modelImportAdd.dataset.importRef = modelImport?.importRef ?? '';
                }

                if (ui.modelsCurrentState) {
                  const stateParts = [
                    t('workspace.label.model-state-serving').replace('{model}', serving?.label ?? t('workspace.label.model-none'))
                  ];
                  if (selected && (!serving || selected.handle !== serving.handle)) {
                    stateParts.push(
                      t('workspace.label.model-state-selected')
                        .replace('{model}', selected.label)
                    );
                  }
                  if (projection.switching) {
                    stateParts.push(resolveText(projection.switching.message));
                  } else if (selected?.runtimeConfigurationPending) {
                    stateParts.push(t('workspace.status.model-runtime-configuration-pending'));
                  }
                  ui.modelsCurrentState.textContent = stateParts.join(' · ');
                }

                if (ui.modelChoice) {
                  const options = models.map((model) => {
                    const option = document.createElement('option');
                    option.value = model.handle;
                    const availability = resolveText(model.display.status);
                    option.textContent = model.label + ' · ' + availability;
                    option.selected = Boolean(model.targeted);
                    return option;
                  });
                  ui.modelChoice.replaceChildren(...options);
                  const anySelectableModel =
                    models.some((model) =>
                      modelHasControl(model, ${jsString(VexLifeModelsFamily.ControlSelect)})
                    );
                  ui.modelChoice.disabled = state.modelsInFlight || !anySelectableModel;
                  ui.modelChoice.onchange = () => {
                    const handle = ui.modelChoice.value;
                    const model = models.find((candidate) => candidate.handle === handle);
                    if (
                      model &&
                      modelHasControl(model, ${jsString(VexLifeModelsFamily.ControlSelect)})
                    ) {
                      void modelControl(${jsString(VexLifeModelsFamily.ControlSelect)}, handle);
                    }
                  };
                }

                if (selected) {
                  const status = selected.downloadedBytes != null
                    ? t('workspace.status.model-downloading')
                        .replace('{done}', gibText(selected.downloadedBytes))
                        .replace('{total}', gibText(selected.totalBytes))
                    : resolveText(selected.display.status);

                  if (ui.modelChoiceFacts) {
                    ui.modelChoiceFacts.textContent = status + ' · ' + modelResourceFacts(selected);
                  }

                  if (ui.modelChoiceNotes) {
                    ui.modelChoiceNotes.textContent = [
                      ...(selected.display.notes || []).map(resolveText),
                      selected.imported ? t('workspace.label.model-imported') : '',
                      selected.standing === 'CANDIDATE' && !selected.imported ? t('workspace.label.model-candidate') : ''
                    ].filter(Boolean).join(' · ');
                  }

                } else {
                  if (ui.modelChoiceFacts) ui.modelChoiceFacts.textContent = '';
                  if (ui.modelChoiceNotes) ui.modelChoiceNotes.textContent = '';
                }

                const storageModels =
                  models.filter((model) => model.presentBytes > 0 || model.imported);
                for (const model of storageModels) {
                  const row = document.createElement('div');
                  row.className = 'model-storage-row';

                  const copy = document.createElement('div');
                  copy.className = 'model-storage-copy';

                  const label = document.createElement('span');
                  label.className = 'model-storage-label';
                  label.textContent = model.label;

                  const detail = document.createElement('span');
                  detail.className = 'model-storage-detail';
                  detail.textContent = [
                    resolveText(model.display.status),
                    modelResourceFacts(model),
                    ...(model.display.notes || []).map(resolveText),
                    model.imported ? t('workspace.label.model-imported') : ''
                  ].filter(Boolean).join(' · ');

                  copy.append(label, detail);

                  const actions = document.createElement('div');
                  actions.className = 'model-storage-controls';

                  for (const control of model.display.controls || []) {
                    if (![
                      ${jsString(VexLifeModelsFamily.ControlDownload)},
                      ${jsString(VexLifeModelsFamily.ControlRedownload)},
                      ${jsString(VexLifeModelsFamily.ControlRemove)},
                      ${jsString(VexLifeModelsFamily.ControlForgetImportedModel)}
                    ].includes(control.controlRef)) continue;
                    const button = document.createElement('button');
                    button.type = 'button';
                    const forgetting =
                      control.controlRef === ${jsString(VexLifeModelsFamily.ControlForgetImportedModel)};
                    button.className = forgetting ? 'model-forget-button' : 'outline-button';
                    if (forgetting) {
                      button.textContent = '×';
                      button.title = resolveText(control.label);
                      button.setAttribute('aria-label', resolveText(control.label));
                    } else {
                      button.textContent = resolveText(control.label);
                    }
                    button.disabled = state.modelsInFlight || Boolean(projection.switching);
                    button.addEventListener('click', () => {
                      void modelControl(control.controlRef, model.handle);
                    });
                    actions.append(button);
                  }

                  row.append(copy, actions);
                  ui.modelsList.append(row);
                }

                if (ui.modelConfiguration) ui.modelConfiguration.hidden = !selected;
                if (selected) {
                  const display = selected.display;
                  if (ui.modelConfigurationRecommendation) {
                    ui.modelConfigurationRecommendation.textContent = [
                      resolveText(display.runtimeConfigurationSummary),
                      resolveText(display.runtimeConfigurationRecommendation)
                    ].filter(Boolean).join(' ');
                  }
                  if (ui.modelPromptCacheEntries) {
                    const choices = display.runtimeConfigurationChoices || [];
                    const options = choices.map((choice) => {
                      const option = document.createElement('option');
                      option.value = String(choice.promptCacheEntries);
                      option.textContent =
                        resolveText(choice.label) + ' · ' + resolveText(choice.standing);
                      option.selected = Boolean(choice.selected);
                      option.disabled = !choice.selected && !choice.controlRef;
                      return option;
                    });
                    ui.modelPromptCacheEntries.replaceChildren(...options);
                    ui.modelPromptCacheEntries.disabled =
                      state.modelsInFlight ||
                      !choices.some((choice) => Boolean(choice.controlRef));
                    ui.modelPromptCacheEntries.onchange = () => {
                      const value = Number(ui.modelPromptCacheEntries.value);
                      const choice =
                        choices.find((candidate) => candidate.promptCacheEntries === value);
                      if (!choice?.controlRef) return;
                      void modelControl(
                        choice.controlRef,
                        selected.handle,
                        choice.promptCacheEntries
                      );
                    };
                  }
                  if (ui.modelUseRecommended) {
                    const useRecommendedControl =
                      (display.controls || []).find(
                        (control) =>
                          control.controlRef ===
                          ${jsString(VexLifeModelsFamily.ControlUseRecommendedRuntimeConfiguration)}
                      ) ?? null;
                    ui.modelUseRecommended.hidden = !useRecommendedControl;
                    ui.modelUseRecommended.disabled =
                      state.modelsInFlight || !useRecommendedControl;
                    ui.modelUseRecommended.onclick = () => {
                      if (!useRecommendedControl) return;
                      void modelControl(
                        useRecommendedControl.controlRef,
                        selected.handle
                      );
                    };
                  }
                  if (ui.modelStart) {
                    const startControl =
                      (display.controls || []).find(
                        (control) =>
                          control.controlRef === ${jsString(VexLifeModelsFamily.ControlUse)}
                      ) ?? null;
                    ui.modelStart.disabled = state.modelsInFlight || !startControl;
                    ui.modelStart.onclick = () => {
                      if (!startControl) return;
                      void modelControl(startControl.controlRef, selected.handle);
                    };
                    ui.modelStart.textContent =
                      startControl
                        ? resolveText(startControl.label)
                        : t('workspace.button.model-start');
                  }
                }

                const lines = [];
                if (projection.switching) {
                  lines.push(resolveText(projection.switching.message));
                } else if (projection.lastOutcome) {
                  lines.push(resolveText(projection.lastOutcome.message));
                }
                if (projection.switchPending && !projection.switching) {
                  lines.push(t('workspace.status.models-switch-pending'));
                }
                if (projection.freeStorageBytes != null) {
                  lines.push(t('workspace.label.model-free-space').replace('{free}', gibText(projection.freeStorageBytes)));
                }
                if (ui.modelsNote) ui.modelsNote.textContent = lines.join(' ');
                if (projection.switching) {
                  setCompanionStatus(
                    'Checking',
                    resolveText(projection.switching.message),
                    servingModelLabel() || ui.companionStatusModel.textContent
                  );
                } else if (ui.companionStatus?.dataset.state !== 'Unavailable') {
                  setCompanionStatus(
                    'Ready',
                    'Ready',
                    servingModelLabel() || ui.companionStatusModel.textContent
                  );
                }
                renderComposer();
              };

              const loadModels = async () => {
                const response = await fetch(service.modelsPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.models = await readJson(response);
                renderModels();
                // Follow only active model work; nothing polls when the relationship is stable.
                if (state.models?.downloading || state.models?.switching) {
                  window.setTimeout(() => { void loadModels().catch(() => {}); }, 1000);
                }
                return state.models;
              };

              const checkModelImport = async () => {
                if (state.modelImportInFlight) return;
                const sourceUrl = String(ui.modelImportUrl?.value ?? '').trim();
                if (!sourceUrl) return;
                state.modelImportInFlight = true;
                renderModels();
                try {
                  const response = await effectFetch(service.modelsImportCheckPath, { method: 'POST', headers: { 'content-type': 'application/json', accept: 'application/json' }, body: JSON.stringify({ sourceUrl }) });
                  state.models = await readJson(response);
                } catch { notify(t('workspace.status.model-import-unavailable')); }
                finally { state.modelImportInFlight = false; renderModels(); }
              };

              const addModelImport = async () => {
                if (state.modelImportInFlight) return;
                const importRef = String(ui.modelImportAdd?.dataset.importRef ?? '');
                if (!importRef) return;
                state.modelImportInFlight = true;
                renderModels();
                try {
                  const response = await effectFetch(service.modelsImportAddPath, { method: 'POST', headers: { 'content-type': 'application/json', accept: 'application/json' }, body: JSON.stringify({ importRef }) });
                  state.models = await readJson(response);
                } catch { notify(t('workspace.status.model-import-unavailable')); }
                finally { state.modelImportInFlight = false; renderModels(); }
              };

              const modelControl = async (controlRef, handle, promptCacheEntries = null) => {
                if (state.modelsInFlight) return;
                if (controlRef === ${jsString(VexLifeModelsFamily.ControlRemove)} && !window.confirm(t('workspace.button.model-remove') + '?')) return;
                if (controlRef === ${jsString(VexLifeModelsFamily.ControlForgetImportedModel)} && !window.confirm(t('workspace.button.model-forget') + '?')) return;
                state.modelsInFlight = true;
                renderModels();
                try {
                  const body = { controlRef, handle };
                  if (promptCacheEntries !== null) body.promptCacheEntries = promptCacheEntries;
                  const response = await effectFetch(service.modelsControlPath, {
                    method: 'POST',
                    headers: { 'content-type': 'application/json', accept: 'application/json' },
                    body: JSON.stringify(body)
                  });
                  state.models = await readJson(response);
                  if (
                    controlRef === ${jsString(VexLifeModelsFamily.ControlUse)} ||
                    controlRef === ${jsString(VexLifeModelsFamily.ControlSelect)} ||
                    controlRef === ${jsString(VexLifeModelsFamily.ControlUseRecommendedRuntimeConfiguration)} ||
                    controlRef === ${jsString(VexLifeModelsFamily.ControlSetPromptCacheEntries)} ||
                    controlRef === ${jsString(VexLifeModelsFamily.ControlForgetImportedModel)}
                  ) {
                    const message = state.models?.switching?.message ?? state.models?.lastOutcome?.message;
                    if (message) notify(resolveText(message));
                  }
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'Models are not available.');
                } finally {
                  state.modelsInFlight = false;
                  renderModels();
                  if (state.models?.downloading || state.models?.switching) {
                    window.setTimeout(() => { void loadModels().catch(() => {}); }, 1000);
                  }
                }
              };

              const localAsrSelectionValue = (option) =>
                JSON.stringify([option.providerRef, option.modelRef]);

              const localAsrSpokenLanguageValue = (choice) =>
                JSON.stringify([choice.mode, choice.languageCode ?? null]);

              const localAsrSpokenLanguageLabel = (choice) => {
                if (choice.labelKey) {
                  return t(choice.labelKey)
                    .replace('{language}', String(choice.languageLabel ?? ''));
                }
                return String(choice.languageLabel ?? choice.languageCode ?? '');
              };

              const localAsrArtifactStatusText = (option) => {
                if (option.downloadedBytes != null) {
                  return t('workspace.status.model-downloading')
                    .replace('{done}', gibText(option.downloadedBytes))
                    .replace('{total}', gibText(option.modelBytes));
                }
                switch (option.artifactState) {
                  case ${jsString(VexLifeLocalAsrModelsFamily.ArtifactPresent)}:
                    return t('workspace.status.model-on-this-mac');
                  case ${jsString(VexLifeLocalAsrModelsFamily.ArtifactPartial)}:
                    return t('workspace.status.model-partly-downloaded');
                  case ${jsString(VexLifeLocalAsrModelsFamily.ArtifactDamaged)}:
                    return t('workspace.status.model-damaged');
                  default:
                    return t('workspace.status.model-not-downloaded');
                }
              };

              const renderLocalAsr = () => {
                if (!ui.localAsrSelection) return;
                const projection = state.localAsr;
                ui.localAsrSelection.replaceChildren();
                ui.localAsrModelsList?.replaceChildren();
                ui.localAsrSpokenLanguage?.replaceChildren();
                ui.localAsrComposerOutput?.replaceChildren();
                if (!projection) {
                  ui.localAsrSelection.disabled = true;
                  if (ui.localAsrSpokenLanguage) {
                    ui.localAsrSpokenLanguage.disabled = true;
                  }
                  if (ui.localAsrComposerOutput) {
                    ui.localAsrComposerOutput.disabled = true;
                  }
                  if (ui.localAsrStatus) {
                    ui.localAsrStatus.textContent = t('workspace.status.local-asr-unavailable');
                  }
                  observeVoiceInputCapability();
                  renderVoiceInput();
                  return;
                }

                for (const option of projection.options ?? []) {
                  const node = document.createElement('option');
                  node.value = localAsrSelectionValue(option);
                  node.textContent =
                    option.providerLabel + ' · ' + option.modelLabel + ' · ' +
                    localAsrArtifactStatusText(option);
                  node.selected =
                    option.providerRef === projection.selectedProviderRef &&
                    option.modelRef === projection.selectedModelRef;
                  node.disabled =
                    !node.selected &&
                    !(option.controls ?? []).some(
                      (control) =>
                        control.controlRef ===
                        ${jsString(VexLifeLocalAsrModelsFamily.ControlUse)}
                    );
                  ui.localAsrSelection.append(node);

                  if (ui.localAsrModelsList) {
                    const row = document.createElement('div');
                    row.className = 'model-row';
                    row.dataset.chosen = String(Boolean(option.selected));

                    const name = document.createElement('div');
                    name.className = 'model-name';
                    name.textContent = option.providerLabel + ' · ' + option.modelLabel;

                    const facts = document.createElement('div');
                    facts.className = 'model-facts';
                    facts.textContent =
                      localAsrArtifactStatusText(option) +
                      ' · ' +
                      formatBytes(option.modelBytes);

                    const notes = document.createElement('div');
                    notes.className = 'model-notes';
                    notes.textContent =
                      option.selected
                        ? t('workspace.label.model-chosen')
                        : '';

                    const controls = document.createElement('div');
                    controls.className = 'model-controls';
                    for (const control of option.controls ?? []) {
                      const button = document.createElement('button');
                      button.type = 'button';
                      button.className = 'outline-button';
                      button.textContent = t(control.labelKey);
                      button.disabled = state.localAsrInFlight || voiceInputSessionOwnsState();
                      button.addEventListener('click', () => {
                        void localAsrModelControl(control.controlRef, option.modelRef);
                      });
                      controls.append(button);
                    }

                    row.append(name, facts, notes, controls);
                    ui.localAsrModelsList.append(row);
                  }
                }

                if (ui.localAsrStorageNote) {
                  ui.localAsrStorageNote.textContent =
                    projection.freeStorageBytes == null
                      ? ''
                      : t('workspace.label.model-free-space')
                          .replace('{free}', gibText(projection.freeStorageBytes));
                }

                if (ui.localAsrSpokenLanguage) {
                  for (const choice of projection.spokenLanguageChoices ?? []) {
                    const node = document.createElement('option');
                    node.value = localAsrSpokenLanguageValue(choice);
                    node.textContent = localAsrSpokenLanguageLabel(choice);
                    node.selected = Boolean(choice.selected);
                    node.disabled = !choice.supportedBySelectedModel && !choice.selected;
                    ui.localAsrSpokenLanguage.append(node);
                  }
                }

                if (ui.localAsrComposerOutput) {
                  for (const choice of projection.composerOutputChoices ?? []) {
                    const node = document.createElement('option');
                    node.value = String(choice.mode);
                    node.textContent = t(choice.labelKey);
                    node.selected = Boolean(choice.selected);
                    node.disabled = !choice.supportedBySelectedModel && !choice.selected;
                    ui.localAsrComposerOutput.append(node);
                  }
                }

                const selected = selectedLocalAsrOption();
                ui.localAsrSelection.disabled =
                  state.localAsrInFlight ||
                  voiceInputSessionOwnsState() ||
                  !(projection.options ?? []).length;
                if (ui.localAsrSpokenLanguage) {
                  ui.localAsrSpokenLanguage.disabled =
                    state.localAsrInFlight ||
                    voiceInputSessionOwnsState() ||
                    !(projection.spokenLanguageChoices ?? []).length;
                }
                if (ui.localAsrComposerOutput) {
                  ui.localAsrComposerOutput.disabled =
                    state.localAsrInFlight ||
                    voiceInputSessionOwnsState() ||
                    !(projection.composerOutputChoices ?? []).length;
                }

                if (ui.localAsrStatus) {
                  if (!selected) {
                    ui.localAsrStatus.textContent = t('workspace.status.local-asr-unavailable');
                  } else if (selected.currentState === 'READY') {
                    ui.localAsrStatus.textContent =
                      t('workspace.status.local-asr-ready')
                        .replace('{provider}', selected.providerLabel)
                        .replace('{model}', selected.modelLabel)
                        .replace('{size}', formatBytes(selected.modelBytes));
                  } else {
                    const statusKey =
                      selected.currentDetailRef === 'PROVIDER_SOURCE_NOT_CURRENT'
                        ? 'workspace.status.local-asr-provider-not-current'
                        : selected.currentDetailRef === 'PROVIDER_EXECUTABLE_UNAVAILABLE'
                          ? 'workspace.status.local-asr-runtime-unavailable'
                          : selected.currentDetailRef === 'MODEL_UNAVAILABLE'
                            ? 'workspace.status.local-asr-model-unavailable'
                            : selected.currentDetailRef === 'MODEL_DIGEST_NOT_CURRENT'
                              ? 'workspace.status.local-asr-model-not-current'
                              : 'workspace.status.local-asr-unavailable';
                    ui.localAsrStatus.textContent = t(statusKey);
                  }
                }

                observeVoiceInputCapability();
                renderVoiceInput();
              };

              const loadLocalAsr = async () => {
                const response = await fetch(service.localAsrPath, {
                  headers: { accept: 'application/json' },
                  cache: 'no-store'
                });
                state.localAsr = await readJson(response);
                renderLocalAsr();
                if (
                  (state.localAsr?.options ?? []).some(
                    (option) => option.downloadedBytes != null
                  )
                ) {
                  window.setTimeout(() => {
                    void loadLocalAsr().catch(() => {});
                  }, 1000);
                }
                return state.localAsr;
              };

              const selectLocalAsr = async (providerRef, modelRef) => {
                if (state.localAsrInFlight) return;
                if (voiceInputSessionOwnsState()) {
                  if (dictation.desiredActive) {
                    await applyVoiceInputControl({
                      controlRef: voiceInput.control.stop,
                      abort: true
                    });
                  }
                  if (voiceInputSessionOwnsState()) return;
                }
                state.localAsrInFlight = true;
                renderLocalAsr();
                try {
                  const response = await effectFetch(service.localAsrPath, {
                    method: 'POST',
                    headers: {
                      'content-type': 'application/json',
                      accept: 'application/json'
                    },
                    body: JSON.stringify({ providerRef, modelRef })
                  });
                  state.localAsr = await readJson(response);
                  const selected = selectedLocalAsrOption();
                  notify(
                    selected
                      ? t('workspace.status.local-asr-selected')
                          .replace('{provider}', selected.providerLabel)
                          .replace('{model}', selected.modelLabel)
                      : t('workspace.status.local-asr-unavailable')
                  );
                } catch (error) {
                  notify(
                    error instanceof Error
                      ? error.message
                      : t('workspace.status.local-asr-unavailable')
                  );
                } finally {
                  state.localAsrInFlight = false;
                  renderLocalAsr();
                }
              };

              const localAsrModelControl = async (controlRef, modelRef) => {
                if (state.localAsrInFlight) return;
                if (
                  controlRef ===
                    ${jsString(VexLifeLocalAsrModelsFamily.ControlRemove)} &&
                  !window.confirm(t('workspace.button.model-remove') + '?')
                ) {
                  return;
                }
                if (voiceInputSessionOwnsState()) {
                  if (dictation.desiredActive) {
                    await applyVoiceInputControl({
                      controlRef: voiceInput.control.stop,
                      abort: true
                    });
                  }
                  if (voiceInputSessionOwnsState()) return;
                }
                state.localAsrInFlight = true;
                renderLocalAsr();
                try {
                  const response = await effectFetch(service.localAsrModelControlPath, {
                    method: 'POST',
                    headers: {
                      'content-type': 'application/json',
                      accept: 'application/json'
                    },
                    body: JSON.stringify({
                      controlRef: String(controlRef),
                      modelRef: String(modelRef)
                    })
                  });
                  state.localAsr = await readJson(response);
                  observeVoiceInputCapability();
                } catch (error) {
                  notify(
                    error instanceof Error
                      ? error.message
                      : t('workspace.status.model-control-not-lawful')
                  );
                } finally {
                  state.localAsrInFlight = false;
                  renderLocalAsr();
                  if (
                    (state.localAsr?.options ?? []).some(
                      (option) => option.downloadedBytes != null
                    )
                  ) {
                    window.setTimeout(() => {
                      void loadLocalAsr().catch(() => {});
                    }, 1000);
                  }
                }
              };

              const selectLocalAsrSpokenLanguage = async (mode, languageCode) => {
                if (state.localAsrInFlight) return;
                if (voiceInputSessionOwnsState()) {
                  if (dictation.desiredActive) {
                    await applyVoiceInputControl({
                      controlRef: voiceInput.control.stop,
                      abort: true
                    });
                  }
                  if (voiceInputSessionOwnsState()) return;
                }
                state.localAsrInFlight = true;
                renderLocalAsr();
                try {
                  const response = await effectFetch(service.localAsrSpokenLanguagePath, {
                    method: 'POST',
                    headers: {
                      'content-type': 'application/json',
                      accept: 'application/json'
                    },
                    body: JSON.stringify({
                      mode: String(mode),
                      languageCode: languageCode == null ? null : String(languageCode)
                    })
                  });
                  state.localAsr = await readJson(response);
                  observeVoiceInputCapability();
                } catch (error) {
                  notify(
                    error instanceof Error
                      ? error.message
                      : t('workspace.status.local-asr-unavailable')
                  );
                } finally {
                  state.localAsrInFlight = false;
                  renderLocalAsr();
                }
              };

              const selectLocalAsrComposerOutput = async (mode) => {
                if (state.localAsrInFlight) return;
                if (voiceInputSessionOwnsState()) {
                  if (dictation.desiredActive) {
                    await applyVoiceInputControl({
                      controlRef: voiceInput.control.stop,
                      abort: true
                    });
                  }
                  if (voiceInputSessionOwnsState()) return;
                }
                state.localAsrInFlight = true;
                renderLocalAsr();
                try {
                  const response = await effectFetch(service.localAsrComposerOutputPath, {
                    method: 'POST',
                    headers: {
                      'content-type': 'application/json',
                      accept: 'application/json'
                    },
                    body: JSON.stringify({ mode: String(mode) })
                  });
                  state.localAsr = await readJson(response);
                  observeVoiceInputCapability();
                } catch (error) {
                  notify(
                    error instanceof Error
                      ? error.message
                      : t('workspace.status.local-asr-unavailable')
                  );
                } finally {
                  state.localAsrInFlight = false;
                  renderLocalAsr();
                }
              };

              const loadMemory = async () => {
                const response = await fetch(service.memoryPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.memory = await readJson(response);
                renderMemory();
                renderVexContext();
                return state.memory;
              };

              const loadVision = async () => {
                const response = await fetch(service.visionPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.vision = await readJson(response);
                root.dataset.vexVisionState = String(state.vision?.state ?? 'HELD');
                root.dataset.vexVisionRouteReachable = String(state.vision?.routeReachable === true);
                renderVision();
                return state.vision;
              };

              const activateVision = async () => {
                if (state.visionInFlight) return;
                const controlRef = state.vision?.display?.primaryControlRef;
                if (!controlRef) {
                  state.view = 'conversation';
                  renderCompanionHead();
                  notify(String(state.vision?.display?.label ?? 'Vision · Held') + ' — ' + String(state.vision?.display?.detail ?? ''));
                  return;
                }

                const requestRef = 'request.vexlife-prod.vision.presentation.' + crypto.randomUUID().toLowerCase();
                state.view = 'conversation';
                state.visionInFlight = true;
                renderCompanionHead();
                renderVision();

                try {
                  const response = await effectFetch(service.visionControlPath, {
                    method: 'POST',
                    headers: { accept: 'application/json', 'content-type': 'application/json' },
                    body: JSON.stringify({ controlRef, requestRef })
                  });
                  const result = await response.json();
                  if (String(result?.requestRef ?? '') !== requestRef || !result?.vision) {
                    throw new Error('Vision result does not match the exact request.');
                  }
                  state.vision = result.vision;
                  if (result.outcome === ${jsString(VexLifeProdServiceContract.VisionControlNotLawfulNow)}) {
                    notify('Vision changed before VexLife acted. ' + result.vision.display.detail);
                  } else if (result.outcome === ${jsString(VexLifeProdServiceContract.VisionControlRuntimeRefused)}) {
                    notify(result.vision.display.label + ' — that request did not complete.');
                  } else {
                    notify(result.vision.display.label);
                  }
                } catch (error) {
                  notify(error instanceof Error ? error.message : 'Vision did not complete that request.');
                } finally {
                  state.visionInFlight = false;
                  await loadVision().catch(() => {
                    renderVision();
                  });
                }
              };

              const conversationRenderIdentity = (conversation) => {
                if (!conversation) return 'NONE';
                const messages = conversation.messages ?? [];
                return [
                  String(conversation.currentConversationHeadSha256 ?? 'NONE'),
                  String(conversation.abandonedAttemptCount ?? 0),
                  ...messages.map((message) => String(message.eventSha256 ?? ''))
                ].join(':');
              };

              const loadConversationTurn = async () => {
                const response = await fetch(service.conversationTurnPath, {
                  headers: { accept: 'application/json' },
                  cache: 'no-store'
                });
                state.conversationTurn = await readJson(response);
                renderComposer();
                return state.conversationTurn;
              };

              const loadConversation = async (renderNow = true) => {
                const response = await fetch(service.conversationPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                const priorIdentity = conversationRenderIdentity(state.conversation);
                const nextConversation = await readJson(response);
                const nextIdentity = conversationRenderIdentity(nextConversation);
                state.conversation = nextConversation;
                const messages = state.conversation?.messages ?? [];
                const latest = [...messages].reverse().find((message) => message.role === 'vex' && message.model);
                if (!state.conversationTurn?.display?.active) {
                  setCompanionStatus(
                    'Ready',
                    t('workspace.status.conversation-ready'),
                    servingModelLabel() || latest?.model || ''
                  );
                }
                if (renderNow && priorIdentity !== nextIdentity) {
                  renderTimeline();
                  if (state.app === apps.archives) renderArchives();
                  if (state.app === apps.vexContext) renderVexContext();
                }
              };

              const loadContext = async () => {
                const response = await fetch(service.contextPath, { headers: { accept: 'application/json' }, cache: 'no-store' });
                state.context = await readJson(response);
                renderSettings();
                renderVexContext();
                if (state.context?.state === service.contextHeldUnsupportedState) {
                  const schema = String(state.context.durablePromptReceiptSchema ?? 'unknown durable schema');
                  notify(
                    'This VexLife build cannot project the latest turn context (' +
                    schema +
                    '). Conversation, Models, diagnostics, and other independent surfaces remain available.'
                  );
                }
                return state.context;
              };

              const completeCanonicalSend = async (result, streamed, preservedProcess = null) => {
                if (streamed) followStreamSpeech(true);
                state.sendInFlight = false;
                state.sendCancelInFlight = false;
                state.sendCancelRequested = false;
                root.dataset.vexLastContextPath = String(result.contextPath ?? '');
                root.dataset.vexLastPromptReceiptRef = String(result.promptContext?.receiptRef ?? '');
                root.dataset.vexLastBrowserContextRef = String(result.promptContext?.browserContextRef ?? '');
                const responseKey = String(result.responseMessageRef ?? '');
                const completedProcess =
                  Array.isArray(preservedProcess)
                    ? preservedProcess
                    : state.liveProcessEntries.length
                      ? (freezeLiveProcess(), snapshotLiveProcess())
                      : [];
                if (responseKey && completedProcess.length) {
                  state.processEntriesByResponse.set(responseKey, completedProcess);
                  if (state.liveProcessManuallyToggled && state.liveProcessExpanded) {
                    state.expandedThoughts.add(responseKey);
                  }
                } else if (responseKey) {
                  state.processEntriesByResponse.delete(responseKey);
                }
                if (responseKey && Array.isArray(result.providerRequests) && result.providerRequests.length > 0) {
                  state.providerRequestsByResponse.set(responseKey, result.providerRequests);
                } else if (responseKey) {
                  state.providerRequestsByResponse.delete(responseKey);
                }
                const reasoning = result.humanReasoning;
                if (
                  reasoning?.state === 'PRESENT_EPHEMERAL' &&
                  typeof reasoning.text === 'string' &&
                  reasoning.text.length > 0
                ) {
                  state.humanReasoningByResponse.set(responseKey, reasoning.text);
                } else if (result.responseMessageRef) {
                  state.humanReasoningByResponse.delete(responseKey);
                  if (!state.processEntriesByResponse.has(responseKey)) state.expandedThoughts.delete(responseKey);
                }
                if (Array.isArray(result.capabilityUses) && result.capabilityUses.length > 0) {
                  state.reachesByResponse.set(String(result.responseMessageRef ?? ''), result.capabilityUses);
                }
                if (
                  responseKey &&
                  state.contextProcessRetention === 'PREVIOUS_TURN'
                ) {
                  for (const key of Array.from(state.humanReasoningByResponse.keys())) {
                    if (!key.startsWith('private.') && key !== responseKey) state.humanReasoningByResponse.delete(key);
                  }
                  for (const key of Array.from(state.providerRequestsByResponse.keys())) {
                    if (key !== responseKey) state.providerRequestsByResponse.delete(key);
                  }
                  for (const key of Array.from(state.processEntriesByResponse.keys())) {
                    if (key !== responseKey) state.processEntriesByResponse.delete(key);
                  }
                  for (const key of Array.from(state.reachesByResponse.keys())) {
                    if (key !== responseKey) state.reachesByResponse.delete(key);
                  }
                }
                state.liveReaches = [];

                // Canonical Conversation/Context own completion. Streaming presentation is only a
                // projection, so refresh from those durable owners before retiring the pending Send.
                try {
                  await Promise.all([loadConversation(false), loadConversationTurn(), loadContext()]);
                } catch (projectionError) {
                  notify(
                    'Turn completed, but the current projection could not be refreshed: ' +
                    (projectionError instanceof Error ? projectionError.message : 'refresh failed.')
                  );
                }

                state.pending = null;
                clearLiveProcessState();
                setCompanionStatus(
                  'Ready',
                  'Ready',
                  servingModelLabel() || result.model || ''
                );
                render();
              };

              const reconcileSendOutcome = async (pending) => {
                // Re-present the exact same idempotency key through the non-streaming contract.
                // The turn owner either returns an already-completed durable result, reports the
                // already-failed attempt, or performs the originally intended Send if the first
                // transport never reached it. It must never create a second model call for one
                // already-completed/failed attempt.
                const response = await effectFetch(service.sendPath, {
                  method: 'POST',
                  headers: { accept: 'application/json', 'content-type': 'application/json' },
                  body: JSON.stringify(pending)
                });
                return readJson(response);
              };

              const cancelSend = async () => {
                const display = state.conversationTurn?.display ?? null;
                const attemptRef = display?.targetAttemptRef ?? null;
                if (
                  state.sendCancelInFlight ||
                  display?.primaryControlRef !== conversationTurn.control.stop ||
                  !attemptRef
                ) return;

                state.sendCancelInFlight = true;
                setCompanionStatus(
                  'Checking',
                  resolveText(display.status),
                  servingModelLabel() || ''
                );
                renderComposer();
                try {
                  const response = await effectFetch(service.cancelPath, {
                    method: 'POST',
                    headers: { accept: 'application/json', 'content-type': 'application/json' },
                    body: JSON.stringify({ attemptRef })
                  });
                  const result = await readJson(response);
                  state.conversationTurn = result.turn ?? state.conversationTurn;
                  if (result.outcome === conversationTurn.outcome.realized) {
                    state.sendCancelRequested = true;
                  }
                  notify(
                    result.outcome === conversationTurn.outcome.realized
                      ? resolveText(result.turn?.display?.status)
                      : t('workspace.status.conversation-cancel-not-active')
                  );
                } catch (error) {
                  notify(
                    error instanceof Error
                      ? error.message
                      : t('workspace.status.conversation-cancel-unavailable')
                  );
                } finally {
                  state.sendCancelInFlight = false;
                  renderComposer();
                }
              };

              const send = async (newAttemptSeed = null) => {
                if (isPrivate()) {
                  await sendPrivate();
                  return;
                }
                if (state.sendInFlight) return;

                const retryingExactAttempt = state.pending !== null && newAttemptSeed === null;
                const content =
                  newAttemptSeed
                    ? String(newAttemptSeed.content ?? '').trim()
                    : retryingExactAttempt
                      ? state.pending.content
                      : ui.draft.value.trim();
                if (!content) return;

                const pending =
                  newAttemptSeed
                    ? Object.freeze({
                        idempotencyKey: 'send.' + crypto.randomUUID().toLowerCase(),
                        content,
                        browserContext: newAttemptSeed.browserContext ?? null
                      })
                    : state.pending ?? Object.freeze({
                        idempotencyKey: 'send.' + crypto.randomUUID().toLowerCase(),
                        content,
                        browserContext: state.includePage ? state.browserContext : null
                      });
                state.pending = pending;
                state.sendInFlight = true;
                state.sendCancelInFlight = false;
                state.sendCancelRequested = false;
                if (!retryingExactAttempt) {
                  // A genuinely new Send owns a new live process slot. Reconciliation of the same
                  // pending idempotency key must preserve the process already observed for it.
                  state.liveReaches = [];
                  state.liveProcessEntries = [];
                  state.liveProcessExpanded = true;
                  state.liveProcessManuallyToggled = false;
                  resetLiveProcessViewports();
                }
                state.streamPhase = 'CheckingRuntime';
                state.streamedContent = '';
                state.streamedReasoning = '';
                // Speech and the visual process projection intentionally share only these raw
                // stream accumulators/events; neither presentation consumer owns the other.
                speech.stream = { reply: 0, thought: 0 };
                if (speech.settings.enabled) audioContext();
                if (!retryingExactAttempt && newAttemptSeed === null) {
                  ui.draft.value = '';
                  sizeDraft();
                }
                setCompanionStatus(
                  'Checking',
                  retryingExactAttempt
                    ? 'Retrying exact Send'
                    : newAttemptSeed
                      ? 'Trying again'
                      : 'Checking Vex',
                  ''
                );
                render();

                try {
                  const response = await effectFetch(service.sendPath, {
                    method: 'POST',
                    headers: { accept: 'text/event-stream', 'content-type': 'application/json' },
                    body: JSON.stringify(pending)
                  });
                  // The service registers the exact Family BeginTurn before it emits SSE headers.
                  // Re-observe that outside execution truth before offering Stop.
                  await loadConversationTurn();
                  let result = null;
                  await readEventStream(response, (event) => {
                    if (event?.type === 'PHASE') {
                      state.streamPhase = String(event.phase ?? '');
                      if (state.streamPhase === 'CheckingRuntime') {
                        setCompanionStatus('Checking', 'Checking Vex', '');
                      } else if (state.streamPhase === 'Reconnecting') {
                        setCompanionStatus('Checking', 'Reconnecting Vex', '');
                      } else if (state.streamPhase === 'Generating') {
                        setCompanionStatus('Checking', 'Thinking', '');
                      }
                      updateLiveTurn();
                      return;
                    }
                    if (event?.type === 'REASONING_DELTA' && typeof event.delta === 'string') {
                      state.streamPhase = 'Reasoning';
                      state.streamedReasoning += event.delta;
                      appendProcessReasoning(event.delta);
                      setCompanionStatus('Checking', 'Thinking', '');
                      updateLiveTurn();
                      // Kokoro consumes the raw aggregate above, never the visual containers.
                      followStreamSpeech(false);
                      return;
                    }
                    if (event?.type === 'REACH' && event.reach) {
                      // A request Vex made (state absent) or what came back (state present). The
                      // returned event settles the newest open request of the same handle/action.
                      const use = event.reach;
                      let settled = false;
                      if (use.state) {
                        for (let index = state.liveReaches.length - 1; index >= 0; index -= 1) {
                          const open = state.liveReaches[index];
                          if (!open.state && open.handle === use.handle && open.action === use.action) {
                            state.liveReaches[index] = use;
                            settled = true;
                            break;
                          }
                        }
                      }
                      if (!settled) state.liveReaches.push(use);
                      projectProcessReach(use);
                      setCompanionStatus('Checking', reachText(use), '');
                      updateLiveTurn();
                      return;
                    }
                    if (event?.type === 'CONTENT_DELTA' && typeof event.delta === 'string') {
                      state.streamPhase = 'Responding';
                      state.streamedContent += event.delta;
                      freezeActiveProcessThought();
                      if (!state.liveProcessManuallyToggled) state.liveProcessExpanded = false;
                      setCompanionStatus('Checking', 'Responding', '');
                      updateLiveTurn();
                      // Final-answer speech remains on the same raw stream as T004.
                      followStreamSpeech(false);
                      return;
                    }
                    if (event?.type === 'FAILED') {
                      const failure = event.error ?? {};
                      const error = new Error(String(failure.message ?? 'Vex turn failed.'));
                      error.vexStatus = 409;
                      error.vexResponse = failure;
                      throw error;
                    }
                    if (event?.type === 'COMPLETED') result = event.response ?? null;
                  });
                  if (!result) throw new Error('Vex stream ended without a completed turn.');
                  await completeCanonicalSend(result, true);
                } catch (error) {
                  // Preserve what the human already saw until the canonical turn owner returns
                  // terminal truth. Presentation transport is not authority to erase process state.
                  freezeLiveProcess();
                  const interruptedProcess = snapshotLiveProcess();

                  let terminalError = error;
                  let status = Number(error?.vexStatus ?? 0);
                  let retryAllowed = error?.vexResponse?.retrySameSendAttemptAllowed;
                  let retireAttempt = status === 400 || retryAllowed === false;

                  if (!retireAttempt) {
                    setCompanionStatus('Checking', 'Reconciling Send', '');
                    render();
                    try {
                      const reconciled = await reconcileSendOutcome(pending);
                      await completeCanonicalSend(reconciled, false, interruptedProcess);
                      return;
                    } catch (reconciliationError) {
                      terminalError = reconciliationError;
                      status = Number(reconciliationError?.vexStatus ?? 0);
                      retryAllowed = reconciliationError?.vexResponse?.retrySameSendAttemptAllowed;
                      retireAttempt = status === 400 || retryAllowed === false;
                    }
                  }

                  state.sendInFlight = false;
                  state.sendCancelInFlight = false;
                  await loadConversationTurn().catch(() => {});
                  const cancelledByPerson = state.sendCancelRequested;
                  state.sendCancelRequested = false;
                  if (retireAttempt) {
                    const failure = terminalError?.vexResponse ?? {};
                    state.failedAttempts.push({
                      idempotencyKey: pending.idempotencyKey,
                      content: pending.content,
                      browserContext: pending.browserContext ?? null,
                      failureCode: String(failure.code ?? terminalError?.name ?? 'TURN_FAILED'),
                      exactNextSafeRoute: failure.exactNextSafeRoute ?? null,
                      processEntries: interruptedProcess,
                      cancelledByPerson
                    });
                    if (cancelledByPerson && !ui.draft.value.trim()) {
                      ui.draft.value = pending.content;
                    }
                    state.pending = null;
                    clearLiveProcessState();
                    setCompanionStatus(
                      cancelledByPerson ? 'Ready' : 'Unavailable',
                      cancelledByPerson
                        ? t('workspace.status.conversation-stopped')
                        : 'Send failed',
                      servingModelLabel() || ''
                    );
                  } else {
                    // Outcome is still unresolved: keep the same idempotency key and the process the
                    // human watched. A later exact retry/reconciliation must not look like a new Send.
                    state.pending = pending;
                    state.streamPhase = 'Reconnecting';
                    setCompanionStatus('Unavailable', 'Send outcome unresolved', '');
                    reportFact(
                      'SEND_OUTCOME_UNRESOLVED_SHOWN',
                      { reason: terminalError instanceof Error ? terminalError.name : 'unknown' }
                    );
                  }

                  sizeDraft();
                  render();
                  notify(
                    cancelledByPerson
                      ? t('workspace.description.conversation-stopped')
                      : terminalError instanceof Error
                        ? terminalError.message
                        : 'Send failed.'
                  );
                }
              };

              const sizeDraft = () => {
                ui.draft.style.height = 'auto';
                ui.draft.style.height = Math.min(ui.draft.scrollHeight + 2, 220) + 'px';
                renderComposer();
              };

              // Browser + Companion keep one allocation/collapse relationship across responsive
              // manifestations. Wide screens place the Seam left/right; narrow screens manifest the
              // same relationship top/bottom. Responsive orientation is renderer-owned and does not
              // create a second spatial state machine.
              const NARROW_BROWSER_DEFAULT_SHARE = 0.34;
              const narrowWorkspace = () => window.matchMedia('(max-width: 860px)').matches;
              const currentSplitShare = (narrow = narrowWorkspace()) =>
                state.allocation ??
                (narrow ? NARROW_BROWSER_DEFAULT_SHARE : browser.authoredBrowserShare);

              const applySplit = () => {
                const narrow = narrowWorkspace();
                // Preserve the lived narrow composition until the human explicitly resizes. Once an
                // allocation is chosen, the same value carries across both renderer orientations.
                const share = currentSplitShare(narrow);
                const percent = Math.round(share * 100);
                const browserCollapsed = state.collapsed === 'before';
                const companionCollapsed = state.collapsed === 'after';
                ui.frame.dataset.collapsed = state.collapsed ?? 'none';
                ui.frame.style.setProperty('--before', (share * 100).toFixed(2) + '%');
                ui.seam.setAttribute('aria-orientation', narrow ? 'horizontal' : 'vertical');
                ui.seam.setAttribute('aria-valuenow', String(percent));
                ui.seam.setAttribute(
                  'aria-valuetext',
                  browserCollapsed
                    ? 'Browser collapsed'
                    : companionCollapsed
                      ? 'Companion collapsed'
                      : 'Browser ' + percent + ' percent ' + (narrow ? 'height' : 'width')
                );
                const browserAction = (browserCollapsed ? 'Show' : 'Hide') + ' browser';
                const companionAction = (companionCollapsed ? 'Show' : 'Hide') + ' companion';
                ui.collapseBefore.setAttribute('aria-label', browserAction);
                ui.collapseBefore.title = browserAction;
                ui.collapseAfter.setAttribute('aria-label', companionAction);
                ui.collapseAfter.title = companionAction;
              };

              const setSplitFromPointer = (clientX, clientY) => {
                const rect = ui.frame.getBoundingClientRect();
                const narrow = narrowWorkspace();
                const span = narrow ? rect.height : rect.width;
                if (span <= 0) return;
                const position = narrow ? (clientY - rect.top) : (clientX - rect.left);
                state.allocation = Math.min(0.85, Math.max(0.15, position / span));
                state.collapsed = 'none';
                applySplit();
              };

              const showInspector = (title, facts, text) => {
                ui.inspectorTitle.textContent = title;
                ui.inspectorFacts.replaceChildren(...facts.flatMap(([term, value]) => {
                  const dt = document.createElement('dt');
                  dt.textContent = term;
                  const dd = document.createElement('dd');
                  dd.textContent = value;
                  return [dt, dd];
                }));
                ui.inspectorText.textContent = text;
                ui.inspector.showModal();
              };

              const openCurrentContext = () => {
                if (!state.browserContext) return;
                showInspector(
                  'Captured page',
                  [['Page', state.browserContext.pageUrl], ['Title', state.browserContext.title || 'None'], ['Read from', 'Separate context-provider rendering']],
                  state.browserContext.text
                );
              };

              loadThoughtVisibility();
              applyTheme();
              initializeQuickActions();
              renderQuickActions();
              applySplit();
              render();

              ui.addressForm.addEventListener('submit', (event) => {
                event.preventDefault();
                try {
                  navigate(ui.query.value);
                  ui.query.blur();
                  ui.addressError.hidden = true;
                } catch (error) {
                  ui.addressError.textContent = error instanceof Error ? error.message : 'Page could not be opened.';
                  ui.addressError.hidden = false;
                }
              });
              ui.query.addEventListener('input', () => { ui.addressError.hidden = true; });
              ui.reload.addEventListener('click', () => {
                try { navigate(ui.query.value); } catch (error) { notify(error.message); }
              });
              ui.appSelector.addEventListener('change', () => chooseApp(ui.appSelector.value));
              ui.replyLanguage.addEventListener('change', () => void chooseLanguage('replyLanguage', ui.replyLanguage.value));
              ui.screenLanguage.addEventListener('change', () => void chooseLanguage('screenLanguage', ui.screenLanguage.value));
              ui.privateMode.addEventListener('click', () => void togglePrivate());
              ui.privatePropose.addEventListener('click', () => {
                void privatePost(service.privateNoteProposePath, {}).catch((error) =>
                  notify(error instanceof Error ? error.message : 'Vex could not propose a note.'));
              });
              ui.privateKeep.addEventListener('click', () => {
                const text = state.privateMode?.proposedNoteOrNull;
                if (!text) return;
                void privatePost(service.privateNoteKeepPath, { text }).catch((error) =>
                  notify(error instanceof Error ? error.message : 'The note was not kept.'));
              });
              ui.privateDecline.addEventListener('click', () => {
                void privatePost(service.privateNoteDeclinePath, {}).catch(() => {});
              });
              ui.family.addEventListener('click', (event) => {
                const member = event.target.closest('.family-member');
                if (member?.dataset.familyHome) navigate(member.dataset.familyHome);
              });
              ui.archivesTimeline.addEventListener('click', (event) => {
                const text = event.target.closest('.archives-entry p');
                if (text) text.dataset.open = String(text.dataset.open !== 'true');
              });

              const markContextProfileDirty = () => {
                state.contextProfileDirty = true;
                renderContextProfile();
              };
              ui.contextProfileChoice.addEventListener('change', () => {
                const profileRef = ui.contextProfileChoice.value;
                const active = currentContextProfile();
                if (!profileRef || profileRef === active?.profileRef) return;
                if (state.contextProfileDirty && !window.confirm('Switch profiles and discard the unsaved changes on this screen?')) {
                  ui.contextProfileChoice.value = active?.profileRef ?? '';
                  return;
                }
                void contextProfileControl(
                  profileRef === 'profile.vexlife.context.product-default' ? 'RESTORE_PRODUCT_DEFAULT' : 'USE',
                  profileRef === 'profile.vexlife.context.product-default' ? {} : { profileRef }
                )
                  .then((result) => {
                    if (result) notify('Using “' + result.profiles.active.definition.name + '” for your next message.');
                  })
                  .catch((error) => notify(error instanceof Error ? error.message : 'That Context Profile could not be selected.'));
              });
              ui.contextProfileName.addEventListener('input', markContextProfileDirty);
              ui.contextProfileFoundationText.addEventListener('input', markContextProfileDirty);
              ui.contextProcessRetentionSelect.addEventListener('change', () => {
                state.contextProcessRetention =
                  ui.contextProcessRetentionSelect.value === 'PREVIOUS_TURN'
                    ? 'PREVIOUS_TURN'
                    : 'SESSION_ONLY';
                renderVexContext();
              });
              ui.contextProfileFoundationMode.addEventListener('change', () => {
                state.contextProfileDirty = true;
                ui.contextProfileFoundationField.hidden =
                  ui.contextProfileFoundationMode.value !== ${jsString(VexLifeContextProfile.FoundationMode.EXTEND_PRODUCT_DEFAULT.name)};
                renderContextProfile();
              });
              ui.contextProfileSave.addEventListener('click', () => void saveContextProfile(false));
              ui.contextProfileSaveAs.addEventListener('click', () => void saveContextProfile(true));
              ui.contextProfileRestore.addEventListener('click', () => {
                if (!window.confirm('Return to this VexLife version’s default context profile? Your saved profiles will stay in Vex Home.')) return;
                void contextProfileControl('RESTORE_PRODUCT_DEFAULT')
                  .then((result) => {
                    if (result) notify('Using VexLife defaults again. Your saved profiles were kept.');
                  })
                  .catch((error) => notify(error instanceof Error ? error.message : 'VexLife defaults could not be restored.'));
              });
              ui.contextProfileHome.addEventListener('click', () => {
                void effectFetch(service.contextProfileHomeRevealPath, {
                  method: 'POST',
                  headers: { accept: 'application/json' }
                })
                  .then(readJson)
                  .then((result) => {
                    if (result?.outcome !== 'REALIZED') notify('The profile files are in Vex Home, but this host could not open its file manager.');
                  })
                  .catch((error) => notify(error instanceof Error ? error.message : 'Vex Home could not be opened.'));
              });
              ui.contextProfileImport.addEventListener('change', () => {
                void importContextProfile(ui.contextProfileImport.files?.[0] ?? null);
              });

              ui.vexLabsTest.addEventListener('click', () => {
                state.companionMode = 'LAB';
                ui.companionMode.value = 'LAB';
                render();
                ui.vexLabsNote.textContent = 'Configuration armed for the next Lab message. Type what you want to ask Vex on the right.';
                ui.draft.focus();
              });
              ui.vexLabsSaveSupported.addEventListener('click', () => {
                void saveVexLabsSupportedSettings();
              });
              ui.vexLabsRestore.addEventListener('click', () => {
                void vexLabsControl('control.vexlife-prod.vex-labs.restore-defaults', true);
              });
              ui.vexLabsReload.addEventListener('click', () => {
                void vexLabsControl('control.vexlife-prod.vex-labs.reload-current-conversation', false);
              });
              ui.vexLabsClear.addEventListener('click', () => {
                void vexLabsControl('control.vexlife-prod.vex-labs.clear-conversation', false);
              });
              ui.vexLabsInputBudget.addEventListener('input', () => {
                const draft = ensureVexLabsDraft();
                const value = Number(ui.vexLabsInputBudget.value);
                if (Number.isFinite(value) && value > 0) draft.inputTokenBudget = Math.floor(value);
                renderVexLabs();
              });
              ui.vexLabsMemoryBudget.addEventListener('input', () => {
                const draft = ensureVexLabsDraft();
                const value = Number(ui.vexLabsMemoryBudget.value);
                if (Number.isFinite(value) && value >= 0) draft.maximumMemoryTokens = Math.floor(value);
                renderVexLabs();
              });
              ui.vexLabsMemoryMode.addEventListener('change', () => {
                const draft = ensureVexLabsDraft();
                draft.memoryMode = ui.vexLabsMemoryMode.value === 'NONE' ? 'NONE' : 'CURRENT';
                void vexLabsControl(
                  draft.memoryMode === 'NONE'
                    ? 'control.vexlife-prod.vex-labs.memory-none'
                    : 'control.vexlife-prod.vex-labs.memory-current',
                  false
                );
              });
              ui.vexLabsFields.addEventListener('click', (event) => {
                const edit = event.target.closest('.vex-labs-field-edit');
                if (!edit) return;
                const fieldRef = String(edit.dataset.fieldRef ?? '');
                const target =
                  fieldRef === vexLabsProfileBudgetFieldRef
                    ? ui.vexLabsInputBudget
                    : fieldRef === vexLabsMemoryBudgetFieldRef
                      ? ui.vexLabsMemoryBudget
                      : null;
                if (target) {
                  target.focus();
                  target.select();
                }
              });

              // Context is admitted only from the Product-owned context frame, only for the exact page
              // whose identity is currently proven (contextTargetPageUrl). A message for any other
              // page is stale or foreign; arrival alone never makes a page eligible.
              window.addEventListener('message', (event) => {
                if (event.source !== ui.contextFrame.contentWindow) return;
                const next = normalizeContext(event.data);
                if (!next || !state.contextTargetPageUrl || next.pageUrl !== state.contextTargetPageUrl) return;
                reportFact('BROWSER_CONTEXT_READY', { ...pageRefs(next.pageUrl), characters: String(next.text.length) });
                state.browserContext = next;
                state.contextLoading = false;
                state.includePage = true;
                renderFrameStatus('Vex page context is ready for the next turn.', 'ContextReady');
                renderComposer();
              });

              // Why an unexpected visible-frame load clears page identity: the visible page is
              // cross-origin, so this presentation cannot observe its new address. A load it did not
              // start means the person navigated inside the page; keeping the old address (and any
              // context captured for it) would let Vex be told about a page that is no longer shown.
              // VISIBLE PAGE LOOKS ELIGIBLE != PRODUCT HAS PROVEN CURRENT PAGE IDENTITY: identity is
              // re-earned only by a Product-owned navigate(...) today. Never infer it from appearance.
              ui.pageFrame.addEventListener('load', () => {
                if (state.humanFrameLoadExpected) {
                  state.humanFrameLoadExpected = false;
                  if (state.contextTargetPageUrl && !state.browserContext) {
                    renderFrameStatus('Page opened directly · Vex page context is available on demand.', 'Direct');
                  } else if (!state.contextTargetPageUrl) {
                    renderFrameStatus('Page opened directly · Vex page context is not available for this site yet.', 'Direct');
                  }
                } else {
                  reportFact('BROWSER_PAGE_UNLOCATED', {
                    previousHost: pageRefs(state.humanPageUrl).host ?? 'none',
                    contextWasReady: String(Boolean(state.browserContext)),
                    contextWasLoading: String(Boolean(state.contextLoading))
                  });
                  state.browserContext = null;
                  state.includePage = false;
                  state.humanPageUrl = null;
                  state.contextTargetPageUrl = null;
                  state.contextLoading = false;
                  ui.contextFrame.removeAttribute('src');
                  ui.query.value = '';
                  ui.query.placeholder = 'Current address unavailable · enter a web address';
                  renderFrameStatus(
                    'The page navigated inside the embedded web view. Its exact address is not observable from this web-hosted presentation, so the stale address and Vex page context were cleared.',
                    'Unlocated'
                  );
                }
                renderComposer();
              });

              ui.visionControl.addEventListener('click', () => {
                void activateVision();
              });

              loadSpeechSettings();
              if (speech.systemSupported) {
                window.speechSynthesis.addEventListener('voiceschanged', () => {
                  renderSpeechVoices();
                  renderSpeech();
                });
              }
              ui.speechSwitch.addEventListener('click', () => {
                speech.settings.enabled = !speech.settings.enabled;
                saveSpeechSettings();
                if (speech.settings.enabled) {
                  audioContext();
                  if (chosenVoice()?.group === 'natural') kokoroWorker().postMessage({ type: 'prepare' });
                } else {
                  stopSpeech();
                }
                renderSpeech();
              });
              ui.speechVoice.addEventListener('change', () => {
                speech.settings.voice = ui.speechVoice.value;
                saveSpeechSettings();
                if (chosenVoice()?.group === 'natural') {
                  audioContext();
                  kokoroWorker().postMessage({ type: 'prepare' });
                }
                renderSpeech();
              });
              ui.speechThoughts.addEventListener('change', () => {
                speech.settings.readThoughts = ui.speechThoughts.checked;
                saveSpeechSettings();
                renderSpeech();
              });
              ui.speechRate.addEventListener('input', () => {
                speech.settings.rate = Math.min(1.6, Math.max(0.7, Number(ui.speechRate.value) || 1));
                saveSpeechSettings();
                if (speech.kokoro.source) speech.kokoro.source.playbackRate.value = speech.settings.rate;
                renderSpeech();
              });
              ui.speechPlay.addEventListener('click', pauseOrResumeSpeech);
              ui.speechStop.addEventListener('click', stopSpeech);
              ui.speechPrevious.addEventListener('click', () => stepReading(-1));
              ui.speechNext.addEventListener('click', () => stepReading(1));
              ui.speechChoose.addEventListener('click', () => {
                speech.choosing = !speech.choosing;
                renderSpeech();
              });
              ui.timeline.addEventListener('click', (event) => {
                if (!speech.choosing) return;
                const item = event.target.closest('.message');
                if (!item?.dataset.eventRef) return;
                event.preventDefault();
                startReading(item.dataset.eventRef);
              });
              document.addEventListener('keydown', (event) => {
                if (event.key !== 'Escape') return;
                if (state.releaseAlertsOpen) {
                  state.releaseAlertsOpen = false;
                  renderReleaseAlerts();
                  renderQuickActions();
                } else if (speech.choosing) {
                  speech.choosing = false;
                  renderSpeech();
                } else if (speech.open) {
                  speech.open = false;
                  renderSpeech();
                } else if (state.languageOpen) {
                  state.languageOpen = false;
                  renderLanguage();
                } else if (state.quickActionsOpen) {
                  state.quickActionsOpen = false;
                  renderQuickActions();
                }
              });

              ui.companionMode.addEventListener('change', () => {
                const next = ui.companionMode.value === 'LAB' ? 'LAB' : 'CONVERSATION';
                if (next === state.companionMode) return;
                if (dictation.desiredActive) {
                  void applyVoiceInputControl({
                    controlRef: voiceInput.control.stop,
                    abort: true
                  });
                }
                state.companionMode = next;
                state.timelineFollowTail = true;
                if (next === 'LAB' && !state.vexLabs) {
                  void loadVexLabs().catch((error) =>
                    notify(error instanceof Error ? error.message : 'Vex Labs could not be loaded.')
                  );
                }
                render();
                ui.draft.focus();
              });

              ui.viewSwitch.addEventListener('click', () => {
                state.view = state.view === 'settings' ? 'conversation' : 'settings';
                renderCompanionHead();
                if (state.view === 'settings') {
                  renderSettings();
                  renderLocalAsr();
                  void loadCurrentState().catch(() => {});
                  void loadMemory().catch(() => {});
                  void loadLocalAsr().catch(() => {});
                }
              });

              ui.contextShare.addEventListener('click', () => {
                if (state.contextLoading) return;

                if (state.browserContext) {
                  state.includePage = !state.includePage;
                  renderComposer();
                  return;
                }

                if (!state.contextTargetPageUrl) return;
                const contextFrameUrl = contextFrameUrlFor(state.contextTargetPageUrl);
                if (!contextFrameUrl) return;

                state.contextLoading = true;
                state.includePage = false;
                reportFact('BROWSER_CONTEXT_REQUESTED', pageRefs(state.contextTargetPageUrl));
                ui.contextFrame.src = contextFrameUrl;
                renderFrameStatus('Reading the current page separately for Vex…', 'Loading');
                renderComposer();
              });

              ui.voiceInput?.addEventListener('click', () => {
                void applyVoiceInputControl();
              });

              ui.stop.addEventListener('click', () => {
                void cancelSend();
              });

              ui.composeForm.addEventListener('submit', (event) => {
                event.preventDefault();
                if (dictation.desiredActive) {
                  void applyVoiceInputControl({
                    controlRef: voiceInput.control.stop,
                    abort: true
                  });
                }
                submitActiveConversation();
              });

              ui.draft.addEventListener('keydown', (event) => {
                if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
                  event.preventDefault();
                  if (dictation.desiredActive) {
                    void applyVoiceInputControl({
                      controlRef: voiceInput.control.stop,
                      abort: true
                    });
                  }
                  submitActiveConversation();
                }
              });

              ui.draft.addEventListener('input', () => {
                if (dictation.desiredActive) {
                  void applyVoiceInputControl({
                    controlRef: voiceInput.control.stop,
                    abort: true
                  });
                }
                sizeDraft();
              });
              ui.timeline.addEventListener('scroll', () => {
                state.timelineScrollTop = ui.timeline.scrollTop;
                state.timelineFollowTail = isElementAtBottom(ui.timeline);
                ui.jumpLatest.hidden = state.timelineFollowTail;
              });
              ui.timeline.addEventListener('click', (event) => {
                const retryFailed = event.target.closest('.failed-attempt-retry');
                if (retryFailed) {
                  if (state.sendInFlight) return;
                  const key = String(retryFailed.dataset.failedAttemptKey ?? '');
                  const failed = state.failedAttempts.find((attempt) => attempt.idempotencyKey === key);
                  if (!failed) return;
                  void send({
                    content: failed.content,
                    // Try Again is one new Send, not a replay of the failed attempt. Re-read the
                    // current explicit browser-context choice so navigation/unlocation cannot
                    // smuggle stale page bytes into the successor attempt.
                    browserContext: state.includePage ? state.browserContext : null
                  });
                  return;
                }

                const liveToggle = event.target.closest('.live-process-toggle');
                if (liveToggle) {
                  state.liveProcessExpanded = !state.liveProcessExpanded;
                  state.liveProcessManuallyToggled = true;
                  updateLiveTurn();
                  return;
                }
                const toggle = event.target.closest('.thought-toggle');
                if (!toggle) return;
                const key = String(toggle.dataset.thoughtKey ?? '');
                if (
                  !key ||
                  (!state.humanReasoningByResponse.has(key) && !state.processEntriesByResponse.has(key))
                ) return;
                if (state.expandedThoughts.has(key)) state.expandedThoughts.delete(key);
                else state.expandedThoughts.add(key);
                renderTimeline();
              });
              ui.jumpLatest.addEventListener('click', () => {
                state.timelineFollowTail = true;
                applyTimelineViewport();
              });

              ui.ticketView.addEventListener('click', openCurrentContext);
              ui.inspectorClose.addEventListener('click', () => ui.inspector.close());

              ui.thoughtVisibility.addEventListener('click', () => {
                state.thoughtsVisible = !state.thoughtsVisible;
                saveThoughtVisibility();
                renderSettings();
                renderTimeline();
              });

              ui.collapseBefore.addEventListener('click', () => {
                state.collapsed = state.collapsed === 'before' ? 'none' : 'before';
                applySplit();
              });
              ui.collapseAfter.addEventListener('click', () => {
                state.collapsed = state.collapsed === 'after' ? 'none' : 'after';
                applySplit();
              });

              ui.seam.addEventListener('pointerdown', (event) => {
                if (event.target.closest('button')) return;
                ui.seam.setPointerCapture(event.pointerId);
                ui.frame.classList.add('resizing');
              });
              ui.seam.addEventListener('pointermove', (event) => {
                if (!ui.seam.hasPointerCapture(event.pointerId)) return;
                setSplitFromPointer(event.clientX, event.clientY);
              });
              ui.seam.addEventListener('pointerup', (event) => {
                if (ui.seam.hasPointerCapture(event.pointerId)) ui.seam.releasePointerCapture(event.pointerId);
                ui.frame.classList.remove('resizing');
              });
              ui.seam.addEventListener('pointercancel', () => ui.frame.classList.remove('resizing'));
              ui.seam.addEventListener('dblclick', () => {
                state.allocation = null;
                state.collapsed = 'none';
                applySplit();
              });
              ui.seam.addEventListener('keydown', (event) => {
                const narrow = narrowWorkspace();
                const current = currentSplitShare(narrow);
                const decrease = narrow ? 'ArrowUp' : 'ArrowLeft';
                const increase = narrow ? 'ArrowDown' : 'ArrowRight';
                if (event.key === decrease || event.key === increase) {
                  event.preventDefault();
                  state.allocation = Math.min(0.85, Math.max(0.15, current + (event.key === decrease ? -0.05 : 0.05)));
                  state.collapsed = 'none';
                  applySplit();
                } else if (event.key === 'Home' || event.key === 'Enter') {
                  event.preventDefault();
                  state.allocation = null;
                  state.collapsed = 'none';
                  applySplit();
                }
              });
              window.addEventListener('resize', applySplit);
              document.addEventListener('visibilitychange', () => {
                if (document.hidden && dictation.desiredActive) {
                  void applyVoiceInputControl({
                    controlRef: voiceInput.control.stop,
                    abort: true
                  });
                } else if (!document.hidden) {
                  void loadReleaseAlerts().catch(() => {});
                }
              });

              observeVoiceInputCapability();
              renderVoiceInput();
              // Local ASR currentness is independent from Conversation/Context/Release startup.
              // One bounded retry covers setup/service convergence without creating stable polling.
              void loadLocalAsr().catch(() => {
                window.setTimeout(() => {
                  void loadLocalAsr().catch(() => {
                    observeVoiceInputCapability();
                    renderVoiceInput();
                  });
                }, 1000);
              });

              void loadWorkspaceStrings().catch(() => applyStaticText());
              ui.modelImportCheck?.addEventListener('click', () => { void checkModelImport(); });
              ui.modelImportUrl?.addEventListener('keydown', (event) => { if (event.key === 'Enter') { event.preventDefault(); void checkModelImport(); } });
              ui.modelImportAdd?.addEventListener('click', () => { void addModelImport(); });


              ui.localAsrSelection?.addEventListener('change', () => {
                try {
                  const [providerRef, modelRef] =
                    JSON.parse(ui.localAsrSelection.value);
                  void selectLocalAsr(
                    String(providerRef),
                    String(modelRef)
                  );
                } catch {
                  notify(t('workspace.status.local-asr-unavailable'));
                }
              });
              ui.localAsrSpokenLanguage?.addEventListener('change', () => {
                try {
                  const [mode, languageCode] =
                    JSON.parse(ui.localAsrSpokenLanguage.value);
                  void selectLocalAsrSpokenLanguage(
                    String(mode),
                    languageCode == null ? null : String(languageCode)
                  );
                } catch {
                  notify(t('workspace.status.local-asr-unavailable'));
                }
              });
              ui.localAsrComposerOutput?.addEventListener('change', () => {
                void selectLocalAsrComposerOutput(
                  ui.localAsrComposerOutput.value
                );
              });

              ui.diagnosticsEnhanced?.addEventListener('click', () => {
                const controlRef = ui.diagnosticsEnhanced.dataset.controlRef;
                if (controlRef) void diagnosticsControl(controlRef);
              });
              ui.diagnosticsClear?.addEventListener('click', () => {
                if (window.confirm(t('workspace.description.diagnostics-clear-confirm'))) void diagnosticsControl('CLEAR_HISTORY');
              });

              void Promise.all([
                loadSourceCurrentness(),
                loadCurrentState(),
                loadConversationTurn(),
                loadConversation(),
                loadVision()
              ])
                .then(() => {
                  render();
                  navigate(browser.initialPageUrl);
                  renderApp();
                  void loadContext().catch((error) => {
                    state.context = null;
                    renderSettings();
                    renderVexContext();
                    notify(
                      error instanceof Error
                        ? 'Vex context is unavailable: ' + error.message
                        : 'Vex context is unavailable in this build.'
                    );
                  });
                  void loadContextProfiles().catch(() => {});
                  void loadVexLabs().catch((error) => {
                    state.vexLabs = null;
                    renderVexLabs();
                    notify(error instanceof Error ? 'Vex Labs is unavailable: ' + error.message : 'Vex Labs is unavailable in this build.');
                  });
                  void loadFoundationalField().catch(() => {});
                  void loadPrivate().catch(() => {});
                  void loadSettings().catch(() => {});
                  void loadMemory().catch(() => {});
                  void loadDiagnostics().catch(() => renderDiagnostics());
                  void loadModels().catch(() => {});
                  // Release truth is outside Product startup truth. Observe it only after the core
                  // presentation is already usable; its own freshness policy suppresses redundant
                  // remote checks on later visibility opportunities.
                  void loadReleaseAlerts().catch(() => {});
                  // Observed: the presentation rendered with current service data.
                  reportFact('PRESENTATION_ARRIVED', { presentation: 'workspace' });
                })
                .catch((error) => {
                  setCompanionStatus('Unavailable', 'Unavailable', '');
                  notify(error instanceof Error ? error.message : 'Local service is not ready.');
                });

              setInterval(() => {
                void loadConversationTurn().catch(() => {});
                if (!state.pending) void loadConversation().catch(() => {});
                if (!state.visionInFlight) void loadVision().catch(() => {});
                if (!state.privateInFlight) void loadPrivate().catch(() => {});
              }, 2500);
            })();
        """
    }

    private fun jsString(
        value: String,
    ): String =
        "\"" +
            value
                .replace(
                    "\\",
                    "\\\\",
                )
                .replace(
                    "\"",
                    "\\\"",
                )
                .replace(
                    "\n",
                    "\\n",
                ) +
            "\""
}

// [VXG RealForever]
