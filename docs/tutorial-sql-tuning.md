# 第二次课：MyBatis SQL 日志辅助 SQL 调优，一小时实操计划

状态：实操大纲。上一小时学习JMeter与Arthas的基本操作，本课独立占用60分钟，沿用“普通用户查询我的订单”的请求链。项目 N+1 的实际定位顺序是 JMeter 暴露接口变慢，再查看开发环境 MyBatis SQL 日志确认重复查询；日志由 Logback 的 `SQL_FILE` appender 接收 `com.me.mapper` DEBUG 输出，写入 `logs/dev/takeMe-8080-sql.log`。Arthas `watch org.apache.ibatis.executor.SimpleExecutor doQuery` 记录的完整请求次数含鉴权为 13/23→4/5；扣除一次鉴权 SQL 后，业务查询为 12/22→3/4。本课以 SQL 日志分析为主，Arthas 只作可选的调用耗时辅助或独立次数采样。

目标：从接口耗时定位数据库访问，找到实际SQL，解释执行计划，提出一个有证据支持的优化方案，并知道如何验证效果。一小时训练常用流程，不能据此认定已经掌握全部SQL调优。

依据：2026-10-13检查源码af9a1cd。项目使用MyBatis-Plus 3.5.5、MySQL Connector/J 8.0.33、Druid连接池；驱动版本不代表数据库服务端版本。本次更新课程，没有附加进程、采集SQL、连接数据库或执行索引变更，所有性能数据待实操记录。

## 工具与课前准备

| 工具 | 本课用途 | 当前情况 |
| --- | --- | --- |
| JMeter | 固定请求与负载，测错误率、吞吐量、P95 | 已安装5.6.3，复用上一课的鉴权查询计划 |
| MyBatis SQL 日志 | 查看实际SQL模板、参数和单次请求内的重复查询 | `logback-spring.xml` 用 `SQL_FILE` appender 输出 `com.me.mapper` DEBUG 日志；`application.yml` 不需配置 `log-impl`。压测 profile 将 mapper 提至 WARN，SQL 文件可能为空 |
| Arthas | 可选：观察线程与Java调用耗时；复核历史 SQL 次数时单独使用 watch | 已安装4.3.5，复用tools/arthas/attach.ps1 |
| MySQL客户端 | 查看表、索引、执行计划与服务端版本 | 已发现D:/tool/MySQL/MySQL Server 8.0/bin/mysql.exe及MySQL Shell |
| IDE与结果记录文件 | 对照源码、保存诊断证据和前后结果 | 使用现有IDE即可 |
| 数据库图形客户端 | 图形化查看执行计划与索引 | 可选，使用自己已有且支持MySQL的客户端，不要求额外安装 |
| MySQL Performance Schema/sys | 按SQL模板观察频次、累计耗时、扫描行数及锁等待线索 | 数据库内置；启用状态、采集项和读取权限待实操确认 |
| MySQL慢查询日志 | 长时间收集超过阈值的查询，补充扫描行数与耗时证据 | 可选；本小时不依赖临时开启全局日志 |

必须先准备好上一课可以成功执行的普通用户查询，确认当前后端PID、实际连接库和账号权限。Arthas连接方式见上一课，凭证只在本机查看。

若本课要实际验证索引变更，课前需准备隔离练习库及足够的合成订单，保留相同数据的对照副本，并让独立练习后端连接它；数据准备时间不计入60分钟。代表性数据包括有较多订单的用户、少量订单的用户和多种状态，不能用十几条数据推断大表收益。当前运行项目可以完成只读定位；没有隔离环境时，本课结束于分析和复测方案，索引变更实操另行安排。

## 时间安排

