#!/usr/bin/env bash
# Fails when the branch changes src/main but leaves the [Unreleased] section of CHANGELOG.md
# as it was. The build workflow runs it on every branch except main.
#
#   bash tools/check-changelog.sh          compares the commits of HEAD with origin/main
#   bash tools/check-changelog.sh <ref>    compares with another base
#
# Only commits count: commit the CHANGELOG line before you run it.

set -euo pipefail
cd "$(dirname "$0")/.."

base_ref="${1:-origin/main}"
base="$(git merge-base "$base_ref" HEAD)"

if git diff --quiet "$base" HEAD -- src/main; then
  echo "src/main is unchanged since ${base_ref}: the CHANGELOG check does not apply."
  exit 0
fi

unreleased() {
  git show "$1:CHANGELOG.md" | awk '
    /^## \[Unreleased\]/ { on = 1; next }
    on && /^## \[/ { exit }
    on { print }
  '
}

if [ "$(unreleased "$base")" = "$(unreleased HEAD)" ]; then
  echo "ERROR: this branch changes src/main, but not the [Unreleased] section of CHANGELOG.md." >&2
  echo "Describe the change for the player under '## [Unreleased]' in CHANGELOG.md, and commit it." >&2
  exit 1
fi

echo "CHANGELOG.md: the [Unreleased] section changes with src/main."
