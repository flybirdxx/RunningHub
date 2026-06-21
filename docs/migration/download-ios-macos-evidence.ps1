param(
    [string] $Repo = "flybirdxx/RunningHub",
    [string] $OutputPath = "docs/migration/evidence/ios-macos-link-and-simulator.md",
    [string] $RunId = "",
    [string] $HeadSha = "",
    [string] $Branch = "",
    [string] $ArtifactName = "ios-macos-link-and-simulator",
    [switch] $SelfTest
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$runJsonFields = "databaseId,workflowName,headBranch,headSha,status,conclusion,createdAt,updatedAt,url,event"
$expectedEvidenceFileName = "ios-macos-link-and-simulator.md"

function Resolve-GitHeadSha {
    param([string] $RequestedHeadSha)

    if (-not [string]::IsNullOrWhiteSpace($RequestedHeadSha)) {
        return $RequestedHeadSha.Trim()
    }

    $resolved = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($resolved)) {
        throw "Unable to resolve current git HEAD. Pass -HeadSha explicitly when downloading iOS evidence outside a Git worktree."
    }
    return $resolved
}

function Invoke-GhRunList {
    param([string] $Repository)

    $json = & gh run list `
        --repo $Repository `
        --workflow "iOS CI" `
        --limit 30 `
        --json $runJsonFields

    if ($LASTEXITCODE -ne 0) {
        throw "gh run list failed for iOS CI with exit code $LASTEXITCODE."
    }

    return @($json | ConvertFrom-Json)
}

function Select-IosEvidenceRun {
    param(
        [object[]] $Runs,
        [string] $RequestedRunId,
        [string] $RequestedHeadSha,
        [string] $RequestedBranch
    )

    $matches = @($Runs | Where-Object {
        $_.workflowName -eq "iOS CI" -and
        $_.event -eq "workflow_dispatch" -and
        $_.status -eq "completed" -and
        $_.conclusion -eq "success"
    })

    if (-not [string]::IsNullOrWhiteSpace($RequestedRunId)) {
        $matches = @($matches | Where-Object { [string] $_.databaseId -eq $RequestedRunId.Trim() })
    }

    if (-not [string]::IsNullOrWhiteSpace($RequestedHeadSha)) {
        $matches = @($matches | Where-Object { $_.headSha -eq $RequestedHeadSha })
    }

    if (-not [string]::IsNullOrWhiteSpace($RequestedBranch)) {
        $matches = @($matches | Where-Object { $_.headBranch -eq $RequestedBranch })
    }

    if ($matches.Count -eq 0) {
        $latest = @($Runs | Select-Object -First 5 | ForEach-Object {
            "$($_.workflowName) event=$($_.event) status=$($_.status) conclusion=$($_.conclusion) branch=$($_.headBranch) sha=$($_.headSha) run=$($_.databaseId)"
        })
        throw "No completed successful workflow_dispatch iOS CI run matched the requested filters. Latest candidates: $($latest -join '; ')"
    }

    return $matches[0]
}

