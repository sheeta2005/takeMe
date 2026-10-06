param(
    [Parameter(Mandatory = $true)][string]$DbPassword,
    [Parameter(Mandatory = $true)][string]$MqPassword,
    [string]$DbUser = 'root',
    [string]$MqUser = 'admin',
    [switch]$Resume,
    [string]$Repository = 'D:\tool\developset\maven\apache-maven-3.9.10\mvn_repo',
    [string]$JavaHome = 'D:\tool\developset\Java\java21.0.7'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$runtime = Join-Path $root 'tools\.runtime'
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
if ((Test-Path (Join-Path $runtime 'environment.json')) -and -not $Resume) { throw '隔离环境已配置，禁止覆盖' }
$secret = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
$environment = @{
    TAKEME_TEST_DB_USER = $DbUser; TAKEME_TEST_DB_PASSWORD = $DbPassword
    TAKEME_TEST_MQ_USER = $MqUser; TAKEME_TEST_MQ_PASSWORD = $MqPassword
    TAKEME_TEST_JWT_SECRET = $secret; TAKEME_TEST_LOG_DIR = ($runtime.Replace('\','/') + '/logs')
}
# 运行时文件仅包含本机凭据，不纳入版本控制。
if ($Resume) {
    $saved = (Get-Content (Join-Path $runtime 'environment.json') -Raw | ConvertFrom-Json).environment
    foreach ($property in $saved.PSObject.Properties) { $environment[$property.Name] = $property.Value }
} else {
    @{ environment = $environment } | ConvertTo-Json | Set-Content (Join-Path $runtime 'environment.json') -Encoding utf8
}
foreach ($name in $environment.Keys) { [Environment]::SetEnvironmentVariable($name, $environment[$name], 'Process') }
$dockerName = 'takeme-loadtest-redis-20261004'
$existing = docker ps -a --filter "name=^/$dockerName$" --format '{{.Names}}'
if ($existing -and -not $Resume) { throw '专用 Redis 容器已存在，不自动重建或清空' }
if (-not $existing) {
    # 缓存与限流共用 Redis，保留 noeviction，避免内存淘汰抹掉安全限流计数。
    docker run -d --name $dockerName -p 127.0.0.1:6381:6379 redis:7 `
        redis-server --maxmemory 64mb --maxmemory-policy noeviction | Out-Null
    if ($LASTEXITCODE -ne 0) { throw '启动专用 Redis 失败' }
}
$headers = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${MqUser}:$MqPassword")) }
$vhost = 'takeme_loadtest_20261004'
Invoke-RestMethod "http://localhost:15672/api/vhosts/$vhost" -Headers $headers -Method Put -ContentType 'application/json' -Body '{}' | Out-Null
$permission = @{ configure='.*'; write='.*'; read='.*' } | ConvertTo-Json
Invoke-RestMethod "http://localhost:15672/api/permissions/$vhost/$MqUser" -Headers $headers -Method Put `
    -ContentType 'application/json' -Body $permission | Out-Null
$jars = @(
    "$Repository\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar",
    "$Repository\org\springframework\security\spring-security-crypto\6.2.4\spring-security-crypto-6.2.4.jar",
    "$Repository\org\springframework\spring-jcl\6.1.6\spring-jcl-6.1.6.jar",
    "$Repository\io\jsonwebtoken\jjwt-api\0.11.5\jjwt-api-0.11.5.jar",
    "$Repository\io\jsonwebtoken\jjwt-impl\0.11.5\jjwt-impl-0.11.5.jar",
    "$Repository\io\jsonwebtoken\jjwt-jackson\0.11.5\jjwt-jackson-0.11.5.jar",
    "$Repository\com\fasterxml\jackson\core\jackson-databind\2.15.4\jackson-databind-2.15.4.jar",
    "$Repository\com\fasterxml\jackson\core\jackson-core\2.15.4\jackson-core-2.15.4.jar",
    "$Repository\com\fasterxml\jackson\core\jackson-annotations\2.15.4\jackson-annotations-2.15.4.jar"
)
foreach ($jar in $jars) { if (-not (Test-Path -LiteralPath $jar)) { throw "依赖不存在：$jar" } }
& (Join-Path $JavaHome 'bin\java.exe') --class-path ($jars -join ';') `
    (Join-Path $PSScriptRoot 'LoadTestFixture.java') $runtime
if ($LASTEXITCODE -ne 0) { throw '合成数据创建失败，保留现场供检查' }
