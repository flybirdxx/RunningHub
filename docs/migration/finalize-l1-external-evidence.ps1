param(
    [string] $Repo = "flybirdxx/RunningHub",
    [string] $OutputDir = "docs/migration/evidence",
    [string] $HeadSha = "",
    [string] $Branch = "",
    [switch] $Wait,
    [int] $WaitTimeoutSeconds = 1800,
    [int] $PollSeconds = 30,
    [ValidateSet("skip", "request", "download", "none")]
    [string] $IosEvidenceMode = "skip",
    [string] $SmokeNotes = "",
    [switch] $ConfirmSimulatorSmokePass,
    [string] $RunId = "",
    [string] $SkipReason = "macOS test environment is currently unavailable, so iOS framework link, Xcode build, and Simulator login/logout/QuickCreate smoke cannot be executed in this workspace.",
    [string] $FollowUpRequired = "Re-run docs/migration/collect-ios-macos-evidence.sh on macOS or trigger docs/migration/request-ios-macos-evidence.ps1 after real Simulator smoke is manually verified.",
    [switch] $StageEvidence,
    [switch] $RunSealCheck,
    [switch] $SelfTest
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-GitHeadSha {
    param([string] $RequestedHeadSha)

    $current = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($current)) {
        throw "Unable to resolve current git HEAD. Run this script inside the repository or pass through a clean checkout."
    }

    if (-not [string]::IsNullOrWhiteSpace($RequestedHeadSha)) {
        $requested = $RequestedHeadSha.Trim()
        if ($requested -ne $current) {
            throw "Requested -HeadSha $requested does not match current git HEAD $current. Check out the target commit before collecting final L1 evidence."
        }
        return $requested
    }

    return $current
}

function Resolve-GitBranch {
    param([string] $RequestedBranch)

    if (-not [string]::IsNullOrWhiteSpace($RequestedBranch)) {
        return $RequestedBranch.Trim()
    }

    $resolved = (& git rev-parse --abbrev-ref HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($resolved) -or $resolved -eq "HEAD") {
        throw "Unable to resolve current git branch. Pass -Branch explicitly when collecting final L1 evidence from a detached HEAD."
    }
    return $resolved
}

