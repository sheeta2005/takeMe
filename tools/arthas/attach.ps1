param(
    [Parameter(Mandatory = $true)][int]$TargetProcessId,
    [string]$JavaHome = 'D:\tool\developset\Java\java21.0.7',
    [string]$ArthasHome = 'D:\tool\developset\takeme-tools\arthas-4.3.5'
)
$ErrorActionPreference = 'Stop'
$runtime = Join-Path (Split-Path $PSScriptRoot -Parent) '.runtime\arthas'
New-Item -ItemType Directory -Path $runtime -Force | Out-Null
foreach ($name in @('arthas-agent.jar','arthas-client.jar','arthas-core.jar','arthas-spy.jar')) {
    Copy-Item -LiteralPath (Join-Path $ArthasHome $name) -Destination $runtime
}
$credentials = Join-Path $runtime 'credentials.json'
if (Test-Path -LiteralPath $credentials) {
    $password = (Get-Content -LiteralPath $credentials -Raw | ConvertFrom-Json).password
} else {
    $password = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
    @{ username = 'takeme'; password = $password } | ConvertTo-Json | Set-Content -LiteralPath $credentials -Encoding utf8
}
$template = Get-Content (Join-Path $PSScriptRoot 'arthas.properties.template') -Raw
$template.Replace('${PASSWORD}', $password) | Set-Content (Join-Path $runtime 'arthas.properties') -Encoding ascii
# 本地凭据不进入 Git，也不放到 JVM 命令行中。
& (Join-Path $JavaHome 'bin\java.exe') -jar (Join-Path $ArthasHome 'arthas-boot.jar') `
    --arthas-home $runtime --target-ip 127.0.0.1 --attach-only $TargetProcessId
if ($LASTEXITCODE -ne 0) { throw 'Arthas 附加失败' }
Write-Output "Arthas 已附加，凭据文件：$credentials"
