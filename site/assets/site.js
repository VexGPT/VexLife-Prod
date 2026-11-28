// VexLife home. Reads what is published; decides nothing.
//   data/releases.json  formed at deploy from GitHub Releases (see docs/release/RELEASES.md);
//                       falls back to the public GitHub API when it is absent.
//   data/models.json    formed at deploy from the Product's model catalog and Mac profile.
//   data/walkthrough.json  the first-run steps (screenshots in walkthrough/<step>.png).
(function () {
  'use strict';

  var Repository = 'VexGPT/VexLife-Prod';
  var StableTag = /^v(\d+)\.(\d+)\.(\d+)$/;
  var MainTag = /^private-service-image-([0-9a-f]{40})$/;
  var MacCommand = /^vexlife-prod-bootstrap-darwin-arm64-[0-9a-f]{40}\.command$/;

  var byId = function (id) { return document.getElementById(id); };
  var text = function (tag, value, className) {
    var node = document.createElement(tag);
    if (className) node.className = className;
    node.textContent = value;
    return node;
  };
  var gib = function (bytes) { return (Number(bytes) / 1073741824).toFixed(1) + ' GiB'; };
  var humanFileSize = function (bytes) {
    var size = Number(bytes);
    if (!isFinite(size) || size <= 0) return '';
    if (size >= 1000000000) {
      return (size / 1000000000).toFixed(size >= 10000000000 ? 1 : 2).replace(/\.0+$/, '') + ' GB';
    }
    if (size >= 1000000) {
      return (size / 1000000).toFixed(size >= 100000000 ? 0 : 1).replace(/\.0$/, '') + ' MB';
    }
    if (size >= 1000) {
      return (size / 1000).toFixed(size >= 100000 ? 0 : 1).replace(/\.0$/, '') + ' KB';
    }
    return Math.round(size) + ' B';
  };
  var day = function (iso) {
    var date = new Date(iso);
    return isNaN(date.getTime()) ? '' : date.toLocaleDateString(document.documentElement.lang || undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  };
  var getJson = function (url) {
    return fetch(url, { headers: { accept: 'application/json' }, cache: 'no-store' }).then(function (response) {
      if (!response.ok) throw new Error('HTTP ' + response.status);
      return response.json();
    });
  };

  // Theme: Foundation dark is home; light is the person's choice, remembered on this browser.
  var toggle = byId('theme-toggle');
  var syncToggle = function () {
    var light = document.documentElement.getAttribute('data-theme') === 'foundation-light';

    // This exact control is moved into the shared Spiral. It remains the one Theme control and
    // renders the same current-theme glyph convention as Vextreme's stable Theme FAB.
    toggle.textContent = light ? '☀️' : '🌙';
    toggle.setAttribute('aria-label', 'Toggle dark / light theme');
    toggle.title = 'Toggle theme';
  };
  toggle.addEventListener('click', function (event) {
    event.stopPropagation();
    var next = document.documentElement.getAttribute('data-theme') === 'foundation-light' ? 'foundation' : 'foundation-light';
    document.documentElement.setAttribute('data-theme', next);
    try { localStorage.setItem('vexlife-theme', next); } catch (error) {}
    syncToggle();
  });
  syncToggle();

  // Releases --------------------------------------------------------------------------------

  var macAssetOf = function (assets) {
    var dmg = assets.filter(function (asset) { return /\.dmg$/.test(asset.name) && /(darwin|macos)/i.test(asset.name); })[0];
    if (dmg) return { kind: 'dmg', name: dmg.name, size: dmg.size, url: dmg.url, sha256: dmg.sha256 || null };
    var command = assets.filter(function (asset) { return MacCommand.test(asset.name); })[0];
    if (command) return { kind: 'command', name: command.name, size: command.size, url: command.url, sha256: command.sha256 || null };
    return null;
  };

  // The GitHub API shape, normalized to the deploy snapshot's shape (without digests).
  var fromApi = function (list) {
    return {
      releases: list
        .filter(function (release) { return !release.draft; })
        .map(function (release) {
          var assets = (release.assets || []).map(function (asset) {
            return { name: asset.name, size: asset.size, url: asset.browser_download_url };
          });
          var stable = StableTag.test(release.tag_name) && !release.prerelease;
          var main = MainTag.exec(release.tag_name);
          var sourceLine = /source-sha:\s*([0-9a-f]{40})/.exec(release.body || '');
          return {
            tag: release.tag_name,
            name: release.name || release.tag_name,
            channel: stable ? 'stable' : main ? 'main' : 'other',
            sourceSha: main ? main[1] : sourceLine ? sourceLine[1] : null,
            publishedAt: release.published_at,
            url: release.html_url,
            mac: macAssetOf(assets)
          };
        })
    };
  };

  var fromSnapshot = function (snapshot) {
    return {
      formedAt: snapshot.formedAt,
      mainSha: snapshot.mainSha,
      releases: (snapshot.releases || []).map(function (release) {
        release.mac = macAssetOf(release.assets || []);
        return release;
      })
    };
  };

  var loadReleases = function () {
    return getJson('data/releases.json')
      .then(fromSnapshot)
      .catch(function () {
        return getJson('https://api.github.com/repos/' + Repository + '/releases?per_page=100').then(fromApi);
      });
  };

  var versionKey = function (tag) {
    var match = StableTag.exec(tag) || [0, 0, 0, 0];
    return Number(match[1]) * 1e6 + Number(match[2]) * 1e3 + Number(match[3]);
  };

  var renderMac = function (catalog) {
    var stable = catalog.releases
      .filter(function (release) { return release.channel === 'stable' && release.mac; })
      .sort(function (a, b) { return versionKey(b.tag) - versionKey(a.tag); });
    var mains = catalog.releases
      .filter(function (release) { return release.channel === 'main' && release.mac; })
      .sort(function (a, b) { return String(b.publishedAt).localeCompare(String(a.publishedAt)); });
    var choices = stable.slice();
    mains.slice(0, 2).forEach(function (release) { choices.push(release); });

    var select = byId('mac-version');
    var badge = byId('mac-badge');
    select.textContent = '';
    if (!choices.length) {
      select.append(text('option', 'Nothing published yet'));
      badge.textContent = 'Not published yet';
      badge.className = 'badge badge-pending';
      byId('download-lede').textContent =
        'No public build has been published yet. A preview appears only when one is intentionally published; stable versions appear when a feature threshold is accepted.';
      return;
    }
    choices.forEach(function (release, index) {
      var label = release.channel === 'stable'
        ? release.tag + ' — Stable · ' + day(release.publishedAt)
        : 'Preview · ' + String(release.sourceSha || '').slice(0, 7) + ' · ' + day(release.publishedAt);
      var option = text('option', label);
      option.value = String(index);
      select.append(option);
    });
    select.disabled = false;

    var show = function () {
      var release = choices[Number(select.value) || 0];
      var mac = release.mac;
      var download = byId('mac-download');
      download.href = mac.url;
      download.removeAttribute('aria-disabled');
      var notes = byId('mac-notes');
      notes.href = release.url;
      notes.removeAttribute('aria-disabled');
      var trustPath = byId('mac-trust-path');
      var showSecurityPath = mac.kind === 'dmg';
      trustPath.hidden = !showSecurityPath;
      badge.textContent = release.channel === 'stable' ? 'Stable' : 'Preview';
      badge.className = 'badge ' + (release.channel === 'stable' ? 'badge-active' : 'badge-info');

      var facts = byId('mac-facts');
      facts.textContent = '';
      var fact = function (term, value, href) {
        facts.append(text('dt', term));
        var dd = document.createElement('dd');
        if (href) {
          var link = text('a', value);
          link.href = href;
          dd.append(link);
        } else {
          dd.textContent = value;
        }
        facts.append(dd);
      };
      fact('File', mac.name);
      if (mac.size) fact('Size', humanFileSize(mac.size));
      if (mac.sha256) fact('SHA-256', mac.sha256);
      if (release.sourceSha) fact('Source', release.sourceSha.slice(0, 12), 'https://github.com/' + Repository + '/commit/' + release.sourceSha);
      fact('Published', day(release.publishedAt));
      if (mac.kind === 'dmg') fact('Apple trust', 'Open-source build · not Developer ID signed or notarized');

      var next = byId('mac-next');
      next.textContent = '';
      next.hidden = false;
      next.className = 'next';

      if (mac.kind === 'dmg') {
        next.classList.add('mac-after-download');
        next.append(text('p', 'After downloading', 'eyebrow'));

        var steps = document.createElement('ol');
        steps.className = 'mac-after-steps';

        var trustStep = function (number, title, body, imageName, alt) {
          var item = document.createElement('li');
          item.className = 'mac-after-step';

          var copy = document.createElement('div');
          copy.className = 'mac-after-step-copy';
          copy.append(text('span', number, 'mac-after-step-number'));
          var words = document.createElement('div');
          words.append(text('h5', title, 'mac-after-step-title'));
          words.append(text('p', body, 'mac-after-step-body'));
          copy.append(words);

          var details = document.createElement('details');
          details.className = 'mac-shot-details';
          var summary = document.createElement('summary');
          summary.className = 'mac-shot-summary';

          var thumb = new Image();
          thumb.className = 'mac-shot-thumb';
          thumb.src = 'walkthrough/' + imageName;
          thumb.alt = '';
          thumb.loading = 'lazy';

          var label = text('span', 'See what this looks like', 'mac-shot-label');
          summary.append(thumb, label);
          details.append(summary);

          var expanded = new Image();
          expanded.className = 'mac-shot-expanded';
          expanded.src = 'walkthrough/' + imageName;
          expanded.alt = alt;
          expanded.loading = 'lazy';
          details.append(expanded);

          item.append(copy, details);
          return item;
        };

        steps.append(
          trustStep(
            '01',
            'Open the DMG',
            'Open the downloaded VexLife disk image, then drag VexLife to Applications.',
            'gatekeeper-not-opened.png',
            'macOS VexLife Not Opened dialog after the first launch'
          ),
          trustStep(
            '02',
            'Open VexLife once',
            'Launch VexLife from Applications. If macOS blocks it, choose Done.',
            'gatekeeper-not-opened.png',
            'macOS VexLife Not Opened dialog'
          ),
          trustStep(
            '03',
            'Open Privacy & Security',
            'Use the button above, or Apple menu → System Settings → Privacy & Security.',
            'gatekeeper-privacy-security.png',
            'macOS Privacy & Security showing the VexLife security area'
          ),
          trustStep(
            '04',
            'Choose Open Anyway',
            'Find the VexLife blocked row and choose Open Anyway only if you trust this exact release.',
            'gatekeeper-open-anyway-confirm.png',
            'macOS confirmation for opening VexLife anyway'
          ),
          trustStep(
            '05',
            'Confirm with macOS',
            'macOS may ask you to authenticate and confirm Open. VexLife does not bypass this decision.',
            'gatekeeper-admin-auth.png',
            'macOS authentication prompt confirming the VexLife exception'
          )
        );
        next.append(steps);

        var help = document.createElement('p');
        help.className = 'mac-trust-help';
        help.append(
          document.createTextNode(
            'If the Privacy & Security button does not open System Settings from your browser, use Apple menu → System Settings → Privacy & Security. '
          )
        );
        var appleHelp = text('a', 'Apple instructions');
        appleHelp.href = 'https://support.apple.com/102445';
        appleHelp.target = '_blank';
        appleHelp.rel = 'noopener noreferrer';
        help.append(appleHelp, document.createTextNode('.'));
        next.append(help);
      } else {
        next.append(text('p', 'After downloading', 'eyebrow'));
        var steps = document.createElement('ol');
        steps.append(text('li', 'Open Terminal (Applications → Utilities).'));
        var run = document.createElement('li');
        run.append(document.createTextNode('Run this. It checks the exact build it downloads, then opens the VexLife setup window:'));
        var command = 'bash ~/Downloads/' + mac.name;
        var row = document.createElement('div');
        row.className = 'command';
        row.append(text('code', command));
        var copy = text('button', 'Copy', 'control');
        copy.type = 'button';
        copy.addEventListener('click', function () {
          if (navigator.clipboard) navigator.clipboard.writeText(command).then(function () { copy.textContent = 'Copied'; }, function () {});
        });
        row.append(copy);
        run.append(row);
        steps.append(run);
        steps.append(text('li', 'This is a script while the signed VexLife app is prepared; macOS may not open it with a double-click because it is not yet notarized.'));
        next.append(steps);
      }
      }
    };
    select.addEventListener('change', show);
    show();
  };

  var renderVersions = function (catalog) {
    var list = byId('version-list');
    list.textContent = '';
    catalog.releases
      .filter(function (release) { return release.channel === 'stable'; })
      .sort(function (a, b) { return versionKey(b.tag) - versionKey(a.tag); })
      .forEach(function (release) {
        var item = document.createElement('li');
        var link = text('a', release.tag);
        link.href = release.url;
        item.append(link, document.createTextNode(' · ' + day(release.publishedAt) + (release.sourceSha ? ' · ' + release.sourceSha.slice(0, 7) : '')));
        list.append(item);
      });
    if (catalog.mainSha) {
      byId('footer-line').append(document.createTextNode(' · main @ ' + catalog.mainSha.slice(0, 7)));
    }
  };

  loadReleases()
    .then(function (catalog) {
      renderMac(catalog);
      renderVersions(catalog);
    })
    .catch(function () {
      var select = byId('mac-version');
      select.textContent = '';
      select.append(text('option', 'Versions could not be loaded'));
      byId('mac-badge').textContent = 'Unavailable';
      byId('download-lede').textContent = 'The list of versions could not be loaded right now. All builds are also on the GitHub Releases page.';
      var download = byId('mac-download');
      download.href = 'https://github.com/' + Repository + '/releases';
      download.textContent = 'Open GitHub Releases';
      download.removeAttribute('aria-disabled');
    });

  // Requirements ------------------------------------------------------------------------------

  getJson('data/models.json')
    .then(function (data) {
      var table = byId('models-table');
      var body = table.querySelector('tbody');
      (data.models || []).forEach(function (model) {
        var row = document.createElement('tr');
        row.append(text('td', model.label));
        row.append(text('td', gib(model.downloadBytes), 'num'));
        row.append(text('td', gib(model.recommendedMemoryBytes), 'num'));
        var standing = document.createElement('td');
        standing.append(text('span', model.standing === 'CERTIFIED' ? 'Certified' : 'Candidate',
          'badge ' + (model.standing === 'CERTIFIED' ? 'badge-active' : 'badge-pending')));
        row.append(standing);
        body.append(row);
      });
      table.hidden = false;
      if (data.mac) {
        byId('requirements-lede').textContent =
          data.mac.chip + ', with at least ' + gib(data.mac.minimumMemoryBytes) +
          ' of memory for the smallest model. Each model wants a different amount; setup suggests the largest that fits, and you can add or remove models later in Settings → Models.';
      }
    })
    .catch(function () {});

  // First run ---------------------------------------------------------------------------------

  getJson('data/walkthrough.json')
    .then(function (data) {
      var list = byId('walkthrough-steps');
      (data.steps || []).forEach(function (step) {
        var item = document.createElement('li');
        item.className = 'card step';
        item.id = 'step-' + step.id;
        // The screenshot replaces the placeholder only once it has actually loaded.
        var shot = text('div', 'Screenshot to come', 'shot');
        var image = new Image();
        image.alt = step.title;
        image.addEventListener('load', function () {
          shot.textContent = '';
          shot.append(image);
        });
        image.src = 'walkthrough/' + step.id + '.png';
        item.append(shot, text('h3', step.title, 'h3'), text('p', step.body, 'card-body'), text('p', step.unchanged, 'unchanged'));
        list.append(item);
      });
      var links = byId('walkthrough-links');
      links.textContent = '';
      links.append(document.createTextNode('The steps are described in '));
      var journey = text('a', 'SETUP-JOURNEY.md');
      journey.href = 'https://github.com/' + Repository + '/blob/main/' + data.source;
      links.append(journey, document.createTextNode('; the workspace screen lens for VexBoard is '));
      var lens = text('a', 'vexlife-prod.screen-lens.json');
      lens.href = 'https://github.com/' + Repository + '/blob/main/' + data.screenLens;
      links.append(lens, document.createTextNode('.'));
    })
    .catch(function () {});
})();
