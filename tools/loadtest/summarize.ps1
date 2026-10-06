param([Parameter(Mandatory = $true)][string]$Name, [int]$WarmupSeconds = 0)
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$directory = Join-Path $root "tools\results\$Name"
$samples = @(Import-Csv (Join-Path $directory 'samples.jtl') -Encoding utf8)
if (-not $samples.Count) { throw '没有采样记录' }
$start = ($samples | Measure-Object -Property timeStamp -Minimum).Minimum
$samples = @($samples | Where-Object { [double]$_.timeStamp -ge $start + $WarmupSeconds * 1000 })
$finish = ($samples | ForEach-Object { [double]$_.timeStamp + [int]$_.elapsed } | Measure-Object -Maximum).Maximum
$start = ($samples | Measure-Object -Property timeStamp -Minimum).Minimum
$seconds = ($finish - $start) / 1000
$rows = @()
foreach ($group in @(@{Name='总计';Group=$samples}) + @($samples | Group-Object label)) {
    $values = @($group.Group | ForEach-Object { [int]$_.elapsed } | Sort-Object)
    $errors = @($group.Group | Where-Object { $_.success -ne 'true' }).Count
    $rows += [pscustomobject]@{
        接口 = $group.Name; 请求数 = $values.Count
        错误数 = $errors; 错误率 = [math]::Round($errors * 100.0 / $values.Count, 3)
        RPS = [math]::Round($values.Count / $seconds, 2)
        平均毫秒 = [math]::Round(($values | Measure-Object -Average).Average, 2)
        P95毫秒 = $values[[math]::Max(0, [math]::Ceiling($values.Count * .95) - 1)]
        P99毫秒 = $values[[math]::Max(0, [math]::Ceiling($values.Count * .99) - 1)]
        最大毫秒 = $values[-1]
    }
}
$rows | ConvertTo-Json | Set-Content (Join-Path $directory 'summary.json') -Encoding utf8
$rows | Format-Table -AutoSize
$samples | Where-Object { $_.success -ne 'true' } | Group-Object responseMessage |
    Select-Object Count, Name | Sort-Object Count -Descending | Select-Object -First 10
