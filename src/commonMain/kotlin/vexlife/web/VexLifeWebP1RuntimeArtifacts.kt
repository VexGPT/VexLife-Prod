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
                bodyFrame: ${jsString(VexLifeWebP1.Refs.BodyFrame.value)},
                browserRegion: ${jsString(VexLifeWebP1.Refs.BrowserRegion.value)},
                companionRegion: ${jsString(VexLifeWebP1.Refs.CompanionRegion.value)},
                browserCompanionHandle: ${jsString(VexLifeWebP1.BrowserCompanionHandle.identity.ref)},
                browserQuery: ${jsString(VexLifeWebP1.BrowserQuery.identity.ref)},
                browserViewport: ${jsString(VexLifeWebP1.BrowserViewport.identity.ref)},
                companion: ${jsString(VexLifeWebP1.Companion.identity.ref)},
                companionHeader: ${jsString(VexLifeWebP1.Refs.CompanionHeader.value)},
                companionContent: ${jsString(VexLifeWebP1.Refs.CompanionContent.value)},
                conversation: ${jsString(VexLifeWebP1.Conversation.identity.ref)},
                conversationTimeline: ${jsString(VexLifeWebP1.ConversationTimeline.identity.ref)},
                conversationJumpLatest: ${jsString(VexLifeWebP1.ConversationJumpLatest.identity.ref)},
                responseProgress: ${jsString(VexLifeWebP1.ResponseProgress.identity.ref)},
                composer: ${jsString(VexLifeWebP1.Composer.identity.ref)},
                send: ${jsString(VexLifeWebP1.Send.identity.ref)},
                settings: ${jsString(VexLifeWebP1.Settings.identity.ref)},
                settingsContext: ${jsString(VexLifeWebP1.SettingsContext.identity.ref)}
              });
              const service = Object.freeze({
                sourceCurrentnessPath: ${jsString(VexLifeProdServiceContract.SourceCurrentnessPath)},
                conversationPath: ${jsString(VexLifeProdServiceContract.ConversationPath)},
                contextPath: ${jsString(VexLifeProdServiceContract.ContextPath)},
                sendPath: ${jsString(VexLifeProdServiceContract.ConversationSendPath)}
              });
              const browser = Object.freeze({
                authoredInitialShare: $authoredBrowserShare,
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
              const bodyFrame = byRef(refs.bodyFrame);
              const browserRegion = byRef(refs.browserRegion);
              const companionRegion = byRef(refs.companionRegion);
              const browserCompanionHandle = byRef(refs.browserCompanionHandle);
              const browserQuery = generatedPart(refs.browserQuery, 'text-field');
              const browserViewport = byRef(refs.browserViewport);
              const companion = byRef(refs.companion);
              const companionHeader = byRef(refs.companionHeader);
              const companionContent = byRef(refs.companionContent);
              const conversation = byRef(refs.conversation);
              const timeline = byRef(refs.conversationTimeline);
              const jumpLatestButton = generatedPart(refs.conversationJumpLatest, 'control');
              const responseProgress = byRef(refs.responseProgress);
              const composer = generatedPart(refs.composer, 'text-field');
              const sendButton = generatedPart(refs.send, 'control');
              const settings = byRef(refs.settings);
              const settingsContext = byRef(refs.settingsContext);

              let currentBrowserContext = null;
              let frame = null;
              let pendingSend = null;

              if (brand) {
                brand.textContent = 'VexLife';
                brand.setAttribute('aria-label', 'VexLife');
              }
              if (companionHeader) {
                companionHeader.setAttribute('aria-label', 'Vex conversation workspace');
                const eyebrow = document.createElement('small');
                eyebrow.dataset.vexGeneratedPart = 'companion-eyebrow';
                eyebrow.textContent = 'CONVERSATION';
                const title = document.createElement('strong');
                title.dataset.vexGeneratedPart = 'companion-title';
                title.textContent = 'Vex';
                const status = document.createElement('span');
                status.dataset.vexGeneratedPart = 'companion-status';
                status.textContent = 'Local companion';
                companionHeader.replaceChildren(eyebrow, title, status);
              }
              if (companionContent) {
                companionContent.setAttribute('aria-label', 'Conversation and current context');
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
              if (sendButton) {
                sendButton.textContent = 'Send';
                sendButton.setAttribute('aria-label', 'Send message to Vex');
              }
              if (settings) settings.setAttribute('aria-label', 'Current Vex context');
              if (settingsContext) settingsContext.setAttribute('aria-live', 'polite');
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
                const item = document.createElement('article');
                item.dataset.vexGeneratedPart = 'conversation-message';
                item.dataset.vexConversationRole = String(message.role ?? 'unknown');
                item.dataset.vexConversationEventRef = String(message.eventRef ?? '');

                const avatar = document.createElement('span');
                avatar.dataset.vexGeneratedPart = 'conversation-avatar';
                avatar.setAttribute('aria-hidden', 'true');
                avatar.textContent = message.role === 'human' ? 'Y' : 'V';

                const body = document.createElement('div');
                body.dataset.vexGeneratedPart = 'conversation-message-body';

                const role = document.createElement('strong');
                role.textContent = message.role === 'human' ? 'You' : 'Vex';

                const content = document.createElement('p');
                content.textContent = String(message.content ?? '');

                body.append(role, content);
                item.append(avatar, body);
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

                if (wasNearBottom || !prefixMatches) {
                  scrollConversationToLatest();
                } else if (appended && jumpLatestButton) {
                  jumpLatestButton.hidden = false;
                }
              };

              const renderContext = (projection) => {
                if (!settingsContext) return;
                settingsContext.replaceChildren();

                const heading = document.createElement('h3');
                heading.textContent = 'Context';
                settingsContext.append(heading);

                const prompt = projection?.promptContext ?? null;
                if (!prompt) {
                  const empty = document.createElement('p');
                  const hasCurrentConversation = Boolean(projection?.currentConversationHeadSha256);
                  empty.textContent = hasCurrentConversation
                    ? 'Current Conversation turn predates VexLife-Prod prompt-context evidence.'
                    : 'No completed Vex turn context yet.';
                  settingsContext.append(empty);
                  settingsContext.dataset.vexContextState = hasCurrentConversation ? 'historical' : 'empty';
                  settingsContext.dataset.vexPromptReceiptRef = '';
                  settingsContext.dataset.vexCapabilityFrameRef = '';
                  return;
                }

                const facts = document.createElement('dl');
                const addFact = (label, value) => {
                  const term = document.createElement('dt');
                  term.textContent = label;
                  const detail = document.createElement('dd');
                  detail.textContent = String(value ?? '');
                  facts.append(term, detail);
                };
                addFact('Mode', prompt.contextMode);
                addFact('Memory', prompt.memoryIncluded ? 'included' : 'not included');
                addFact('Browser', prompt.browserContextIncluded ? 'included' : 'not included');
                addFact('Working continuity', prompt.workingContinuityIncluded ? 'included' : 'not included');
                settingsContext.append(facts);

                const frameProjection = prompt.capabilityFrame ?? null;
                const capabilityHeading = document.createElement('h4');
                capabilityHeading.textContent = 'Capabilities Vex received';
                settingsContext.append(capabilityHeading);

                if (!frameProjection) {
                  const unavailable = document.createElement('p');
                  unavailable.textContent = prompt.capabilityFrameIncluded
                    ? 'Capability frame recorded; detailed state snapshot is unavailable for this historical receipt.'
                    : 'No capability frame recorded for this turn.';
                  settingsContext.append(unavailable);
                  settingsContext.dataset.vexCapabilityFrameRef = '';
                } else {
                  const groups = [
                    ['AVAILABLE', frameProjection.availableCapabilityRefs],
                    ['HELD', frameProjection.heldCapabilityRefs],
                    ['UNAVAILABLE', frameProjection.unavailableCapabilityRefs],
                    ['UNKNOWN', frameProjection.unknownCapabilityRefs],
                    ['OMITTED', frameProjection.omittedCapabilityRefs]
                  ];
                  for (const [label, refsForState] of groups) {
                    const group = document.createElement('section');
                    const title = document.createElement('strong');
                    title.textContent = label;
                    group.append(title);
                    const list = document.createElement('ul');
                    for (const capabilityRef of refsForState ?? []) {
                      const item = document.createElement('li');
                      item.textContent = String(capabilityRef);
                      list.append(item);
                    }
                    if (!list.childElementCount) {
                      const item = document.createElement('li');
                      item.textContent = 'None';
                      list.append(item);
                    }
                    group.append(list);
                    settingsContext.append(group);
                  }

                  const coverage = document.createElement('p');
                  coverage.textContent =
                    'Frame coverage: ' +
                    String(frameProjection.usedTokens ?? 0) +
                    ' / ' +
                    String(frameProjection.tokenBudget ?? 0) +
                    ' estimated tokens' +
                    (frameProjection.truncated ? ' (truncated)' : '');
                  settingsContext.append(coverage);

                  if ((frameProjection.omissionReasons ?? []).length) {
                    const omissions = document.createElement('p');
                    omissions.textContent =
                      'Omission reasons: ' +
                      frameProjection.omissionReasons.join(', ');
                    settingsContext.append(omissions);
                  }

                  const authority = document.createElement('p');
                  authority.textContent = frameProjection.effectAuthorityGranted
                    ? 'Effect authority: granted'
                    : 'Effect authority: not granted';
                  settingsContext.append(authority);
                  settingsContext.dataset.vexCapabilityFrameRef = String(frameProjection.frameRef ?? '');
                }

                settingsContext.dataset.vexContextState = 'current';
                settingsContext.dataset.vexPromptReceiptRef = String(prompt.receiptRef ?? '');
              };

              const readJson = async (response) => {
                const value = await response.json();
                if (!response.ok) {
                  const error = new Error(value?.message || value?.code || ('HTTP ' + response.status));
                  error.vexStatus = response.status;
                  error.vexResponse = value;
                  throw error;
                }
                return value;
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
              };

              const currentBrowserShare = () => {
                if (!bodyFrame) return browser.authoredInitialShare;
                const raw = getComputedStyle(bodyFrame)
                  .getPropertyValue('--vex-browser-share')
                  .trim();
                const parsed = Number.parseFloat(raw);
                return Number.isFinite(parsed) ? parsed / 100 : browser.authoredInitialShare;
              };

              const syncBrowserShareAccessibility = () => {
                if (!browserCompanionHandle) return;
                const browserPercent = Math.round(currentBrowserShare() * 100);
                browserCompanionHandle.setAttribute(
                  'aria-valuenow',
                  String(browserPercent)
                );
                browserCompanionHandle.setAttribute(
                  'aria-valuetext',
                  'Browser ' + browserPercent + '%, Vex companion ' + (100 - browserPercent) + '%'
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

              applyBrowserShare(browser.authoredInitialShare);
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
                    browserContext: currentBrowserContext
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

              composer?.addEventListener('keydown', (event) => {
                if (event.key !== 'Enter' || !(event.metaKey || event.ctrlKey)) return;
                event.preventDefault();
                void send();
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
