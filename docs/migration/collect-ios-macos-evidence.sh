#!/usr/bin/env bash
set -euo pipefail

OUTPUT_PATH="docs/migration/evidence/ios-macos-link-and-simulator.md"
SIMULATOR_SMOKE_RESULT="pending"
SMOKE_NOTES=""
SELF_TEST="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --output)
      OUTPUT_PATH="$2"
      shift 2
      ;;
    --simulator-smoke-pass)
      SIMULATOR_SMOKE_RESULT="pass"
      shift
      ;;
    --smoke-notes)
      SMOKE_NOTES="$2"
      shift 2
      ;;
    --self-test)
      SELF_TEST="true"
      shift
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 2
      ;;
  esac
done

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

write_evidence() {
  local output_path="$1"
  local link_result="$2"
  local simulator_smoke_result="$3"
  local smoke_notes="$4"
  local gradle_output="$5"
  local head_sha="$6"
  local xcodebuild_result="$7"
  local xcodebuild_output="$8"

  mkdir -p "$(dirname "$output_path")"
  cat > "$output_path" <<EOF
# iOS macOS link and Simulator smoke evidence

capturedAt: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
headSha: $head_sha
host: $(uname -a)
linkCommand: ./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64
linkResult: $link_result
xcodebuildCommand: xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination generic/platform=iOS Simulator build CODE_SIGNING_ALLOWED=NO
xcodebuildResult: $xcodebuild_result
simulatorSmokeResult: $simulator_smoke_result

## Simulator smoke scope

- Simulator login flow observed: $simulator_smoke_result
- Simulator logout/session flow observed: $simulator_smoke_result
- Simulator QuickCreate flow observed: $simulator_smoke_result

## Notes

$smoke_notes

## Gradle output tail

\`\`\`text
$gradle_output
\`\`\`

## Xcode build output tail

\`\`\`text
$xcodebuild_output
\`\`\`
EOF
}

if [[ "$SELF_TEST" == "true" ]]; then
  TMP_PATH="${TMPDIR:-/tmp}/rh-ios-macos-evidence-selftest.md"
  write_evidence "$TMP_PATH" "pass" "pass" "self-test" "BUILD SUCCESSFUL" "expected-sha" "pass" "** BUILD SUCCEEDED **"
  grep -q "headSha: expected-sha" "$TMP_PATH"
  grep -q "linkResult: pass" "$TMP_PATH"
  grep -q "xcodebuildResult: pass" "$TMP_PATH"
  grep -q "simulatorSmokeResult: pass" "$TMP_PATH"
  grep -q ":composeApp:linkDebugFrameworkIosSimulatorArm64" "$TMP_PATH"
  grep -q "xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub" "$TMP_PATH"
  grep -q "BUILD SUCCESSFUL" "$TMP_PATH"
  grep -q "\*\* BUILD SUCCEEDED \*\*" "$TMP_PATH"
  rm -f "$TMP_PATH"
  echo "SelfTest passed."
  exit 0
fi

cd "$ROOT_DIR"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "This evidence must be collected on macOS because iOS framework link is skipped on Windows/Linux." >&2
  exit 1
fi

if [[ "$SIMULATOR_SMOKE_RESULT" != "pass" ]]; then
  echo "Pass --simulator-smoke-pass only after manually verifying Simulator login, logout/session, and QuickCreate smoke." >&2
  exit 1
fi

if [[ -z "${SMOKE_NOTES//[[:space:]]/}" ]]; then
  echo "Pass --smoke-notes with the Simulator device, OS, and checked flows before collecting final evidence." >&2
  exit 1
fi

HEAD_SHA="$(git rev-parse HEAD)"
if [[ -z "${HEAD_SHA//[[:space:]]/}" ]]; then
  echo "Unable to resolve current git HEAD for iOS evidence." >&2
  exit 1
fi

TMP_OUTPUT="$(mktemp)"
if ./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64 > "$TMP_OUTPUT" 2>&1; then
  GRADLE_TAIL="$(tail -n 120 "$TMP_OUTPUT")"
  if ! grep -q "BUILD SUCCESSFUL" "$TMP_OUTPUT"; then
    echo "Gradle completed but BUILD SUCCESSFUL was not found in output." >&2
    cat "$TMP_OUTPUT" >&2
    rm -f "$TMP_OUTPUT"
    exit 1
  fi
else
  cat "$TMP_OUTPUT" >&2
  rm -f "$TMP_OUTPUT"
  exit 1
fi

XCODE_OUTPUT="$(mktemp)"
if xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' build CODE_SIGNING_ALLOWED=NO > "$XCODE_OUTPUT" 2>&1; then
  XCODE_TAIL="$(tail -n 120 "$XCODE_OUTPUT")"
  if ! grep -q "\*\* BUILD SUCCEEDED \*\*" "$XCODE_OUTPUT"; then
    echo "xcodebuild completed but BUILD SUCCEEDED was not found in output." >&2
    cat "$XCODE_OUTPUT" >&2
    rm -f "$TMP_OUTPUT" "$XCODE_OUTPUT"
    exit 1
  fi
  write_evidence "$OUTPUT_PATH" "pass" "$SIMULATOR_SMOKE_RESULT" "$SMOKE_NOTES" "$GRADLE_TAIL" "$HEAD_SHA" "pass" "$XCODE_TAIL"
  rm -f "$TMP_OUTPUT" "$XCODE_OUTPUT"
  echo "iosEvidence=$OUTPUT_PATH"
else
  cat "$XCODE_OUTPUT" >&2
  rm -f "$TMP_OUTPUT" "$XCODE_OUTPUT"
  exit 1
fi
