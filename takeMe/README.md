# 后端配置与启动

后端使用 JDK 21 和 Maven。运行前需准备包含项目业务表的 MySQL 数据库、Redis、RabbitMQ 和 OSS 配置；配置模板不包含真实账号或密钥，也不会自动初始化数据库。

## 仓库中的配置

- `server/src/main/resources/application.yml`：公共配置，默认激活 `dev,community`。
- `server/src/main/resources/application-dev.example.yml`：本地开发模板，敏感字段通过环境变量读取。
- `server/src/main/resources/application-community.yml`：小区单 JVM 部署参数。
- `server/src/main/resources/application-loadtest.yml`：隔离压测参数，不能作为正式环境配置直接使用。

克隆后，在本目录执行以下命令创建本地配置；已有本地配置时不要覆盖：

```powershell
Copy-Item server/src/main/resources/application-dev.example.yml server/src/main/resources/application-dev.yml
```

`application-dev.yml` 被 Git 精确忽略，真实凭据不要填入 example 或其他公共 YAML。

## 必需的环境变量

通过 IDE 运行配置或当前终端的进程环境变量设置以下字段：

| 变量 | 用途 |
| --- | --- |
| `TAKEME_DB_PASSWORD` | MySQL 密码 |
| `TAKEME_RABBITMQ_USERNAME` | RabbitMQ 账号 |
| `TAKEME_RABBITMQ_PASSWORD` | RabbitMQ 密码 |
| `TAKEME_JWT_SECRET` | 独立的随机 JWT 签名密钥，至少 32 字节 |
| `TAKEME_OSS_ENDPOINT` | OSS 地域端点 |
| `TAKEME_OSS_BUCKET_NAME` | OSS 存储桶 |
| `TAKEME_OSS_ACCESS_KEY_ID` | OSS 访问密钥 ID |
| `TAKEME_OSS_ACCESS_KEY_SECRET` | OSS 访问密钥 |
| `TAKEME_OSS_DOMAIN` | OSS 文件访问域名 |

数据库默认连接本机 `takeme`，用户名默认 `root`；可通过 `TAKEME_DB_URL` 和 `TAKEME_DB_USERNAME` 覆盖。Redis 默认 `localhost:6379`，RabbitMQ 默认 `localhost:5672`。默认日志目录为启动目录下的 `logs`。

不需要设置压测专用的 `TAKEME_TEST_*` 变量来启动普通开发环境。`community` 会将数据库最大连接数设为 16、等待超时设为 3000 毫秒，并覆盖模板中的默认值。

## 构建与启动

配置完成后，在本目录执行：

```powershell
mvn -pl server -am package -DskipTests
java -Xms1024m -Xmx1024m -XX:+UseG1GC -jar server/target/server-1.0-SNAPSHOT.jar
```

默认 HTTP 端口为 8080。也可以直接从 IDE 启动 `com.me.TakeMeApplication`。

本地开发配置位于 resources 内，会被当前 Maven 构建复制到 JAR 中；不要将带有真实凭据的开发构建包或本地日志作为面试附件公开分享。需要分享时，使用从仓库重新克隆的工作区构建，并通过外部配置注入实际运行参数。
