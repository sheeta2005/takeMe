$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'start-app.ps1') -Mode baseline
& (Join-Path $PSScriptRoot 'run.ps1') -Name baseline-final-warmup -Threads 60 -Rps 30 -Seconds 180 -Ramp 15
& (Join-Path $PSScriptRoot 'snapshot.ps1') -Name baseline-final-before
& (Join-Path $PSScriptRoot 'run.ps1') -Name baseline-peak -Threads 300 -Rps 180 -Seconds 600 -Ramp 30
& (Join-Path $PSScriptRoot 'snapshot.ps1') -Name baseline-peak-after
& (Join-Path $PSScriptRoot 'run.ps1') -Name baseline-burst -Threads 600 -Rps 360 -Seconds 600 -Ramp 30
& (Join-Path $PSScriptRoot 'snapshot.ps1') -Name baseline-burst-after
& (Join-Path $PSScriptRoot 'stop-app.ps1')
foreach ($mode in @('threads','pool','mq')) {
    # 每步只追加一组候选参数，探测结果只作筛选，不替代正式复测。
    & (Join-Path $PSScriptRoot 'start-app.ps1') -Mode $mode
    & (Join-Path $PSScriptRoot 'run.ps1') -Name "$mode-warmup" -Threads 60 -Rps 30 -Seconds 60 -Ramp 10
    & (Join-Path $PSScriptRoot 'run.ps1') -Name "$mode-probe" -Threads 600 -Rps 360 -Seconds 120 -Ramp 30
    & (Join-Path $PSScriptRoot 'snapshot.ps1') -Name "$mode-after"
    & (Join-Path $PSScriptRoot 'stop-app.ps1')
}
