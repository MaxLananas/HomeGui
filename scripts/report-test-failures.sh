#!/usr/bin/env bash
#
# Turns JUnit XML reports into a readable failure report.
#
# CI logs are large and noisy; this pulls out exactly the tests that failed, why, and
# puts them in the job summary plus a workflow annotation so a red build can be
# diagnosed without opening the raw log.
set -uo pipefail

cd "$(dirname "$0")/.."

exec python3 - <<'PY'
import glob
import os
import sys
import xml.etree.ElementTree as ET

failures = []
totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}

for path in sorted(glob.glob("*/build/test-results/test/*.xml")):
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError as error:
        failures.append("%s: unreadable report (%s)" % (path, error))
        continue
    for key in totals:
        totals[key] += int(root.get(key, "0"))
    for case in root.iter("testcase"):
        for problem in list(case.iter("failure")) + list(case.iter("error")):
            message = (problem.get("message") or "").strip()
            text = (problem.text or "").strip()
            body = message or text.splitlines()[0] if text else message
            stack = [line for line in text.splitlines()
                     if "com.maxlananas.homegui" in line][:3]
            failures.append("%s.%s\n    %s\n%s" % (
                case.get("classname"), case.get("name"), body,
                "\n".join("    " + line.strip() for line in stack)))

print("Tests: %(tests)d, failures: %(failures)d, errors: %(errors)d, skipped: %(skipped)d" % totals)

if not failures:
    print("No test failure was recorded; the build failed earlier than the tests.")
    sys.exit(0)

with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
    summary.write("## Failing tests\n\n")
    summary.write("| Tests | Failures | Errors | Skipped |\n|---|---|---|---|\n")
    summary.write("| %(tests)d | %(failures)d | %(errors)d | %(skipped)d |\n\n" % totals)
    for failure in failures:
        summary.write("```\n" + failure + "\n```\n\n")

annotation = "\n".join(failures)[:3500]
annotation = annotation.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
print("::error title=%d failing test(s)::%s" % (len(failures), annotation))
PY
