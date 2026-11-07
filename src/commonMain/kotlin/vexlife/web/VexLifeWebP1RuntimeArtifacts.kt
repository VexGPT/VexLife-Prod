package vexlife.web

import vexlife.browser.VexLifeProdBrowserContract
import vexlife.service.VexLifeProdServiceContract

/**
 * Product-specific runtime realization over the accepted generated P1 surface.
 *
 * The generic VexWeb generator remains transport-agnostic. This projection binds existing P1
 * coordinates/capabilities to the existing local service contract and isolated frame host.
 */
object VexLifeWebP1RuntimeArtifacts {
    fun generate(): VexWebArtifacts.Bundle {
        val base =
            VexWebArtifacts.generate(
                VexLifeWebP1.Surface,
            )

        return base.copy(
            css =
                base.css.trimEnd() +
                    "\n\n" +
                    VexLifeWebP1ExperienceStyle.render(),
            javascript =
                base.javascript.trimEnd() +
                    "\n\n" +
                    runtimeJavascript().trimIndent() +
                    "\n",
        )
    }

    private fun runtimeJavascript(): String {
        val browserCompanionSeam =
            VexLifeWebP1.BodyFrame.seams
                .filterIsInstance<VexWeb.Seam.Vertical>()
                .single {
                    it.ref ==
                        VexLifeWebP1.Refs.BrowserCompanionSeam
                }
        val browserCompanionAllocation =
            browserCompanionSeam.allocation as
                VexWeb.Parameter.Allocation.Ratio
        val authoredBrowserShare =
            browserCompanionAllocation.beforeParts
                .toDouble() /
                browserCompanionAllocation.totalParts
                    .toDouble()

        val admittedHosts =
            VexLifeProdBrowserContract.AdmittedHosts
                .joinToString(
                    separator = ",",
                    prefix = "[",
                    postfix = "]",
                ) {
                    jsString(
                        it,
                    )
                }

        return """
            // Generated VexLife-Prod P1 runtime projection. Do not edit by hand.
            (() => {
              const root = document.querySelector('[data-vex-screen="${jsSelectorValue(VexLifeWebP1.Surface.screenRef)}"]');
              if (!root) return;

              const refs = Object.freeze({
                brand: ${jsString(VexLifeWebP1.Brand.identity.ref)},
                vexSummon: ${jsString(VexLifeWebP1.VexSummon.identity.ref)},
                bodyFrame: ${jsString(VexLifeWebP1.Refs.BodyFrame.value)},
                browserRegion: ${jsString(VexLifeWebP1.Refs.BrowserRegion.value)},
                companionRegion: ${jsString(VexLifeWebP1.Refs.CompanionRegion.value)},
                browserCompanionHandle: ${jsString(VexLifeWebP1.BrowserCompanionHandle.identity.ref)},
                browserQuery: ${jsString(VexLifeWebP1.BrowserQuery.identity.ref)},
                appSelector: ${jsString(VexLifeWebP1.AppSelector.identity.ref)},
                quickActions: ${jsString(VexLifeWebP1.QuickActions.identity.ref)},
                browserFrameStatus: ${jsString(VexLifeWebP1.BrowserFrameStatus.identity.ref)},
                browserViewport: ${jsString(VexLifeWebP1.BrowserViewport.identity.ref)},
                companion: ${jsString(VexLifeWebP1.Companion.identity.ref)},
                companionHeader: ${jsString(VexLifeWebP1.Refs.CompanionHeader.value)},
                companionContent: ${jsString(VexLifeWebP1.Refs.CompanionContent.value)},
                companionStatus: ${jsString(VexLifeWebP1.CompanionStatus.identity.ref)},
                companionViewSwitch: ${jsString(VexLifeWebP1.CompanionViewSwitch.identity.ref)},
                conversation: ${jsString(VexLifeWebP1.Conversation.identity.ref)},
                conversationTimeline: ${jsString(VexLifeWebP1.ConversationTimeline.identity.ref)},
                conversationJumpLatest: ${jsString(VexLifeWebP1.ConversationJumpLatest.identity.ref)},
                responseProgress: ${jsString(VexLifeWebP1.ResponseProgress.identity.ref)},
                composer: ${jsString(VexLifeWebP1.Composer.identity.ref)},
                contextReceipt: ${jsString(VexLifeWebP1.ContextReceipt.identity.ref)},
                contextShare: ${jsString(VexLifeWebP1.ContextShare.identity.ref)},
                send: ${jsString(VexLifeWebP1.Send.identity.ref)},
                stop: ${jsString(VexLifeWebP1.Stop.identity.ref)},
                contextInspector: ${jsString(VexLifeWebP1.ContextInspector.identity.ref)},
                settings: ${jsString(VexLifeWebP1.Settings.identity.ref)},
                settingsContext: ${jsString(VexLifeWebP1.SettingsContext.identity.ref)},
                settingsResources: ${jsString(VexLifeWebP1.SettingsResources.identity.ref)},
                settingsHome: ${jsString(VexLifeWebP1.SettingsHome.identity.ref)},
                contextWindow: ${jsString(VexLifeWebP1.ContextWindow.identity.ref)},
                memoryResource: ${jsString(VexLifeWebP1.MemoryResource.identity.ref)},
                thoughtVisibility: ${jsString(VexLifeWebP1.ThoughtVisibility.identity.ref)},
                lowMemoryAlert: ${jsString(VexLifeWebP1.LowMemoryAlert.identity.ref)},
                homeDirectories: ${jsString(VexLifeWebP1.HomeDirectories.identity.ref)},
                homeAddDirectory: ${jsString(VexLifeWebP1.HomeAddDirectory.identity.ref)},
                continuityMode: ${jsString(VexLifeWebP1.ContinuityMode.identity.ref)},
                dreamSync: ${jsString(VexLifeWebP1.DreamSync.identity.ref)},
                cleanChat: ${jsString(VexLifeWebP1.CleanChat.identity.ref)}
              });
              const service = Object.freeze({
                sourceCurrentnessPath: ${jsString(VexLifeProdServiceContract.SourceCurrentnessPath)},
                conversationPath: ${jsString(VexLifeProdServiceContract.ConversationPath)},
                contextPath: ${jsString(VexLifeProdServiceContract.ContextPath)},
                sendPath: ${jsString(VexLifeProdServiceContract.ConversationSendPath)}
              });
              const browser = Object.freeze({
                authoredBrowserShare: $authoredBrowserShare,
                admittedHosts: $admittedHosts,
                frameHostSuffix: ${jsString(VexLifeProdBrowserContract.FrameHostSuffix)},
                frameHostPort: ${VexLifeProdBrowserContract.FrameHostPort},
                initialPageUrl: ${jsString(VexLifeProdBrowserContract.InitialPageUrl)},
                contextCharacterLimit: ${VexLifeProdBrowserContract.ContextCharacterLimit},
                maxTitleCharacters: ${VexLifeProdBrowserContract.MaxTitleCharacters},
                captureRendered: ${jsString(VexLifeProdBrowserContract.CaptureRendered)},
                messageType: 'vexlife.browser-context/v1'
              });

              const byRef = (ref) => root.querySelector('[data-vex-ref="' + ref + '"]');
              const generatedPart = (ref, part) => byRef(ref)?.querySelector('[data-vex-generated-part="' + part + '"]') ?? null;
              const brand = byRef(refs.brand);
              const vexSummonButton = generatedPart(refs.vexSummon, 'control');
              const bodyFrame = byRef(refs.bodyFrame);
              const browserRegion = byRef(refs.browserRegion);
              const companionRegion = byRef(refs.companionRegion);
              const browserCompanionHandle = byRef(refs.browserCompanionHandle);
              const browserQuery = generatedPart(refs.browserQuery, 'text-field');
              const appSelectorButton = generatedPart(refs.appSelector, 'control');
              const quickActions = byRef(refs.quickActions);
              const browserFrameStatus = byRef(refs.browserFrameStatus);
              const browserViewport = byRef(refs.browserViewport);
              const companion = byRef(refs.companion);
              const companionHeader = byRef(refs.companionHeader);
              const companionContent = byRef(refs.companionContent);
              const companionStatus = byRef(refs.companionStatus);
              const companionViewSwitchButton = generatedPart(refs.companionViewSwitch, 'control');
              const conversation = byRef(refs.conversation);
              const timeline = byRef(refs.conversationTimeline);
              const jumpLatestButton = generatedPart(refs.conversationJumpLatest, 'control');
              const responseProgress = byRef(refs.responseProgress);
              const composer = generatedPart(refs.composer, 'text-field');
              const contextReceipt = byRef(refs.contextReceipt);
              const contextShareButton = generatedPart(refs.contextShare, 'control');
              const sendButton = generatedPart(refs.send, 'control');
              const stopButton = generatedPart(refs.stop, 'control');
              const contextInspector = byRef(refs.contextInspector);
              const settings = byRef(refs.settings);
              const settingsContext = byRef(refs.settingsContext);
              const settingsResources = byRef(refs.settingsResources);
              const settingsHome = byRef(refs.settingsHome);
              const contextWindow = byRef(refs.contextWindow);
              const memoryResource = byRef(refs.memoryResource);
              const thoughtVisibilityButton = generatedPart(refs.thoughtVisibility, 'control');
              const lowMemoryAlertButton = generatedPart(refs.lowMemoryAlert, 'control');
              const homeDirectoriesButton = generatedPart(refs.homeDirectories, 'control');
              const homeAddDirectoryButton = generatedPart(refs.homeAddDirectory, 'control');
              const continuityModeButton = generatedPart(refs.continuityMode, 'control');
              const dreamSyncButton = generatedPart(refs.dreamSync, 'control');
              const cleanChatButton = generatedPart(refs.cleanChat, 'control');

              let currentBrowserContext = null;
              let includeBrowserContext = false;
              let activeCompanionView = 'conversation';
              let frame = null;
              let pendingSend = null;

              if (brand) {
                brand.setAttribute('aria-label', 'VexLife');
                const mark = document.createElement('span');
                mark.dataset.vexGeneratedPart = 'brand-mark';
                mark.textContent = 'ⓥ';
                const brandName = document.createElement('strong');
                brandName.dataset.vexGeneratedPart = 'brand-name';
                brandName.textContent = 'VexLife';
                brand.replaceChildren(mark, brandName);
              }
              if (vexSummonButton) {
                vexSummonButton.textContent = 'V';
                vexSummonButton.setAttribute('aria-label', 'Summon Vex');
                vexSummonButton.title = 'Summon Vex';
              }
              if (appSelectorButton) {
                appSelectorButton.textContent = 'Browser';
                appSelectorButton.setAttribute('aria-label', 'Current app: Browser');
                appSelectorButton.disabled = true;
              }
              if (quickActions) quickActions.setAttribute('aria-label', 'Quick actions');
              if (companionHeader) {
                companionHeader.setAttribute('aria-label', 'Vex companion');
                const title = document.createElement('h1');
                title.dataset.vexGeneratedPart = 'companion-title';
                title.textContent = 'Converse with Vex';
                companionHeader.prepend(title);
              }
              if (companionStatus) {
                companionStatus.textContent = 'Checking';
                companionStatus.dataset.vexReady = 'false';
              }
              if (companionViewSwitchButton) {
                companionViewSwitchButton.textContent = 'Settings';
                companionViewSwitchButton.setAttribute('aria-expanded', 'false');
              }
              if (browserQuery) {
                browserQuery.placeholder = 'Open a Vex page URL';
                browserQuery.rows = 1;
                browserQuery.setAttribute('aria-label', 'Vex browser URL');
                browserQuery.setAttribute('spellcheck', 'false');
              }
              if (timeline) {
                timeline.setAttribute('role', 'log');
                timeline.setAttribute('aria-live', 'polite');
                timeline.setAttribute('aria-relevant', 'additions text');
                timeline.setAttribute('aria-label', 'Conversation with Vex');
              }
              if (jumpLatestButton) {
                jumpLatestButton.textContent = 'New messages';
                jumpLatestButton.hidden = true;
              }
              if (responseProgress) {
                responseProgress.setAttribute('role', 'status');
                responseProgress.setAttribute('aria-live', 'polite');
              }
              if (composer) {
                composer.placeholder = 'Message Vex…';
                composer.setAttribute('aria-label', 'Message Vex');
              }
              if (contextShareButton) {
                contextShareButton.textContent = 'Include this page';
                contextShareButton.setAttribute('aria-pressed', 'false');
              }
              if (sendButton) {
                sendButton.textContent = 'Send';
                sendButton.setAttribute('aria-label', 'Send message to Vex');
              }
              if (settings) {
                settings.setAttribute('aria-label', 'Settings');
                settings.hidden = true;
              }
              if (settingsContext) settingsContext.setAttribute('aria-live', 'polite');
              const setHeldControl = (button, label) => {
                if (!button) return;
                button.textContent = label;
                button.disabled = true;
                button.dataset.vexHeld = 'true';
              };
              setHeldControl(stopButton, 'Stop');
              setHeldControl(thoughtVisibilityButton, 'Viewable thoughts');
              setHeldControl(lowMemoryAlertButton, 'Low memory alert');
              setHeldControl(homeDirectoriesButton, 'View accessible directories');
              setHeldControl(homeAddDirectoryButton, 'Add');
              setHeldControl(continuityModeButton, 'Continuity mode · Not connected');
              setHeldControl(dreamSyncButton, 'Dream sync · Not connected');
              setHeldControl(cleanChatButton, 'Start clean chat');
              if (browserCompanionHandle) {
                browserCompanionHandle.tabIndex = 0;
                browserCompanionHandle.setAttribute('role', 'separator');
                browserCompanionHandle.setAttribute('aria-label', 'Resize browser and Vex companion');
                browserCompanionHandle.setAttribute('aria-orientation', 'vertical');
                browserCompanionHandle.setAttribute('aria-valuemin', '0');
                browserCompanionHandle.setAttribute('aria-valuemax', '100');
                browserCompanionHandle.setAttribute(
                  'aria-keyshortcuts',
                  'ArrowLeft ArrowRight Home End Enter'
                );
              }

              const ensureSettingsHeading = (host, text) => {
                if (!host || host.querySelector('[data-vex-generated-part="settings-heading"]')) return;
                const heading = document.createElement('h2');
                heading.dataset.vexGeneratedPart = 'settings-heading';
                heading.textContent = text;
                host.prepend(heading);
              };
              ensureSettingsHeading(settingsContext, 'Context window');
              ensureSettingsHeading(settingsResources, 'Memory');
              ensureSettingsHeading(settingsHome, 'VexLife Home');

              const renderMeter = (host, used, total, label) => {
                if (!host) return;
                host.replaceChildren();
                const fill = document.createElement('span');
                fill.dataset.vexGeneratedPart = 'meter-fill';
                const safeTotal = Number(total) > 0 ? Number(total) : 0;
                const safeUsed = Math.max(0, Number(used) || 0);
                fill.style.width = safeTotal > 0 ? Math.min(100, (safeUsed / safeTotal) * 100) + '%' : '0%';
                const text = document.createElement('span');
                text.dataset.vexGeneratedPart = 'meter-text';
                text.textContent = label;
                host.append(fill, text);
              };
              renderMeter(memoryResource, 0, 0, 'Memory not connected');

              const setProgress = (message, state = 'idle') => {
                if (!responseProgress) return;
                responseProgress.textContent = message ?? '';
                responseProgress.dataset.vexProgressState = state;
              };

              const frameUrlFor = (pageUrl) => {
                const parsed = new URL(pageUrl);
                if (parsed.protocol !== 'https:' || parsed.port || !browser.admittedHosts.includes(parsed.hostname)) {
                  throw new Error('Page is outside the admitted Vex-owned GitHub Pages boundary.');
                }
                return 'http://' + parsed.hostname + browser.frameHostSuffix + ':' + browser.frameHostPort + parsed.pathname + parsed.search + parsed.hash;
              };

              const normalizeContext = (value) => {
                if (!value || value.type !== browser.messageType || typeof value.pageUrl !== 'string' || typeof value.text !== 'string') return null;
                try {
                  const page = new URL(value.pageUrl);
                  if (page.protocol !== 'https:' || page.port || !browser.admittedHosts.includes(page.hostname)) return null;
                } catch {
                  return null;
                }
                const text = value.text
                  .slice(0, browser.contextCharacterLimit)
                  .replace(/\r\n?/gu, '\n')
                  .trim();
                if (!text) return null;
                const title = typeof value.title === 'string' && value.title.length > 0
                  ? value.title.slice(0, browser.maxTitleCharacters)
                  : null;
                return Object.freeze({
                  pageUrl: value.pageUrl,
                  title,
                  text,
                  capture: browser.captureRendered
                });
              };

              const navigate = (pageUrl) => {
                if (!frame || !browserQuery) return;
                const framed = frameUrlFor(pageUrl);
                currentBrowserContext = null;
                browserQuery.value = pageUrl;
                frame.src = framed;
                setProgress('Loading browser page…', 'browser-loading');
              };

              const isNearBottom = (element) =>
                element.scrollHeight - element.scrollTop - element.clientHeight < 72;

              const scrollConversationToLatest = () => {
                if (!timeline) return;
                timeline.scrollTop = timeline.scrollHeight;
                if (jumpLatestButton) jumpLatestButton.hidden = true;
              };

              const createConversationItem = (message) => {
                const item = document.createElement('li');
                item.dataset.vexGeneratedPart = 'conversation-message';
                item.dataset.vexConversationRole = String(message.role ?? 'unknown');
                item.dataset.vexConversationEventRef = String(message.eventRef ?? '');

                const index = document.createElement('span');
                index.dataset.vexGeneratedPart = 'conversation-index';
                const sequence = Number(message.sequence);
                index.textContent = '[' + String(Number.isFinite(sequence) ? sequence : 0).padStart(2, '0') + ']';

                const bubble = document.createElement('div');
                bubble.dataset.vexGeneratedPart = 'conversation-bubble';
                const paragraph = document.createElement('p');
                paragraph.textContent = String(message.content ?? '');
                bubble.append(paragraph);

                const speaker = document.createElement('div');
                speaker.dataset.vexGeneratedPart = 'conversation-speaker';
                speaker.textContent = message.role === 'human' ? 'You' : 'Vex';

                item.append(index, bubble, speaker);
                return item;
              };

              const renderConversation = (projection) => {
                if (!timeline) return;
                const messages = projection.messages ?? [];
                const wasNearBottom = isNearBottom(timeline);
                const existingRefs = [...timeline.children]
                  .map((item) => item.dataset.vexConversationEventRef ?? '');
                const desiredRefs = messages.map((message) => String(message.eventRef ?? ''));
                const prefixMatches =
                  existingRefs.length <= desiredRefs.length &&
                  existingRefs.every((ref, index) => ref === desiredRefs[index]);

                if (!prefixMatches) timeline.replaceChildren();

                const startIndex = prefixMatches ? existingRefs.length : 0;
                let appended = false;
                for (let index = startIndex; index < messages.length; index += 1) {
                  timeline.append(createConversationItem(messages[index]));
                  appended = true;
                }

                timeline.dataset.vexConversationHead =
                  projection.currentConversationHeadSha256 ?? '';
                const latestModel = [...messages].reverse().find((message) => message.role === 'vex' && message.model)?.model ?? '';
                if (companionStatus) {
                  companionStatus.textContent = latestModel ? 'Ready · ' + latestModel : 'Ready';
                  companionStatus.dataset.vexReady = 'true';
                }

                if (wasNearBottom || !prefixMatches) {
                  scrollConversationToLatest();
                } else if (appended && jumpLatestButton) {
                  jumpLatestButton.hidden = false;
                }
              };

              const renderContext = (projection) => {
                const prompt = projection?.promptContext ?? null;
                if (prompt) {
                  const used = Number(prompt.providerMaterializedInputTokenEstimate ?? 0);
                  const total = Number(prompt.hardTokenLimit ?? prompt.inputTokenBudget ?? 0);
                  renderMeter(
                    contextWindow,
                    used,
                    total,
                    total > 0 ? used.toLocaleString() + ' / ' + total.toLocaleString() + ' tokens used' : 'Context measured'
                  );
                } else {
                  renderMeter(contextWindow, 0, 0, 'Context not measured yet');
                }

                if (settingsContext) {
                  settingsContext.dataset.vexContextState = prompt ? 'current' : 'empty';
                  settingsContext.dataset.vexPromptReceiptRef = String(prompt?.receiptRef ?? '');
                  settingsContext.dataset.vexCapabilityFrameRef = String(prompt?.capabilityFrame?.frameRef ?? '');
                }

                if (contextReceipt) {
                  contextReceipt.dataset.vexContextIncluded = String(includeBrowserContext && currentBrowserContext !== null);
                  contextReceipt.replaceChildren();
                  if (includeBrowserContext && currentBrowserContext) {
                    const title = document.createElement('strong');
                    title.textContent = currentBrowserContext.title || 'Included page';
                    const line = document.createElement('div');
                    line.textContent = currentBrowserContext.pageUrl;
                    contextReceipt.append(title, line);
                  }
                }
              };

              const loadConversation = async () => {
                const response = await fetch(service.conversationPath, {
                  method: 'GET',
                  headers: { 'accept': 'application/json' },
                  cache: 'no-store'
                });
                renderConversation(await readJson(response));
              };

              const loadContext = async () => {
                const response = await fetch(service.contextPath, {
                  method: 'GET',
                  headers: { 'accept': 'application/json' },
                  cache: 'no-store'
                });
                renderContext(await readJson(response));
              };

              const loadSourceCurrentness = async () => {
                const response = await fetch(service.sourceCurrentnessPath, {
                  method: 'GET',
                  headers: { 'accept': 'application/json' },
                  cache: 'no-store'
                });
                const currentness = await readJson(response);
                root.dataset.vexServiceSourceSha = String(currentness.sourceSha ?? '');
                root.dataset.vexServicePid = String(currentness.processPid ?? '');
                root.dataset.vexFrameHostOrigin = String(currentness.frameHostOrigin ?? '');
                if (companionStatus && companionStatus.dataset.vexReady !== 'true') {
                  companionStatus.textContent = currentness.runtimeCurrentness === 'CURRENT' ? 'Ready' : String(currentness.runtimeCurrentness ?? 'Checking');
                  companionStatus.dataset.vexReady = String(currentness.runtimeCurrentness === 'CURRENT');
                }
              };

              const currentBrowserShare = () => {
                if (!bodyFrame) return browser.authoredBrowserShare;
                const raw = getComputedStyle(bodyFrame)
                  .getPropertyValue('--vex-browser-share')
                  .trim();
                const parsed = Number.parseFloat(raw);
                return Number.isFinite(parsed) ? parsed / 100 : browser.authoredBrowserShare;
              };

              const syncBrowserShareAccessibility = () => {
                if (!browserCompanionHandle) return;
                const boundaryPercent = Math.round(currentBrowserShare() * 100);
                browserCompanionHandle.setAttribute(
                  'aria-valuenow',
                  String(boundaryPercent)
                );
                browserCompanionHandle.setAttribute(
                  'aria-valuetext',
                  'Browser ' + boundaryPercent + '%, Vex companion ' + (100 - boundaryPercent) + '%'
                );
              };

              const applyBrowserShare = (share) => {
                if (!bodyFrame) return;
                const clamped = Math.max(0, Math.min(1, share));
                bodyFrame.style.setProperty(
                  '--vex-browser-share',
                  (clamped * 100).toFixed(3).replace(/0+$/u, '').replace(/\.$/u, '') + '%'
                );
                root.dataset.vexBrowserShare = String(clamped);
                syncBrowserShareAccessibility();
              };

              const adjustBrowserShare = (delta) => {
                applyBrowserShare(currentBrowserShare() + delta);
              };

              applyBrowserShare(browser.authoredBrowserShare);
              window.addEventListener('resize', syncBrowserShareAccessibility);

              const send = async () => {
                const content = composer?.value?.trim() ?? '';
                if ((!content && !pendingSend) || !sendButton) return;
                sendButton.disabled = true;
                const retrying = pendingSend !== null;
                if (!pendingSend) {
                  pendingSend = Object.freeze({
                    idempotencyKey: 'send.' + crypto.randomUUID().toLowerCase(),
                    content,
                    browserContext: includeBrowserContext ? currentBrowserContext : null
                  });
                }
                setProgress(retrying ? 'Retrying the prior exact send…' : 'Vex is responding…', 'sending');
                try {
                  const response = await fetch(service.sendPath, {
                    method: 'POST',
                    headers: {
                      'accept': 'application/json',
                      'content-type': 'application/json'
                    },
                    body: JSON.stringify(pendingSend)
                  });
                  const result = await readJson(response);
                  pendingSend = null;
                  if (composer) composer.value = '';
                  await Promise.all([
                    loadConversation(),
                    loadContext()
                  ]);
                  root.dataset.vexLastContextPath = String(result.contextPath ?? '');
                  root.dataset.vexLastPromptReceiptRef = String(result.promptContext?.receiptRef ?? '');
                  root.dataset.vexLastBrowserContextRef = String(result.promptContext?.browserContextRef ?? '');
                  setProgress('', 'complete');
                } catch (error) {
                  const status = Number(error?.vexStatus ?? 0);
                  const retryAllowed = error?.vexResponse?.retrySameSendAttemptAllowed;
                  if (status === 400 || retryAllowed === false) pendingSend = null;
                  setProgress(error instanceof Error ? error.message : 'Send failed.', 'error');
                } finally {
                  sendButton.disabled = false;
                }
              };

              if (browserViewport) {
                frame = document.createElement('iframe');
                frame.dataset.vexGeneratedPart = 'nested-browser';
                frame.title = 'Vex browser';
                frame.referrerPolicy = 'no-referrer';
                frame.setAttribute('sandbox', 'allow-scripts allow-same-origin');
                frame.addEventListener('load', () => {
                  currentBrowserContext = null;
                  root.dataset.vexCurrentBrowserPage = '';
                  setProgress('Waiting for browser page context…', 'browser-loading');
                });
                frame.style.border = '0';
                frame.style.width = '100%';
                frame.style.height = '100%';
                browserViewport.append(frame);
              }

              window.addEventListener('message', (event) => {
                if (!frame || event.source !== frame.contentWindow) return;
                const next = normalizeContext(event.data);
                if (!next) return;
                currentBrowserContext = next;
                if (browserQuery) browserQuery.value = next.pageUrl;
                root.dataset.vexCurrentBrowserPage = next.pageUrl;
                if (browserFrameStatus) {
                  browserFrameStatus.textContent = '';
                  browserFrameStatus.dataset.vexFrameState = 'current';
                }
                if (contextReceipt && includeBrowserContext) {
                  contextReceipt.dataset.vexContextIncluded = 'true';
                  contextReceipt.replaceChildren();
                  const title = document.createElement('strong');
                  title.textContent = next.title || 'Included page';
                  const line = document.createElement('div');
                  line.textContent = next.pageUrl;
                  contextReceipt.append(title, line);
                }
                setProgress('', 'browser-current');
              });

              browserQuery?.addEventListener('keydown', (event) => {
                if (event.key !== 'Enter' || event.shiftKey) return;
                event.preventDefault();
                try {
                  navigate(browserQuery.value.trim());
                } catch (error) {
                  setProgress(error instanceof Error ? error.message : 'Page is not admitted.', 'browser-error');
                }
              });

              timeline?.addEventListener('scroll', () => {
                if (isNearBottom(timeline) && jumpLatestButton) {
                  jumpLatestButton.hidden = true;
                }
              });

              jumpLatestButton?.addEventListener('click', () => {
                scrollConversationToLatest();
              });

              contextShareButton?.addEventListener('click', () => {
                includeBrowserContext = !includeBrowserContext;
                contextShareButton.setAttribute('aria-pressed', String(includeBrowserContext));
                contextShareButton.textContent = includeBrowserContext ? 'Page included' : 'Include this page';
                if (contextReceipt) {
                  contextReceipt.dataset.vexContextIncluded = String(includeBrowserContext && currentBrowserContext !== null);
                }
                void loadContext();
              });

              companionViewSwitchButton?.addEventListener('click', () => {
                activeCompanionView = activeCompanionView === 'conversation' ? 'settings' : 'conversation';
                if (conversation) conversation.hidden = activeCompanionView !== 'conversation';
                if (settings) settings.hidden = activeCompanionView !== 'settings';
                companionViewSwitchButton.textContent = activeCompanionView === 'conversation' ? 'Settings' : 'Return';
                companionViewSwitchButton.setAttribute('aria-expanded', String(activeCompanionView === 'settings'));
              });

              composer?.addEventListener('keydown', (event) => {
                if (event.key !== 'Enter' || !(event.metaKey || event.ctrlKey)) return;
                event.preventDefault();
                void send();
              });

              vexSummonButton?.addEventListener('click', () => {
                if (currentBrowserShare() >= 0.96) {
                  applyBrowserShare(browser.authoredBrowserShare);
                }
                composer?.focus();
              });

              if (browserCompanionHandle && bodyFrame) {
                browserCompanionHandle.addEventListener('pointerdown', (event) => {
                  event.preventDefault();
                  browserCompanionHandle.setPointerCapture(event.pointerId);
                });

                browserCompanionHandle.addEventListener('pointermove', (event) => {
                  if (!browserCompanionHandle.hasPointerCapture(event.pointerId)) return;
                  const rect = bodyFrame.getBoundingClientRect();
                  if (rect.width <= 0) return;
                  applyBrowserShare((event.clientX - rect.left) / rect.width);
                });

                browserCompanionHandle.addEventListener('pointerup', (event) => {
                  if (browserCompanionHandle.hasPointerCapture(event.pointerId)) {
                    browserCompanionHandle.releasePointerCapture(event.pointerId);
                  }
                });

                browserCompanionHandle.addEventListener('keydown', (event) => {
                  if (event.key === 'ArrowLeft') {
                    event.preventDefault();
                    adjustBrowserShare(-0.04);
                  } else if (event.key === 'ArrowRight') {
                    event.preventDefault();
                    adjustBrowserShare(0.04);
                  } else if (event.key === 'Home') {
                    event.preventDefault();
                    applyBrowserShare(0);
                  } else if (event.key === 'End') {
                    event.preventDefault();
                    applyBrowserShare(1);
                  } else if (event.key === 'Enter') {
                    event.preventDefault();
                    applyBrowserShare(0.5);
                  }
                });
              }

              sendButton?.addEventListener('click', () => {
                void send();
              });

              void Promise.all([
                loadSourceCurrentness(),
                loadConversation(),
                loadContext()
              ]).catch((error) => {
                setProgress(error instanceof Error ? error.message : 'Local service is not ready.', 'error');
              });

              if (frame && browserQuery) navigate(browser.initialPageUrl);
            })();
        """
    }

    private fun jsSelectorValue(
        value: String,
    ): String =
        value
            .replace(
                "\\",
                "\\\\",
            )
            .replace(
                "\"",
                "\\\"",
            )

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
