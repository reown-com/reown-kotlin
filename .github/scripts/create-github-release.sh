#!/usr/bin/env bash
# Creates the BOM_<version> tag and GitHub release, then starts the sample apps release build.
#
# Usage: create-github-release.sh <tag> <sha>
# Needs GH_TOKEN with contents:write (tag, release) and actions:write (dispatch).
#
# Runs with GITHUB_TOKEN, so it doesn't depend on a personal access token that expires.
# A release created with GITHUB_TOKEN does not fire `release: published`, so the samples
# workflow is dispatched explicitly with the same settings the release event uses.
#
# Safe to re-run: an existing tag is reused, and if the release already exists (for example
# someone created it by hand, which already fired the release event) nothing else happens.
set -euo pipefail

TAG="${1:?tag required}"
SHA="${2:?sha required}"
REPO="${GITHUB_REPOSITORY:?GITHUB_REPOSITORY not set}"

# git/ref (singular) matches the exact ref. git/refs/tags/<name> also returns 200 for refs
# that only start with <name> (BOM_1.6.1 would match BOM_1.6.10).
if gh api "repos/$REPO/git/ref/tags/$TAG" >/dev/null 2>&1; then
  echo "Tag $TAG already exists, reusing it"
else
  gh api "repos/$REPO/git/refs" -f ref="refs/tags/$TAG" -f sha="$SHA" >/dev/null
  echo "Created tag $TAG at $SHA"
fi

if gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then
  echo "Release $TAG already exists, skipping release creation and sample build"
  exit 0
fi

gh release create "$TAG" \
  --repo "$REPO" \
  --verify-tag \
  --title "$TAG" \
  --generate-notes

gh workflow run ci_release_sample.yml \
  --repo "$REPO" \
  --ref "$TAG" \
  -f build_type=Release \
  -f wallet=true \
  -f dapp=true \
  -f modal=true \
  -f pos=false
echo "Dispatched the sample apps release build for $TAG"
