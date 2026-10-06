param(
    [Parameter(Mandatory = $true)][ValidateSet('before','after')][string]$Phase,
    [ValidatePattern('^[a-z0-9_-]+$')][string]$Name
)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$directory = Join-Path $root ("tools\results\" + $(if ($Name) { $Name } else { "optimization-$Phase" }))
New-Item -ItemType Directory -Path $directory -Force | Out-Null
$state = (Get-Content "$root\tools\.runtime\environment.json" -Raw | ConvertFrom-Json).environment
$env:MYSQL_PWD = $state.TAKEME_TEST_DB_PASSWORD
$mysql = 'D:\tool\MySQL\MySQL Server 8.0\bin\mysql.exe'
$start = (Get-Date).AddHours(1)
$end = (Get-Date).AddHours(4)
$condition = if ($Phase -eq 'before') {
    "CONCAT(i.service_date,' ',i.service_time) BETWEEN '$($start.ToString('yyyy-MM-dd HH:mm'))' AND '$($end.ToString('yyyy-MM-dd HH:mm'))'"
} else {
    "i.service_date BETWEEN '$($start.ToString('yyyy-MM-dd'))' AND '$($end.ToString('yyyy-MM-dd'))' AND (i.service_date > '$($start.ToString('yyyy-MM-dd'))' OR i.service_time >= '$($start.ToString('HH:mm'))') AND (i.service_date < '$($end.ToString('yyyy-MM-dd'))' OR i.service_time <= '$($end.ToString('HH:mm'))')"
}
$payment = if ($Phase -eq 'before') {
    "SELECT COALESCE(SUM(CASE WHEN payment_method='mock' AND payment_status IN (1,2) AND payment_time>=CURDATE() AND payment_time<CURDATE()+INTERVAL 1 DAY THEN amount WHEN payment_method='mock_refund' AND payment_status=2 AND refund_time>=CURDATE() AND refund_time<CURDATE()+INTERVAL 1 DAY THEN -amount ELSE 0 END),0) FROM payment_transaction WHERE (payment_time>=CURDATE() AND payment_time<CURDATE()+INTERVAL 1 DAY) OR (refund_time>=CURDATE() AND refund_time<CURDATE()+INTERVAL 1 DAY)"
} else {
    "SELECT COALESCE((SELECT SUM(amount) FROM payment_transaction WHERE payment_method='mock' AND payment_status IN (1,2) AND payment_time>=CURDATE() AND payment_time<CURDATE()+INTERVAL 1 DAY),0)-COALESCE((SELECT SUM(amount) FROM payment_transaction WHERE payment_method='mock_refund' AND payment_status=2 AND refund_time>=CURDATE() AND refund_time<CURDATE()+INTERVAL 1 DAY),0)"
}
# 只在合成压测库执行 SELECT 和 EXPLAIN ANALYZE，不接触真实业务记录。
$sql = @"
SELECT DATABASE(),NOW(),(SELECT COUNT(*) FROM ``order``) orders,(SELECT COUNT(*) FROM order_item) items,(SELECT COUNT(*) FROM payment_transaction) payments;
SHOW INDEX FROM ``order``;
SHOW INDEX FROM order_item;
SHOW INDEX FROM payment_transaction;
EXPLAIN ANALYZE SELECT * FROM ``order`` WHERE user_id=1 ORDER BY create_time DESC,id DESC LIMIT 10;
EXPLAIN ANALYZE SELECT * FROM ``order`` WHERE user_id=1 ORDER BY create_time DESC,id DESC LIMIT 1000,10;
EXPLAIN ANALYZE SELECT i.* FROM order_item i WHERE i.volunteer_id IS NULL AND i.item_status=0 AND $condition AND i.order_id IN (SELECT id FROM ``order`` WHERE status IN (0,1,2)) ORDER BY i.create_time DESC,i.id DESC LIMIT 10;
EXPLAIN ANALYZE SELECT COUNT(*) FROM order_item i WHERE i.volunteer_id IS NULL AND i.item_status=0 AND $condition AND i.order_id IN (SELECT id FROM ``order`` WHERE status IN (0,1,2));
EXPLAIN ANALYZE $payment;
"@
& $mysql '-h' '127.0.0.1' '-u' 'root' '-D' 'takeme_loadtest_20261004' '--default-character-set=utf8mb4' '-e' $sql |
    Set-Content "$directory\sql-plans.txt" -Encoding utf8
if ($LASTEXITCODE -ne 0) { throw '执行计划采集失败' }
& docker exec takeme-loadtest-redis-20261004 redis-cli CONFIG GET maxmemory maxmemory-policy hz active-expire-effort |
    Set-Content "$directory\redis-config.txt" -Encoding utf8
& docker exec takeme-loadtest-redis-20261004 redis-cli INFO memory |
    Set-Content "$directory\redis-memory.txt" -Encoding utf8
& docker exec takeme-loadtest-redis-20261004 redis-cli INFO stats |
    Set-Content "$directory\redis-stats.txt" -Encoding utf8
$account = Import-Csv "$root\tools\.runtime\accounts.csv" -Encoding utf8 | Select-Object -First 1
$credentials = Get-Content "$root\tools\.runtime\arthas\credentials.json" -Raw | ConvertFrom-Json
$auth = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("$($credentials.username):$($credentials.password)"))
function Observe($Name, $Command, $Path, $Token) {
    # 诊断请求与计分压测分开，保存 SQL 模板，不保存 token 或 SQL 实参。
    $job = Start-ThreadJob -ArgumentList $Command, $auth -ScriptBlock {
        param($command, $auth)
        Invoke-RestMethod 'http://127.0.0.1:8563/api' -Method Post -Headers @{Authorization="Basic $auth"} `
            -ContentType 'application/json' -Body (@{action='exec';command=$command} | ConvertTo-Json)
    }
    try {
        Start-Sleep -Seconds 2
        $response = Invoke-RestMethod "http://127.0.0.1:9081$Path" -Headers @{Authorization="Bearer $Token"}
        if ($response.code -ne 200) { throw "诊断请求失败：$Path" }
        $result = Receive-Job $job -Wait
        $result | ConvertTo-Json -Depth 30 | Set-Content "$directory\$Name.json" -Encoding utf8
        if ($result.state -ne 'SUCCEEDED' -or @($result.body.results | Where-Object statusCode -eq -1).Count) {
            throw "Arthas 诊断失败：$Name"
        }
    } finally {
        Remove-Job $job -Force
    }
}
$userQueries = if ($Phase -eq 'before') { 13 } else { 4 }
$volunteerQueries = if ($Phase -eq 'before') { 23 } else { 5 }
$watch = "watch org.apache.ibatis.executor.SimpleExecutor doQuery '{params[0].getId(),params[4].getSql(),#cost}' '@java.lang.Thread@currentThread().getName().startsWith(`"http-nio`")' -x 2 -n "
Observe 'elder-sql' "$watch$userQueries" '/api/user/order/list?pageNum=1&pageSize=10' $account.userToken
Observe 'volunteer-sql' "$watch$volunteerQueries" '/api/volunteer/order/available?pageNum=1&pageSize=10' $account.volunteerToken
Observe 'elder-trace' 'trace com.me.service.Impl.OrderServiceImpl getMyOrderList -n 1' '/api/user/order/list?pageNum=1&pageSize=10' $account.userToken
Observe 'volunteer-trace' 'trace com.me.service.Impl.OrderServiceImpl getAvailableOrderList -n 1' '/api/volunteer/order/available?pageNum=1&pageSize=10' $account.volunteerToken
# 仅删除隔离 Redis 的看板键，复现真实冷缓存回源，不清空整个 Redis。
& docker exec takeme-loadtest-redis-20261004 redis-cli DEL admin:dashboard:data | Out-Null
Observe 'dashboard-trace' 'trace com.me.service.Impl.AdminDashboardServiceImpl getDashboardData -n 1' '/api/admin/dashboard' $account.adminToken
Write-Output "诊断证据已保存：$directory"
