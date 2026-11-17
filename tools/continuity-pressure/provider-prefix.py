#!/usr/bin/env python3
"""Re-render a continuity-pressure run's provider calls with the Product's pinned chat template.

    python3 -I tools/continuity-pressure/provider-prefix.py \
        --zip <result.zip> --model-dir <dir with the pinned template + tokenizer> [--out report.json]

The model directory must hold the exact pinned members of the certified Product model
(HereticArtifactCatalog): chat_template.jinja, tokenizer.json and tokenizer_config.json. Their
digests are checked before anything is rendered.

What it reports is a deterministic projection of the pinned runtime, not provider telemetry:

- every call rendered with the pinned template (transformers semantics: trim_blocks,
  lstrip_blocks, add_generation_prompt) and tokenized with the pinned tokenizer;
- whether the call is admissible to the template at all (it raises on a system message that is
  not first);
- the prompt-cache reuse mlx-lm 0.32.0's server would give it. That server checkpoints a chat
  prompt at the end of its system segment and at the end of its user segment (just before the
  generation prompt), and after generation at the end of the full sequence. qwen3_5 layers are
  linear attention (ArraysCache) and full attention (KVCache); ArraysCache is not trimmable, so a
  stored cache is reused only when its tokens are an exact prefix of the new prompt. The
  full-sequence checkpoint is left out: live thinking tokens never match the empty think block
  the template renders for a replayed reply, so it cannot be a prefix of a later prompt.
- for comparison, the reuse a trimmable (pure-KV) cache would allow: the longest common token
  prefix with any earlier stored checkpoint.

--without-current-state is a counterfactual, labelled as one: it drops VexLife's current-state
envelope from every call before rendering, to size what a representation whose per-turn state is
replayed at its position (or absent) could reuse on this runtime. It is not a prompt the Product
sends.

[VXG RealForever]
"""

import argparse
import hashlib
import json
import sys
import zipfile
from pathlib import Path

import jinja2
from jinja2.sandbox import ImmutableSandboxedEnvironment
from tokenizers import Tokenizer

# HereticArtifactCatalog members of artifact.devex.qwen3.8-27b-heretic-mlx-4bit.808b82a2.
PINNED = {
    "chat_template.jinja": "c3cf9e34abf4f9e36c2d72165aa9c132d3e2a725b6c2586aaa3a8af9d7a81041",
    "tokenizer.json": "06b9509352d2af50381ab2247e083b80d32d5c0aba91c272ca9ff729b6a0e523",
    "tokenizer_config.json": "5d9c0d3314a253b714ddbbfeef7a55e9295c45c3da9f5fd9907dae6ebe6cc731",
}
PROMPT_CACHE_SIZE = 10  # mlx_lm.server --prompt-cache-size default; Product passes none.


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class TemplateError(Exception):
    pass


def load(model_dir: Path):
    for name, digest in PINNED.items():
        actual = sha256(model_dir / name)
        if actual != digest:
            raise SystemExit(f"{name} digest {actual} is not the pinned {digest}")

    def raise_exception(message):
        raise TemplateError(message)

    env = ImmutableSandboxedEnvironment(trim_blocks=True, lstrip_blocks=True)
    env.globals["raise_exception"] = raise_exception
    env.filters["tojson"] = lambda value, **kw: json.dumps(value, ensure_ascii=False, **kw)
    template = env.from_string((model_dir / "chat_template.jinja").read_text())
    tokenizer = Tokenizer.from_file(str(model_dir / "tokenizer.json"))
    think = tokenizer.token_to_id("<think>")
    if think is None:
        raise SystemExit("pinned tokenizer has no <think> token")
    return template, tokenizer, think


def render(template, messages, add_generation_prompt, kwargs):
    return template.render(messages=messages, add_generation_prompt=add_generation_prompt, tools=None, **kwargs)


def encode(tokenizer, text):
    return tokenizer.encode(text, add_special_tokens=False).ids


def segments(template, tokenizer, think, messages, kwargs, prompt):
    """mlx-lm 0.32 server segmentation: (sys_end, tail_start) of a chat prompt ending in a user turn."""
    if messages[-1]["role"] != "user":
        return 0, len(prompt)
    num_system = 0
    for message in messages:
        if message["role"] == "system":
            num_system += 1
        else:
            break
    sys_end = 0
    if num_system > 0:
        sys_tokens = encode(
            tokenizer,
            render(template, messages[:num_system] + [{"role": "user", "content": ""}], False, kwargs),
        )
        for i, (a, b) in enumerate(zip(sys_tokens, prompt)):
            if a != b:
                sys_end = i
                break
    tail_start = len(prompt)
    window = prompt[max(0, len(prompt) - 11):]
    for offset in range(len(window) - 1, -1, -1):
        if window[offset] == think:
            tail_start = len(prompt) - len(window) + offset
            break
    return sys_end, tail_start


