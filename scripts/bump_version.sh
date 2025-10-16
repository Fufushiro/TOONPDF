#!/usr/bin/env bash
set -euo pipefail

# Bump app version across Gradle, README, and optionally CHANGELOG.
# Usage:
#   scripts/bump_version.sh -v 0.3.1 [-c 6] [--changelog] [-d YYYY-MM-DD] [-n]
#
# Options:
#   -v  New versionName (required), e.g. 0.3.1
#   -c  New versionCode (optional). If not provided, it will auto-increment.
#   --changelog  Insert a new stub entry for this version in CHANGELOG.md if not present.
#   -d  Date to use in changelog (default: today, YYYY-MM-DD)
#   -n  Dry run (print changes without modifying files)
#
# Effects:
#   - Updates app/build.gradle.kts versionName and versionCode.
#   - Updates README.md occurrences:
#       - "# PDFTOON vX.Y.Z"
#       - "## 🚀 ¿Qué hay de nuevo en X.Y.Z?"
#       - "**PDFTOON vX.Y.Z**" (closing tagline)
#   - Optionally inserts a new section in CHANGELOG.md: "## [X.Y.Z] - YYYY-MM-DD" with empty subsections.

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_GRADLE="$ROOT_DIR/app/build.gradle.kts"
README="$ROOT_DIR/README.md"
CHANGELOG="$ROOT_DIR/CHANGELOG.md"

new_version=""
new_code=""
with_changelog=false
changelog_date="$(date +%F)"
dry_run=false

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    -v)
      new_version="$2"; shift 2 ;;
    -c)
      new_code="$2"; shift 2 ;;
    --changelog)
      with_changelog=true; shift ;;
    -d)
      changelog_date="$2"; shift 2 ;;
    -n)
      dry_run=true; shift ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

if [[ -z "$new_version" ]]; then
  echo "Error: -v <version> is required" >&2
  exit 1
fi

if [[ ! -f "$APP_GRADLE" ]]; then
  echo "Error: Not found $APP_GRADLE" >&2
  exit 1
fi

# Extract current versionName and versionCode
current_name=$(grep -Po 'versionName\s*=\s*"\K[^"]+' "$APP_GRADLE") || current_name=""
current_code=$(grep -Po 'versionCode\s*=\s*\K\d+' "$APP_GRADLE") || current_code=""

if [[ -z "$current_name" || -z "$current_code" ]]; then
  echo "Error: Could not parse current version from $APP_GRADLE" >&2
  exit 1
fi

if [[ -z "$new_code" ]]; then
  # Auto-increment versionCode by 1 if not provided
  if [[ "$current_code" =~ ^[0-9]+$ ]]; then
    new_code=$(( current_code + 1 ))
  else
    echo "Warning: current versionCode '$current_code' is not numeric, defaulting to 1" >&2
    new_code=1
  fi
fi

say() { echo "[bump-version] $*"; }
run() { if $dry_run; then say "DRY: $*"; else eval "$*"; fi }

say "Current versionName=$current_name, versionCode=$current_code"
say "New versionName=$new_version, versionCode=$new_code"

# 1) Update Gradle: versionName & versionCode
run "sed -i -E 's/(versionName\\s*=\\s*")$current_name(\")/\\1$new_version\\2/' '$APP_GRADLE'"
run "sed -i -E 's/(versionCode\\s*=\\s*)$current_code/\\1$new_code/' '$APP_GRADLE'"

# 2) Update README
if [[ -f "$README" ]]; then
  # H1: # PDFTOON vX.Y.Z
  run "sed -i -E 's/(^# PDFTOON v)$current_name(\s*$)/\\1$new_version\\2/' '$README'"
  # "¿Qué hay de nuevo en X.Y.Z?" (keep any prefix like emojis or hashes)
  run "sed -i -E 's/(¿Qué hay de nuevo en )$current_name(\?)/\\1$new_version\\2/' '$README'"
  # Closing tagline: **PDFTOON vX.Y.Z**
  run "sed -i -E 's/(\*\*PDFTOON v)$current_name(\*\*)/\\1$new_version\\2/' '$README'"
else
  say "README not found; skipping README update"
fi

# 3) Optionally insert a new entry in CHANGELOG.md if not present
if $with_changelog; then
  if [[ -f "$CHANGELOG" ]]; then
    if grep -q "\[$new_version\]" "$CHANGELOG"; then
      say "CHANGELOG already has entry [$new_version]; skipping insert"
    else
      say "Inserting new CHANGELOG entry for $new_version ($changelog_date)"
      tmpfile=$(mktemp)
      awk -v ver="$new_version" -v d="$changelog_date" '
        BEGIN { inserted=0 }
        {
          if (!inserted && /^## \[/) {
            print "## [" ver "] - " d "\n";
            print "### Added\n- \n\n### Changed\n- \n\n### Fixed\n- \n";
            inserted=1;
          }
          print;
        }
      ' "$CHANGELOG" > "$tmpfile"
      if $dry_run; then
        say "DRY: would update CHANGELOG with new stub entry"
        rm -f "$tmpfile"
      else
        mv "$tmpfile" "$CHANGELOG"
      fi
    fi
  else
    say "CHANGELOG.md not found; skipping changelog insert"
  fi
fi

say "Done."

