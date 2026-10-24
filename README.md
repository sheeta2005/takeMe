# TakeMe 社区助老平台

一个前后端分离的社区助老项目。老人可以预约服务，志愿者接单并完成服务，管理员负责人员、订单和日常审批。

目前包含助餐、助洁、助医、代购和陪伴五类服务，支付与退款走模拟流程。

## 主要功能

- **老人端**：浏览服务套餐、购物车结算、预约下单、查看订单、评价服务、管理地址和消息。
- **志愿者端**：接取服务、更新服务进度、查看服务记录与积分、提交请假申请。
- **管理端**：管理用户和志愿者、维护服务套餐、处理订单与审批、群发通知、查看数据看板。

## 技术栈

- 后端：Java 21、Spring Boot 3.2、MyBatis-Plus、MySQL
- 前端：Vue 3、TypeScript、Vite、Element Plus、Pinia、ECharts
- 其他：Redis 缓存与限流、RabbitMQ 异步通知、WebSocket 在线提醒、JWT 登录认证、阿里云 OSS 文件存储

订单通过数据库事务、行锁和唯一约束处理重复提交与并发接单；通知先随业务写入 Outbox，再投递到消息队列，失败后可重试。

## 目录

| 路径 | 内容 |
| --- | --- |
| `takeMe/server` | 后端接口与业务逻辑 |
| `takeMe/pojo` | 实体及接口数据对象 |
| `takeMe/common` | 公共类与工具 |
| `takeMe/redis-starter` | 缓存与限流 |
| `takeMe/mq-starter` | 消息队列配置与可靠投递支持 |
| `takeMeFront/takeMeFront` | Vue 前端 |
| `docs` | 业务说明、数据库迁移与验证记录 |
| `tools` | 启动、测试和压测脚本 |

## 本地运行

准备 JDK 21、Maven、Node.js、MySQL、Redis 和 RabbitMQ，以及用于上传文件的 OSS 配置。

### 后端

1. 在 IDEA 中导入 `takeMe/pom.xml`。
2. 将 `takeMe/server/src/main/resources/application-dev.example.yml` 复制为同目录下的 `application-dev.yml`，按模板配置数据库、Redis、RabbitMQ、JWT 和 OSS 的环境变量。
3. 准备好业务数据库，再运行 `com.me.TakeMeApplication`。仓库中的共享运行配置名为 **TakeMeApplication**。

后端默认端口为 `8080`，接口文档地址为 [Swagger UI](http://localhost:8080/swagger-ui.html)。完整配置和命令行启动方式见 [后端说明](takeMe/README.md)。

`docs/migrations` 中是增量 SQL，不包含完整建库脚本；首次运行需要另行准备业务表。数据库和 OSS 等真实凭据放在本地配置或环境变量中。

### 前端

在仓库根目录打开终端，执行：

```bash
cd takeMeFront/takeMeFront
npm install
npm run dev
```

浏览器打开终端提示的地址即可。前端开发请求默认连接 `http://localhost:8080`，请先启动后端。

## 其他说明

- [业务与组件说明](docs/business-simplification.md)
- [测试与压测工具](tools/README.md)

项目按单个社区、单个后端实例设计。压测过程和结果见对应文档。
