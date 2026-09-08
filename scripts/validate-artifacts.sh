#!/usr/bin/env bash
#
# Validates every release JAR HomeGui produces.
#
# Usage:
#   scripts/validate-artifacts.sh                 # validate everything that was built
#   scripts/validate-artifacts.sh fabric-1.21.11  # validate one module's artifact
#
# A JAR is only accepted when it is non empty, internally consistent, named
# homegui-<version>-<loader>-<minecraft>.jar, and its metadata agrees with that name
# about the loader, the Minecraft version and the Java version. Development output,
# sources and test classes are rejected, and two artifacts may never claim the same
# loader and Minecraft pair.
set -euo pipefail

cd "$(dirname "$0")/.."
VERSION=$(sed -n 's/^mod_version=//p' gradle.properties)

exec python3 - "$VERSION" "${1:-}" <<'PY'
import json, os, re, subprocess, sys, zipfile

version = sys.argv[1]
wanted = sys.argv[2]

NAME = re.compile(r"^homegui-(\d+\.\d+\.\d+)-([a-z]+)-([0-9.]+)\.jar$")
BANNED = re.compile(r"Tests?\.class$|(^|/)(\.git|run|out)/|homegui-export\.json|\.tmp$")

METADATA_FILE = {
    "fabric": "fabric.mod.json",
    "neoforge": "META-INF/neoforge.mods.toml",
    "forge": "META-INF/mods.toml",
}


def expected_java(minecraft):
    """Minecraft declares its own minimum Java; the metadata has to agree with it."""
    if minecraft.startswith("26."):
        return "25"
    if minecraft.startswith("1.20.") and minecraft.split(".")[1] in ("1", "2", "3", "4"):
        return "17"
    if minecraft.startswith("1.1") or minecraft.startswith("1.16"):
        return "8"
    return "21"


def check(jar):
    problems = []
    name = os.path.basename(jar)
    match = NAME.match(name)
    if not match:
        return ["%s: name must be homegui-<version>-<loader>-<minecraft>.jar" % name]
    jar_version, loader, minecraft = match.groups()
    if jar_version != version:
        problems.append("%s: version %s, gradle.properties says %s" % (name, jar_version, version))
    if loader not in METADATA_FILE:
        problems.append("%s: unknown loader %s" % (name, loader))
        return problems

    size = os.path.getsize(jar)
    if size < 20_000:
        problems.append("%s: only %d bytes, too small to be a real artifact" % (name, size))
    if subprocess.run(["unzip", "-tqq", jar]).returncode != 0:
        problems.append("%s: the archive is damaged" % name)
        return problems

    with zipfile.ZipFile(jar) as archive:
        entries = archive.namelist()
        listing = set(entries)
        metadata_name = METADATA_FILE[loader]
        if metadata_name not in listing:
            problems.append("%s: missing %s" % (name, metadata_name))
        else:
            problems += check_metadata(name, loader, minecraft,
                                       archive.read(metadata_name).decode("utf-8"))

        for required in ("homegui.mixins.json", "assets/homegui/icon.png",
                         "LICENSE_homegui", "assets/homegui/lang/en_us.json"):
            if required not in listing:
                problems.append("%s: missing %s" % (name, required))

        classes = [entry for entry in entries if entry.endswith(".class")]
        if not classes:
            problems.append("%s: contains no classes" % name)
        for package in ("com/maxlananas/homegui/core/", "com/maxlananas/homegui/ui/",
                        "com/maxlananas/homegui/mc/"):
            if not any(entry.startswith(package) for entry in entries):
                problems.append("%s: %s classes are not bundled" % (name, package.split("/")[-2]))

        for entry in entries:
            if BANNED.search(entry):
                problems.append("%s: contains development output %s" % (name, entry))
            if "/src/" in entry or entry.endswith(".java") or entry.endswith("-sources.jar"):
                problems.append("%s: contains sources (%s)" % (name, entry))

    if problems:
        for problem in problems:
            print("FAIL  " + problem)
        return problems
    print("OK    %s (%d bytes, %d classes)" % (name, size, len(classes)))
    return []


def check_metadata(name, loader, minecraft, metadata):
    problems = []
    if loader == "fabric":
        data = json.loads(metadata)
        if data.get("id") != "homegui":
            problems.append("%s: wrong mod id %r" % (name, data.get("id")))
        if data.get("version") != version:
            problems.append("%s: metadata version %r" % (name, data.get("version")))
        if data.get("environment") != "client":
            problems.append("%s: environment must be client" % name)
        if data.get("license") != "CC-BY-NC-4.0":
            problems.append("%s: wrong license %r" % (name, data.get("license")))
        if not data.get("icon"):
            problems.append("%s: no icon declared" % name)
        depends = data.get("depends", {})
        if depends.get("minecraft") != minecraft:
            problems.append("%s: metadata targets Minecraft %r" % (name, depends.get("minecraft")))
        if depends.get("java") != ">=" + expected_java(minecraft):
            problems.append("%s: metadata java %r, expected >=%s"
                            % (name, depends.get("java"), expected_java(minecraft)))
        if depends.get("fabric-api") != "*":
            problems.append("%s: fabric-api dependency missing" % name)
        if not data.get("entrypoints", {}).get("client"):
            problems.append("%s: no client entrypoint" % name)
        if "homegui.mixins.json" not in data.get("mixins", []):
            problems.append("%s: mixin config not declared" % name)
        return problems

    if 'modId = "homegui"' not in metadata and 'modId="homegui"' not in metadata:
        problems.append("%s: mod id not declared" % name)
    if '"%s"' % minecraft not in metadata and minecraft not in metadata:
        problems.append("%s: Minecraft %s not in the declared range" % (name, minecraft))
    if "clientSideOnly" not in metadata:
        problems.append("%s: client-only flag not declared" % name)
    if "homegui.mixins.json" not in metadata:
        problems.append("%s: mixin config not declared" % name)
    return problems


libraries = []
if os.path.isdir("artifacts"):
    for candidate in sorted(os.listdir("artifacts")):
        if wanted and candidate != wanted:
            continue
        path = os.path.join("artifacts", candidate, "build", "libs")
        if os.path.isdir(path):
            libraries.append(path)

jars = []
for path in libraries:
    for entry in sorted(os.listdir(path)):
        if entry.endswith(".jar") and not entry.endswith(("-sources.jar", "-dev.jar")):
            jars.append(os.path.join(path, entry))

if not jars:
    print("FAIL  no release JAR was built" + (" for " + wanted if wanted else ""))
    raise SystemExit(1)

seen = {}
for jar in jars:
    match = NAME.match(os.path.basename(jar))
    if not match:
        continue
    key = (match.group(2), match.group(3))
    if key in seen:
        print("FAIL  two artifacts claim %s %s" % key)
        raise SystemExit(1)
    seen[key] = jar

problems = 0
for jar in jars:
    problems += len(check(jar))
print("Checked %d artifact(s), %d problem(s)." % (len(jars), problems))
raise SystemExit(1 if problems else 0)
PY
