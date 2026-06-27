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

$sessionId = $payload.session_id
if ([string]::IsNullOrWhiteSpace($sessionId)) {
    exit 0
}

$safeSessionId = ($sessionId -replace '[^A-Za-z0-9_.-]', '_')
$trackFile = Join-Path ([IO.Path]::GetTempPath()) "runninghub_codex_edits_$safeSessionId.txt"
$reviewFileThreshold = 2

if (Test-Path -LiteralPath $trackFile) {
    $fileCount = (Get-Content -LiteralPath $trackFile | Sort-Object -Unique | Measure-Object).Count
    Remove-Item -LiteralPath $trackFile -Force
    if ($fileCount -ge $reviewFileThreshold) {
        [Console]::Error.WriteLine("[Rule] $fileCount project files were edited. Review with .codex/skills/code_review/SKILL.md before completion.")
        exit 2
    }
}

exit 0
