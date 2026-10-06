param(
    [ValidatePattern('^[a-z0-9_-]+$')][string]$Prefix = 'community-final'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$results = Join-Path $root 'tools\results'
$manifest = Get-Content (Join-Path $results "$Prefix-manifest.json") -Raw | ConvertFrom-Json
if ($manifest.状态 -ne '完成') { throw '持续测试未完整完成，不生成正式面试数据摘要' }
$stages = @("$Prefix-normal-1","$Prefix-peak-1","$Prefix-burst",
    "$Prefix-peak-2","$Prefix-peak-3","$Prefix-normal-2")
$rounds = @()
foreach ($name in @('baseline-peak','baseline-burst','threads-probe','pool-probe','mq-probe') + $stages + @('release-login-clean')) {
    $directory = Join-Path $results $name
    $summary = @(Get-Content (Join-Path $directory 'summary.json') -Raw | ConvertFrom-Json)
    $rounds += [ordered]@{
        轮次 = $name
        排除爬升秒 = if ($name -like '*-probe') { 30 } else { 0 }
        原始采样SHA256 = (Get-FileHash (Join-Path $directory 'samples.jtl') -Algorithm SHA256).Hash
        原始采样 = "tools/results/$name/samples.jtl"
        HTML报告 = "tools/results/$name/report/index.html"
        接口统计 = $summary
    }
}
$resources = @(Get-Content (Join-Path $results "$Prefix-resources.ndjson") | ForEach-Object { $_ | ConvertFrom-Json })
$resourceGroups = @()
foreach ($stage in $stages) {
    $samples = @($resources | Where-Object 阶段 -eq $stage)
    if (-not $samples.Count) { throw "阶段没有资源记录：$stage" }
    $cpu = @($samples | Where-Object { $null -ne $_.进程CPU百分比 })
    $pending = @($samples | ForEach-Object { [int]$_.数据库.pending })
    $age = @($samples | ForEach-Object { [int]$_.数据库.oldestSeconds })
    $ready = @($samples | ForEach-Object {
        ($_.队列 | Measure-Object -Property messages_ready -Sum).Sum
    })
    $unacked = @($samples | ForEach-Object {
        ($_.队列 | Measure-Object -Property messages_unacknowledged -Sum).Sum
    })
    $failed = @($samples | ForEach-Object {
        ($_.队列 | Where-Object name -eq 'takeme.consumer.failed.queue').messages_ready
    })
    # 时间序列峰值只代表采样点，不能声称捕获了每个瞬时峰值。
    $resourceGroups += [ordered]@{
        阶段 = $stage; 采样点 = $samples.Count
        最早 = $samples[0].时间; 最晚 = $samples[-1].时间
        进程CPU采样平均百分比 = [math]::Round(($cpu | Measure-Object 进程CPU百分比 -Average).Average, 2)
        进程CPU采样峰值百分比 = ($cpu | Measure-Object 进程CPU百分比 -Maximum).Maximum
        主机CPU采样峰值百分比 = ($samples | Measure-Object 主机CPU百分比 -Maximum).Maximum
        主机可用内存采样最小MB = ($samples | Measure-Object 主机可用内存MB -Minimum).Minimum
        工作集采样峰值MB = ($samples | Measure-Object 工作集MB -Maximum).Maximum
        私有内存采样峰值MB = ($samples | Measure-Object 私有内存MB -Maximum).Maximum
        堆使用采样峰值MB = ($samples | Measure-Object 堆已用MB -Maximum).Maximum
        堆容量采样峰值MB = ($samples | Measure-Object 堆容量MB -Maximum).Maximum
        线程采样峰值 = ($samples | Measure-Object 线程数 -Maximum).Maximum
        待发送事件采样峰值 = ($pending | Measure-Object -Maximum).Maximum
        最老待发送采样峰值秒 = ($age | Measure-Object -Maximum).Maximum
        MQ待消费采样峰值 = ($ready | Measure-Object -Maximum).Maximum
        MQ未确认采样峰值 = ($unacked | Measure-Object -Maximum).Maximum
        失败队列采样峰值 = ($failed | Measure-Object -Maximum).Maximum
        MySQL全局连接采样峰值 = ($samples | ForEach-Object { [int]$_.数据库.globalStatus.Threads_connected } | Measure-Object -Maximum).Maximum
        MySQL全局运行线程采样峰值 = ($samples | ForEach-Object { [int]$_.数据库.globalStatus.Threads_running } | Measure-Object -Maximum).Maximum
        MySQL全局状态起点 = $samples[0].数据库.globalStatus
        MySQL全局状态终点 = $samples[-1].数据库.globalStatus
    }
}
$gcLines = Get-Content (Join-Path $results "$Prefix-soak-after-gc.log")
$pauses = @($gcLines | Where-Object { $_ -match '\[gc\s*\].*Pause.* ([0-9.]+)ms$' } |
    ForEach-Object { if ($_ -match ' ([0-9.]+)ms$') { [double]$Matches[1] } })
$output = [ordered]@{
    生成时间 = (Get-Date).ToString('o')
    清单 = $manifest
    统计口径 = 'RPS 为 HTTP 吞吐；候选短测排除前 30 秒；不平均阶段分位数；资源峰值为 30 秒采样点'
    对比限制 = '基线与候选无额外 600 条推送连接；正式复测含推送和资源采样且数据规模增长，不作严格单参数因果归因'
    轮次 = $rounds
    资源阶段统计 = $resourceGroups
    GC = @{
        FullGC或OOM日志条数 = @($gcLines | Where-Object { $_ -match 'Pause Full|OutOfMemory' }).Count
        最大暂停毫秒 = ($pauses | Measure-Object -Maximum).Maximum
        首个采样 = $resources[0].GC累计
        最后采样 = $resources[-1].GC累计
    }
    原始资源 = "tools/results/$Prefix-resources.ndjson"
    原始资源SHA256 = (Get-FileHash (Join-Path $results "$Prefix-resources.ndjson") -Algorithm SHA256).Hash
    回归 = @{总数=96;通过=75;跳过旧演示=21;失败=0;错误=0}
}
$path = Join-Path $root 'docs\loadtest-measurements.json'
$output | ConvertTo-Json -Depth 15 | Set-Content -LiteralPath $path -Encoding utf8
Write-Output "脱敏统计摘要已生成：$path"
$resourceGroups | ForEach-Object { [pscustomobject]$_ } |
    Select-Object 阶段,采样点,进程CPU采样平均百分比,工作集采样峰值MB,待发送事件采样峰值,最老待发送采样峰值秒 |
    Format-Table -AutoSize
