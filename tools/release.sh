#!/usr/bin/env bash
# Checks the release, then pushes the tag that starts it.
#
# The version comes from gradle.properties. Edit modVersion there first, write the
# CHANGELOG section and the update.json entry for it, and commit. Then run this script.
#
#   bash tools/release.sh            check, tag and push
#   bash tools/release.sh --dry-run  show what would happen, change nothing
#
# GitHub does the rest: the `release` workflow builds the jar, runs the tests, creates the
# GitHub release from the CHANGELOG section, and sends the jar to CurseForge.
#
# The checks below also run in the workflow. They run here first so a mistake costs five
# seconds instead of a failed run and a tag to delete.

set -euo pipefail
cd "$(dirname "$0")/.."

DRY=0
[ "${1:-}" = "--dry-run" ] && DRY=1

version="$(grep -E '^modVersion=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"
tag="v${version}"

echo "Version : ${version}"
echo "Tag     : ${tag}"

# 1. Every tracked change must be committed: the tag points at a commit, not at your desk.
#    Untracked files only draw a warning. They reach neither the tag nor the jar.
if ! git diff --quiet HEAD -- .; then
  echo "ERROR: tracked files carry uncommitted changes. Commit them first." >&2
  git status --short --untracked-files=no >&2
  exit 1
fi
untracked="$(git ls-files --others --exclude-standard)"
if [ -n "$untracked" ]; then
  echo "WARNING: these files are untracked and stay out of the release:" >&2
  echo "$untracked" | sed 's/^/  /' >&2
fi

# 2. The tag must be free.
if git rev-parse "$tag" >/dev/null 2>&1; then
  echo "ERROR: the tag ${tag} already exists. Raise modVersion in gradle.properties." >&2
  exit 1
fi

# 3. The CHANGELOG must carry a section for this version. The workflow reads it as the
#    release notes, and stops when it is empty.
if ! grep -q "^## \[${version}\]" CHANGELOG.md; then
  echo "ERROR: CHANGELOG.md carries no section '## [${version}]'." >&2
  exit 1
fi

# 4. update.json must announce this version, otherwise the in-game notice stays behind.
if ! grep -q "\"${version}\"" update.json; then
  echo "ERROR: update.json does not list ${version}. Add the entry and the promo." >&2
  exit 1
fi

# 5. The build must pass here too. A red CI run after the tag is pushed means deleting the
#    tag, and deleting a tag other people may already have fetched.
#    ForgeGradle 2.3 only runs on JDK 8.
jv="$("${JAVA_HOME:-/usr}/bin/java" -version 2>&1 | head -1)"
case "$jv" in
  *'"1.8'*) : ;;
  *) echo "ERROR: JDK 8 is required. JAVA_HOME points at: ${jv}" >&2; exit 1 ;;
esac

if [ "$DRY" = "1" ]; then
  echo "--- dry run: release notes ---"
  awk -v v="## [${version}]" '
    index($0, v) == 1 { on = 1; next }
    on && /^## \[/ { exit }
    on { print }
  ' CHANGELOG.md
  echo "--- dry run: would build, then push the tag ${tag} ---"
  exit 0
fi

./gradlew build --no-daemon

git tag -a "$tag" -m "Release ${version}"
git push origin "$tag"

echo
echo "Tag ${tag} pushed. GitHub now builds the release and sends it to CurseForge."
echo "Watch it here: https://github.com/programgames/PackagedAutoTerminals/actions"
