param(
    [string] $Repo = "flybirdxx/RunningHub",
    [string] $WorkflowFile = "ios-ci.yml",
    [string] $Branch = "",
    [string] $HeadSha = "",
    [string] $SmokeNotes = "",
    [switch] $ConfirmSimulatorSmokePass,
    [switch] $Wait,
    [int] $WaitTimeoutSeconds = 1800,
    [int] $PollSeconds = 30,
    [switch] $SelfTest
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-GitHeadSha {
    param([string] $RequestedHeadSha)

    if (-not [string]::IsNullOrWhiteSpace($RequestedHeadSha)) {
        return $RequestedHeadSha.Trim()
    }

    $resolved = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($resolved)) {
        throw "Unable to resolve current git HEAD. Pass -HeadSha explicitly when requesting iOS evidence outside a Git worktree."
    }
    return $resolved
}

function Resolve-GitBranch {
    param([string] $RequestedBranch)

    if (-not [string]::IsNullOrWhiteSpace($RequestedBranch)) {
        return $RequestedBranch.Trim()
    }

    $resolved = (& git rev-parse --abbrev-ref HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($resolved) -or $resolved -eq "HEAD") {
        throw "Unable to resolve current git branch. Pass -Branch explicitly when requesting iOS evidence from a detached HEAD."
    }
    return $resolved
}

function Assert-SmokeConfirmation {
    param(
        [bool] $Confirmed,
        [string] $Notes
    )

    if (-not $Confirmed) {
        throw "Pass -ConfirmSimulatorSmokePass only after manually verifying Simulator login, logout/session, and QuickCreate smoke."
    }
    if ([string]::IsNullOrWhiteSpace($Notes)) {
        throw "Pass -SmokeNotes with the Simulator device, OS, and checked login/logout/QuickCreate path."
    }
}

function Invoke-GhWorkflowRun {
    param(
        [string] $Repository,
        [string] $Workflow,
        [string] $Ref,
        [string] $Notes
    )

    & gh workflow run $Workflow `
        --repo $Repository `
        --ref $Ref `
        -f "simulator_smoke_pass=true" `
        -f "smoke_notes=$Notes"

    if ($LASTEXITCODE -ne 0) {
        throw "gh workflow run failed for $Workflow on $Ref with exit code $LASTEXITCODE."
    }
}

function Invoke-IosEvidenceDownload {
    param(
        [string] $Repository,
        [string] $TargetHeadSha,
        [string] $TargetBranch,
        [int] $TimeoutSeconds,
        [int] $PollIntervalSeconds
    )

    $scriptPath = Join-Path $PSScriptRoot "download-ios-macos-evidence.ps1"
    & powershell -NoProfile -ExecutionPolicy Bypass -File $scriptPath `
        -Repo $Repository `
        -HeadSha $TargetHeadSha `
        -Branch $TargetBranch `
        -Wait `
        -WaitTimeoutSeconds $TimeoutSeconds `
        -PollSeconds $PollIntervalSeconds

    if ($LASTEXITCODE -ne 0) {
        throw "download-ios-macos-evidence.ps1 failed with exit code $LASTEXITCODE."
    }
}

function Invoke-SelfTest {
    Assert-SmokeConfirmation -Confirmed $true -Notes "iPhone 16 Simulator, iOS 18, login/logout/QuickCreate checked."

    $failedMissingConfirm = $false
    try {
        Assert-SmokeConfirmation -Confirmed $false -Notes "valid notes"
    } catch {
        $failedMissingConfirm = $true
    }
    if (-not $failedMissingConfirm) {
        throw "SelfTest failed: missing confirmation was accepted."
    }

    $failedMissingNotes = $false
    try {
        Assert-SmokeConfirmation -Confirmed $true -Notes " "
    } catch {
        $failedMissingNotes = $true
    }
    if (-not $failedMissingNotes) {
        throw "SelfTest failed: blank notes were accepted."
    }

    Write-Host "SelfTest passed."
}

if ($SelfTest) {
    Invoke-SelfTest
    exit 0
}

$resolvedHeadSha = Resolve-GitHeadSha -RequestedHeadSha $HeadSha
$resolvedBranch = Resolve-GitBranch -RequestedBranch $Branch
Assert-SmokeConfirmation -Confirmed $ConfirmSimulatorSmokePass.IsPresent -Notes $SmokeNotes

Write-Host "targetHeadSha=$resolvedHeadSha"
Write-Host "targetBranch=$resolvedBranch"

Invoke-GhWorkflowRun `
    -Repository $Repo `
    -Workflow $WorkflowFile `
    -Ref $resolvedBranch `
    -Notes $SmokeNotes

Write-Host "workflowDispatchRequested=true"

if ($Wait) {
    Invoke-IosEvidenceDownload `
        -Repository $Repo `
        -TargetHeadSha $resolvedHeadSha `
        -TargetBranch $resolvedBranch `
        -TimeoutSeconds $WaitTimeoutSeconds `
        -PollIntervalSeconds $PollSeconds
}
