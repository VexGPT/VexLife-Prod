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
            javascript =
                base.javascript.trimEnd() +
                    "\n\n" +
                    runtimeJavascript().trimIndent() +
                    "\n",
        )
    }

    private fun runtimeJavascript(): String {
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
                browserQuery: ${jsString(VexLifeWebP1.BrowserQuery.identity.ref)},
                browserViewport: ${jsString(VexLifeWebP1.BrowserViewport.identity.ref)},
                conversationTimeline: ${jsString(VexLifeWebP1.ConversationTimeline.identity.ref)},
                responseProgress: ${jsString(VexLifeWebP1.ResponseProgress.identity.ref)},
                composer: ${jsString(VexLifeWebP1.Composer.identity.ref)},
                send: ${jsString(VexLifeWebP1.Send.identity.ref)}
              });
              const service = Object.freeze({
                sourceCurrentnessPath: ${jsString(VexLifeProdServiceContract.SourceCurrentnessPath)},
                conversationPath: ${jsString(VexLifeProdServiceContract.ConversationPath)},
                sendPath: ${jsString(VexLifeProdServiceContract.ConversationSendPath)}
              });
              const browser = Object.freeze({
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
              const browserQuery = generatedPart(refs.browserQuery, 'text-field');
              const browserViewport = byRef(refs.browserViewport);
              const timeline = byRef(refs.conversationTimeline);
              const responseProgress = byRef(refs.responseProgress);
              const composer = generatedPart(refs.composer, 'text-field');
              const sendButton = generatedPart(refs.send, 'control');

              let currentBrowserContext = null;
              let frame = null;

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

              const renderConversation = (projection) => {
                if (!timeline) return;
                timeline.replaceChildren();
                for (const message of projection.messages ?? []) {
                  const item = document.createElement('article');
                  item.dataset.vexConversationRole = String(message.role ?? 'unknown');
                  item.dataset.vexConversationEventRef = String(message.eventRef ?? '');
                  const role = document.createElement('strong');
                  role.textContent = message.role === 'human' ? 'You' : 'Vex';
                  const content = document.createElement('p');
                  content.textContent = String(message.content ?? '');
                  item.append(role, content);
                  timeline.append(item);
                }
                timeline.dataset.vexConversationHead = projection.currentConversationHeadSha256 ?? '';
              };

              const readJson = async (response) => {
                const value = await response.json();
                if (!response.ok) {
                  const message = value?.message || value?.code || ('HTTP ' + response.status);
                  throw new Error(message);
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

              const send = async () => {
                const content = composer?.value?.trim() ?? '';
                if (!content || !sendButton) return;
                sendButton.disabled = true;
                setProgress('Vex is responding…', 'sending');
                try {
                  const body = {
                    sendAttemptRef: 'send.' + crypto.randomUUID().toLowerCase(),
                    content,
                    browserContext: currentBrowserContext
                  };
                  const response = await fetch(service.sendPath, {
                    method: 'POST',
                    headers: {
                      'accept': 'application/json',
                      'content-type': 'application/json'
                    },
                    body: JSON.stringify(body)
                  });
                  const result = await readJson(response);
                  if (composer) composer.value = '';
                  await loadConversation();
                  root.dataset.vexLastContextRef = String(result.contextRef ?? '');
                  root.dataset.vexLastPromptReceiptRef = String(result.promptContext?.receiptRef ?? '');
                  root.dataset.vexLastBrowserContextRef = String(result.promptContext?.browserContextRef ?? '');
                  setProgress('', 'complete');
                } catch (error) {
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

              sendButton?.addEventListener('click', () => {
                void send();
              });

              void Promise.all([
                loadSourceCurrentness(),
                loadConversation()
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
