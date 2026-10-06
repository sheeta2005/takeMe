param(
    [ValidateSet('baseline','threads','pool','mq','tuned','off','community')][string]$Mode = 'baseline',
    [int]$ConnectionLimit = 1000,
    [string]$JavaHome = 'D:\tool\developset\Java\java21.0.7'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$runtime = Join-Path $root 'tools\.runtime'
$state = Get-Content (Join-Path $runtime 'environment.json') -Raw | ConvertFrom-Json
foreach ($name in $state.environment.PSObject.Properties.Name) {
    [Environment]::SetEnvironmentVariable($name, $state.environment.$name, 'Process')
}
$jar = Join-Path $root 'takeMe\server\target\server-1.0-SNAPSHOT.jar'
if (Get-NetTCPConnection -LocalPort 9081 -State Listen -ErrorAction SilentlyContinue) {
    throw '9081 已被占用，请先停止此前的隔离测试应用'
}
$arguments = @('-Xms1024m','-Xmx1024m','-XX:+UseG1GC',
    "-Djdk.net.unixdomain.tmpdir=$runtime",
    '-Xlog:gc*:file=tools/.runtime/gc.log:time,uptime,level,tags:filecount=5,filesize=20M',
    '-jar', $jar)
# 发布配置复核不靠命令行覆盖参数，直接使用随应用打包的社区配置。
$arguments += if ($Mode -eq 'community') { '--spring.profiles.active=dev,loadtest,community' } else { '--spring.profiles.active=dev,loadtest' }
if ($Mode -eq 'off') { $arguments += '--middleware.enabled=false' }
if ($Mode -in @('threads','pool','mq','tuned')) {
    # 候选参数单独运行对比，未验证之前不覆盖开发环境默认值。
    $arguments += @('--server.tomcat.threads.max=80','--server.tomcat.threads.min-spare=10',
        '--server.tomcat.accept-count=100',"--server.tomcat.max-connections=$ConnectionLimit")
}
if ($Mode -in @('pool','mq','tuned')) {
    $arguments += @('--me.datasource.druid.max-active=16','--me.datasource.druid.max-wait=3000')
}
if ($Mode -in @('mq','tuned')) {
    $arguments += @('--spring.rabbitmq.listener.simple.prefetch=20',
        '--me.rabbitmq.listener.simple.concurrency=1','--me.rabbitmq.listener.simple.max-concurrency=3')
}
$process = Start-Process (Join-Path $JavaHome 'bin\java.exe') -ArgumentList $arguments `
    -WorkingDirectory $root -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $runtime "app-$Mode.log") `
    -RedirectStandardError (Join-Path $runtime "app-$Mode-error.log")
@{ pid = $process.Id; mode = $Mode; jar = $jar } | ConvertTo-Json |
    Set-Content (Join-Path $runtime 'app-process.json') -Encoding utf8
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 2
    if ($process.HasExited) { throw '测试应用退出，请检查启动日志' }
    try {
        $response = Invoke-WebRequest 'http://127.0.0.1:9081/api/user/info' -SkipHttpErrorCheck -TimeoutSec 2
        if ($response.StatusCode -eq 401) { Write-Output "隔离应用已启动：PID=$($process.Id)，模式=$Mode"; return }
    } catch { }
}
throw '等待应用启动超时'
