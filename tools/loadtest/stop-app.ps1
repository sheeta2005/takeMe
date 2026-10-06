$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$record = Get-Content (Join-Path $root 'tools\.runtime\app-process.json') -Raw | ConvertFrom-Json
$process = Get-CimInstance Win32_Process -Filter "ProcessId = $($record.pid)"
if (-not $process) { Write-Output '隔离应用已停止'; return }
# 仅允许停止登记过且命令行确实属于隔离测试应用的进程。
if ($process.Name -ne 'java.exe' -or -not $process.CommandLine.Contains($record.jar) -or
    $process.CommandLine -notlike '*dev,loadtest*') {
    throw 'PID 已被其他程序复用，禁止停止'
}
Stop-Process -Id $record.pid
Write-Output "隔离应用已停止：PID=$($record.pid)"
