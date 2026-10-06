param(
    [Parameter(Mandatory = $true)][ValidateSet('before','after')][string]$Phase,
    [ValidatePattern('^[a-z0-9_-]+$')][string]$Prefix,
    [switch]$BurstOnly
)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$runtime = Join-Path $root 'tools\.runtime'
$prefix = if ($Prefix) { $Prefix } else { "optimization-$Phase" }
$manifestPath = "$root\tools\results\$prefix-manifest.json"
if (Test-Path $manifestPath) { throw '禁止覆盖历史对照轮次' }
$record = Get-Content "$runtime\app-process.json" -Raw | ConvertFrom-Json
# 清单记录运行中应用的实际参数，不能把尚未重启的旧 JVM 标记成新配置。
$app = Get-CimInstance Win32_Process -Filter "ProcessId=$($record.pid)"
if (-not $app -or $app.Name -ne 'java.exe') { throw '隔离应用未运行，无法记录实际 JVM 参数' }
$jvmArguments = @([regex]::Matches($app.CommandLine,
    '(?<!\S)-X(?:ms|mx)\S+|(?<!\S)-XX:\+UseG1GC') | ForEach-Object { $_.Value }) -join ' '
if (-not $jvmArguments) { throw '无法读取实际 JVM 参数，禁止生成错误测量清单' }
$manifest = [ordered]@{
    开始时间=(Get-Date).ToString('o'); 状态='运行中'; 应用PID=$record.pid
    构建SHA256=(Get-FileHash $record.jar -Algorithm SHA256).Hash
    JVM参数=$jvmArguments; 推送连接数=600; 采样间隔秒=10
    限制='同机短对照，不替代上一轮 60 分钟测试；合成交易增量保留，非固定数据集严格因果实验'
}
$environment = (Get-Content "$runtime\environment.json" -Raw | ConvertFrom-Json).environment
$env:MYSQL_PWD = $environment.TAKEME_TEST_DB_PASSWORD
$counts = & 'D:\tool\MySQL\MySQL Server 8.0\bin\mysql.exe' '-u' $environment.TAKEME_TEST_DB_USER `
    --database=takeme_loadtest_20261004 --batch --skip-column-names `
    '--execute=SELECT JSON_OBJECT("orders",(SELECT COUNT(*) FROM `order`),"items",(SELECT COUNT(*) FROM order_item),"payments",(SELECT COUNT(*) FROM payment_transaction));'
if ($LASTEXITCODE -ne 0) { throw '数据规模采集失败' }
$manifest.开测数据规模 = $counts | ConvertFrom-Json
$manifest | ConvertTo-Json | Set-Content $manifestPath -Encoding utf8
$stopFile = "$runtime\compare-stop-$([guid]::NewGuid().ToString('N'))"
$phaseFile = "$runtime\phase-$prefix.txt"
$sampler = Start-Process (Get-Process -Id $PID).Path -ArgumentList @(
    '-NoProfile','-File',"$PSScriptRoot\sample-resources.ps1",
    '-OutputPath',"$root\tools\results\$prefix-resources.ndjson",'-StopFile',$stopFile,
    '-PhaseFile',$phaseFile,'-IntervalSeconds','10') -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput "$root\tools\results\$prefix-resources.log" `
    -RedirectStandardError "$root\tools\results\$prefix-resources-error.log"
$sockets = Start-Process 'D:\tool\Node\node.exe' -ArgumentList @(
    "$PSScriptRoot\websocket-check.mjs","$runtime\websocket-accounts.json",'1200',$stopFile
) -WindowStyle Hidden -PassThru -RedirectStandardOutput "$root\tools\results\$prefix-websocket.log" `
    -RedirectStandardError "$root\tools\results\$prefix-websocket-error.log"
try {
    Start-Sleep -Seconds 5
    if ($sockets.HasExited -or $sampler.HasExited) { throw '采样或推送进程启动失败' }
    $stages = @(
        @{name='warmup';threads=60;rps=30;seconds=60},
        @{name='peak';threads=300;rps=180;seconds=120},
        @{name='burst';threads=600;rps=360;seconds=180}
    )
    if ($BurstOnly) { $stages = @($stages | Where-Object name -ne 'peak') }
    foreach ($stage in $stages) {
        "$prefix-$($stage.name)" | Set-Content $phaseFile
        & "$PSScriptRoot\run.ps1" -Name "$prefix-$($stage.name)" -Threads $stage.threads `
            -Rps $stage.rps -Seconds $stage.seconds -Ramp 15
        & "$PSScriptRoot\summarize.ps1" -Name "$prefix-$($stage.name)"
        if ($sockets.HasExited -or $sampler.HasExited) { throw '采样或推送进程提前退出' }
    }
    & "$PSScriptRoot\snapshot.ps1" -Name "$prefix-completed"
    $manifest.状态 = '完成'
} catch {
    $manifest.状态 = '失败或中断'
    throw
} finally {
    'stop' | Set-Content $stopFile
    if (-not $sampler.WaitForExit(15000) -or -not $sockets.WaitForExit(15000) -or
        $sampler.ExitCode -ne 0 -or $sockets.ExitCode -ne 0) {
        $manifest.状态 = '辅助采样失败'
    }
    $manifest.结束时间 = (Get-Date).ToString('o')
    $manifest | ConvertTo-Json | Set-Content $manifestPath -Encoding utf8
}
if ($manifest.状态 -ne '完成') { throw '本轮对照未完整结束' }
