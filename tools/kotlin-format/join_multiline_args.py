# Join multi-line (...) spans with no braces/comments/raw strings so ktfmt can re-break them by width.
# Usage: python3 join_multiline_args.py File.kt ...  (edits in place; see README.md)
"""Collapse multi-line (...) spans that contain no braces/comments/raw strings so ktfmt can re-break
them by width. String and char literals (with ${} templates) are skipped exactly."""
import re, sys

def spans(s):
    out = []          # (start, end) of joinable paren spans, end inclusive
    stack = []        # [start, ok, has_newline]
    i, n = 0, len(s)
    tmpl = []         # stack of brace depth inside ${} templates
    def mark_bad():
        for fr in stack: fr[1] = False
    while i < n:
        c = s[i]
        if s.startswith('//', i):
            mark_bad(); j = s.find('\n', i); i = n if j < 0 else j; continue
        if s.startswith('/*', i):
            mark_bad(); j = s.find('*/', i + 2); i = n if j < 0 else j + 2; continue
        if s.startswith('"""', i):
            mark_bad(); j = s.find('"""', i + 3); i = n if j < 0 else j + 3; continue
        if c == '"':
            i += 1
            depth = 0
            while i < n:
                if s[i] == '\\': i += 2; continue
                if s.startswith('${', i):
                    # skip template expression with nested braces/strings (simple nesting)
                    d = 1; i += 2
                    while i < n and d:
                        if s[i] == '{': d += 1
                        elif s[i] == '}': d -= 1
                        elif s[i] == '"':
                            i += 1
                            while i < n and s[i] != '"':
                                if s[i] == '\\': i += 1
                                i += 1
                        i += 1
                    continue
                if s[i] == '"': i += 1; break
                if s[i] == '\n': mark_bad(); break
                i += 1
            continue
        if c == "'":
            j = i + 1
            if j < n and s[j] == '\\': j += 1
            k = s.find("'", j + 1)
            if k != -1 and k - i <= 8: i = k + 1; continue
        if c in '{}':
            mark_bad()
        elif c == '(':
            stack.append([i, True, False])
        elif c == ')':
            if stack:
                st, ok, nl = stack.pop()
                if ok and nl: out.append((st, i))
        elif c == '\n':
            for fr in stack: fr[2] = True
        i += 1
    return out

def join(s, st, en):
    inner = s[st + 1:en]
    inner = re.sub(r',\s*$', '', inner)                 # trailing comma before ')'
    inner = re.sub(r'^\s*\n\s*', '', inner)              # newline right after '('
    inner = re.sub(r'\s*\n\s*$', '', inner)              # newline right before ')'
    inner = re.sub(r'\s*\n\s*', ' ', inner)              # interior newlines
    inner = re.sub(r'\(\s+', '(', inner)
    return s[:st + 1] + inner + s[en:]

for p in sys.argv[1:]:
    s = open(p).read()
    for _ in range(50):
        sp = spans(s)
        if not sp: break
        # innermost spans never overlap each other, so joining them from the end keeps indices valid
        inner = [a for a in sp if not any(b != a and a[0] < b[0] and b[1] < a[1] for b in sp)]
        for st, en in sorted(inner, key=lambda x: -x[0]):
            s = join(s, st, en)
    open(p, 'w').write(s)
