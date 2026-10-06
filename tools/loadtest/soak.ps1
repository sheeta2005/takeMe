param(
    [ValidateSet('threads','pool','mq','tuned','community')][string]$Mode = 'community',
    [ValidatePattern('^[a-z0-9_-]+$')][string]$Prefix = 'verified'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$runtime = Join-Path $root 'tools\.runtime'
$manifestPath = Join-Path $root "tools\results\$Prefix-manifest.json"
if (Test-Path -LiteralPath $manifestPath) { throw '持续测试轮次已存在，禁止覆盖' }
& (Join-Path $PSScriptRoot 'start-app.ps1') -Mode $Mode -ConnectionLimit 2000
$processRecord = Get-Content (Join-Path $runtime 'app-process.json') -Raw | ConvertFrom-Json
$machine = Get-CimInstance Win32_ComputerSystem
$manifest = [ordered]@{
    开始时间 = (Get-Date).ToString('o'); 状态 = '运行中'; 模式 = $Mode; 应用PID = $processRecord.pid
    构建SHA256 = (Get-FileHash -LiteralPath $processRecord.jar -Algorithm SHA256).Hash
    逻辑处理器数 = $machine.NumberOfLogicalProcessors
    物理内存GB = [math]::Round($machine.TotalPhysicalMemory / 1GB, 2)
    JVM参数 = '-Xms1024m -Xmx1024m -XX:+UseG1GC'
    JMeter版本 = '5.6.3'; Java版本 = '21.0.7'; 推送连接数 = 600; 采样间隔秒 = 30
}
$environment = (Get-Content (Join-Path $runtime 'environment.json') -Raw | ConvertFrom-Json).environment
$env:MYSQL_PWD = $environment.TAKEME_TEST_DB_PASSWORD
$counts = & 'D:\tool\MySQL\MySQL Server 8.0\bin\mysql.exe' "-u$($environment.TAKEME_TEST_DB_USER)" `
    --default-character-set=utf8mb4 --database=takeme_loadtest_20261004 --batch --skip-column-names `
    '--execute=SELECT JSON_OBJECT("老人",(SELECT COUNT(*) FROM `user`),"志愿者",(SELECT COUNT(*) FROM volunteer),"管理员",(SELECT COUNT(*) FROM admin),"订单",(SELECT COUNT(*) FROM `order`),"服务项",(SELECT COUNT(*) FROM order_item));'
if ($LASTEXITCODE -ne 0) { throw '测试数据规模记录失败' }
$manifest.开测数据规模 = $counts | ConvertFrom-Json
$manifest | ConvertTo-Json | Set-Content -LiteralPath $manifestPath -Encoding utf8
$accounts = Import-Csv (Join-Path $runtime 'accounts.csv') -Encoding utf8 | Select-Object -First 600
$accounts | ConvertTo-Json -Depth 3 | Set-Content (Join-Path $runtime 'websocket-accounts.json') -Encoding utf8
$stopFile = Join-Path $runtime ("ws-stop-" + [guid]::NewGuid().ToString('N'))
$phaseFile = Join-Path $runtime "phase-$Prefix.txt"
$sampler = Start-Process (Get-Process -Id $PID).Path -ArgumentList @(
    '-NoProfile','-File',(Join-Path $PSScriptRoot 'sample-resources.ps1'),
    '-OutputPath',(Join-Path $root "tools\results\$Prefix-resources.ndjson"),
    '-StopFile',$stopFile,'-PhaseFile',$phaseFile) -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $root "tools\results\$Prefix-resources.log") `
    -RedirectStandardError (Join-Path $root "tools\results\$Prefix-resources-error.log")
$socketProcess = Start-Process (Get-Command node).Source -ArgumentList @(
    (Join-Path $PSScriptRoot 'websocket-check.mjs'),(Join-Path $runtime 'websocket-accounts.json'),
    '4500',$stopFile) -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $root "tools\results\$Prefix-websocket.log") `
    -RedirectStandardError (Join-Path $root "tools\results\$Prefix-websocket-error.log")
try {
Start-Sleep -Seconds 5
if ($socketProcess.HasExited) { throw 'WebSocket 连接验证启动失败' }
if ($sampler.HasExited) { throw '资源采样启动失败' }
"$Prefix-warmup" | Set-Content -LiteralPath $phaseFile
& (Join-Path $PSScriptRoot 'run.ps1') -Name "$Prefix-warmup" -Threads 60 -Rps 30 -Seconds 180 -Ramp 15
if ($sampler.HasExited) { throw '预热期间资源采样退出' }
& (Join-Path $PSScriptRoot 'snapshot.ps1') -Name "$Prefix-soak-before"
# 同一 JVM 连续运行至少 60 分钟，穿插真实业务低谷、高峰及一次突发。
$stages = @(
    @{Name="$Prefix-normal-1";Threads=60;Rps=30},
    @{Name="$Prefix-peak-1";Threads=300;Rps=180},
    @{Name="$Prefix-burst";Threads=600;Rps=360},
    @{Name="$Prefix-peak-2";Threads=300;Rps=180},
    @{Name="$Prefix-peak-3";Threads=300;Rps=180},
    @{Name="$Prefix-normal-2";Threads=60;Rps=30}
)
foreach ($stage in $stages) {
    $stage.Name | Set-Content -LiteralPath $phaseFile
    & (Join-Path $PSScriptRoot 'run.ps1') -Name $stage.Name -Threads $stage.Threads `
        -Rps $stage.Rps -Seconds 600 -Ramp 30
    & (Join-Path $PSScriptRoot 'snapshot.ps1') -Name "$($stage.Name)-after"
    if ($socketProcess.HasExited) { throw '持续测试期间 WebSocket 验证退出' }
    if ($sampler.HasExited) { throw '持续测试期间资源采样退出' }
    & (Join-Path $PSScriptRoot 'summarize.ps1') -Name $stage.Name
}
& (Join-Path $PSScriptRoot 'snapshot.ps1') -Name "$Prefix-soak-after"
} catch {
    $manifest.状态 = '中断或失败'
    $manifest.结束时间 = (Get-Date).ToString('o')
    $manifest | ConvertTo-Json | Set-Content -LiteralPath $manifestPath -Encoding utf8
    throw
} finally {
# 通过本轮专用信号文件正常关闭连接，而非强制终止无关 Node 进程。
'stop' | Set-Content -LiteralPath $stopFile
$samplerStopped = $sampler.WaitForExit(15000)
if (-not $samplerStopped -or $sampler.ExitCode -ne 0) {
    Write-Warning '资源采样未正常结束，请检查错误日志'
}
}
if (-not $samplerStopped -or $sampler.ExitCode -ne 0) {
    $manifest.状态 = '资源采样失败'
    $manifest | ConvertTo-Json | Set-Content -LiteralPath $manifestPath -Encoding utf8
    throw '不能把资源采样失败的轮次记录为完整通过'
}
if (-not $socketProcess.WaitForExit(15000) -or $socketProcess.ExitCode -ne 0) {
    throw 'WebSocket 连接释放验证失败'
}
$manifest.状态 = '完成'
$manifest.结束时间 = (Get-Date).ToString('o')
$manifest | ConvertTo-Json | Set-Content -LiteralPath $manifestPath -Encoding utf8
# 应用保留供本机试用，Arthas 仍须手工附加。
