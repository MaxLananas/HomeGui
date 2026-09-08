#!/usr/bin/env bash
#
# Makes a red build readable without opening the raw log.
#
# Usage: .github/report-failure.sh <gradle log>
#
# Writes the failing tests and the relevant part of the Gradle output to the job
# summary, and re-emits the most important lines as workflow annotations so they are
# visible on the run page, in the pull request timeline and through the API.
set -uo pipefail

LOG="${1:-build.log}"
cd "$(dirname "$0")/.."

SUMMARY="${GITHUB_STEP_SUMMARY:-/dev/null}"

if [ -x scripts/report-test-failures.sh ]; then
    ./scripts/report-test-failures.sh || true
fi

{
    echo
    echo '### Gradle output'
    echo
    echo '```text'
    if [ -f "$LOG" ]; then
        tail -n 120 "$LOG"
    else
        echo "no build log was produced"
    fi
    echo '```'
} >> "$SUMMARY"

# Every javac error with its detail lines, not just the tail of the log: the first
# error in a file is the one that explains the rest, and a tail cuts it off.
MESSAGE=$(if [ -f "$LOG" ]; then
    {
        grep -E -A4 '^[^ ]*\.java:[0-9]+: error:' "$LOG" | head -c 2400
        echo
        grep -E -A6 '(FAILURE:|What went wrong:|Execution failed for task)' "$LOG" | head -c 600
    }
else
    echo "no build log was produced"
fi)

MESSAGE=${MESSAGE//'%'/'%25'}
MESSAGE=${MESSAGE//$'\r'/'%0D'}
MESSAGE=${MESSAGE//$'\n'/'%0A'}
echo "::error title=Build failure::${MESSAGE:0:3000}"
