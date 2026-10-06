$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$directory = "$root\tools\results\optimization-cold-cache"
if (Test-Path $directory) { throw '禁止覆盖冷缓存验证证据' }
New-Item -ItemType Directory $directory | Out-Null
$account = Import-Csv "$root\tools\.runtime\accounts.csv" -Encoding utf8 | Select-Object -First 1
$credentials = Get-Content "$root\tools\.runtime\arthas\credentials.json" -Raw | ConvertFrom-Json
$auth = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("$($credentials.username):$($credentials.password)"))
$observer = Start-ThreadJob -ArgumentList $auth -ScriptBlock {
    param($auth)
    Invoke-RestMethod 'http://127.0.0.1:8563/api' -Method Post -Headers @{Authorization="Basic $auth"} `
        -ContentType 'application/json' -Body (@{
            action='exec';command='monitor com.me.service.Impl.AdminDashboardServiceImpl getDashboardData -c 1 -n 3'
        } | ConvertTo-Json)
}
$client = [Net.Http.HttpClient]::new()
try {
    Start-Sleep -Seconds 2
    # 删除专用 Redis 的单个看板键，然后同时发起 20 个真实只读请求。
    & docker exec takeme-loadtest-redis-20261004 redis-cli DEL admin:dashboard:data | Out-Null
    $client.DefaultRequestHeaders.Authorization = [Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer',$account.adminToken)
    $clock = [Diagnostics.Stopwatch]::StartNew()
    $requests = @(1..20 | ForEach-Object { $client.GetAsync('http://127.0.0.1:9081/api/admin/dashboard') })
    $successes = 0
    foreach ($request in $requests) {
        $response = $request.GetAwaiter().GetResult()
        $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
        if ($response.StatusCode -ne 200 -or $body.code -ne 200) { throw '冷缓存并发请求失败' }
        $successes++
        $response.Dispose()
    }
    $clock.Stop()
    @{请求数=$requests.Count;成功数=$successes;批次耗时毫秒=$clock.ElapsedMilliseconds} |
        ConvertTo-Json | Set-Content "$directory\http.json" -Encoding utf8
    $observation = Receive-Job $observer -Wait
    $observation | ConvertTo-Json -Depth 15 | Set-Content "$directory\arthas-monitor.json" -Encoding utf8
    if ($observation.state -ne 'SUCCEEDED') { throw '冷缓存观察失败' }
    & docker exec takeme-loadtest-redis-20261004 redis-cli TTL admin:dashboard:data |
        Set-Content "$directory\dashboard-ttl.txt" -Encoding utf8
    & docker exec takeme-loadtest-redis-20261004 redis-cli DEL service:available:2 | Out-Null
    $result = Invoke-RestMethod 'http://127.0.0.1:9081/api/user/service/list?type=2' `
        -Headers @{Authorization="Bearer $($account.userToken)"}
    if ($result.code -ne 200) { throw '目录冷缓存请求失败' }
    & docker exec takeme-loadtest-redis-20261004 redis-cli TTL service:available:2 |
        Set-Content "$directory\directory-ttl.txt" -Encoding utf8
    Write-Output "冷缓存验证成功，请求数=$successes"
} finally {
    $client.Dispose()
    Remove-Job $observer -Force
}
