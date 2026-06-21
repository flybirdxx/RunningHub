param(
    [string] $Repo = "flybirdxx/RunningHub",
    [string] $OutputDir = "docs/migration/evidence",
    [string] $HeadSha = "",
    [string] $Branch = "",
    [switch] $Wait,
    [int] $WaitTimeoutSeconds = 1800,
    [int] $PollSeconds = 30,
    [switch] $SelfTest
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$runJsonFields = "databaseId,workflowName,headBranch,headSha,status,conclusion,createdAt,updatedAt,url,event"

function Resolve-GitHeadSha {
    param([string] $RequestedHeadSha)

    if (-not [string]::IsNullOrWhiteSpace($RequestedHeadSha)) {
        return $RequestedHeadSha.Trim()
    }

    $resolved = (& git rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($resolved)) {
        throw "Unable to resolve current git HEAD. Pass -HeadSha explicitly when collecting CI evidence outside a Git worktree."
    }
    return $resolved
}

function Invoke-GhRunList {
    param(
        [string] $Repository,
        [string] $WorkflowName
    )

    $json = & gh run list `
        --repo $Repository `
        --workflow $WorkflowName `
        --limit 20 `
        --json $runJsonFields

    if ($LASTEXITCODE -ne 0) {
        throw "gh run list failed for workflow '$WorkflowName' with exit code $LASTEXITCODE."
    }

    return @($json | ConvertFrom-Json)
}

function Select-SuccessfulWorkflowRun {
    param(
        [object[]] $Runs,
        [string] $WorkflowName,
        [string] $RequestedHeadSha,
        [string] $RequestedBranch
    )

    $matches = @($Runs | Where-Object {
        $_.workflowName -eq $WorkflowName -and
        $_.status -eq "completed" -and
        $_.conclusion -eq "success"
    })

    if (-not [string]::IsNullOrWhiteSpace($RequestedHeadSha)) {
        $matches = @($matches | Where-Object { $_.headSha -eq $RequestedHeadSha })
    }

    if (-not [string]::IsNullOrWhiteSpace($RequestedBranch)) {
        $matches = @($matches | Where-Object { $_.headBranch -eq $RequestedBranch })
    }

    if ($matches.Count -eq 0) {
        $latest = @($Runs | Select-Object -First 5 | ForEach-Object {
            "$($_.workflowName) status=$($_.status) conclusion=$($_.conclusion) branch=$($_.headBranch) sha=$($_.headSha)"
        })
        throw "No successful completed run found for '$WorkflowName'. Latest candidates: $($latest -join '; ')"
    }

    return $matches[0]
}

function Wait-SuccessfulWorkflowRun {
    param(
        [string] $Repository,
        [string] $WorkflowName,
        [string] $RequestedHeadSha,
        [string] $RequestedBranch,
        [int] $TimeoutSeconds,
        [int] $PollIntervalSeconds,
        [scriptblock] $RunProvider = $null
    )

    $startedAt = Get-Date
    $deadline = $startedAt.AddSeconds([Math]::Max(1, $TimeoutSeconds))
    $sleepSeconds = [Math]::Max(1, $PollIntervalSeconds)

    while ($true) {
        if ($null -ne $RunProvider) {
            $runs = @(& $RunProvider)
        } else {
            $runs = Invoke-GhRunList -Repository $Repository -WorkflowName $WorkflowName
        }

        $matches = @($runs | Where-Object {
            $_.workflowName -eq $WorkflowName
        })

        if (-not [string]::IsNullOrWhiteSpace($RequestedHeadSha)) {
            $matches = @($matches | Where-Object { $_.headSha -eq $RequestedHeadSha })
        }

        if (-not [string]::IsNullOrWhiteSpace($RequestedBranch)) {
            $matches = @($matches | Where-Object { $_.headBranch -eq $RequestedBranch })
        }

        $successful = @($matches | Where-Object {
            $_.status -eq "completed" -and $_.conclusion -eq "success"
        })
        if ($successful.Count -gt 0) {
            return $successful[0]
        }

        $completedFailures = @($matches | Where-Object {
            $_.status -eq "completed" -and $_.conclusion -ne "success"
        })
        if ($completedFailures.Count -gt 0) {
            $latestFailure = $completedFailures[0]
            throw "Workflow '$WorkflowName' completed without success for target head. status=$($latestFailure.status) conclusion=$($latestFailure.conclusion) branch=$($latestFailure.headBranch) sha=$($latestFailure.headSha) url=$($latestFailure.url)"
        }

        $now = Get-Date
        if ($now -ge $deadline) {
            $latest = @($matches | Select-Object -First 5 | ForEach-Object {
                "$($_.workflowName) status=$($_.status) conclusion=$($_.conclusion) branch=$($_.headBranch) sha=$($_.headSha)"
            })
            throw "Timed out waiting for successful completed run for '$WorkflowName'. Latest candidates: $($latest -join '; ')"
        }

        $elapsedSeconds = [int]($now - $startedAt).TotalSeconds
        $latestStatus = @($matches | Select-Object -First 3 | ForEach-Object {
            "status=$($_.status) conclusion=$($_.conclusion) branch=$($_.headBranch) sha=$($_.headSha)"
        })
        Write-Host "waiting workflow='$WorkflowName' status=in_progress elapsedSeconds=$elapsedSeconds latest=$($latestStatus -join '; ')"

        if ($null -ne $RunProvider) {
            throw "SelfTest wait provider did not return a completed successful run."
        }

        Start-Sleep -Seconds $sleepSeconds
    }
}

function Save-GitHubActionsEvidence {
    param(
        [object] $Run,
        [string] $Path
    )

    $parent = Split-Path -Parent $Path
    if (-not [string]::IsNullOrWhiteSpace($parent) -and -not (Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    $Run |
        ConvertTo-Json -Depth 6 |
        Set-Content -Path $Path -Encoding UTF8
}

function Assert-NonBlankEvidenceField {
    param(
        [object] $Evidence,
        [string] $Path,
        [string] $FieldName
    )

    $property = $Evidence.PSObject.Properties[$FieldName]
    if ($null -eq $property -or [string]::IsNullOrWhiteSpace([string] $property.Value)) {
        throw "Evidence file $Path does not contain a non-blank $FieldName."
    }
}

function Assert-GitHubActionsEvidence {
    param(
        [string] $Path,
        [string] $WorkflowName
    )

    if (-not (Test-Path $Path)) {
        throw "Evidence file was not created: $Path"
    }

    $evidence = Get-Content -Raw -Path $Path | ConvertFrom-Json
    if ($evidence.workflowName -ne $WorkflowName) {
        throw "Evidence file $Path has workflowName=$($evidence.workflowName), expected $WorkflowName."
    }

    if ($evidence.status -ne "completed" -or $evidence.conclusion -ne "success") {
        throw "Evidence file $Path does not describe a completed successful run."
    }

    foreach ($fieldName in @("databaseId", "headSha", "url")) {
        Assert-NonBlankEvidenceField -Evidence $evidence -Path $Path -FieldName $fieldName
    }
}

function Assert-MatchingWorkflowHeadSha {
    param(
        [object] $AndroidRun,
        [object] $IosRun
    )

    if ($AndroidRun.headSha -ne $IosRun.headSha) {
        throw "Android CI and iOS CI evidence must use the same headSha. Android=$($AndroidRun.headSha) iOS=$($IosRun.headSha). Pass -HeadSha to bind both workflows to the target commit."
    }
}

function Save-WorkflowEvidence {
    param(
        [string] $Repository,
        [string] $WorkflowName,
        [string] $OutputPath,
        [string] $RequestedHeadSha,
        [string] $RequestedBranch,
        [bool] $ShouldWait,
        [int] $TimeoutSeconds,
        [int] $PollIntervalSeconds
    )

    if ($ShouldWait) {
        $run = Wait-SuccessfulWorkflowRun `
            -Repository $Repository `
            -WorkflowName $WorkflowName `
            -RequestedHeadSha $RequestedHeadSha `
            -RequestedBranch $RequestedBranch `
            -TimeoutSeconds $TimeoutSeconds `
            -PollIntervalSeconds $PollIntervalSeconds
    } else {
        $runs = Invoke-GhRunList -Repository $Repository -WorkflowName $WorkflowName
        $run = Select-SuccessfulWorkflowRun `
            -Runs $runs `
            -WorkflowName $WorkflowName `
            -RequestedHeadSha $RequestedHeadSha `
            -RequestedBranch $RequestedBranch
    }

    Save-GitHubActionsEvidence -Run $run -Path $OutputPath
    Assert-GitHubActionsEvidence -Path $OutputPath -WorkflowName $WorkflowName
    return $run
}

function Invoke-SelfTest {
    $androidRuns = @(
        [pscustomobject]@{
            databaseId = 1002
            workflowName = "Android CI"
            headBranch = "feature/kmp-refactoring"
            headSha = "newer-failed"
            status = "completed"
            conclusion = "failure"
            createdAt = "2026-06-21T10:00:00Z"
            updatedAt = "2026-06-21T10:05:00Z"
            url = "https://github.com/flybirdxx/RunningHub/actions/runs/1002"
            event = "push"
        },
        [pscustomobject]@{
            databaseId = 1001
            workflowName = "Android CI"
            headBranch = "feature/kmp-refactoring"
            headSha = "expected-sha"
            status = "completed"
            conclusion = "success"
            createdAt = "2026-06-21T09:00:00Z"
            updatedAt = "2026-06-21T09:05:00Z"
            url = "https://github.com/flybirdxx/RunningHub/actions/runs/1001"
            event = "push"
        }
    )
    $iosRuns = @(
        [pscustomobject]@{
            databaseId = 2001
            workflowName = "iOS CI"
            headBranch = "feature/kmp-refactoring"
            headSha = "expected-sha"
            status = "completed"
            conclusion = "success"
            createdAt = "2026-06-21T09:10:00Z"
            updatedAt = "2026-06-21T09:20:00Z"
            url = "https://github.com/flybirdxx/RunningHub/actions/runs/2001"
            event = "push"
        }
    )

    $selected = Select-SuccessfulWorkflowRun `
        -Runs $androidRuns `
        -WorkflowName "Android CI" `
        -RequestedHeadSha "expected-sha" `
        -RequestedBranch "feature/kmp-refactoring"
    $selectedIos = Select-SuccessfulWorkflowRun `
        -Runs $iosRuns `
        -WorkflowName "iOS CI" `
        -RequestedHeadSha "expected-sha" `
        -RequestedBranch "feature/kmp-refactoring"
    $waitSelected = Wait-SuccessfulWorkflowRun `
        -Repository "flybirdxx/RunningHub" `
        -WorkflowName "Android CI" `
        -RequestedHeadSha "expected-sha" `
        -RequestedBranch "feature/kmp-refactoring" `
        -TimeoutSeconds 1 `
        -PollIntervalSeconds 1 `
        -RunProvider { $androidRuns }

    if ($selected.databaseId -ne 1001) {
        throw "SelfTest failed: unexpected run selected."
    }
    if ($waitSelected.databaseId -ne 1001) {
        throw "SelfTest failed: unexpected waited run selected."
    }
    Assert-MatchingWorkflowHeadSha -AndroidRun $selected -IosRun $selectedIos

    $tempEvidencePath = Join-Path ([System.IO.Path]::GetTempPath()) "rh-github-actions-selftest.json"
    try {
        Save-GitHubActionsEvidence -Run $selected -Path $tempEvidencePath
        Assert-GitHubActionsEvidence -Path $tempEvidencePath -WorkflowName "Android CI"
    } finally {
        if (Test-Path $tempEvidencePath) {
            Remove-Item -LiteralPath $tempEvidencePath -Force
        }
    }

    Write-Host "SelfTest passed."
}

if ($SelfTest) {
    Invoke-SelfTest
    exit 0
}

$androidOutputPath = Join-Path $OutputDir "github-actions-android.json"
$iosOutputPath = Join-Path $OutputDir "github-actions-ios.json"
$resolvedHeadSha = Resolve-GitHeadSha -RequestedHeadSha $HeadSha

Write-Host "targetHeadSha=$resolvedHeadSha"

$androidRun = Save-WorkflowEvidence `
    -Repository $Repo `
    -WorkflowName "Android CI" `
    -OutputPath $androidOutputPath `
    -RequestedHeadSha $resolvedHeadSha `
    -RequestedBranch $Branch `
    -ShouldWait $Wait.IsPresent `
    -TimeoutSeconds $WaitTimeoutSeconds `
    -PollIntervalSeconds $PollSeconds

$iosRun = Save-WorkflowEvidence `
    -Repository $Repo `
    -WorkflowName "iOS CI" `
    -OutputPath $iosOutputPath `
    -RequestedHeadSha $resolvedHeadSha `
    -RequestedBranch $Branch `
    -ShouldWait $Wait.IsPresent `
    -TimeoutSeconds $WaitTimeoutSeconds `
    -PollIntervalSeconds $PollSeconds
Assert-MatchingWorkflowHeadSha -AndroidRun $androidRun -IosRun $iosRun

Write-Host "androidEvidence=$androidOutputPath"
Write-Host "iosEvidence=$iosOutputPath"
