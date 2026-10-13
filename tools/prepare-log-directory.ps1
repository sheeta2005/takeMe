param([string]$LogDir = $env:TAKEME_LOG_DIR)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
if ([string]::IsNullOrWhiteSpace($LogDir)) { $LogDir = 'logs/dev' }
if (-not [IO.Path]::IsPathRooted($LogDir)) { $LogDir = Join-Path $root $LogDir }
$LogDir = [IO.Path]::GetFullPath($LogDir)
# GC 日志由 JVM 初始化时打开，必须在启动 Java 前准备目录。
New-Item -ItemType Directory -Path $LogDir, (Join-Path $LogDir 'diagnostics') -Force | Out-Null
$LogDir
