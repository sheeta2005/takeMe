param(
    [Parameter(Mandatory = $true)][string]$Username,
    [Parameter(Mandatory = $true)][string]$Password,
    [string]$VirtualHost = '/'
)
$ErrorActionPreference = 'Stop'
$baseUri = 'http://localhost:15672/api'
$vhostPath = [uri]::EscapeDataString($VirtualHost)
$credential = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${Username}:$Password"))
$headers = @{ Authorization = "Basic $credential" }
$failedExchange = 'takeme.consumer.failed.exchange'
# 通知保留近期事件并将淘汰消息归档；延时业务和归档入口拒绝新发布，避免主动淘汰已有任务。
$policies = @(
    @{ name='takeme-notification-capacity'; pattern='^(notification\.user\.queue|notification\.volunteer\.queue|notification\.admin\.queue)$'; definition=@{'max-length'=30000; overflow='drop-head'; 'dead-letter-exchange'=$failedExchange; 'dead-letter-routing-key'='failed'} },
    @{ name='takeme-approval-capacity'; pattern='^(approval\.admin\.queue|approval\.result\.volunteer\.queue)$'; definition=@{'max-length'=2000; overflow='drop-head'; 'dead-letter-exchange'=$failedExchange; 'dead-letter-routing-key'='failed'} },
    @{ name='takeme-broadcast-capacity'; pattern='^notification\.broadcast\.queue$'; definition=@{'max-length'=100; overflow='drop-head'; 'dead-letter-exchange'=$failedExchange; 'dead-letter-routing-key'='failed'} },
    @{ name='takeme-order-delay-capacity'; pattern='^order\.create\.delay\.queue$'; definition=@{'max-length'=5000; overflow='reject-publish'} },
    @{ name='takeme-order-timeout-capacity'; pattern='^order\.cancel\.dlx\.queue$'; definition=@{'max-length'=5000; overflow='reject-publish'} },
    @{ name='takeme-volunteer-delay-capacity'; pattern='^volunteer\.start\.timeout\.delay\.queue$'; definition=@{'max-length'=1000; overflow='reject-publish'} },
    @{ name='takeme-volunteer-timeout-capacity'; pattern='^volunteer\.start\.timeout\.queue$'; definition=@{'max-length'=1000; overflow='reject-publish'} },
    @{ name='takeme-consumer-failed-capacity'; pattern='^takeme\.consumer\.failed\.queue$'; definition=@{'max-length'=5000; overflow='reject-publish'} }
)
foreach ($policy in $policies) {
    $body = @{
        pattern = $policy.pattern
        definition = $policy.definition
        'apply-to' = 'queues'
        priority = 50
    } | ConvertTo-Json -Depth 5
    $uri = "$baseUri/policies/$vhostPath/$($policy.name)"
    Invoke-RestMethod -Uri $uri -Method Put -Headers $headers -ContentType 'application/json' -Body $body | Out-Null
    Write-Host "已应用队列策略: $($policy.name)"
}
