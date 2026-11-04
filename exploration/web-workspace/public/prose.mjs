// Render model text as readable prose without ever interpreting it as HTML.
// Supports paragraphs, line breaks, headings, bullet/numbered lists, fenced code, **bold**,
// *italic* and `code`. Everything is built from text nodes.
// [VXG RealForever]

function inline(parent, text) {
  const pattern = /(`[^`\n]+`|\*\*[^*\n]+\*\*|\*[^*\n]+\*)/gu;
  let last = 0;
  for (const match of text.matchAll(pattern)) {
    if (match.index > last) parent.append(document.createTextNode(text.slice(last, match.index)));
    const token = match[0];
    let node;
    if (token.startsWith('`')) {
      node = document.createElement('code');
      node.textContent = token.slice(1, -1);
    } else if (token.startsWith('**')) {
      node = document.createElement('strong');
      node.textContent = token.slice(2, -2);
    } else {
      node = document.createElement('em');
      node.textContent = token.slice(1, -1);
    }
    parent.append(node);
    last = match.index + token.length;
  }
  if (last < text.length) parent.append(document.createTextNode(text.slice(last)));
}

export function renderProse(container, text) {
  container.replaceChildren();
  const lines = text.replace(/\r\n?/gu, '\n').split('\n');
  let i = 0;
  while (i < lines.length) {
    const line = lines[i];

    if (/^```/u.test(line)) {
      const body = [];
      i += 1;
      while (i < lines.length && !/^```/u.test(lines[i])) {
        body.push(lines[i]);
        i += 1;
      }
      i += 1;
      const pre = document.createElement('pre');
      const code = document.createElement('code');
      code.textContent = body.join('\n');
      pre.append(code);
      container.append(pre);
      continue;
    }

    if (line.trim() === '') {
      i += 1;
      continue;
    }

    const heading = /^(#{1,6})\s+(.*)$/u.exec(line);
    if (heading) {
      const node = document.createElement('p');
      node.className = 'prose-heading';
      inline(node, heading[2]);
      container.append(node);
      i += 1;
      continue;
    }

    const bullet = /^\s*[-*•]\s+/u;
    const numbered = /^\s*\d+[.)]\s+/u;
    if (bullet.test(line) || numbered.test(line)) {
      const ordered = numbered.test(line) && !bullet.test(line);
      const list = document.createElement(ordered ? 'ol' : 'ul');
      const itemPattern = ordered ? numbered : bullet;
      while (i < lines.length && itemPattern.test(lines[i])) {
        const item = document.createElement('li');
        inline(item, lines[i].replace(itemPattern, ''));
        list.append(item);
        i += 1;
      }
      container.append(list);
      continue;
    }

    const paragraph = [];
    while (i < lines.length && lines[i].trim() !== '' && !/^```/u.test(lines[i]) && !/^#{1,6}\s/u.test(lines[i])
      && !bullet.test(lines[i]) && !numbered.test(lines[i])) {
      paragraph.push(lines[i]);
      i += 1;
    }
    const node = document.createElement('p');
    paragraph.forEach((part, index) => {
      if (index > 0) node.append(document.createElement('br'));
      inline(node, part);
    });
    container.append(node);
  }
}
