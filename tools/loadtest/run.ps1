param(
    [Parameter(Mandatory = $true)][string]$Name,
    [int]$Threads = 60, [int]$Rps = 30, [int]$Seconds = 600, [int]$Ramp = 30,
    [ValidateSet('community','login')][string]$Plan = 'community',
    [string]$JavaHome = 'D:\tool\developset\Java\java21.0.7',
    [string]$JMeterHome = 'D:\tool\developset\takeme-tools\apache-jmeter-5.6.3'
)
$ErrorActionPreference = 'Stop'
if ($Name -notmatch '^[a-z0-9_-]+$') { throw '结果目录名称仅允许字母数字及下划线连字符' }
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$result = Join-Path $root "tools\results\$Name"
if (Test-Path -LiteralPath $result) { throw '结果目录已存在，禁止覆盖历史测量' }
New-Item -ItemType Directory -Path $result -Force | Out-Null
if ($Plan -eq 'community') { & (Join-Path $PSScriptRoot 'refresh-fixture.ps1') }
$env:JAVA_HOME = $JavaHome
$env:HEAP = '-Xms512m -Xmx1024m'
# 正式测量使用 CLI；JTL 不存储请求体和鉴权头。
& (Join-Path $JMeterHome 'bin\jmeter.bat') -n -t (Join-Path $PSScriptRoot "$Plan.jmx") `
    "-Jaccounts=$root\tools\.runtime\accounts.csv" "-Jthreads=$Threads" "-Jtpm=$($Rps * 60)" `
    "-JloginPassword=$root\tools\.runtime\login-password.txt" `
    "-Jduration=$Seconds" "-Jramp=$Ramp" -Jjmeter.save.saveservice.output_format=csv `
    -Jjmeter.save.saveservice.print_field_names=true -Jjmeter.save.saveservice.assertion_results_failure_message=true `
    -l (Join-Path $result 'samples.jtl') -j (Join-Path $result 'jmeter.log') `
    -e -o (Join-Path $result 'report')
if ($LASTEXITCODE -ne 0) { throw 'JMeter 执行失败，请检查本轮日志' }
Write-Output "压测结果：$result"
