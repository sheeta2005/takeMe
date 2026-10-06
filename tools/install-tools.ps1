param([string]$InstallRoot = 'D:\tool\developset\takeme-tools')
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Path $InstallRoot -Force | Out-Null

# 官方 HTTPS 下载并校验摘要；工具二进制放在项目外。
$jmeterVersion = '5.6.3'
$jmeterZip = Join-Path $InstallRoot "apache-jmeter-$jmeterVersion.zip"
$jmeterUrl = "https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-$jmeterVersion.zip"
if (-not (Test-Path -LiteralPath $jmeterZip)) {
    Invoke-WebRequest $jmeterUrl -OutFile $jmeterZip
}
$checksum = (Invoke-WebRequest "$jmeterUrl.sha512").Content
$expected = [regex]::Match($checksum, '[0-9a-fA-F]{128}').Value
if (-not $expected -or (Get-FileHash $jmeterZip -Algorithm SHA512).Hash -ne $expected) {
    throw 'JMeter SHA512 校验失败'
}
$jmeterHome = Join-Path $InstallRoot "apache-jmeter-$jmeterVersion"
if (-not (Test-Path -LiteralPath $jmeterHome)) {
    Expand-Archive -LiteralPath $jmeterZip -DestinationPath $InstallRoot
}

# 从 Maven Central 元数据确认可用发布版本，不猜测 Arthas 的最新版本。
$metadata = Invoke-RestMethod 'https://repo.maven.apache.org/maven2/com/taobao/arthas/arthas-packaging/maven-metadata.xml'
$arthasVersion = $metadata.metadata.versioning.release
$arthasZip = Join-Path $InstallRoot "arthas-$arthasVersion.zip"
$arthasUrl = "https://repo.maven.apache.org/maven2/com/taobao/arthas/arthas-packaging/$arthasVersion/arthas-packaging-$arthasVersion-bin.zip"
if (-not (Test-Path -LiteralPath $arthasZip)) {
    Invoke-WebRequest $arthasUrl -OutFile $arthasZip
}
$arthasHash = (Invoke-WebRequest "$arthasUrl.sha1").Content.Trim()
if ((Get-FileHash $arthasZip -Algorithm SHA1).Hash -ne $arthasHash) {
    throw 'Arthas 下载摘要校验失败'
}
$arthasHome = Join-Path $InstallRoot "arthas-$arthasVersion"
if (-not (Test-Path -LiteralPath $arthasHome)) {
    Expand-Archive -LiteralPath $arthasZip -DestinationPath $arthasHome
}
[pscustomobject]@{ JMeterHome = $jmeterHome; ArthasHome = $arthasHome; ArthasVersion = $arthasVersion }
