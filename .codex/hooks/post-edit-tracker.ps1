$ErrorActionPreference = "Stop"

$stdin = [Console]::In.ReadToEnd()
if ([string]::IsNullOrWhiteSpace($stdin)) {
    exit 0
}

try {
    $payload = $stdin | ConvertFrom-Json
} catch {
    exit 0
}

$filePath = $payload.file_path
$sessionId = $payload.session_id
if ([string]::IsNullOrWhiteSpace($filePath) -or [string]::IsNullOrWhiteSpace($sessionId)) {
    exit 0
}

if ($filePath -match '\.(kt|kts|java|xml|gradle|swift|md|json|ya?ml|properties)$') {
    $safeSessionId = ($sessionId -replace '[^A-Za-z0-9_.-]', '_')
    $trackFile = Join-Path ([IO.Path]::GetTempPath()) "runninghub_codex_edits_$safeSessionId.txt"
    Add-Content -LiteralPath $trackFile -Value $filePath -Encoding UTF8
}

exit 0
