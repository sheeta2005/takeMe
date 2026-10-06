$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$account = Import-Csv (Join-Path $root 'tools\.runtime\accounts.csv') -Encoding utf8 | Select-Object -First 1
$userHeader = @{Authorization="Bearer $($account.userToken)"}
$volunteerHeader = @{Authorization="Bearer $($account.volunteerToken)"}
$adminHeader = @{Authorization="Bearer $($account.adminToken)"}
function Call-Api($Path, $Headers, $Body = $null) {
    $args = @{Uri="http://127.0.0.1:9081$Path";Headers=$Headers;Method='Get'}
    if ($null -ne $Body) {
        $args.Method = 'Post'; $args.ContentType = 'application/json'
        $args.Body = ($Body | ConvertTo-Json -Depth 8 -Compress)
    }
    $result = Invoke-RestMethod @args
    if ($result.code -ne 200) { throw "业务失败：$Path，$($result.msg)" }
    return $result.data
}
$appointment = (Get-Date).AddHours(3)
$body = @{
    order = @{requestId=[guid]::NewGuid().ToString()}
    items = @(@{serviceId=3;quantity=1;serviceDate=$appointment.ToString('yyyy-MM-dd')
        serviceTime=$appointment.ToString('HH:mm');address='合成链路验证地址'})
}
$order = Call-Api '/api/user/order/create' $userHeader $body
$retry = Call-Api '/api/user/order/create' $userHeader $body
if ($order.id -ne $retry.id) { throw '重试产生重复订单' }
$null = Call-Api '/api/user/payment/mock' $userHeader @{orderId=$order.id}
$itemId = $order.items[0].id
$null = Call-Api "/api/volunteer/order/confirm?orderItemId=$itemId" $volunteerHeader @{}
$claimed = Call-Api "/api/user/order/detail?orderId=$($order.id)" $userHeader
if ($claimed.status -ne 1 -or $claimed.items[0].volunteerId -ne 1) { throw '接单状态异常' }
$null = Call-Api "/api/user/order/cancel?orderId=$($order.id)" $userHeader @{}
$cancelled = Call-Api "/api/user/order/detail?orderId=$($order.id)" $userHeader
if ($cancelled.status -ne 5 -or $null -ne $cancelled.items[0].volunteerId) { throw '取消未释放志愿者' }
Write-Output '真实 HTTP 建单、重试、支付、接单、取消链路通过'

$broadcast = @{receiverType=2;type=0;title='压测广播';content='仅用于合成账号的可靠群发验证'}
$null = Call-Api '/api/admin/message/send' $adminHeader $broadcast
Write-Output '群发已登记；后台等待落库，使用数据库统计核对接收数量'