| 时间 | 操作 | 验收 |
| --- | --- | --- |
| 0—5分钟 | 确定接口、进程、数据库、测试参数，认识四种耗时 | 能区分接口、Java方法、数据库访问和数据库执行耗时 |
| 5—15分钟 | trace订单查询，thread排除连接获取等等待 | 能指出接下来应该深入哪个节点 |
| 15—25分钟 | 查看MyBatis SQL日志，核对SQL模板、参数和重复模式；需要具体次数时另做 Arthas watch 采样 | 能对应分页COUNT、页数据、服务项、评价查询，并说清两种证据的用途 |
| 25—38分钟 | 查看表结构、索引，阅读EXPLAIN与EXPLAIN ANALYZE | 能解释访问方式、索引、估计行数和实际执行数据 |
| 38—48分钟 | 对比候选优化，选择一个进行隔离验证 | 优化与WHERE、ORDER BY和数据分布直接对应 |
| 48—57分钟 | 清理增强后用同一JMeter计划复测 | 同时检查结果一致性、SQL计划和接口表现 |
| 57—60分钟 | 独立复述证据，完成记录与清理 | 能说明问题、依据、方案、结果或待验证事项 |

## 2.1：0—15分钟，接口慢是否来自SQL

问题：看到接口慢，怎样区分SQL执行慢、连接池等待和Java对象装配慢？

主链：GET /api/user/order/list → OrderController.list → OrderServiceImpl.getMyOrderList → Mapper分页与批量查询 → 订单VO返回。

1. 固定一个有订单的普通用户，pageNum=1、pageSize=10，先不传status与orderNo。JMeter标签使用同一“查询订单”，记录业务code和返回总数。
2. 用上一课的trace观察getMyOrderList，先捕获少量调用，不先加耗时阈值。trace默认只展开被观察方法的一层调用，需要根据慢节点继续向下定位。
3. Mapper节点包含MyBatis、连接获取、JDBC网络等待及结果映射等开销；某节点耗时高只能证明该路径值得检查，不能直接证明MySQL正在全表扫描。
4. 如出现等待，用thread查看请求线程的栈。Druid获取连接的等待与JDBC读取响应的等待要分别分析；CPU不高不能排除数据库等待。
5. 在记录中区分JMeter端到端时间、业务方法时间、MyBatis查询路径时间和数据库端执行时间，不能把它们当成同一指标。

必读路径：

1. takeMe/server/src/main/java/com/me/controller/user/OrderController.java，list：输入鉴权及分页参数，调用订单服务，输出分页结果。
2. takeMe/server/src/main/java/com/me/service/Impl/OrderServiceImpl.java，getMyOrderList：用户过滤、可选状态和订单编号过滤、稳定排序、分页、服务项与评价批量读取。
3. takeMe/server/src/main/resources/application.yml，spring.datasource：确认使用Druid；实际参数还需对照启用的profile。默认配置启用dev、community，不能只读dev就断定最终连接池参数。

验收：能够说出当前证据支持“数据库访问路径慢”，还是支持“对象装配慢”，证据不足时指出下一步。

## 2.2：15—25分钟，从MyBatis SQL日志确认SQL形态，单独采样调用次数

问题：LambdaQueryWrapper没有写SQL文本，运行时到底执行了哪些SQL？

主链：Wrapper与分页插件 → MyBatis MappedStatement/BoundSql → SQL日志 → JDBC → 数据库。

1. 开发环境通过 `logback-spring.xml` 的 `SQL_FILE` appender 输出 `com.me.mapper` DEBUG 日志到 `logs/dev/takeMe-8080-sql.log`；`application.yml` 不需要设置 `log-impl`。`application-loadtest.yml` 将 Mapper logger 设为 WARN，因此计分压测的 SQL 文件可能为空。不要将含个人信息的绑定参数写入共享日志。
2. 先用JMeter单线程、固定账号和pageSize=10请求一次订单列表，按请求时间窗口查看日志，记录MappedStatement、SQL模板和参数。先单线程便于区分一条请求所触发的COUNT、主列表、服务项和评价查询。
3. 对照pageSize=10和20，观察是否出现同一条按order_id查询服务项的SQL随订单数量重复。当前源码已经批量查询服务项和评价，不把“N+1待修复”当成既定结论；若要演示历史问题，应以可运行的旧版本和其日志为准。
4. 志愿者列表同样检查是否逐项查询父订单和老人信息，再与当前按本页ID批量读取的实现对照。
5. SQL模板可能含问号占位符。通过日志里的参数映射核对本次已知测试输入；不能将含问号的模板直接粘贴进客户端执行，更不能随意字符串替换后声称等价。

