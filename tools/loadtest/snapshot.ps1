param([Parameter(Mandatory = $true)][string]$Name)
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$state = (Get-Content (Join-Path $root 'tools\.runtime\environment.json') -Raw | ConvertFrom-Json).environment
$record = Get-Content (Join-Path $root 'tools\.runtime\app-process.json') -Raw | ConvertFrom-Json
$process = Get-Process -Id $record.pid
$auth = "$($state.TAKEME_TEST_MQ_USER):$($state.TAKEME_TEST_MQ_PASSWORD)"
$headers = @{Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($auth))}
$queues = Invoke-RestMethod 'http://localhost:15672/api/queues/takeme_loadtest_20261004' -Headers $headers
$snapshot = @{
    时间 = (Get-Date).ToString('o'); 应用PID = $record.pid; 模式 = $record.mode
    工作集MB = [math]::Round($process.WorkingSet64 / 1MB, 2)
    CPU累计秒 = $process.CPU; 线程数 = $process.Threads.Count
    队列 = @($queues | Select-Object name,messages,messages_ready,messages_unacknowledged,consumers)
}
$snapshot | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $root "tools\results\$Name.json") -Encoding utf8
Copy-Item (Join-Path $root 'tools\.runtime\gc.log') (Join-Path $root "tools\results\$Name-gc.log")
$snapshot
$env:MYSQL_PWD = $state.TAKEME_TEST_DB_PASSWORD
& 'D:\tool\MySQL\MySQL Server 8.0\bin\mysql.exe' "-u$($state.TAKEME_TEST_DB_USER)" `
    --database=takeme_loadtest_20261004 --execute='SELECT status, COUNT(*) AS events FROM mq_outbox GROUP BY status; SELECT COUNT(*) AS dangling_claims FROM order_item i LEFT JOIN `order` o ON o.id=i.order_id WHERE (i.item_status IN (1,2) AND i.volunteer_id IS NULL) OR (o.status=5 AND i.item_status<>5); SELECT COUNT(*) AS duplicate_requests FROM (SELECT user_id, request_id FROM `order` WHERE request_id IS NOT NULL GROUP BY user_id,request_id HAVING COUNT(*)>1) d; SHOW GLOBAL STATUS WHERE Variable_name IN ("Threads_connected","Threads_running","Max_used_connections","Innodb_row_lock_waits","Innodb_row_lock_time","Innodb_buffer_pool_reads","Innodb_buffer_pool_read_requests");' |
    Set-Content (Join-Path $root "tools\results\$Name-db.txt") -Encoding utf8