function Invoke-GhRunDownload {
    param(
        [string] $Repository,
        [string] $DatabaseId,
        [string] $Name,
        [string] $Directory
    )

    & gh run download $DatabaseId `
        --repo $Repository `
        --name $Name `
        --dir $Directory

    if ($LASTEXITCODE -ne 0) {
        throw "gh run download failed for run $DatabaseId and artifact '$Name' with exit code $LASTEXITCODE."
    }
}

function Assert-IosEvidenceMarkdown {
    param([string] $Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "iOS evidence markdown was not found at $Path."
    }

    $text = Get-Content -Raw -LiteralPath $Path
    foreach ($snippet in @(
        "headSha:",
        "host:",
        "Darwin",
        "linkCommand: ./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64",
        "linkResult: pass",
        "xcodebuildCommand: xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub",
        "xcodebuildResult: pass",
        "simulatorSmokeResult: pass",
        "## Notes"
    )) {
        if ($text -notlike "*$snippet*") {
            throw "iOS evidence markdown at $Path is missing required snippet: $snippet"
        }
    }
}

function Copy-IosEvidenceArtifact {
    param(
        [string] $DownloadDirectory,
        [string] $DestinationPath
    )

    $source = Get-ChildItem -LiteralPath $DownloadDirectory -Recurse -File -Filter $expectedEvidenceFileName |
        Select-Object -First 1
    if ($null -eq $source) {
        throw "Artifact download did not contain $expectedEvidenceFileName."
    }

    Assert-IosEvidenceMarkdown -Path $source.FullName

    $parent = Split-Path -Parent $DestinationPath
    if (-not [string]::IsNullOrWhiteSpace($parent) -and -not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    Copy-Item -LiteralPath $source.FullName -Destination $DestinationPath -Force
    Assert-IosEvidenceMarkdown -Path $DestinationPath
}

function Invoke-SelfTest {
    $runs = @(
        [pscustomobject]@{
            databaseId = 3002
            workflowName = "iOS CI"
            headBranch = "feature/kmp-refactoring"
            headSha = "expected-sha"
            status = "completed"
            conclusion = "failure"
            createdAt = "2026-06-21T10:00:00Z"
            updatedAt = "2026-06-21T10:05:00Z"
            url = "https://github.com/flybirdxx/RunningHub/actions/runs/3002"
            event = "workflow_dispatch"
        },
        [pscustomobject]@{
            databaseId = 3001
            workflowName = "iOS CI"
            headBranch = "feature/kmp-refactoring"
            headSha = "expected-sha"
            status = "completed"
            conclusion = "success"
            createdAt = "2026-06-21T09:00:00Z"
            updatedAt = "2026-06-21T09:15:00Z"
            url = "https://github.com/flybirdxx/RunningHub/actions/runs/3001"
            event = "workflow_dispatch"
        }
    )

    $selected = Select-IosEvidenceRun `
        -Runs $runs `
        -RequestedRunId "" `
        -RequestedHeadSha "expected-sha" `
        -RequestedBranch "feature/kmp-refactoring"
    if ($selected.databaseId -ne 3001) {
        throw "SelfTest failed: unexpected iOS evidence run selected."
    }

    $tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("rh-ios-evidence-selftest-" + [guid]::NewGuid().ToString("N"))
    $artifactDir = Join-Path $tempRoot "artifact"
    $outputPath = Join-Path $tempRoot $expectedEvidenceFileName
    try {
        New-Item -ItemType Directory -Path $artifactDir -Force | Out-Null
        @"
# iOS macOS link and Simulator smoke evidence

capturedAt: 2026-06-21T00:00:00Z
headSha: expected-sha
host: Darwin self-test
linkCommand: ./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64
linkResult: pass
xcodebuildCommand: xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination generic/platform=iOS Simulator build CODE_SIGNING_ALLOWED=NO
xcodebuildResult: pass
simulatorSmokeResult: pass

## Notes

self-test
"@ | Set-Content -LiteralPath (Join-Path $artifactDir $expectedEvidenceFileName) -Encoding UTF8

        Copy-IosEvidenceArtifact -DownloadDirectory $artifactDir -DestinationPath $outputPath
    } finally {
        if (Test-Path -LiteralPath $tempRoot) {
            Remove-Item -LiteralPath $tempRoot -Recurse -Force
        }
    }

    Write-Host "SelfTest passed."
}

if ($SelfTest) {
    Invoke-SelfTest
    exit 0
}

$resolvedHeadSha = Resolve-GitHeadSha -RequestedHeadSha $HeadSha
$runs = Invoke-GhRunList -Repository $Repo
$selectedRun = Select-IosEvidenceRun `
    -Runs $runs `
    -RequestedRunId $RunId `
    -RequestedHeadSha $resolvedHeadSha `
    -RequestedBranch $Branch

$downloadDir = Join-Path ([System.IO.Path]::GetTempPath()) ("rh-ios-evidence-" + [guid]::NewGuid().ToString("N"))
try {
    New-Item -ItemType Directory -Path $downloadDir -Force | Out-Null
    Invoke-GhRunDownload `
        -Repository $Repo `
        -DatabaseId ([string] $selectedRun.databaseId) `
        -Name $ArtifactName `
        -Directory $downloadDir
    Copy-IosEvidenceArtifact -DownloadDirectory $downloadDir -DestinationPath $OutputPath
} finally {
    if (Test-Path -LiteralPath $downloadDir) {
        Remove-Item -LiteralPath $downloadDir -Recurse -Force
    }
}

Write-Host "iosEvidence=$OutputPath"
Write-Host "sourceRun=$($selectedRun.databaseId)"