SQL日志展示的是MyBatis提交给JDBC的语句和绑定参数，不等于纯MySQL执行耗时。历史原始 watch 结果是包含鉴权 SQL 的 13/23→4/5；按业务 SQL 口径各扣除一次鉴权查询后，得到 12/22→3/4。Arthas `watch org.apache.ibatis.executor.SimpleExecutor doQuery` 用于单独统计调用次数，重复 SQL 形态仍由 MyBatis SQL 日志确认。并发日志可能交错，诊断时要用固定账号、单线程或明确的请求关联方式；不要把并发窗口内的所有 SQL 都算到同一个请求。

选读：takeMe/server/src/main/java/com/me/config/MyBatisPlusConfig.java的分页插件；MyBatis BoundSql官方API。仓库的diagnose-performance.ps1含其他诊断操作，不整段运行来代替本课的8080单接口练习。

验收：能把SQL模板对应到源码调用，并解释为什么一次分页请求可能执行多条SQL。

## 2.3：25—38分钟，用数据库执行计划验证猜测

问题：SQL是否使用合适索引？扫描了多少数据？排序是否成为成本？

1. 在数据库客户端确认服务端版本与当前库，查看订单表结构和现有索引。仓库迁移文件只能证明有索引方案，不能证明当前库已经执行迁移。
2. 对捕获并核对参数的SELECT先看EXPLAIN：type、possible_keys、key、key_len、rows、filtered、Extra。rows是估计值；Using filesort表示额外排序操作，不代表一定落盘或一定有性能问题；ALL对很小的表也未必不合理。
3. 再看EXPLAIN ANALYZE的actual time、rows、loops，与估计值对比。MySQL 8.0.18起支持该能力，它会实际执行查询；本课仅分析已确认成本可控的查询，较老服务端采用普通EXPLAIN并标明缺少实际执行数据。
4. 同一用户分别检查第一页、较深页、带status筛选的页；再换订单较多/较少的用户。只有查询实际有足够结果，才能观察深分页的扫描成本。
5. COUNT与页数据分别分析：第一页数据查询很快，并不代表COUNT同样快。不要把分页总数计算偷偷删掉来换取“性能提升”。

本项目的基础查询特点：user_id等值过滤，create_time与id降序排序，LIMIT分页；status为可选等值条件；orderNo使用包含匹配。以运行时捕获的SQL为准，不凭源码手写的近似SQL替代执行计划证据。

验收：能明确区分“有索引”“优化器选择了索引”和“选择后确实减少成本”。

## 2.4：38—48分钟，比较优化方向，选择一个验证

问题：应该增加索引、改SQL，还是先处理等待？

| 证据与场景 | 候选方向 | 必须比较的代价 |
| --- | --- | --- |
| 用户过滤后仍有大量无关扫描或排序 | 检查(user_id, create_time DESC, id DESC)组合索引 | 是否已有同等索引、数据选择性、实际扫描成本、写入和存储代价 |
| 常用status筛选仍过滤大量记录 | 比较(user_id, status, create_time DESC, id DESC) | 无status查询通常无法直接利用这套索引完成相同排序，不能盲目替换原索引 |
| 包含匹配orderNo成为热点 | 对比精确、前缀、包含搜索各自方案 | 必须保留用户要求的搜索语义，不能擅自把包含改成精确匹配 |
| 深分页扫描明显增加 | 比较基于(create_time,id)的游标分页 | 改变跳页、分页接口和前端交互，不属于仅增加索引即可完成的改动 |
| 查询次数随订单数增加 | 检查逐条查询，考虑批量查询 | 当前本接口已有批量查询，先确认实际运行代码 |
| 连接池获取、锁等待主导耗时 | 检查连接使用、长事务和阻塞关系 | 单纯加索引或调大连接池不一定解决问题，需分别取证 |

