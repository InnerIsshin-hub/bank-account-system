# 网上银行系统

Spring Boot 3 + MyBatis-Plus + MySQL 后端，Vue 3 + Vite + Element Plus 前端，根目录 `agent/` 为 TypeScript + React Agent 工作台，根目录 `algorithm/` 为 Python 算法服务。当前实现覆盖注册开户、JWT 登录、账户查询、转账闭环、流水、审计、通知、规则风控、业务 demo、Agent 工具 API、资金风险检测、证件 OCR/人脸检测与基础运维监控。

## 运行环境

- JDK 17
- Maven 3.9+
- Node.js 20+
- MySQL 8.0，数据库名 `bank_db`
- Redis / RabbitMQ 为本地可演示组件，Kafka / RocketMQ 为架构增强 demo 预留，可通过 Docker Compose 启动

## 快速启动

完整容器方式：

```bash
docker compose up --build
```

本地开发方式：

```bash
docker compose up -d mysql redis rabbitmq kafka rocketmq-namesrv rocketmq-broker
mysql -h127.0.0.1 -uroot -p050130 bank_db < backend/src/main/resources/db/schema.sql
mysql -h127.0.0.1 -uroot -p050130 bank_db < backend/src/main/resources/db/data.sql

cd backend
mvn spring-boot:run

cd ../frontend
npm install
npm run dev

cd ../agent
npm install
npm run dev

cd ../algorithm
# 本地 OCR 需要系统安装 tesseract-ocr 和 tesseract-ocr-chi-sim；Dockerfile 已内置
python3 -m pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8090
```

默认端口：

- 后端：`http://localhost:8080`
- 前端：`http://localhost:5173`
- TypeScript Agent 工作台：`http://localhost:5174`
- 算法服务：`http://localhost:8090`
- Actuator：`http://localhost:8080/actuator/health`
- Prometheus：`http://localhost:8080/actuator/prometheus`

## 关键配置

后端配置优先从环境变量读取，`backend/src/main/resources/application.yml.example` 可直接复制为运行模板。

- `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
- `JWT_SECRET`
- `BANK_DATA_SECRET`
- `REDIS_HOST`、`RABBITMQ_HOST`、`KAFKA_BOOTSTRAP_SERVERS`
- `BANK_AGENT_API_KEY`：可选，当前 Agent 默认使用规则和工具编排，不在代码中保存密钥

## 算法服务

- `POST /api/aml/evaluate`：输入交易列表，输出账户/交易维度的洗钱风险分、等级、动作和原因。
- `POST /api/kyc/ocr-face`：输入证件图片 base64，输出 OCR 字段、人脸区域、图像质量和 KYC 决策。
- 当前算法为可解释规则/图算法与传统 CV/OCR，不执行深度学习模型训练。

## 演示账号

- 张明：身份证号 `110101199001010011`，登录密码 `Demo@123`，交易密码 `123456`
- 李娜：身份证号 `110101199002020022`，登录密码 `Demo@123`，交易密码 `123456`
- 系统管理员：身份证号 `110101198801010099`，登录密码 `Admin@123`，交易密码 `654321`

## 常见问题

- Java 缺失：安装 OpenJDK 17 后重试。
- 前端接口 404：确认 Vite 代理指向 `http://localhost:8080`，或设置 `VITE_API_BASE_URL`。
- 登录后 401：清理浏览器 `localStorage` 里的旧 token 并重新登录。
- MySQL 连接失败：确认 `bank_db` 已创建，且 root 密码为 `050130` 或已通过 `DB_PASSWORD` 覆盖。

## docker image启动命令
powershell：

docker rm -f bank-runtime  //若存在，先删除

//自行修改挂载文件夹
docker run -d --name bank-runtime `
  -p 8080:8080 `
  -p 5173:5173 `
  -p 5174:5174 `
  -p 8090:8090 `
  -p 15672:15672 `
  -v "E:\university_learn\good:/workspace/" `
  onlinebank /workspace/bank-account-system/start.sh