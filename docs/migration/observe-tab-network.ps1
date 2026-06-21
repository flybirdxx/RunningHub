param(
    [string] $Serial = $env:ANDROID_SERIAL,
    [string] $PackageName = "com.runninghub.app.debug",
    [string] $ActivityName = "com.runninghub.app.MainActivity",
    [int] $DurationSeconds = 120,
    [int] $StableWindowSeconds = 30,
    [string] $OutputPath = "",
    [string] $OperationNotes = "",
    [switch] $Launch,
    [switch] $ForceStopBeforeLaunch,
    [switch] $SelfTest
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$logTag = "RunningHubNetwork"
$samplePattern = "started=(\d+)\s+completed=(\d+)\s+inFlight=(\d+)"

function Resolve-AdbSerial {
    param([string] $RequestedSerial)

    if (-not [string]::IsNullOrWhiteSpace($RequestedSerial)) {
        return $RequestedSerial
    }

    $deviceLines = @(adb devices |
        Select-Object -Skip 1 |
        Where-Object { $_ -match "\sdevice$" })

    if ($deviceLines.Count -eq 0) {
        throw "No adb device is available. Start an AVD or connect a device first."
    }

    if ($deviceLines.Count -gt 1) {
        throw "Multiple adb devices detected. Set -Serial or ANDROID_SERIAL."
    }

    return ($deviceLines[0] -split "\s+")[0]
}

function Invoke-CheckedAdb {
    param(
        [string] $TargetSerial,
        [string[]] $Arguments
    )

    $output = & adb -s $TargetSerial @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($Arguments -join ' ') failed with exit code $LASTEXITCODE."
    }
    return $output
}

function New-NetworkSampleFromLine {
    param(
        [string] $Line,
        [datetime] $CapturedAt = (Get-Date)
    )

    $match = [regex]::Match($Line, $samplePattern)
    if (-not $match.Success) {
        return $null
    }

    return [pscustomobject]@{
        CapturedAt = $CapturedAt
        Started = [long]$match.Groups[1].Value
        Completed = [long]$match.Groups[2].Value
        InFlight = [int]$match.Groups[3].Value
        Raw = $Line.Trim()
    }
}

function Get-NetworkObservationSummary {
    param(
        [object[]] $Samples,
        [int] $WindowSeconds,
        [datetime] $ReferenceTime = [datetime]::MinValue
    )

    if ($Samples.Count -eq 0) {
        throw "Cannot summarize empty network samples."
    }

    $firstSample = $Samples[0]
    $lastSample = $Samples[$Samples.Count - 1]
    if ($ReferenceTime -eq [datetime]::MinValue) {
        $ReferenceTime = $lastSample.CapturedAt
    }
    $stableStartTime = $ReferenceTime.AddSeconds(-$WindowSeconds)
    $stableSamples = @($Samples | Where-Object { $_.CapturedAt -ge $stableStartTime })
    if ($stableSamples.Count -eq 0) {
        $stableSamples = @($lastSample)
    }

    return [pscustomobject]@{
        First = $firstSample
        Last = $lastSample
        StableSampleCount = [int]$stableSamples.Count
        StableStartedDelta = [long]($stableSamples[-1].Started - $stableSamples[0].Started)
        MaxStableInFlight = [int](($stableSamples | Measure-Object -Property InFlight -Maximum).Maximum)
    }
}

function New-NetworkObservationEvidence {
    param(
        [object] $Summary,
        [object[]] $Samples,
        [string] $Result,
        [string] $TargetSerial,
        [string] $TargetPackageName,
        [string] $Notes,
        [int] $ObservedDurationSeconds,
        [int] $ObservedStableWindowSeconds
    )

    return [pscustomobject]@{
        schemaVersion = 1
        capturedAt = (Get-Date).ToString("o")
        device = $TargetSerial
        packageName = $TargetPackageName
        durationSeconds = $ObservedDurationSeconds
        stableWindowSeconds = $ObservedStableWindowSeconds
        operationNotes = $Notes
        sampleCount = $Samples.Count
        result = $Result
        first = [pscustomobject]@{
            started = $Summary.First.Started
            completed = $Summary.First.Completed
            inFlight = $Summary.First.InFlight
        }
        last = [pscustomobject]@{
            started = $Summary.Last.Started
            completed = $Summary.Last.Completed
            inFlight = $Summary.Last.InFlight
        }
        stableWindowStartedDelta = $Summary.StableStartedDelta
        stableWindowMaxInFlight = $Summary.MaxStableInFlight
        stableWindowSampleCount = $Summary.StableSampleCount
        samples = @($Samples | ForEach-Object {
            [pscustomobject]@{
                capturedAt = $_.CapturedAt.ToString("o")
                started = $_.Started
                completed = $_.Completed
                inFlight = $_.InFlight
            }
        })
    }
}

