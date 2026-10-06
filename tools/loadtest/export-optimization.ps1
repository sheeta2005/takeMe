$ErrorActionPreference = 'Stop'
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$rounds = @()
foreach ($phase in @('before','after')) {
    $prefix = "optimization-$phase-controlled"
    $manifest = Get-Content "$root\tools\results\$prefix-manifest.json" -Raw | ConvertFrom-Json
    if ($manifest.状态 -ne '完成') { throw "不能导出未完成轮次：$prefix" }
    $name = "$prefix-burst"
    $summary = Get-Content "$root\tools\results\$name\summary.json" -Raw | ConvertFrom-Json
    $samples = @(Import-Csv "$root\tools\results\$name\samples.jtl")
    $first = [double](($samples | Measure-Object timeStamp -Minimum).Minimum)
    $last = ($samples | ForEach-Object { [double]$_.timeStamp + [double]$_.elapsed } | Measure-Object -Maximum).Maximum
    $resources = @(Get-Content "$root\tools\results\$prefix-resources.ndjson" |
        ForEach-Object { $_ | ConvertFrom-Json } | Where-Object 阶段 -eq $name)
    $pauses = @(
        foreach ($line in Get-Content "$root\tools\results\$prefix-completed-gc.log") {
            if ($line -match '^\[([^\]]+)\].*GC\((\d+)\) Pause (.+) (\d+)M->(\d+)M\((\d+)M\) ([0-9.]+)ms$') {
                $time = [DateTimeOffset]::ParseExact($Matches[1],"yyyy-MM-dd'T'HH:mm:ss.fffzzz",[Globalization.CultureInfo]::InvariantCulture).ToUnixTimeMilliseconds()
                if ($time -ge $first -and $time -le $last) {
                    [pscustomobject]@{ms=[double]$Matches[7];kind=$Matches[3];afterMB=[int]$Matches[5]}
                }
            }
        }
    )
    $outbox = $resources | ForEach-Object { $_.数据库.pending } | Measure-Object -Maximum
    $oldest = $resources | ForEach-Object { $_.数据库.oldestSeconds } | Measure-Object -Maximum
    $rounds += [pscustomobject]@{
        阶段=$phase;清单=$manifest;接口统计=$summary
        原始采样SHA256=(Get-FileHash "$root\tools\results\$name\samples.jtl" -Algorithm SHA256).Hash
        原始采样="tools/results/$name/samples.jtl"
        资源采样SHA256=(Get-FileHash "$root\tools\results\$prefix-resources.ndjson" -Algorithm SHA256).Hash
        实测资源=[ordered]@{
            采样次数=$resources.Count
            平均进程CPU百分比=[math]::Round(($resources | Measure-Object 进程CPU百分比 -Average).Average,2)
            主机CPU峰值百分比=($resources | Measure-Object 主机CPU百分比 -Maximum).Maximum
            主机最少可用内存MB=($resources | Measure-Object 主机可用内存MB -Minimum).Minimum
            进程工作集峰值MB=($resources | Measure-Object 工作集MB -Maximum).Maximum
            堆已用峰值MB=($resources | Measure-Object 堆已用MB -Maximum).Maximum
            线程峰值=($resources | Measure-Object 线程数 -Maximum).Maximum
            Outbox待发送峰值=$outbox.Maximum
            Outbox最老等待秒=$oldest.Maximum
        }
        计分阶段GC=[ordered]@{
            Young次数=@($pauses | Where-Object kind -like 'Young*').Count
            Full次数=@($pauses | Where-Object kind -like 'Full*').Count
            总暂停毫秒=[math]::Round(($pauses | Measure-Object ms -Sum).Sum,3)
            最大暂停毫秒=($pauses | Measure-Object ms -Maximum).Maximum
            GC后堆最大MB=($pauses | Measure-Object afterMB -Maximum).Maximum
            暂停占比百分比=[math]::Round(($pauses | Measure-Object ms -Sum).Sum / ($last-$first)*100,3)
        }
    }
}
$counts = @()
foreach ($pair in @(@{phase='before';dir='optimization-before'},@{phase='after';dir='optimization-after-final'})) {
    foreach ($api in @('elder','volunteer')) {
        $path = "$root\tools\results\$($pair.dir)\$api-sql.json"
        $data = Get-Content $path -Raw | ConvertFrom-Json
        $counts += [pscustomobject]@{
            阶段=$pair.phase;接口=$api;包含鉴权的SQL数=@($data.body.results | Where-Object type -eq watch).Count
            证据="tools/results/$($pair.dir)/$api-sql.json";SHA256=(Get-FileHash $path -Algorithm SHA256).Hash
        }
    }
}
$gc = Get-Content "$root\tools\results\optimization-gc.json" -Raw | ConvertFrom-Json
$monitor = Get-Content "$root\tools\results\optimization-cold-cache\arthas-monitor.json" -Raw | ConvertFrom-Json
$loads = ($monitor.body.results | Where-Object type -eq monitor |
    ForEach-Object { $_.monitorDataList } |
    Where-Object className -eq 'com.me.service.Impl.AdminDashboardServiceImpl' |
    Measure-Object total -Sum).Sum
$report = [ordered]@{
    生成时间=(Get-Date).ToString('o')
    对照限制='相同硬件、JVM和社区参数，600条实际长连接，60秒预热+180秒突发；无Maven/Arthas与计分重叠；合成数据增长、缓存与主机其他进程状态不同，不作严格单变量因果归因；未重新进行优化后60分钟持续测试'
    计分轮次=$rounds;Arthas_SQL次数=$counts
    原正式持续测试GC=$gc
    冷看板并发验证=[ordered]@{
        HTTP=(Get-Content "$root\tools\results\optimization-cold-cache\http.json" -Raw | ConvertFrom-Json)
        业务回源次数=$loads
        看板剩余TTL秒=[int](Get-Content "$root\tools\results\optimization-cold-cache\dashboard-ttl.txt")
        目录剩余TTL秒=[int](Get-Content "$root\tools\results\optimization-cold-cache\directory-ttl.txt")
        限制='诊断阶段含Arthas增强，批次耗时不是无增强的接口P95；TTL为读取时的剩余时间'
    }
    Redis=[ordered]@{
        压测实例='127.0.0.1:6381';maxmemory字节=67108864;策略='noeviction'
        开发共享实例='127.0.0.1:6379';开发maxmemory字节=0;开发策略='noeviction'
        说明='仅改变专用压测实例运行时上限；新建容器启动参数及部署参考配置已补齐，不声称旧容器CONFIG SET持久化'
    }
    收尾一致性证据=[ordered]@{
        文件='tools/results/optimization-final-consistency.txt'
        SHA256=(Get-FileHash "$root\tools\results\optimization-final-consistency.txt" -Algorithm SHA256).Hash
        说明='该时点全部Outbox已发送，无孤立服务项、无重复提交、无非ISO预约；后台补偿后续可能新增通知，须区分收尾时点与压测终点'
    }
    回归=[ordered]@{全量总数=111;全量执行成功=90;既有演示跳过=21;失败=0;最终索引专项执行成功=73}
}
$report | ConvertTo-Json -Depth 20 | Set-Content "$root\docs\sql-gc-redis-measurements.json" -Encoding utf8
$rounds | ForEach-Object {
    $_.接口统计 | Where-Object 接口 -eq '总计'
    $_.实测资源 | ConvertTo-Json
    $_.计分阶段GC | ConvertTo-Json
}
