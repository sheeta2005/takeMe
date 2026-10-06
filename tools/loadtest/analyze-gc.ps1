$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$path = Join-Path $root 'tools\results\community-final-soak-after-gc.log'
$lines = Get-Content $path
$pauses = @(
    foreach ($line in $lines) {
        if ($line -match '^\[([^\]]+)\].*GC\((\d+)\) Pause (.+) (\d+)M->(\d+)M\((\d+)M\) ([0-9.]+)ms$') {
            $date = [DateTimeOffset]::ParseExact($Matches[1], "yyyy-MM-dd'T'HH:mm:ss.fffzzz", [Globalization.CultureInfo]::InvariantCulture)
            [pscustomobject]@{end=$date.ToUnixTimeMilliseconds();ms=[double]$Matches[7];kind=$Matches[3];afterMB=[int]$Matches[5]}
        }
    }
)
$failures = @(Import-Csv "$root\tools\results\community-final-burst\samples.jtl" | Where-Object success -ne 'true')
$overlaps = @(
    foreach ($failure in $failures) {
        $start = [double]$failure.timeStamp
        $end = $start + [double]$failure.elapsed
        $total = 0.0
        foreach ($pause in $pauses) {
            $total += [math]::Max(0, [math]::Min($end,$pause.end)-[math]::Max($start,$pause.end-$pause.ms))
        }
        [pscustomobject]@{接口=$failure.label;开始毫秒=$start;耗时毫秒=[double]$failure.elapsed;重叠停顿毫秒=[math]::Round($total,2)}
    }
)
$manifest = Get-Content "$root\tools\results\community-final-manifest.json" -Raw | ConvertFrom-Json
$duration = ([DateTimeOffset]::Parse($manifest.结束时间)-[DateTimeOffset]::Parse($manifest.开始时间)).TotalSeconds
$totalMs = ($pauses | Measure-Object ms -Sum).Sum
$result = [ordered]@{
    日志SHA256=(Get-FileHash $path -Algorithm SHA256).Hash
    日志说明='含启动预热；停顿总量占比以正式清单时长计算，是保守上界；不是 GC 总 CPU 占比'
    清单时长秒=[math]::Round($duration,2)
    停顿次数=$pauses.Count
    Young次数=@($pauses | Where-Object kind -like 'Young*').Count
    Full次数=@($lines | Where-Object { $_ -match 'Pause Full' -and $_ -match 'ms$' }).Count
    内存异常条目=@($lines | Where-Object { $_ -match 'OutOfMemory|to-space exhausted|Evacuation Failure' }).Count
    累计停顿毫秒=[math]::Round($totalMs,2)
    最大停顿毫秒=($pauses | Measure-Object ms -Maximum).Maximum
    停顿时长占比上界百分比=[math]::Round($totalMs / ($duration * 1000) * 100,3)
    GC后堆最大MB=($pauses | Measure-Object afterMB -Maximum).Maximum
    压测失败数=$failures.Count
    失败请求最大重叠停顿毫秒=($overlaps | Measure-Object 重叠停顿毫秒 -Maximum).Maximum
    失败请求=$overlaps
}
$result | ConvertTo-Json -Depth 8 | Set-Content "$root\tools\results\optimization-gc.json" -Encoding utf8
[pscustomobject]$result | Select-Object * -ExcludeProperty 失败请求 | ConvertTo-Json
