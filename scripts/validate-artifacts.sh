#!/usr/bin/env bash
set -euo pipefail

VERSION=$(sed -n 's/^mod_version=//p' gradle.properties)
MINECRAFT=$(sed -n 's/^minecraft_version=//p' gradle.properties)
mapfile -t jars < <(find build/libs -maxdepth 1 -type f -name "homegui-fabric-${VERSION}.jar")

if (( ${#jars[@]} != 1 )); then
  echo "Expected exactly one Fabric release JAR, found ${#jars[@]}" >&2
  exit 1
fi
jar=${jars[0]}
test "$(stat -c%s "$jar")" -gt 20000
unzip -tqq "$jar"

metadata=$(unzip -p "$jar" fabric.mod.json)
python3 - "$VERSION" "$MINECRAFT" "$metadata" <<'PY'
import json, sys
version, minecraft, raw = sys.argv[1:]
data = json.loads(raw)
assert data["id"] == "homegui"
assert data["version"] == version
assert data["environment"] == "client"
assert data["depends"]["minecraft"] == minecraft
assert data["depends"]["java"] == ">=21"
assert data["depends"]["fabric-api"] == "*"
PY

for required in homegui.mixins.json assets/homegui/icon.png LICENSE_homegui; do
  unzip -Z1 "$jar" | grep -Fxq "$required" || { echo "Missing $required" >&2; exit 1; }
done

if unzip -Z1 "$jar" | grep -Eq '(^|/)(.*Test|\.git|run/|homegui-export\.json)'; then
  echo "Release JAR contains development files" >&2
  exit 1
fi

echo "Validated $(basename "$jar") ($(stat -c%s "$jar") bytes)"
