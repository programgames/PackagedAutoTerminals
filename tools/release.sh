#!/usr/bin/env bash
# Builds the jar, tags the commit, and creates the GitHub release.
#
# The version comes from gradle.properties. Edit modVersion there first, write the
# CHANGELOG section for it, and commit. Then run this script.
#
#   bash tools/release.sh            build, tag and publish
#   bash tools/release.sh --dry-run  show what would happen, change nothing
#
# Once the release is published, the `release` workflow sends the jar to CurseForge.

set -euo pipefail
cd "$(dirname "$0")/.."

DRY=0
[ "${1:-}" = "--dry-run" ] && DRY=1

version="$(grep -E '^modVersion=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"
mc="$(grep -E '^mcVersion=' gradle.properties | cut -d= -f2 | tr -d '[:space:]')"
tag="v${version}"
jar="build/libs/packagedautoterminals-${mc}-${version}.jar"
sources="build/libs/packagedautoterminals-${mc}-${version}-sources.jar"

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

# 3. The CHANGELOG must carry a section for this version.
if ! grep -q "^## \[${version}\]" CHANGELOG.md; then
  echo "ERROR: CHANGELOG.md carries no section '## [${version}]'." >&2
  exit 1
fi

# 4. ForgeGradle 2.3 only runs on JDK 8. Stop here rather than inside Gradle.
jv="$("${JAVA_HOME:-/usr}/bin/java" -version 2>&1 | head -1)"
case "$jv" in
  *'"1.8'*) : ;;
  *) echo "ERROR: JDK 8 is required. JAVA_HOME points at: ${jv}" >&2; exit 1 ;;
esac

# 5. update.json must announce this version, otherwise the in-game notice stays behind.
if ! grep -q "\"${version}\"" update.json; then
  echo "ERROR: update.json does not list ${version}. Add the entry and the promo." >&2
  exit 1
fi

# The release notes are the CHANGELOG section of this version, without its title line.
notes="$(awk -v v="## [${version}]" '
  index($0, v) == 1 { on = 1; next }
  on && /^## \[/ { exit }
  on { print }
' CHANGELOG.md)"

pre=""
case "$version" in *alpha*|*beta*|*rc*) pre="--prerelease" ;; esac

if [ "$DRY" = "1" ]; then
  echo "--- dry run: notes ---"
  echo "$notes"
  echo "--- dry run: would build, tag ${tag}, and publish ${jar} ---"
  exit 0
fi

./gradlew clean build --no-daemon

[ -f "$jar" ] || { echo "ERROR: ${jar} is missing after the build." >&2; exit 1; }

git tag -a "$tag" -m "Release ${version}"
git push origin "$tag"

files=("$jar")
[ -f "$sources" ] && files+=("$sources")

gh release create "$tag" "${files[@]}" \
  --title "PackagedAuto Terminals ${version}" \
  --notes "$notes" $pre

echo "Done. The release workflow now sends the jar to CurseForge."
