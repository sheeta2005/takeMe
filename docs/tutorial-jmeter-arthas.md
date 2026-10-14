# takeMe：JMeter 与 Arthas 一小时实操计划

目标：独立完成带登录的接口压测，解释结果，并用 Arthas 观察线程、调用耗时和方法返回信息。一小时覆盖常用工作流程；复杂故障排查需要后续练习。

基于源码版本 af9a1cd；2026-10-13 检查时后端监听8080，前端监听5173，后端进程PID为6236。PID仅为检查时的值，重启后需重新确认。本计划没有实际发起压测或附加Arthas，没有测得性能结论。

前置基础：能区分GET与POST、认识JSON和请求头。练习链路：POST /api/user/login → 返回data.token → GET /api/user/order/list → 返回code与data。使用一个已启用的普通用户账号，管理员和志愿者账号不能代替普通用户登录。

课程安排：本文件为第一次课，60分钟掌握工具基本流程；[第二次课：MyBatis SQL 日志辅助 SQL 调优](tutorial-sql-tuning.md)另占60分钟，继续学习SQL定位、执行计划、索引与效果验证。下文第1—6课为第一次课内部的分段。

范围说明：本文件的 Arthas 内容是通用线程和方法诊断练习。本项目 N+1 问题由 JMeter 压测暴露接口变慢，再通过开发环境 MyBatis SQL 日志确认重复关联查询；该日志由 Logback 写入 `logs/dev/takeMe-8080-sql.log`。Arthas `watch org.apache.ibatis.executor.SimpleExecutor doQuery` 只用于独立统计 SQL 次数，不是发现重复 SQL 形态的依据；本课的 trace 也不是这次问题的 SQL 定位依据。

## 时间安排

| 时间 | 学习任务 | 当场验收 |
| --- | --- | --- |
| 0—5分钟 | 打开工具，明确接口和账号 | 找到JMeter线程组，确认目标端口8080 |
| 5—13分钟 | 创建一个HTTP请求并检查响应 | 1线程、1循环，业务code为200 |
| 13—22分钟 | 登录、提取token、带鉴权查询、断言 | 登录一次后查询多次，失败响应会标红 |
| 22—30分钟 | 线程、升压、定时器、命令行报告 | 完成1线程和5线程对比，记录P95与错误率 |
| 30—36分钟 | 找到后端PID，附加Arthas | 确认目标进程并进入控制台 |
| 36—43分钟 | dashboard、jvm、thread | 解释堆内存、GC、线程CPU和线程栈 |
| 43—51分钟 | sc、sm、trace、watch、monitor | 捕获订单查询的一次调用，并说出各命令作用 |
| 51—57分钟 | JMeter与Arthas联动 | 从接口响应时间走到方法调用耗时，形成证据链 |
| 57—60分钟 | 脱离教程复做，清理诊断会话 | 独立完成一次请求与方法观察，正确停止Arthas |

## 第1课：0—13分钟，发出并验证一次真实请求

状态：实操大纲。问题：请求发出去了，怎样判断接口确实成功？

主链：JMeter HTTP请求 → UserLoginController.login → UserService.login → JWT返回 → 查看结果树与业务断言。

1. 打开 D:/tool/developset/takeme-tools/apache-jmeter-5.6.3/bin/jmeter.bat。
2. 测试计划右键 → 添加 → 线程（用户）→ 线程组。设置线程数1、Ramp-up 1秒、循环次数1。
3. 在线程组下添加“HTTP请求默认值”：协议http、服务器127.0.0.1、端口8080。域名栏不填写http前缀。
4. 添加“HTTP信息头管理器”：Content-Type为application/json。
5. 添加“HTTP请求”：POST，路径/api/user/login；Body Data填写用户名和密码两个字段，使用自己的普通用户账号。
6. 添加“查看结果树”，执行一次，检查请求、HTTP状态、响应正文。登录成功应有code=200和data.token。

必读路径：takeMe/server/src/main/java/com/me/controller/user/UserLoginController.java 的 login；输入LoginDTO，下一步UserService.login，输出LoginVO。选读takeMe/pojo/src/main/java/com/me/vo/LoginVO.java核对token字段。

必须理解：JMeter主要模拟HTTP请求，不会像浏览器一样执行页面JavaScript；本课请求目标是后端8080。HTTP 200不等于业务成功，必须查看code字段。

验收：独立指出请求方法、路径、正文、请求头和响应字段。登录失败先处理账号、身份和JSON配置，不先增加线程。

## 第2课：13—22分钟，登录关联与断言

状态：实操大纲。问题：后续查询怎样使用刚登录得到的凭证？

主链：一次登录 → JSON提取器 → JMeter变量token → Authorization请求头 → OrderController.list → OrderServiceImpl.getMyOrderList → 分页订单。

