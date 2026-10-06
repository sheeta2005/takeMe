# 本机验证、JMeter 与 Arthas

## 隔离范围

压测后端只监听 `127.0.0.1:9081`，数据库固定为 `takeme_loadtest_20261004`，RabbitMQ 使用同名独立虚拟主机，Redis 使用专用容器 `takeme-loadtest-redis-20261004` 的本机端口 6381。不向原开发库写入压测账号或订单，不清空原队列。

生成 2500 名老人、250 名志愿者、25 名管理员、101000 个订单及 202000 个服务项。历史数据只用于合成测试，不复制真实账号、地址或历史订单。每轮开始仅刷新固定合成候选单的预约时间，保持 1000 个候选订单，不会修改历史数据或已经生成的支付退款记录。

若隔夜导致候选正常超时，开测前用 `loadtest/refresh-fixture.ps1 -Renew` 新增 1000 个候选、2000 个服务项，并登记新主键范围。旧候选不复活、不删除；新增候选同样只是列表容量占位，未逐笔生成支付流水。新的实际订单规模写入本轮清单，不再声称仍是初始数量。

`.runtime/` 保存本机凭据、签名测试 Token、启动进程信息、GC 日志；`results/` 保存测量产物。两个目录都被本目录的 `.gitignore` 忽略，不提交 Git。现有本地密钥文件不需要改动。

## 安装与启动

`install-tools.ps1` 下载 Apache JMeter 5.6.3 和 Maven Central 元数据指定的 Arthas 发布包，校验摘要后解压到项目外。此次本机安装路径为 `D:\tool\developset\takeme-tools`，Arthas 安装版本为 4.3.5。

本机 PATH 默认 Java 8；项目及压测明确使用 `D:\tool\developset\Java\java21.0.7`，不改变全局 PATH。

先构建 `takeMe/server/target/server-1.0-SNAPSHOT.jar`，再通过以下脚本操作：

- `loadtest/setup.ps1`：输入本机数据库及 MQ 凭据，创建隔离资源和合成数据。已存在时拒绝覆盖；`-Resume` 只用于第一次初始化失败后继续，不删除已存在的测试库。
- `loadtest/start-app.ps1 -Mode baseline`：启动默认参数隔离应用。Windows JDK 的 Unix Domain 临时路径显式指向本项目运行时目录，避免本机短路径临时目录导致 NIO 启动失败。
- `loadtest/start-app.ps1 -Mode community`：直接验证打包后的社区配置，不用命令行覆盖 Tomcat、数据库池或 MQ 参数。
- `loadtest/stop-app.ps1`：核对记录的 PID、Java 命令行及隔离配置后停止，只处理本脚本启动的应用。
- `loadtest/run.ps1 -Name <新名称> -Threads 300 -Rps 180 -Seconds 600`：CLI 运行压测，已有结果目录不会覆盖。
- `loadtest/summarize.ps1 -Name <名称>`：计算总量和各接口的 RPS、平均值、P95、P99、业务错误率。
- `loadtest/snapshot.ps1 -Name <名称>`：保存应用内存、累计 CPU、线程数、队列积压、GC 和数据库一致性检查。
- `loadtest/workflow-check.ps1`：真实 HTTP 验证建单、重试、模拟支付、志愿者接单、取消及群发。
- `loadtest/notification-check.mjs`：以真实连接验证订单取消提醒到达时，通知已经持久化。

## 流量模型

这是容量验证假设，不是观测到的社区真实流量：

| 场景 | 虚拟用户 | 目标请求速率 | 时长 |
| --- | ---: | ---: | ---: |
| 常规 | 60 | 30 RPS | 10 分钟 |
| 高峰 | 300 | 180 RPS | 10 分钟，最终参数重复 3 轮 |
| 突发 | 600 | 360 RPS | 10 分钟 |
| 持续运行 | 同一 JVM 连续承受上述高低峰 | 分段变化 | 至少 60 分钟 |

主业务选择比例为服务目录 30%、老人订单列表 25%、详情 10%、消息列表 15%、志愿者候选服务 10%、管理员看板 5%、创建订单 5%。创建订单后还有重复提交校验、模拟支付、取消退款、取消后状态校验，所以最终 HTTP 采样占比不同于上述主操作比例。

采用 0.5 至 1.5 秒浏览思考时间，所有线程共享目标吞吐限制，连接复用。预签名 Token 只用于已登录业务，不包含 BCrypt 密码计算；登录另用 `login.jmx` 测试，保持 BCrypt 强度 12 和正常账号/IP 限流。

对每个请求检查 JSON 业务码，建单重试必须返回相同 ID，取消后必须为状态 5。并发抢单竞争另由真实数据库重复回归及 HTTP 链路验证，不将大量“状态不允许”错误当作成功压测。

完整流转中的开始服务和完成需要真实预约时间窗口，因此不在容量测试中为追求 TPS 绕过时间限制。该容量结果不是完整人工服务的每秒处理能力。

## 对比顺序

