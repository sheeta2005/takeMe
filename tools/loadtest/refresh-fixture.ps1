param([switch]$Renew)
$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$state = (Get-Content (Join-Path $root 'tools\.runtime\environment.json') -Raw | ConvertFrom-Json).environment
$env:MYSQL_PWD = $state.TAKEME_TEST_DB_PASSWORD
$date = (Get-Date).AddHours(3).ToString('yyyy-MM-dd')
$time = (Get-Date).AddHours(3).ToString('HH:mm')
$rangePath = Join-Path $root 'tools\.runtime\candidate-range.json'
if ($Renew) {
    # 隔夜取消的合成候选不复活，只新增相同规模的测试占位数据，保留旧测量现场。
    $prefix = 'LOADC_' + [guid]::NewGuid().ToString('N') + '_'
    $sql = @"
START TRANSACTION;
INSERT INTO ``order`` (order_no,user_id,total_price,service_date,service_time,address,status,create_time)
SELECT CONCAT('$prefix',id-100000),user_id,200,'$date','$time','合成候选地址',0,NOW()
FROM ``order`` WHERE id BETWEEN 100001 AND 101000 ORDER BY id;
INSERT INTO order_item (order_id,service_id,service_name,service_price,quantity,item_price,service_type,service_date,service_time,address,item_status,create_time)
SELECT o.id,3,'合成助餐候选',100,1,100,2,'$date','$time','合成候选地址',0,NOW()
FROM ``order`` o JOIN (SELECT 1 n UNION ALL SELECT 2) copies
WHERE o.order_no LIKE '$prefix%';
COMMIT;
SELECT JSON_OBJECT('orderStart',MIN(o.id),'orderEnd',MAX(o.id),'itemStart',MIN(i.id),'itemEnd',MAX(i.id),'orderCount',COUNT(DISTINCT o.id))
FROM ``order`` o JOIN order_item i ON i.order_id=o.id WHERE o.order_no LIKE '$prefix%';
"@
    $json = & 'D:\tool\MySQL\MySQL Server 8.0\bin\mysql.exe' "-u$($state.TAKEME_TEST_DB_USER)" `
        --default-character-set=utf8mb4 --database=takeme_loadtest_20261004 --batch --skip-column-names "--execute=$sql"
    if ($LASTEXITCODE -ne 0) { throw '新增合成候选失败' }
    $range = $json | ConvertFrom-Json
    if ($range.orderCount -ne 1000 -or $range.orderEnd - $range.orderStart -ne 999 -or
        $range.itemEnd - $range.itemStart -ne 1999) { throw '新增候选数量或主键范围异常' }
    $range | ConvertTo-Json | Set-Content -LiteralPath $rangePath -Encoding utf8
}
$range = if (Test-Path -LiteralPath $rangePath) {
    Get-Content -LiteralPath $rangePath -Raw | ConvertFrom-Json
} else { [pscustomobject]@{orderStart=100001;orderEnd=101000;itemStart=200001;itemEnd=202000} }
# 仅刷新登记过的合成候选；不改历史状态、用户测试交易或模拟支付退款流水。
$sql = "UPDATE ``order`` SET service_date='$date',service_time='$time' WHERE id BETWEEN $($range.orderStart) AND $($range.orderEnd) AND status=0; UPDATE order_item SET service_date='$date',service_time='$time' WHERE id BETWEEN $($range.itemStart) AND $($range.itemEnd) AND item_status=0 AND volunteer_id IS NULL; SELECT COUNT(*) FROM ``order`` WHERE id BETWEEN $($range.orderStart) AND $($range.orderEnd) AND status=0;"
$count = & 'D:\tool\MySQL\MySQL Server 8.0\bin\mysql.exe' "-u$($state.TAKEME_TEST_DB_USER)" `
    --database=takeme_loadtest_20261004 --batch --skip-column-names "--execute=$sql"
if ($LASTEXITCODE -ne 0 -or $count -ne '1000') { throw '候选不足 1000，请在开测前用 -Renew 新增隔离合成候选' }
