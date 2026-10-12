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

数据库默认连接本机 `takeme`，用户名默认 `root`；可通过 `TAKEME_DB_URL` 和 `TAKEME_DB_USERNAME` 覆盖。Redis 默认 `localhost:6379`，RabbitMQ 默认 `localhost:5672`。开发日志统一写入仓库根目录下的 `logs/dev`，启动脚本支持通过 `TAKEME_LOG_DIR` 覆盖。

不需要设置压测专用的 `TAKEME_TEST_*` 变量来启动普通开发环境。`community` 会将数据库最大连接数设为 16、等待超时设为 3000 毫秒，并覆盖模板中的默认值。

## 构建与启动

配置完成后，在本目录执行：

```powershell
mvn -pl server -am package -DskipTests
& ../tools/start-app.ps1
```

默认 HTTP 端口为 8080。启动脚本先创建日志和诊断目录，再启动 JVM；工作目录固定为仓库根目录。`-JavaHome` 可指定其他 JDK 21 安装目录。

## 日志目录与 IDEA 启动

应用、错误、SQL 和 GC 日志保持独立文件，不合并。普通开发与隔离压测使用不同目录：

| 目录 / 文件 | 用途 |
| --- | --- |
| `logs/dev/takeMe-8080.log` | 开发应用日志，包含业务与框架日志 |
| `logs/dev/takeMe-8080-error.log` | ERROR 日志及异常堆栈，也保留在应用日志中 |
| `logs/dev/takeMe-8080-sql.log` | SQL 调试日志 |
| `logs/dev/takeMe-8080-gc.log` | JVM GC 日志 |
| `logs/dev/diagnostics/java_pid*.hprof` | OOM 时生成的堆转储 |
| `logs/loadtest/` | 9081 压测应用、错误、SQL、GC、控制台日志及诊断文件 |

应用日志沿用每日或 100MB 滚动、保留 7 天；5GB 上限分别作用于应用、错误、SQL 三个输出器，不是整个目录的共同上限。开发 GC 沿用 5 个轮转文件、每个 10MB；压测沿用 5 个轮转文件、每个 20MB。压测关闭 mapper 的 DEBUG 输出，SQL 文件可能为空。堆转储不参与日志滚动，需要按诊断需要手动清理。

从 IDEA 打开仓库根目录或 `takeMe` 子目录均可选择共享配置 **TakeMeApplication-统一日志**。该配置使用 Windows PowerShell 和 IDEA 内置 Shell Script 支持，在启动前运行“准备日志目录”，再执行构建并启动应用；开发工作目录始终为仓库根目录。保留现有 IDEA 的 2GB 堆参数，命令行启动脚本沿用 1GB 堆参数。

若 IDEA 已打开项目但尚未显示新配置，请重新打开项目以加载 `.run` 文件。已有本地 server 启动配置也已同步日志路径和启动前任务。不要只在 Java 启动类中创建 GC 目录，JVM 会在进入 main 方法之前打开 GC 日志。

需要修改 IDEA 日志目录时，同步调整“准备日志目录”的 `-LogDir` 参数，以及应用 VM options 中的 `logging.file.path`、GC 文件路径、`HeapDumpPath`；共享 IDEA 配置默认固定使用 `logs/dev`。脚本启动会自动为应用、GC 和转储同步目录，外部指定的相对路径也按仓库根目录解析。端口改为其他值时，应同步修改 GC 文件名中的端口标识。

旧日志保留原位，不自动移动或删除；新日志使用上述目录。日志及 `.hprof` 文件由 Git 忽略。

本地开发配置位于 resources 内，会被当前 Maven 构建复制到 JAR 中；不要将带有真实凭据的开发构建包或本地日志作为面试附件公开分享。需要分享时，使用从仓库重新克隆的工作区构建，并通过外部配置注入实际运行参数。
