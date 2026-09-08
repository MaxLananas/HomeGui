#!/usr/bin/env bash
#
# Pushes the CHANGELOG.md section for the current version to the matching Modrinth
# version. Publishing sets the changelog once; this keeps the public page in step with
# the file in the repository when the wording is corrected afterwards.
#
# The token is read from MODRINTH_API_KEY and is never printed.
set -euo pipefail

cd "$(dirname "$0")/.."
PROJECT_ID=$(sed -n 's/^modrinth_project_id=//p' gradle.properties)
VERSION=$(sed -n 's/^mod_version=//p' gradle.properties)

: "${MODRINTH_API_KEY:?MODRINTH_API_KEY must be set}"

exec python3 - "$PROJECT_ID" "$VERSION" <<'PY'
import json
import os
import pathlib
import sys
import urllib.request

project_id, version = sys.argv[1], sys.argv[2]
token = os.environ["MODRINTH_API_KEY"]
api = "https://api.modrinth.com/v2"


def request(path, method="GET", payload=None):
    data = json.dumps(payload).encode() if payload is not None else None
    call = urllib.request.Request(api + path, data=data, method=method)
    call.add_header("User-Agent", "HomeGui/release-tooling")
    call.add_header("Authorization", token)
    if data:
        call.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(call) as response:
        body = response.read().decode()
        return response.status, body


text = pathlib.Path("CHANGELOG.md").read_text(encoding="utf-8")
start = text.find("## " + version)
if start < 0:
    sys.exit("CHANGELOG.md has no section for " + version)
end = text.find("\n## ", start + 1)
changelog = (text if end < 0 else text[:end])[start:].strip()

_, body = request("/project/%s/version" % project_id)
versions = json.loads(body)
targets = [v for v in versions if v["version_number"].split("-")[0] == version]
if not targets:
    sys.exit("no Modrinth version matches " + version)

for target in targets:
    if target["changelog"] == changelog:
        print("unchanged: %s" % target["version_number"])
        continue
    status, _ = request("/version/%s" % target["id"], "PATCH", {"changelog": changelog})
    print("updated %s (%s) -> HTTP %d" % (target["version_number"], target["id"], status))
PY
