#!/usr/bin/env python3
# Copyright (C) 2025-2026 TESOBE GmbH
# SPDX-License-Identifier: AGPL-3.0-or-later
# Licensed under the GNU Affero General Public License v3 or later: see the LICENSE and NOTICE files.
"""Write snapshot.json: what an OBP-API documents for each call in contract.json.

    python3 obp-api-contract/snapshot.py --base https://k8s.openbankproject.com \
        [--fallback https://apisandbox.openbankproject.com]

For every version used in contract.json it reads /obp/{v}/resource-docs/{v}/obp once,
from --base, or from --fallback when --base does not serve that version (a deployment
that switched it off). It finds the endpoint each call reaches and records its operation
id and every response field path, taken from typed_success_response_body. A call with no
endpoint is recorded as missing, so the contract test fails on it. Needs no credentials;
uses only the standard library.

Matching: OBP writes placeholders in capitals (BANK_ID) and so are some literal segments
(the transaction request type OPEN_CORRIDOR_PROMISE). A segment matches when it is equal,
when the documented one is in capitals (a placeholder for a value the call fills in), or
when both are in capitals. The documented path with the most equal segments wins.
"""
import argparse
import datetime
import json
import os
import re
import sys
import urllib.error
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
PLACEHOLDER = re.compile(r"^[A-Z][A-Z0-9_]*$")


def get_json(url):
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=300) as resp:
        return json.load(resp)


def segments(path):
    return [s for s in path.split("?")[0].split("/") if s]


def match_score(contract_path, documented_path):
    """Number of equal segments, or None when the paths cannot be the same endpoint."""
    a, b = segments(contract_path), segments(documented_path)
    if len(a) != len(b):
        return None
    score = 0
    for x, y in zip(a, b):
        if x == y:
            score += 1
        elif not PLACEHOLDER.match(y):
            return None
    return score


def schema_fields(schema, prefix=""):
    """Every field path in a JSON Schema: `a`, `a.b`, `a.list[]`, `a.list[].c`."""
    out = set()
    if not isinstance(schema, dict):
        return out
    if schema.get("type") == "array" or "items" in schema:
        out |= schema_fields(schema.get("items", {}), prefix + "[]")
    for name, sub in (schema.get("properties") or {}).items():
        path = f"{prefix}.{name}" if prefix else name
        out.add(path)
        out |= schema_fields(sub, path)
        if isinstance(sub, dict) and (sub.get("type") == "array" or "items" in sub):
            out.add(path + "[]")
    return out


def version_of(path):
    return segments(path)[1]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", required=True, help="OBP-API base URL")
    ap.add_argument("--fallback", help="OBP-API that still serves the versions --base switched off")
    ap.add_argument("--out", default=os.path.join(HERE, "snapshot.json"))
    args = ap.parse_args()
    base = args.base.rstrip("/")
    contract = json.load(open(os.path.join(HERE, "contract.json")))
    calls = contract["calls"]

    fallback = args.fallback.rstrip("/") if args.fallback else None
    docs, docs_from = {}, {}
    for v in sorted({version_of(c["path"]) for c in calls}):
        for host in [base] + ([fallback] if fallback else []):
            url = f"{host}/obp/{v}/resource-docs/{v}/obp"
            print(f"reading {url}", file=sys.stderr)
            try:
                docs[v] = get_json(url)["resource_docs"]
                docs_from[v] = host
                break
            except urllib.error.HTTPError as e:
                print(f"  HTTP {e.code}", file=sys.stderr)
        if v not in docs:
            sys.exit(f"no OBP-API given serves the resource docs of {v}")

    def about(host):
        newest = max((v for v in docs if docs_from[v] == host),
                     key=lambda v: tuple(int(p) for p in v[1:].split(".")))
        root = get_json(f"{host}/obp/{newest}/root")
        return {"version": root.get("version"), "git_commit": root.get("git_commit")}
    hosts = {h: about(h) for h in sorted(set(docs_from.values()))}

    endpoints = []
    for c in calls:
        v = version_of(c["path"])
        scored = []
        for d in docs[v]:
            if d["request_verb"] != c["verb"]:
                continue
            score = match_score(c["path"], d.get("specified_url") or d["request_url"])
            if score is not None:
                scored.append((score, d))
        scored.sort(key=lambda sd: -sd[0])
        entry = {"verb": c["verb"], "path": c["path"], "docs_from": docs_from[v]}
        if scored and len(scored) > 1 and scored[0][0] == scored[1][0]:
            entry["ambiguous"] = sorted(d["operation_id"] for s, d in scored if s == scored[0][0])
        if scored:
            d = scored[0][1]
            entry.update({
                "operation_id": d["operation_id"],
                "documented_path": d.get("specified_url") or d["request_url"],
                "fields": sorted(schema_fields(d.get("typed_success_response_body") or {})),
            })
        else:
            entry["missing"] = True
        endpoints.append(entry)

    snapshot = {
        "about": "Written by obp-api-contract/snapshot.py; do not edit by hand.",
        "obp_api": hosts,
        "taken_at": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "endpoints": endpoints,
    }
    with open(args.out, "w") as f:
        json.dump(snapshot, f, indent=1, ensure_ascii=False)
        f.write("\n")
    missing = [e["verb"] + " " + e["path"] for e in endpoints if e.get("missing")]
    print(f"wrote {args.out}: {len(endpoints)} calls, {len(missing)} with no endpoint", file=sys.stderr)
    for m in missing:
        print(f"  no endpoint: {m}", file=sys.stderr)


if __name__ == "__main__":
    main()