1. 将登录请求放进“仅一次控制器（Once Only Controller）”；它对每个线程执行一次，不随普通循环反复登录。
2. 登录请求下添加“JSON提取器”：变量名token、JSONPath为$.data.token、匹配编号1、默认值TOKEN_MISSING。
3. 在线程组下新增“HTTP请求”：GET /api/user/order/list。Parameters添加pageNum=1、pageSize=10。
4. 将Authorization信息头放在查询请求下，值为Bearer ${token}；Bearer和变量之间有一个空格。不同线程拥有自己的JMeter变量。
5. 登录与查询分别添加“JSON断言”：$.code的预期值200；登录再断言$.data.token存在。token出现TOKEN_MISSING时先查登录响应。
6. 设置1线程、3循环。结果应为登录1次、查询3次。故意改错查询token一次，确认断言能识别失败，再恢复。

必读路径：takeMe/server/src/main/java/com/me/controller/user/OrderController.java 的 list；takeMe/common/src/main/java/com/me/utils/JwtUtil.java 的 getUserIdFromAuthHeader；takeMe/server/src/main/java/com/me/service/Impl/OrderServiceImpl.java 的 getMyOrderList。输入token与分页参数，输出当前用户的订单分页。

选读：takeMe/redis-starter/src/main/java/com/me/redis/aspect/RateLimitAspect.java。当前登录每账号10次/60秒；如果返回业务code=429，等待窗口结束或选其他有效账号，不关闭限流来掩盖问题。

## 第3课：22—30分钟，控制负载并阅读报告

状态：实操大纲。问题：线程数与每秒请求数是否相同？接口表现是否随并发变化？

主链：线程组与定时器 → 重复订单查询 → JTL结果 → HTML报告 → 比较两轮指标。

1. 查询请求下添加“固定定时器”，先用500毫秒，模拟每次查询之间的等待。
2. 先用1线程、30秒记录基线；再用5线程、Ramp-up 5秒、30秒对比。线程组使用持续时间时，循环设为持续运行。
3. GUI只调试少量请求；正式测量禁用“查看结果树”，保存独立练习JMX，命令行模式运行并输出JTL和HTML报告。带运行的下一轮教学再逐项解释-n、-t、-l、-e、-o。
4. 只比较“查询订单”同一标签，登录样本单独查看。每轮结果使用新目录，报告输出目录必须为空或尚不存在。
5. 记录线程数、定时器、成功数、错误率、吞吐量、平均响应时间、P95。增加线程通常增加负载，但线程数不等于RPS；固定定时器也不保证精确吞吐。

验收：解释P95为约95%的请求耗时不超过该值；说明错误率来自断言后的成功状态；不能只凭平均值宣布系统正常。

参考文件只阅读：tools/loadtest/run.ps1体现CLI输出方式。现有tools/loadtest/login.jmx和community.jmx针对9081及专用数据，并非本次8080练习计划；不直接运行现有建库或刷新数据脚本。

本机同时运行压测工具与后端，会竞争CPU和内存；两轮小负载练习用于学习比较，不代表生产容量。若加压后响应时间明显升高或出现连续错误，先停止加压查看失败响应。

## 第4课：30—43分钟，附加运行中进程并看整体状态

状态：实操大纲。问题：后端是否在忙，忙在哪里？

主链：8080监听进程 → Arthas附加 → JVM及线程采样 → 选择线程 → 查看调用栈。

1. 在PowerShell通过Get-NetTCPConnection查询8080的OwningProcess；使用JDK的jps再核对它是TakeMeApplication，不选择IDE或JMeter。
2. 工具目录：D:/tool/developset/takeme-tools/arthas-4.3.5。优先使用项目tools/arthas/attach.ps1，TargetProcessId填当前后端PID。采用同一Windows用户和匹配权限运行。
3. 该脚本仅附加，不直接进入交互终端。附加成功后浏览器打开http://127.0.0.1:8563；用户名takeme，密码在本机tools/.runtime/arthas/credentials.json查看。不将凭证贴入学习记录。
4. 执行dashboard，观察CPU、heap、GC计数和耗时变化；Ctrl+C结束当前连续观察。执行jvm查看运行环境。
5. 执行thread -n 3查看采样区间内最忙的线程与栈；thread 后加实际线程ID查看单个栈。thread -b检查阻塞其他线程的锁持有者线索，它不等于完整死锁证明。

必读路径：tools/arthas/attach.ps1 → tools/arthas/arthas.properties.template → 本地Arthas服务。输出是诊断会话，不会重新启动业务应用。

验收：区分JVM进程PID与Java线程ID；知道WAITING状态本身不表示故障；不把GC累计耗时当作刚发生的一次GC停顿。

