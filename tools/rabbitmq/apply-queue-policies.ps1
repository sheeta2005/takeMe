param(
    [Parameter(Mandatory = $true)][string]$Username,
    [Parameter(Mandatory = $true)][string]$Password,
    [string]$VirtualHost = '/',
    [switch]$VerifyOnly
)
$ErrorActionPreference = 'Stop'
$baseUri = 'http://localhost:15672/api'
$vhostPath = [uri]::EscapeDataString($VirtualHost)
$credential = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("${Username}:$Password"))
$headers = @{ Authorization = "Basic $credential" }
# 所有业务和失败入口均拒绝新发布，由发布确认及原消息重试承接，避免淘汰旧消息。
$policies = @(
    @{ name='takeme-notification-capacity'; queues=@('notification.user.queue', 'notification.volunteer.queue', 'notification.admin.queue'); definition=@{'max-length'=30000; 'max-length-bytes'=64MB; overflow='reject-publish'} },
    @{ name='takeme-approval-capacity'; queues=@('approval.admin.queue', 'approval.result.volunteer.queue'); definition=@{'max-length'=2000; 'max-length-bytes'=16MB; overflow='reject-publish'} },
    @{ name='takeme-broadcast-capacity'; queues=@('notification.broadcast.queue'); definition=@{'max-length'=100; 'max-length-bytes'=8MB; overflow='reject-publish'} },
    @{ name='takeme-order-delay-capacity'; queues=@('order.create.delay.queue'); definition=@{'max-length'=5000; 'max-length-bytes'=32MB; overflow='reject-publish'} },
    @{ name='takeme-order-timeout-capacity'; queues=@('order.cancel.dlx.queue'); definition=@{'max-length'=5000; 'max-length-bytes'=32MB; overflow='reject-publish'} },
    @{ name='takeme-volunteer-delay-capacity'; queues=@('volunteer.start.timeout.delay.queue'); definition=@{'max-length'=1000; 'max-length-bytes'=8MB; overflow='reject-publish'} },
    @{ name='takeme-volunteer-timeout-capacity'; queues=@('volunteer.start.timeout.queue'); definition=@{'max-length'=1000; 'max-length-bytes'=8MB; overflow='reject-publish'} },
    @{ name='takeme-consumer-failed-capacity'; queues=@('takeme.consumer.failed.queue'); definition=@{'max-length'=5000; 'max-length-bytes'=64MB; overflow='reject-publish'} }
)

# 所有队列通过预检后再写策略，防止下调容量时影响已有积压。
$queues = Invoke-RestMethod -Uri "$baseUri/queues/$vhostPath" -Headers $headers -TimeoutSec 15
foreach ($policy in $policies) {
    foreach ($queueName in $policy.queues) {
        $queue = $queues | Where-Object { $_.name -ceq $queueName } | Select-Object -First 1
        if ($null -eq $queue) { throw "队列尚未声明，请先启动应用: $queueName" }
        if (-not $VerifyOnly) {
            # 管理统计不可用时不推断为零，等待统计就绪再执行。
            if ($null -eq $queue.messages_ready -or $null -eq $queue.message_bytes_ready) {
                throw "队列容量统计尚未就绪，请稍后重试: $queueName"
            }
            if ($queue.messages_ready -gt $policy.definition['max-length'] -or
                $queue.message_bytes_ready -gt $policy.definition['max-length-bytes']) {
                throw "现有积压超过拟设容量，请先排空或调整容量: $queueName"
            }
        }
    }
}

if (-not $VerifyOnly) {
    foreach ($policy in $policies) {
        $escapedNames = @($policy.queues | ForEach-Object { [regex]::Escape($_) })
        $body = @{
            pattern = '^(' + ($escapedNames -join '|') + ')$'
            definition = $policy.definition
            'apply-to' = 'queues'
            priority = 50
        } | ConvertTo-Json -Depth 5
        $uri = "$baseUri/policies/$vhostPath/$($policy.name)"
        Invoke-RestMethod -Uri $uri -Method Put -Headers $headers -ContentType 'application/json' -Body $body -TimeoutSec 15 | Out-Null
        Write-Host "已应用队列策略: $($policy.name)"
    }
}

# 校验队列的实际生效值，捕获策略优先级冲突、声明参数覆盖及漏配。
$verificationDeadline = [DateTime]::UtcNow.AddSeconds(10)
foreach ($policy in $policies) {
    foreach ($queueName in $policy.queues) {
        $queuePath = [uri]::EscapeDataString($queueName)
        do {
            $queue = Invoke-RestMethod -Uri "$baseUri/queues/$vhostPath/$queuePath" -Headers $headers -TimeoutSec 15
            $effective = $queue.effective_policy_definition
            $matchesPolicy = $queue.policy -ceq $policy.name -and $null -ne $effective
            foreach ($key in $policy.definition.Keys) {
                if ($effective.$key -ne $policy.definition[$key]) { $matchesPolicy = $false }
            }
            # 写入后的管理统计异步刷新，全轮最多等待 10 秒；只读检查发现差异直接失败。
            if ($matchesPolicy -or $VerifyOnly -or [DateTime]::UtcNow -ge $verificationDeadline) { break }
            Start-Sleep -Milliseconds 250
        } while ($true)
        if ($queue.policy -cne $policy.name -or $null -eq $effective) {
            throw "队列未命中预期容量策略: $queueName"
        }
        foreach ($key in $policy.definition.Keys) {
            if ($effective.$key -ne $policy.definition[$key]) {
                throw "队列生效策略与预期不符: $queueName, $key"
            }
            # 数值上限同时受声明参数约束，声明中的 overflow 也可能覆盖普通策略。
            $argument = "x-$key"
            if ($null -ne $queue.arguments.$argument -and $queue.arguments.$argument -ne $policy.definition[$key]) {
                throw "队列声明参数与容量策略冲突: $queueName, $argument"
            }
        }
        if ($null -ne $effective.'dead-letter-exchange' -and $queueName -eq 'takeme.consumer.failed.queue') {
            throw "失败入口不应再配置死信转发，请检查其他生效策略: $queueName"
        }
        Write-Host "队列容量校验通过: $queueName"
    }
}
