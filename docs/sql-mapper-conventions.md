# SQL 分层规范与迁移记录

## 实现边界

| 场景 | 实现位置 |
| --- | --- |
| 单表普通新增、按主键读取或更新、删除 | Mapper 的 MyBatis-Plus BaseMapper 方法 |
| 简单条件查询、普通分页、无状态比较和 NULL 写入的更新 | Mapper 的具名 default 方法，内部构造 MP Wrapper |
| FOR UPDATE、SKIP LOCKED 当前读 | XML Mapper |
| 比较旧状态后更新的 CAS | XML Mapper |
| 显式写 NULL，或参数允许清空字段 | XML Mapper |
| COUNT、SUM、GROUP BY 等聚合统计 | XML Mapper |
| 固定投影及覆盖索引查询 | XML Mapper |
| 跨表关联、子查询、数据库表达式、幂等插入、限量清理 | XML Mapper |

Service 负责事务、权限、业务校验、状态流转及结果组装，调用 Mapper 方法，不创建或传递 Wrapper。Controller、定时任务和消息消费者同样不拼装查询条件。现有 ServiceImpl/IService 的普通 CRUD 委托保留；SQL 的实现仍由 BaseMapper 提供。

简单查询统一在 Mapper 内使用 Lambda Wrapper。涉及复杂 SQL 时，在接口声明具名方法，在对应 XML 中实现；保留 Mapper、Param 等接口元数据注解，禁止 Select、Update、Insert、Delete 及其 Provider 注解，也不将执行选项留在方法注解中。

## 三批实施结果

1. 注解迁移：核对当前仓库实际为 17 个 Mapper，其中 12 个含 SQL 注解，共 30 条，全部移入对应 XML。原有 13 处查询缓存控制保留在 XML 属性中，锁语句和参数绑定保持原语义。
2. 分层收口：简单条件和分页移入 Mapper；CAS、头像清空、账号脱敏、统计、编号投影、跨表可接服务筛选及 Outbox 结果回写统一使用 XML。最终共 13 个 XML 文件、68 条自定义语句。
3. 回归与约束：调整原单元测试的 Mapper 模拟方法；集成测试加载生产使用的 XML；增加映射完整性、注解禁用、Wrapper 分层和当前读缓存策略检查，以及动态分页、CAS、投影和 NULL 写入的数据库回归。

## 验证记录

2026-10-24 首轮执行 Maven 全模块测试，并开启随机 MySQL 测试库回归：190 项中 159 项通过、31 项跳过，失败 0、错误 0。补充分层检查后，同时开启随机 MySQL 测试库和随机 RabbitMQ 虚拟主机回归：192 项中 171 项通过、21 项历史演示测试跳过，失败 0、错误 0。其中 MySQL 106 项、RabbitMQ 10 项及分层约束 6 项全部通过。

数据库测试复制业务库表结构到随机临时库，只在该临时库准备和清理记录，结束后删除临时库。验证覆盖并发接单、审批、事务回滚、退款幂等、显式清空字段、动态筛选、分页总数、消息归档及清理边界。

在项目根目录执行普通回归：`mvn -f takeMe/pom.xml -pl server -am test`。

开启真实数据库回归时，通过环境变量 TAKEME_TEST_MYSQL_PASSWORD 提供本地测试连接密码，并增加参数 `-Dtakeme.mysql.tests=true`。数据库账号需要具备创建和删除测试库的权限，测试表结构取自本地 takeme 库。

同时验证 RabbitMQ 时，通过环境变量 TAKEME_TEST_RABBIT_PASSWORD 提供本机测试密码，并增加参数 `-Dtakeme.rabbit.tests=true`。测试仅在随机虚拟主机中声明队列和投递消息，结束后删除该虚拟主机；账号需要具备对应管理权限。

分层约束测试位于 server 模块的 MapperArchitectureTest，普通回归即可运行，不连接数据库。

补充检查覆盖业务层导入、全限定类名及链式查询，避免绕过 Wrapper 分层限制；Mapper 内禁止使用 MP 的 SQL 片段、聚合和固定投影接口，普通查询已有的固定 LIMIT 保留。另已逐条比对原有 30 条注解 SQL 与迁移后 XML，忽略空白排版后文本全部一致。

Maven 打包成功，生成的应用 JAR 包含全部 13 个 XML 文件，内容与源码逐字节一致。