## 第5课：43—51分钟，从接口定位到具体方法

状态：实操大纲。问题：当前加载的代码是什么？调用时间主要花在哪里？

主链：GET订单列表 → getMyOrderList → Mapper查询及对象装配 → trace耗时树 → watch返回信息。

按顺序认识并练习以下命令：

| 命令 | 本项目练习 | 看什么 |
| --- | --- | --- |
| sc | sc -d com.me.service.Impl.OrderServiceImpl | 类是否加载、加载器、来源 |
| sm | sm com.me.service.Impl.OrderServiceImpl getMyOrderList | 方法是否存在 |
| trace | trace com.me.service.Impl.OrderServiceImpl getMyOrderList -n 3 | 3次调用内部各节点耗时 |
| watch | watch com.me.service.Impl.OrderServiceImpl getMyOrderList '{params[3].pageNum,params[3].pageSize,returnObj.total}' -s -x 1 -n 3 | 成功返回时的分页参数与总条数 |
| monitor | monitor -c 5 -n 3 com.me.service.Impl.OrderServiceImpl getMyOrderList | 每5秒汇总一次调用数、失败和平均耗时 |
| jad | jad com.me.service.Impl.OrderServiceImpl getMyOrderList | 运行中加载的方法实现，时间够再看 |

注意包名是Impl，大小写必须一致。trace/watch安装观察后，再触发查询，否则不会凭空显示历史调用。默认trace只展示当前被增强方法的一层调用，不自动展开全部数据库内部调用。先观察不加耗时过滤的trace，得到正常值后再练习条件#cost>10；无输出可能只是没有超过10毫秒。

必读：takeMe/server/src/main/java/com/me/service/Impl/OrderServiceImpl.java 的 getMyOrderList。参数位置0是用户ID、1是状态、2是订单编号、3是分页DTO；返回IPage<OrderVO>。watch只看本课需要的分页字段，不展开完整用户资料。

验收：能够分别说明thread、trace、watch解决什么问题。Arthas观察会增加运行开销，带trace的结果不能直接当成无诊断时的性能基线。

## 第6课：51—60分钟，联动与独立复做

状态：实操大纲。问题：怎样把“接口变慢”变成可复查的证据？

主链：JMeter查询负载 → 报告P95和错误率 → Arthas整体资源与线程 → 方法trace → 结论或下一步待证假设。

1. 用5线程查询计划运行30秒，记录时间范围。
2. 先观察dashboard和thread，再短时捕获3条getMyOrderList的trace。对比Mapper查询节点与对象装配节点；证据不足时只提出下一步检查对象，不直接说“数据库慢”。
3. JMeter响应时间包括请求传输、过滤器、控制器等环节；trace这里只测被观察的方法，所以两者不必相等。也不能靠聚合P95和随机3条trace建立逐请求对应关系。
4. 用三句话复述：请求是否成功；哪项指标随并发变化；当前证据支持什么、还需要查什么。
5. 最后三分钟独立复做：发出一条带token查询、执行一条方法观察、解释结果。
6. Ctrl+C停止当前持续命令；reset com.me.service.Impl.OrderServiceImpl恢复本课增强类；stop关闭Arthas服务并恢复增强，不停止业务项目。quit仅退出当前客户端，Arthas服务仍在。

完成标准：能够从空测试计划搭出带鉴权的请求链；能够比较错误率、吞吐与P95；能够附加正确进程，使用dashboard、thread、trace、watch；能够退出诊断而不关闭业务应用。

## 后续再学

CSV多账号参数化、录制器、精确吞吐控制、分布式压测；Arthas的profiler、tt、类加载器深度排查。事务写接口、订单创建、评价并发的压测安排到独立数据练习，不在本小时内混入。

如果源码、命令或原理看不懂，可继续追问；本文件给出学习路线与验收点，后续按实际操作逐段讲解。

## 官方资料

- [JMeter组件、JSON提取和断言](https://jmeter.apache.org/usermanual/component_reference.html)
- [JMeter GUI调试与CLI测量](https://jmeter.apache.org/usermanual/best-practices.html)
- [JMeter HTML报告](https://jmeter.apache.org/usermanual/generating-dashboard.html)
- [Arthas入门](https://arthas.aliyun.com/doc/quick-start.html)
- [Arthas线程](https://arthas.aliyun.com/doc/thread.html)
- [Arthas调用耗时](https://arthas.aliyun.com/doc/trace.html)
- [Arthas方法观察](https://arthas.aliyun.com/doc/watch.html)
- Arthas清理：[reset](https://arthas.aliyun.com/doc/reset.html)、[stop](https://arthas.aliyun.com/doc/stop.html)、[quit](https://arthas.aliyun.com/doc/quit.html)