`loadtest/explore.ps1` 完成同一构建产物的预热、高峰、突发，再逐组比较 Tomcat、数据库池、MQ 消费线程及预取参数。短探测只筛选候选，不替代正式复测。

`loadtest/soak.ps1 -Mode community -Prefix <新轮次>` 使用选定参数，先预热，再在同一 JVM 连续运行六段各 10 分钟负载：常规、高峰、突发、高峰、高峰、常规。三轮高峰的 JTL 分开保存。这是混合高低峰的 60 分钟持续运行，不是固定高峰的额外 60 分钟测试。轮次已存在时拒绝覆盖。

最终持续运行同时保持 600 条真实 WebSocket 连接，使用前端已有的 30 秒心跳间隔。连接上限给 HTTP 与推送连接留出余量；结束后核对在线人数归零。HTTP 虚拟线程数与真实推送连接数是两个不同指标。

服务与负载发生器共享本机；MySQL、Redis 和 RabbitMQ 也运行本机。报告结果不直接等于线上机器容量，不用关闭安全校验、减少 BCrypt 强度或丢弃通知来提高吞吐。

社区参数独立保存在 `takeMe/server/src/main/resources/application-community.yml`，默认激活 `dev,community`；隔离测试使用 `dev,loadtest,community`。正式环境 JVM 堆通过启动参数选择 `-Xms1024m -Xmx1024m -XX:+UseG1GC`，不能通过 YAML 设置；历史压测记录中的 `-Xms512m -Xmx1024m` 只代表当时的实测环境。正式 Redis 参考配置位于 `tools/redis/community.conf`，使用 `maxmemory 64mb` 和 `allkeys-lru`；压测 Redis 配置保持独立，不随正式策略变更。数据库最大连接数、MySQL 缓冲池和 Redis 连接数量没有为了制造优化结果而盲目增加。生产部署不能照搬本地开发凭据。

本次只修改部署参考文件和应用启动参数，不执行正式 Redis 的在线配置变更，也不重启现有应用。正式 Redis 需由部署启动命令加载 `tools/redis/community.conf` 才生效；应用需重新启动才使用新堆参数。JMeter 是独立负载发生器，仍保留自身 `-Xms512m -Xmx1024m` 参数。

## 指标记录

每轮保存全部 HTTP 采样，汇总总量、实际 RPS、业务错误数、平均值、P95、P99、最大响应时间及各接口结果。RPS 是 HTTP 请求吞吐，不是订单业务吞吐。

持续测试保存 `<轮次>-manifest.json`，记录开始/结束时间、完成状态、构建摘要、逻辑 CPU、物理内存、JVM 参数、工具版本及连接数。中断不能算作完成，恢复必须使用新轮次。

`sample-resources.ps1` 每 30 秒追加 `<轮次>-resources.ndjson`，记录阶段、进程和主机 CPU、主机可用内存、进程工作集/私有内存、线程、JVM 堆、GC 累计统计、MySQL 全局连接/锁等待、Outbox 积压/最老事件年龄及各队列待消费/未确认数。采样只读，不附加 Arthas；低频采样仍有少量观测开销。CPU 按进程 CPU 秒增量除以墙钟时间和逻辑 CPU 数计算，工作集不能当作 Java 堆大小。

数据库全局状态包含同机其他连接，不将它当作本应用独占指标。Redis 命中率、端到端推送延迟分位数、Druid 活跃/排队时序未做完整独立采集，不补造数据。后续若以这些指标定位，应在独立诊断轮次增加观测后再复测。

面试所需的调整过程和证据索引见 `docs/loadtest-interview.md`，完整结论见 `docs/loadtest-verification.md`。

完整持续测试结束后运行 `loadtest/export-results.ps1 -Prefix <轮次>`，生成可提交的 `docs/loadtest-measurements.json`。脚本对未完成轮次拒绝导出正式结论；摘要不包含 JWT、密码、个人资料或消息正文。

## Arthas

通过 `arthas/attach.ps1 -TargetProcessId <项目 Java PID>` 手动附加。模板只监听 127.0.0.1，端口 3658、8563，随机本机密码保存在 `.runtime/arthas/credentials.json`，本地连接也必须认证。

已验证无凭据 HTTP 请求返回 401，认证后能读取线程信息，两个监听端口都为 127.0.0.1。正式测量前执行 Arthas `stop`；诊断与容量测量分开。

适合使用 `dashboard`、`jvm`、`thread -n 3`；慢接口用限定次数与耗时条件的 `trace`、`monitor`。不要输出包含老人完整地址、电话或密码的参数。模板禁用代码重定义、重转换、编译、对象执行及堆导出等高风险命令，不开启自动附加或远程 Tunnel。

## 产物与清理

原始采样为 `results/<轮次>/samples.jtl`，HTML 为 `results/<轮次>/report/index.html`，统计为 `summary.json`，构建摘要为 `results/build-artifact.json`。

未提供自动销毁数据库、容器或虚拟主机的脚本。完成后可以先停止隔离应用；需要删除隔离环境时必须明确核对库名、容器名及虚拟主机，不操作现有开发服务。
