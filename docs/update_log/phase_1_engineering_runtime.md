# 第 1 阶段：工程可运行

- 新增根目录 `README.md`、`docker-compose.yml`，补齐 MySQL、Redis、RabbitMQ、Kafka、RocketMQ、后端、前端服务说明。
- 新增后端/前端 Dockerfile 与前端 Nginx 代理配置。
- Java 版本降为 17 LTS，后端增加 Actuator、Prometheus、H2 测试依赖。
- 新增 `schema.sql`、`data.sql`，覆盖用户、安全、账户、订单、流水、联系人、KYC、通知、审计、风控、产品、贷款、信用卡、消息 outbox、对账、Agent 草稿等表。
- `application.yml.example` 改为环境变量模板，数据库、JWT、数据密钥、Redis/MQ/Agent 配置均可注入。
- 完成统一 `Result`、`BusinessException`、错误码、全局异常处理、traceId、MyBatis 分页/乐观锁/自动填充配置。

验证：`mvn -q -DskipTests compile` 通过；`docker` 命令当前容器未安装，Compose 未在本机执行启动验证。
