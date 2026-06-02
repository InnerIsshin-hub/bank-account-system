# 环境组件与密码

- MySQL：`root / 050130`，数据库 `bank_db`
- Redis：无密码
- RabbitMQ：`bank / bank123`，管理台 `http://localhost:15672`
- Kafka：Docker Compose 预留，本地明文监听 `localhost:9092`
- RocketMQ：Docker Compose 预留，NameServer `localhost:9876`，Broker `localhost:10911`，无密码
- Vue 前端：`http://localhost:5173`
- TypeScript Agent 工作台：`http://localhost:5174`，源码目录 `agent/`
- Python 算法服务：`http://localhost:8090`，源码目录 `algorithm/`
- JWT / 数据加密密钥：通过 `JWT_SECRET`、`BANK_DATA_SECRET` 注入，默认值仅用于本地 demo

Agent API Key 不写入代码或日志，运行时通过 `BANK_AGENT_API_KEY` 注入。