function Save-NetworkObservationEvidence {
    param(
        [object] $Evidence,
        [string] $Path
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        return
    }

    $parent = Split-Path -Parent $Path
    if (-not [string]::IsNullOrWhiteSpace($parent) -and -not (Test-Path $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }

    $Evidence |
        ConvertTo-Json -Depth 6 |
        Set-Content -Path $Path -Encoding UTF8
}

function Invoke-SelfTest {
    $baseTime = Get-Date
    $sampleLines = @(
        "06-21 10:00:00.000 I/RunningHubNetwork: started=1 completed=0 inFlight=1",
        "this line is intentionally ignored",
        "06-21 10:00:01.000 I/RunningHubNetwork: started=1 completed=1 inFlight=0",
        "06-21 10:00:02.000 I/RunningHubNetwork: started=1 completed=1 inFlight=0"
    )

    $samples = New-Object System.Collections.Generic.List[object]
    for ($index = 0; $index -lt $sampleLines.Count; $index += 1) {
        $sample = New-NetworkSampleFromLine -Line $sampleLines[$index] -CapturedAt $baseTime.AddSeconds($index)
        if ($null -ne $sample) {
            $samples.Add($sample)
        }
    }

    if ($samples.Count -ne 3) {
        throw "SelfTest failed: expected 3 parsed samples, got $($samples.Count)."
    }

    $summary = Get-NetworkObservationSummary `
        -Samples $samples.ToArray() `
        -WindowSeconds 2 `
        -ReferenceTime $baseTime.AddSeconds(3)

    if ($summary.StableStartedDelta -ne 0) {
        throw "SelfTest failed: expected stable started delta 0, got $($summary.StableStartedDelta)."
    }

    if ($summary.MaxStableInFlight -ne 0) {
        throw "SelfTest failed: expected stable max inFlight 0, got $($summary.MaxStableInFlight)."
    }

    if ($summary.StableSampleCount -ne 2) {
        throw "SelfTest failed: expected stable sample count 2, got $($summary.StableSampleCount)."
    }

    $evidence = New-NetworkObservationEvidence `
        -Summary $summary `
        -Samples $samples.ToArray() `
        -Result "pass_candidate" `
        -TargetSerial "self-test" `
        -TargetPackageName "com.runninghub.app.debug" `
        -Notes "logged-in History -> QuickCreate -> Profile idle" `
        -ObservedDurationSeconds 3 `
        -ObservedStableWindowSeconds 2

    if ($evidence.sampleCount -ne 3 -or $evidence.stableWindowSampleCount -ne 2 -or $evidence.result -ne "pass_candidate" -or [string]::IsNullOrWhiteSpace($evidence.operationNotes)) {
        throw "SelfTest failed: evidence object is invalid."
    }

    $tempEvidencePath = Join-Path ([System.IO.Path]::GetTempPath()) "rh-tab-network-selftest.json"
    try {
        Save-NetworkObservationEvidence -Evidence $evidence -Path $tempEvidencePath
        $savedEvidence = Get-Content -Raw -Path $tempEvidencePath | ConvertFrom-Json
        if ($savedEvidence.result -ne "pass_candidate" -or $savedEvidence.sampleCount -ne 3 -or $savedEvidence.stableWindowSampleCount -ne 2 -or [string]::IsNullOrWhiteSpace($savedEvidence.operationNotes)) {
            throw "SelfTest failed: saved evidence JSON is invalid."
        }
    } finally {
        if (Test-Path $tempEvidencePath) {
            Remove-Item -LiteralPath $tempEvidencePath -Force
        }
    }

    Write-Host "SelfTest passed: parser and stable-window summary are valid."
}

if ($SelfTest) {
    Invoke-SelfTest
    exit 0
}

if ($DurationSeconds -lt 1) {
    throw "-DurationSeconds must be greater than 0."
}

if ($StableWindowSeconds -lt 1 -or $StableWindowSeconds -gt $DurationSeconds) {
    throw "-StableWindowSeconds must be between 1 and DurationSeconds."
}

if ($ForceStopBeforeLaunch -and -not $Launch) {
    throw "-ForceStopBeforeLaunch requires -Launch."
}

$targetSerial = Resolve-AdbSerial -RequestedSerial $Serial

Write-Host "AC-11 tab network counter observation"
Write-Host "device=$targetSerial package=$PackageName duration=${DurationSeconds}s stableWindow=${StableWindowSeconds}s"
Write-Host "During sampling, switch tabs while logged in: History -> QuickCreate -> another tab, then keep the app idle."
Write-Host "The script reads only logcat tag $logTag; it does not read URL, Header, Body, Token, or Cookie."
if ([string]::IsNullOrWhiteSpace($OperationNotes)) {
    Write-Host "operationNotes=missing; final L1 evidence requires -OperationNotes with the logged-in tab path."
} else {
    Write-Host "operationNotes=$OperationNotes"
}

if ($ForceStopBeforeLaunch) {
    Invoke-CheckedAdb -TargetSerial $targetSerial -Arguments @("shell", "am", "force-stop", $PackageName) | Out-Null
    Start-Sleep -Seconds 1
}

Invoke-CheckedAdb -TargetSerial $targetSerial -Arguments @("logcat", "-c") | Out-Null

if ($Launch) {
    Invoke-CheckedAdb -TargetSerial $targetSerial -Arguments @(
        "shell",
        "am",
        "start",
        "-n",
        "$PackageName/$ActivityName"
    ) | Out-Null
}

$startTime = Get-Date
$deadline = $startTime.AddSeconds($DurationSeconds)
$samples = New-Object System.Collections.Generic.List[object]

while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 1
    $lines = Invoke-CheckedAdb -TargetSerial $targetSerial -Arguments @("logcat", "-d", "-s", $logTag)

    foreach ($line in $lines) {
        $sample = New-NetworkSampleFromLine -Line $line
        if ($null -eq $sample) {
            continue
        }

        if ($samples.Count -eq 0 -or $samples[$samples.Count - 1].Raw -ne $sample.Raw) {
            $samples.Add($sample)
            Write-Host $sample.Raw
        }
    }

    Invoke-CheckedAdb -TargetSerial $targetSerial -Arguments @("logcat", "-c") | Out-Null
}

if ($samples.Count -eq 0) {
    throw "No $logTag counter samples captured. Confirm the debug app is installed and running."
}

$summary = Get-NetworkObservationSummary `
    -Samples $samples.ToArray() `
    -WindowSeconds $StableWindowSeconds

Write-Host ""
Write-Host "Observation summary"
Write-Host "first: started=$($summary.First.Started) completed=$($summary.First.Completed) inFlight=$($summary.First.InFlight)"
Write-Host "last:  started=$($summary.Last.Started) completed=$($summary.Last.Completed) inFlight=$($summary.Last.InFlight)"
Write-Host "stableWindowStartedDelta=$($summary.StableStartedDelta)"
Write-Host "stableWindowMaxInFlight=$($summary.MaxStableInFlight)"
Write-Host "stableWindowSampleCount=$($summary.StableSampleCount)"

$result = if ($summary.StableStartedDelta -eq 0 -and $summary.MaxStableInFlight -eq 0) {
    "pass_candidate"
} else {
    "needs_review"
}

$evidence = New-NetworkObservationEvidence `
    -Summary $summary `
    -Samples $samples.ToArray() `
    -Result $result `
    -TargetSerial $targetSerial `
    -TargetPackageName $PackageName `
    -Notes $OperationNotes `
    -ObservedDurationSeconds $DurationSeconds `
    -ObservedStableWindowSeconds $StableWindowSeconds
Save-NetworkObservationEvidence -Evidence $evidence -Path $OutputPath

if ($result -eq "pass_candidate") {
    Write-Host "result=pass_candidate"
    Write-Host "Stable window has no request growth and no in-flight request. Paste the operation path and output into AC-11 evidence."
} else {
    Write-Host "result=needs_review"
    Write-Host "Stable window still has request growth or in-flight work. Review it with the active tab and business task."
}

if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
    Write-Host "evidencePath=$OutputPath"
}
