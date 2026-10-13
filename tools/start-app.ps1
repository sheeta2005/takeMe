param([string]$JavaHome = 'D:\tool\developset\Java\java21.0.7')
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$jar = Join-Path $root 'takeMe/server/target/server-1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw '请先在 takeMe 目录构建 server 模块' }
$logDir = & (Join-Path $PSScriptRoot 'prepare-log-directory.ps1')
$arguments = @('-Xms1024m', '-Xmx1024m', '-XX:+UseG1GC',
    '-XX:+HeapDumpOnOutOfMemoryError',
    "-XX:HeapDumpPath=$logDir/diagnostics",
    "-Xlog:gc*:file=$logDir/takeMe-8080-gc.log:time,uptime,level,tags:filecount=5,filesize=10M",
    "-Dlogging.file.path=$logDir", '-jar', $jar)
# 与 IDEA 使用同一工作目录，保证相对路径不会随调用位置变化。
Push-Location $root
try {
    & (Join-Path $JavaHome 'bin/java.exe') @arguments
    if ($LASTEXITCODE -ne 0) { throw "应用退出，退出码：$LASTEXITCODE" }
} finally {
    Pop-Location
}