def is_prefix(key, tokens):
    return len(key) <= len(tokens) and tokens[: len(key)] == key


def common_prefix(a, b):
    n = 0
    for x, y in zip(a, b):
        if x != y:
            break
        n += 1
    return n


CURRENT_STATE_HEADER = "VEXLIFE_CURRENT_STATE/v1"


def analyze(calls, template, tokenizer, think, without_current_state=False):
    stored = []  # LRU of checkpoint token lists, most recent last
    rows = []
    for call in calls:
        messages = [{"role": m["role"], "content": m["content"]} for m in call["messages"]]
        if without_current_state:
            messages = [m for m in messages if not m["content"].startswith(CURRENT_STATE_HEADER)]
        kwargs = dict(call.get("chat_template_kwargs") or {})
        row = {"ordinal": call["ordinal"], "call": call["call"], "messageCount": len(messages)}
        try:
            text = render(template, messages, True, kwargs)
        except TemplateError as error:
            row.update({"templateAdmitted": False, "templateError": str(error)})
            rows.append(row)
            continue
        prompt = encode(tokenizer, text)
        sys_end, tail_start = segments(template, tokenizer, think, messages, kwargs, prompt)
        exact = max((len(k) for k in stored if is_prefix(k, prompt)), default=0)
        trimmable = max((min(common_prefix(k, prompt), len(prompt) - 1) for k in stored), default=0)
        row.update(
            {
                "templateAdmitted": True,
                "promptTokens": len(prompt),
                "systemSegmentTokens": sys_end,
                "userSegmentEnd": tail_start,
                "reusedTokens": exact,
                "prefilledTokens": len(prompt) - exact,
                "reusedTokensIfTrimmable": max(exact, trimmable),
            }
        )
        rows.append(row)
        for key in ([prompt[:sys_end]] if sys_end > 0 else []) + [prompt[:tail_start]]:
            if key in stored:
                stored.remove(key)
            stored.append(key)
        del stored[:-PROMPT_CACHE_SIZE]
    admitted = [r for r in rows if r["templateAdmitted"]]
    total = sum(r["promptTokens"] for r in admitted)
    reused = sum(r["reusedTokens"] for r in admitted)
    return {
        "calls": len(rows),
        "templateRejectedCalls": len(rows) - len(admitted),
        "promptTokens": total,
        "reusedTokens": reused,
        "prefilledTokens": total - reused,
        "reusedTokensIfTrimmable": sum(r["reusedTokensIfTrimmable"] for r in admitted),
        "rows": rows,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--zip", required=True)
    parser.add_argument("--model-dir", required=True)
    parser.add_argument("--out")
    parser.add_argument("--without-current-state", action="store_true")
    args = parser.parse_args()
    template, tokenizer, think = load(Path(args.model_dir))
    with zipfile.ZipFile(args.zip) as bundle:
        result = json.loads(bundle.read("result.json"))
        calls = [json.loads(line) for line in bundle.read("implementation/provider-calls.jsonl").splitlines() if line]
    report = {
        "schemaVersion": "vexlife-prod.claude-c20.provider-prefix-projection/v1",
        "truthClass": "DETERMINISTIC_PROJECTION_OF_PINNED_RUNTIME_NOT_TELEMETRY",
        "experimentRef": result["experimentRef"],
        "sourceSha": result["source"]["sourceSha"],
        "pinned": PINNED,
        "runtime": "mlx-lm==0.32.0 server, qwen3_5 hybrid cache (non-trimmable), prompt-cache-size 10",
        "jinja2": jinja2.__version__,
        "counterfactual": "WITHOUT_CURRENT_STATE_ENVELOPE" if args.without_current_state else None,
    }
    report.update(analyze(calls, template, tokenizer, think, args.without_current_state))
    text = json.dumps(report, ensure_ascii=False)
    if args.out:
        Path(args.out).write_text(text + "\n")
    summary = {k: v for k, v in report.items() if k != "rows"}
    print(json.dumps(summary, ensure_ascii=False))


if __name__ == "__main__":
    sys.exit(main())