本课优先复盘基础分页索引：docs/migrations/20261005_sql_performance.sql已有idx_order_user_created。先确认实际索引和执行计划；若已经合适且查询没有明显成本，不新增重复索引。

隔离库验证只改变一个因素，保留相同数据和SQL；可以在两份等价练习表中对比有无候选索引。每次变更都记录恢复方式，继续验证筛选、排序、分页和总数一致。索引能服务查询仍可能需要回表，组合索引并不自动等于覆盖索引。

验收：提出一个“因为观察到X，所以验证Y，通过Z判断是否有效”的方案。若没有发现瓶颈，能够说明“当前没有证据需要改动”，而不是强行制造优化。

## 2.5：48—60分钟，复测并形成结论

问题：执行计划变漂亮了，用户接口是否真的改善？

1. 关闭本机临时SQL日志级别；若曾使用Arthas观察业务方法，再恢复增强类并停止诊断会话。正式计分压测与诊断采样分开，避免把增强及日志开销计入优化收益。
2. 使用同一JMeter查询计划，保持账号、数据、筛选、分页、线程数、定时器、持续时间和运行环境一致；统一预热条件。
3. 至少做三轮同等短测，并记录有效样本数。30秒、小负载结果只适合课堂观察；P95会随样本和背景负载波动，不能仅凭一次结果或几个毫秒差异宣称提升。
4. 先检查业务code、返回总数、订单与服务项ID、排序和评价信息，再比较错误率、吞吐量和P95；同时保留查询次数、SQL计划及扫描数据。
5. 如需更广泛的SQL统计，认识Performance Schema的digest聚合：看调用次数、累计时间、扫描行数，而不只挑“最慢的一次”。累计统计应比较同一时间窗口的增量，不当作刚刚那轮压测的数据。需要锁等待分析时结合线程与数据库锁信息。
6. 最后三分钟口头复述：哪里慢、如何证明、修改了什么、结果是否一致、收益是否重复出现。使用stop结束Arthas会话，业务进程继续运行。

记录模板：

| 项目 | 优化前 | 优化后 |
| --- | --- | --- |
| 版本、数据量、用户订单数量 | 待填 | 待填 |
| 请求条件、并发、节奏、持续时间、样本数 | 待填 | 待填 |
| 业务结果及排序是否一致 | 待填 | 待填 |
| 实际SQL、索引、执行计划 | 待填 | 待填 |
| SQL次数、数据库实际rows/loops与耗时 | 待填 | 待填 |
| 接口错误率、吞吐量、P95 | 待填 | 待填 |
| 重复测试结果与结论 | 待填 | 待填 |

结课标准：从接口走到SQL和执行计划；能够选择有依据的优化方向；有隔离环境时完成一次对照验证，无隔离环境时提交完整诊断与复测方案。没有测量数据不填写提升百分比。

## 范围与后续

本课暂不展开SQL重写代码、业务接口改造、连接池参数变更、全局慢日志配置、Redis替代查询和数据库容量调优。后续可分别练习锁等待、深分页、COUNT优化及高频SQL聚合。源码或原理看不懂时，可沿本课的具体请求和方法继续追问。

## 官方资料

- [Arthas trace：调用层级与诊断开销](https://arthas.aliyun.com/doc/trace.html)
- [Arthas watch：方法观察点与表达式](https://arthas.aliyun.com/doc/watch.html)
- [MyBatis BoundSql：SQL模板与参数映射](https://mybatis.org/mybatis-3/apidocs/org/apache/ibatis/mapping/BoundSql.html)
- [MySQL EXPLAIN与EXPLAIN ANALYZE](https://dev.mysql.com/doc/refman/8.0/en/explain.html)
- [MySQL组合索引](https://dev.mysql.com/doc/refman/8.0/en/multiple-column-indexes.html)
- [MySQL Performance Schema语句聚合](https://dev.mysql.com/doc/mysql-perfschema-excerpt/8.0/en/performance-schema-statement-summary-tables.html)
- [MySQL慢查询日志](https://dev.mysql.com/doc/refman/8.0/en/slow-query-log.html)