function Assert-CleanPreCollectionState {
    param(
        [string[]] $UnstagedFiles = $null,
        [string[]] $StagedFiles = $null,
        [string[]] $UntrackedFiles = $null
    )

    $unstaged = @(& git diff --name-only | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($LASTEXITCODE -ne 0) {
        throw "git diff --name-only failed while checking pre-collection state."
    }
    if ($null -ne $UnstagedFiles) {
        $unstaged = @($UnstagedFiles | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    }

    $staged = @(& git diff --cached --name-only | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($LASTEXITCODE -ne 0) {
        throw "git diff --cached --name-only failed while checking pre-collection state."
    }
    if ($null -ne $StagedFiles) {
        $staged = @($StagedFiles | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    }

    $untracked = @(& git ls-files --others --exclude-standard | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($LASTEXITCODE -ne 0) {
        throw "git ls-files --others --exclude-standard failed while checking pre-collection state."
    }
    if ($null -ne $UntrackedFiles) {
        $untracked = @($UntrackedFiles | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    }

    if ($unstaged.Count -gt 0 -or $staged.Count -gt 0 -or $untracked.Count -gt 0) {
        throw "Collect final L1 evidence only after code/config/documentation changes are committed and untracked files are handled. Unstaged=$($unstaged -join ', ') Staged=$($staged -join ', ') Untracked=$($untracked -join ', ')"
    }
}

function Invoke-CiEvidenceCollection {
    param(
        [string] $Repository,
        [string] $TargetHeadSha,
        [string] $TargetBranch,
        [string] $EvidenceOutputDir,
        [bool] $ShouldWait,
        [int] $TimeoutSeconds,
        [int] $PollIntervalSeconds
    )

    $scriptPath = Join-Path $PSScriptRoot "collect-github-actions-evidence.ps1"
    $arguments = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $scriptPath,
        "-Repo", $Repository,
        "-OutputDir", $EvidenceOutputDir,
        "-HeadSha", $TargetHeadSha,
        "-Branch", $TargetBranch
    )
    if ($ShouldWait) {
        $arguments += @("-Wait", "-WaitTimeoutSeconds", $TimeoutSeconds, "-PollSeconds", $PollIntervalSeconds)
    }

    & powershell @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "collect-github-actions-evidence.ps1 failed with exit code $LASTEXITCODE."
    }
}

function Write-SkippedIosEvidence {
    param(
        [string] $Path,
        [string] $TargetHeadSha,
        [string] $Reason,
        [string] $FollowUp
    )

    if ([string]::IsNullOrWhiteSpace($Reason)) {
        throw "Skip evidence requires a non-blank skip reason."
    }
    if ([string]::IsNullOrWhiteSpace($FollowUp) -or $FollowUp.Trim().Equals("false", [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Skip evidence requires a non-blank follow-up value that is not false."
    }

    $parent = Split-Path -Parent $Path
    if (-not [string]::IsNullOrWhiteSpace($parent) -and -not (Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    $capturedAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    $lines = @(
        "# iOS macOS link and Simulator smoke evidence",
        "",
        "capturedAt: $capturedAt",
        "headSha: $TargetHeadSha",
        "overallResult: skipped",
        "host: Windows local environment; macOS unavailable",
        "linkCommand: ./gradlew --console=plain :composeApp:linkDebugFrameworkIosSimulatorArm64",
        "linkResult: skipped",
        "xcodebuildCommand: xcodebuild -project iosApp/iosApp.xcodeproj -scheme RunningHub -configuration Debug -sdk iphonesimulator -destination generic/platform=iOS Simulator build CODE_SIGNING_ALLOWED=NO",
        "xcodebuildResult: skipped",
        "simulatorSmokeResult: skipped",
        "skipReason: $Reason",
        "followUpRequired: $FollowUp",
        "",
        "## Simulator smoke scope",
        "",
        "- Simulator login flow observed: skipped",
        "- Simulator logout/session flow observed: skipped",
        "- Simulator QuickCreate flow observed: skipped",
        "",
        "## Notes",
        "",
        "User explicitly allowed retaining a skip record for the unavailable macOS environment. This file is not a passing iOS runtime proof; it records the current limitation and preserves the follow-up required before claiming macOS/iOS runtime coverage.",
        "",
        "## Gradle output tail",
        "",
        '```text',
        "Skipped because macOS is unavailable in the current Windows workspace.",
        '```',
        "",
        "## Xcode build output tail",
        "",
        '```text',
        "Skipped because xcodebuild and iOS Simulator are unavailable in the current Windows workspace.",
        '```'
    )

    $normalized = ($lines -join "`n") + "`n"
    $utf8NoBom = New-Object System.Text.UTF8Encoding -ArgumentList $false
    [System.IO.File]::WriteAllText($Path, $normalized, $utf8NoBom)
}

function Invoke-IosEvidenceRequest {
    param(
        [string] $Repository,
        [string] $TargetHeadSha,
        [string] $TargetBranch,
        [string] $Notes,
        [bool] $Confirmed,
        [bool] $ShouldWait,
        [int] $TimeoutSeconds,
        [int] $PollIntervalSeconds
    )

    $scriptPath = Join-Path $PSScriptRoot "request-ios-macos-evidence.ps1"
    $arguments = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $scriptPath,
        "-Repo", $Repository,
        "-HeadSha", $TargetHeadSha,
        "-Branch", $TargetBranch,
        "-SmokeNotes", $Notes
    )
    if ($Confirmed) {
        $arguments += "-ConfirmSimulatorSmokePass"
    }
    if ($ShouldWait) {
        $arguments += @("-Wait", "-WaitTimeoutSeconds", $TimeoutSeconds, "-PollSeconds", $PollIntervalSeconds)
    }

    & powershell @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "request-ios-macos-evidence.ps1 failed with exit code $LASTEXITCODE."
    }
}

function Invoke-IosEvidenceDownload {
    param(
        [string] $Repository,
        [string] $TargetHeadSha,
        [string] $TargetBranch,
        [string] $RequestedRunId,
        [bool] $ShouldWait,
        [int] $TimeoutSeconds,
        [int] $PollIntervalSeconds
    )

    $scriptPath = Join-Path $PSScriptRoot "download-ios-macos-evidence.ps1"
    $arguments = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $scriptPath,
        "-Repo", $Repository,
        "-HeadSha", $TargetHeadSha,
        "-Branch", $TargetBranch
    )
    if (-not [string]::IsNullOrWhiteSpace($RequestedRunId)) {
        $arguments += @("-RunId", $RequestedRunId)
    }
    if ($ShouldWait) {
        $arguments += @("-Wait", "-WaitTimeoutSeconds", $TimeoutSeconds, "-PollSeconds", $PollIntervalSeconds)
    }

    & powershell @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "download-ios-macos-evidence.ps1 failed with exit code $LASTEXITCODE."
    }
}

function Get-L1EvidencePaths {
    param([string] $EvidenceOutputDir)

    return @(
        (Join-Path $EvidenceOutputDir "github-actions-android.json"),
        (Join-Path $EvidenceOutputDir "github-actions-ios.json"),
        (Join-Path $EvidenceOutputDir "android-tab-network.json"),
        (Join-Path $EvidenceOutputDir "android-logout-network.json"),
        (Join-Path $EvidenceOutputDir "ios-macos-link-and-simulator.md")
    )
}

function Add-L1EvidenceFiles {
    param([string[]] $EvidencePaths)

    & git add -- @EvidencePaths
    if ($LASTEXITCODE -ne 0) {
        throw "git add failed while staging final L1 evidence files."
    }
}

function Invoke-L1SealEvidenceCheck {
    $gradleWrapper = Join-Path $PSScriptRoot "..\..\gradlew.bat"
    & $gradleWrapper --console=plain checkL1SealEvidence
    if ($LASTEXITCODE -ne 0) {
        throw "checkL1SealEvidence failed with exit code $LASTEXITCODE."
    }
}

function Invoke-SelfTest {
    $tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("rh-l1-finalize-" + [System.Guid]::NewGuid().ToString("N"))
    $tempPath = Join-Path $tempDir "ios-macos-link-and-simulator.md"
    try {
        Write-SkippedIosEvidence `
            -Path $tempPath `
            -TargetHeadSha "expected-sha" `
            -Reason "macOS unavailable in self-test." `
            -FollowUp "Run macOS evidence later."

        $evidence = Get-Content -Raw -Path $tempPath
        foreach ($snippet in @("headSha: expected-sha", "overallResult: skipped", "linkResult: skipped", "xcodebuildResult: skipped", "simulatorSmokeResult: skipped", "followUpRequired: Run macOS evidence later.")) {
            if ($evidence -notlike "*$snippet*") {
                throw "SelfTest failed: skipped evidence is missing '$snippet'."
            }
        }

        $failedBlankReason = $false
        try {
            Write-SkippedIosEvidence -Path $tempPath -TargetHeadSha "expected-sha" -Reason " " -FollowUp "later"
        } catch {
            $failedBlankReason = $true
        }
        if (-not $failedBlankReason) {
            throw "SelfTest failed: blank skip reason was accepted."
        }

        $failedFalseFollowUp = $false
        try {
            Write-SkippedIosEvidence -Path $tempPath -TargetHeadSha "expected-sha" -Reason "reason" -FollowUp "false"
        } catch {
            $failedFalseFollowUp = $true
        }
        if (-not $failedFalseFollowUp) {
            throw "SelfTest failed: false follow-up was accepted."
        }

        Assert-CleanPreCollectionState -UnstagedFiles @() -StagedFiles @() -UntrackedFiles @()

        $failedDirtyState = $false
        try {
            Assert-CleanPreCollectionState -UnstagedFiles @("dirty.kt") -StagedFiles @() -UntrackedFiles @()
        } catch {
            $failedDirtyState = $true
        }
        if (-not $failedDirtyState) {
            throw "SelfTest failed: unstaged file was accepted."
        }

        $failedUntrackedState = $false
        try {
            Assert-CleanPreCollectionState -UnstagedFiles @() -StagedFiles @() -UntrackedFiles @("new-file.txt")
        } catch {
            $failedUntrackedState = $true
        }
        if (-not $failedUntrackedState) {
            throw "SelfTest failed: untracked file was accepted."
        }

        $expectedEvidencePaths = @(
            "evidence/github-actions-android.json",
            "evidence/github-actions-ios.json",
            "evidence/android-tab-network.json",
            "evidence/android-logout-network.json",
            "evidence/ios-macos-link-and-simulator.md"
        )
        $actualEvidencePaths = @(Get-L1EvidencePaths -EvidenceOutputDir "evidence" | ForEach-Object { $_.Replace("\", "/") })
        foreach ($expectedPath in $expectedEvidencePaths) {
            if ($expectedPath -notin $actualEvidencePaths) {
                throw "SelfTest failed: evidence staging list is missing $expectedPath."
            }
        }
    } finally {
        if (Test-Path $tempDir) {
            Remove-Item -LiteralPath $tempDir -Recurse -Force
        }
    }

    Write-Host "SelfTest passed."
}

if ($SelfTest) {
    Invoke-SelfTest
    exit 0
}

$resolvedHeadSha = Resolve-GitHeadSha -RequestedHeadSha $HeadSha
$resolvedBranch = Resolve-GitBranch -RequestedBranch $Branch
$iosEvidencePath = Join-Path $OutputDir "ios-macos-link-and-simulator.md"
$evidencePaths = Get-L1EvidencePaths -EvidenceOutputDir $OutputDir

Assert-CleanPreCollectionState

Write-Host "targetHeadSha=$resolvedHeadSha"
Write-Host "targetBranch=$resolvedBranch"
Write-Host "preCollectionClean=true"

Invoke-CiEvidenceCollection `
    -Repository $Repo `
    -TargetHeadSha $resolvedHeadSha `
    -TargetBranch $resolvedBranch `
    -EvidenceOutputDir $OutputDir `
    -ShouldWait $Wait.IsPresent `
    -TimeoutSeconds $WaitTimeoutSeconds `
    -PollIntervalSeconds $PollSeconds

if ($IosEvidenceMode -eq "skip") {
    Write-SkippedIosEvidence `
        -Path $iosEvidencePath `
        -TargetHeadSha $resolvedHeadSha `
        -Reason $SkipReason `
        -FollowUp $FollowUpRequired
    Write-Host "iosEvidenceMode=skip"
    Write-Host "iosEvidence=$iosEvidencePath"
} elseif ($IosEvidenceMode -eq "request") {
    Invoke-IosEvidenceRequest `
        -Repository $Repo `
        -TargetHeadSha $resolvedHeadSha `
        -TargetBranch $resolvedBranch `
        -Notes $SmokeNotes `
        -Confirmed $ConfirmSimulatorSmokePass.IsPresent `
        -ShouldWait $Wait.IsPresent `
        -TimeoutSeconds $WaitTimeoutSeconds `
        -PollIntervalSeconds $PollSeconds
    Write-Host "iosEvidenceMode=request"
} elseif ($IosEvidenceMode -eq "download") {
    Invoke-IosEvidenceDownload `
        -Repository $Repo `
        -TargetHeadSha $resolvedHeadSha `
        -TargetBranch $resolvedBranch `
        -RequestedRunId $RunId `
        -ShouldWait $Wait.IsPresent `
        -TimeoutSeconds $WaitTimeoutSeconds `
        -PollIntervalSeconds $PollSeconds
    Write-Host "iosEvidenceMode=download"
} else {
    Write-Host "iosEvidenceMode=none"
}

if ($StageEvidence) {
    Add-L1EvidenceFiles -EvidencePaths $evidencePaths
    Write-Host "evidenceStaged=true"
}

if ($RunSealCheck) {
    Invoke-L1SealEvidenceCheck
    Write-Host "sealEvidenceCheck=pass"
} elseif ($StageEvidence) {
    Write-Host "Next: ./gradlew.bat --console=plain checkL1SealEvidence"
} else {
    Write-Host "Next: git add docs/migration/evidence && ./gradlew.bat --console=plain checkL1SealEvidence"
}
