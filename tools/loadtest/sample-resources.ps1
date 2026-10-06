param(
    [Parameter(Mandatory = $true)][string]$OutputPath,
    [Parameter(Mandatory = $true)][string]$StopFile,
    [Parameter(Mandatory = $true)][string]$PhaseFile,
    [int]$IntervalSeconds = 30,
    [string]$JavaHome = 'D:\tool\developset\Java\java21.0.7'
)
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$OutputEncoding = [Text.UTF8Encoding]::new($false)
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$record = Get-Content (Join-Path $root 'tools\.runtime\app-process.json') -Raw | ConvertFrom-Json
$state = (Get-Content (Join-Path $root 'tools\.runtime\environment.json') -Raw | ConvertFrom-Json).environment
if (Test-Path -LiteralPath $OutputPath) { throw '资源采样文件已存在，禁止覆盖' }
$headers = @{ Authorization = 'Basic ' + [Convert]::ToBase64String(
    [Text.Encoding]::UTF8.GetBytes("$($state.TAKEME_TEST_MQ_USER):$($state.TAKEME_TEST_MQ_PASSWORD)")) }
$env:MYSQL_PWD = $state.TAKEME_TEST_DB_PASSWORD
$sql = @'
SELECT JSON_OBJECT(
 'pending', (SELECT COUNT(*) FROM mq_outbox WHERE status=0),
 'oldestSeconds', (SELECT TIMESTAMPDIFF(SECOND,MIN(create_time),NOW()) FROM mq_outbox WHERE status=0),
 'globalStatus', (SELECT JSON_OBJECTAGG(VARIABLE_NAME,VARIABLE_VALUE)
 FROM performance_schema.global_status WHERE VARIABLE_NAME IN
 ('Threads_connected','Threads_running','Innodb_row_lock_time','Innodb_row_lock_waits')));
'@
$clock = [Diagnostics.Stopwatch]::StartNew()
$previousTime = $null
$previousCpu = $null
while (-not (Test-Path -LiteralPath $StopFile)) {
    $process = Get-Process -Id $record.pid -ErrorAction Stop
    $now = $clock.Elapsed.TotalSeconds
    $cpu = $process.CPU
    # 进程 CPU 秒的增量按逻辑处理器归一化，不能把累计 CPU 秒误写为使用率。
    $cpuPercent = if ($null -ne $previousTime) {
        [math]::Round(100 * ($cpu - $previousCpu) / ($now - $previousTime) / [Environment]::ProcessorCount, 2)
    } else { $null }
    $previousTime = $now
    $previousCpu = $cpu
    $memory = Get-CimInstance Win32_OperatingSystem
    $hostCpu = Get-CimInstance Win32_PerfFormattedData_PerfOS_Processor -Filter "Name='_Total'"
    $queues = Invoke-RestMethod 'http://localhost:15672/api/queues/takeme_loadtest_20261004' -Headers $headers
    $database = & 'D:\tool\MySQL\MySQL Server 8.0\bin\mysql.exe' "-u$($state.TAKEME_TEST_DB_USER)" `
        --default-character-set=utf8mb4 --database=takeme_loadtest_20261004 --batch --skip-column-names "--execute=$sql"
    if ($LASTEXITCODE -ne 0) { throw '数据库采样失败' }
    $gc = @(& (Join-Path $JavaHome 'bin\jstat.exe') -gc $record.pid)
    if ($LASTEXITCODE -ne 0 -or $gc.Count -lt 2) { throw 'JVM 资源采样失败' }
    $names = $gc[0].Trim() -split '\s+'
    $values = $gc[1].Trim() -split '\s+'
    $jvm = @{}
    for ($i = 0; $i -lt $names.Count; $i++) {
        $jvm[$names[$i]] = [double]::Parse($values[$i], [Globalization.CultureInfo]::InvariantCulture)
    }
    $sample = [ordered]@{
        时间 = (Get-Date).ToString('o')
        阶段 = if (Test-Path -LiteralPath $PhaseFile) { (Get-Content -LiteralPath $PhaseFile -Raw).Trim() } else { '启动' }
        应用PID = $record.pid
        进程CPU百分比 = $cpuPercent
        主机CPU百分比 = $hostCpu.PercentProcessorTime
        主机可用内存MB = [math]::Round($memory.FreePhysicalMemory / 1024, 2)
        工作集MB = [math]::Round($process.WorkingSet64 / 1MB, 2)
        私有内存MB = [math]::Round($process.PrivateMemorySize64 / 1MB, 2)
        线程数 = $process.Threads.Count
        堆已用MB = [math]::Round(($jvm.S0U + $jvm.S1U + $jvm.EU + $jvm.OU) / 1024, 2)
        堆容量MB = [math]::Round(($jvm.S0C + $jvm.S1C + $jvm.EC + $jvm.OC) / 1024, 2)
        GC累计 = $jvm
        数据库 = $database | ConvertFrom-Json
        队列 = @($queues | Select-Object name,messages_ready,messages_unacknowledged,consumers)
    }
    # 一条记录一行，异常退出时也保留已经完成的观测，不包含凭据或老人个人资料。
    $sample | ConvertTo-Json -Depth 8 -Compress | Add-Content -LiteralPath $OutputPath -Encoding utf8
    for ($wait = 0; $wait -lt $IntervalSeconds -and -not (Test-Path -LiteralPath $StopFile); $wait++) {
        Start-Sleep -Seconds 1
    }
}
