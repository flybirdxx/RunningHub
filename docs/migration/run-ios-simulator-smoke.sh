#!/usr/bin/env bash
set -euo pipefail

APP_PATH="build/xcode/DerivedData/Build/Products/Debug-iphonesimulator/RunningHub.app"
BUNDLE_ID="com.runninghub.app.ios"
OUTPUT_PATH="build/xcode/simulator-launch-smoke.txt"
SELF_TEST="false"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --app-path)
      APP_PATH="$2"
      shift 2
      ;;
    --bundle-id)
      BUNDLE_ID="$2"
      shift 2
      ;;
    --output)
      OUTPUT_PATH="$2"
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

if [[ "$SELF_TEST" == "true" ]]; then
  IMPLEMENTATION_TEXT="$(awk 'found { print } /^cd "\$ROOT_DIR"$/ { found = 1 }' "$0")"
  grep -q "xcrun simctl bootstatus" <<< "$IMPLEMENTATION_TEXT"
  grep -q "xcrun simctl install" <<< "$IMPLEMENTATION_TEXT"
  grep -q "xcrun simctl launch" <<< "$IMPLEMENTATION_TEXT"
  grep -q "xcrun simctl io" <<< "$IMPLEMENTATION_TEXT"
  grep -q "xcrun simctl spawn" <<< "$IMPLEMENTATION_TEXT"
  grep -q "startupCrashScan: pass" <<< "$IMPLEMENTATION_TEXT"
  grep -q "screenshotPath:" <<< "$IMPLEMENTATION_TEXT"
  grep -q "simulatorLaunchResult: pass" <<< "$IMPLEMENTATION_TEXT"
  echo "SelfTest passed."
  exit 0
fi

cd "$ROOT_DIR"

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "iOS Simulator smoke must run on macOS." >&2
  exit 1
fi

if [[ ! -d "$APP_PATH" ]]; then
  echo "Built app was not found at $APP_PATH." >&2
  exit 1
fi

DEVICE_TYPE="$(
  xcrun simctl list devicetypes --json | python3 -c '
import json
import sys

data = json.load(sys.stdin)
devices = data.get("devicetypes", [])
iphones = [item for item in devices if "iPhone" in item.get("name", "")]
preferred_names = ["iPhone 16", "iPhone 15", "iPhone 14", "iPhone 13"]
for preferred in preferred_names:
    for item in iphones:
        if item.get("name") == preferred:
            print(item["identifier"])
            raise SystemExit(0)
if iphones:
    print(iphones[-1]["identifier"])
    raise SystemExit(0)
raise SystemExit("No iPhone device type is available.")
'
)"

RUNTIME_ID="$(
  xcrun simctl list runtimes --json | python3 -c '
import json
import re
import sys

data = json.load(sys.stdin)
runtimes = []
for item in data.get("runtimes", []):
    if not item.get("isAvailable", False):
        continue
    text = " ".join(str(item.get(key, "")) for key in ("identifier", "name", "platform"))
    if "iOS" not in text:
        continue
    version = item.get("version", "0")
    parts = tuple(int(part) for part in re.findall(r"\d+", version))
    runtimes.append((parts, item["identifier"]))
if not runtimes:
    raise SystemExit("No available iOS runtime is installed.")
print(sorted(runtimes)[-1][1])
'
)"

DEVICE_NAME="RunningHub CI Smoke"
UDID="$(xcrun simctl create "$DEVICE_NAME" "$DEVICE_TYPE" "$RUNTIME_ID")"
TEMP_FILES=()

cleanup() {
  xcrun simctl shutdown "$UDID" >/dev/null 2>&1 || true
  xcrun simctl delete "$UDID" >/dev/null 2>&1 || true
  for temp_file in "${TEMP_FILES[@]}"; do
    rm -f "$temp_file"
  done
}
trap cleanup EXIT

mkdir -p "$(dirname "$OUTPUT_PATH")"
SCREENSHOT_PATH="$(dirname "$OUTPUT_PATH")/$(basename "$OUTPUT_PATH" .txt)-screenshot.png"
CRASH_PATTERNS="Uncaught Kotlin exception|Terminating app|SIGABRT|IllegalStateException|CADisableMinimumFrameDurationOnPhone|EXC_"

xcrun simctl boot "$UDID"
xcrun simctl bootstatus "$UDID" -b
xcrun simctl install "$UDID" "$APP_PATH"
LAUNCH_OUTPUT="$(xcrun simctl launch "$UDID" "$BUNDLE_ID")"
sleep 5
xcrun simctl io "$UDID" screenshot "$SCREENSHOT_PATH" >/dev/null

OS_LOG_OUTPUT="$(mktemp)"
CRASH_MATCHES="$(mktemp)"
TEMP_FILES+=("$OS_LOG_OUTPUT" "$CRASH_MATCHES")
if ! xcrun simctl spawn "$UDID" log show --style compact --last 2m \
  --predicate 'process == "RunningHub" OR eventMessage CONTAINS[c] "com.runninghub.app.ios"' \
  > "$OS_LOG_OUTPUT" 2>/dev/null; then
  echo "Unable to collect Simulator OS log for startup crash scan." >&2
  exit 1
fi

if grep -E "$CRASH_PATTERNS" "$OS_LOG_OUTPUT" > "$CRASH_MATCHES"; then
  xcrun simctl terminate "$UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true
  cat > "$OUTPUT_PATH" <<EOF
capturedAt: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
appPath: $APP_PATH
bundleId: $BUNDLE_ID
deviceType: $DEVICE_TYPE
runtime: $RUNTIME_ID
screenshotPath: $SCREENSHOT_PATH
simulatorLaunchResult: pass
startupCrashScan: fail

## Launch output

\`\`\`text
$LAUNCH_OUTPUT
\`\`\`

## Startup crash matches

\`\`\`text
$(cat "$CRASH_MATCHES")
\`\`\`
EOF
  echo "Simulator startup crash scan failed. See $OUTPUT_PATH." >&2
  exit 1
fi

xcrun simctl terminate "$UDID" "$BUNDLE_ID" >/dev/null 2>&1 || true

cat > "$OUTPUT_PATH" <<EOF
capturedAt: $(date -u +"%Y-%m-%dT%H:%M:%SZ")
appPath: $APP_PATH
bundleId: $BUNDLE_ID
deviceType: $DEVICE_TYPE
runtime: $RUNTIME_ID
screenshotPath: $SCREENSHOT_PATH
simulatorLaunchResult: pass
startupCrashScan: pass

## Launch output

\`\`\`text
$LAUNCH_OUTPUT
\`\`\`
EOF

echo "simulatorSmokePath=$OUTPUT_PATH"
