# 第 7 阶段：运行组件与 TypeScript Agent 收尾

- 已按 `ref.md` 在当前 Ubuntu 容器内完成 MySQL、Redis、RabbitMQ 运行验证：`bank_db` 23 张表，Redis `PONG`，RabbitMQ 通知队列可见。
- MySQL 使用 `root / 050130`，Redis 无密码，RabbitMQ 使用 `bank / bank123`；统一记录见 `docs/environment_components.md`。
- Vue 内原 Agent 页面已改为工作台入口，不再承载 Agent 业务逻辑。
- Agent 已迁移到根目录 `agent/`，使用 TypeScript + React + Vite 实现独立工作台，支持 demo 登录、技能发现、动态参数表单、对话和工具调用。
- 后端 Agent 技能保持白名单和结构化参数，只允许查询、预校验和创建待确认草稿，不直接执行资金划拨。
- 补充根目录 `.gitignore`，忽略 `node_modules`、`dist`、`target` 和 TypeScript 构建中间文件。

验证：

- `mvn -q test` 通过。
- `frontend/` 执行 `npm run build` 通过。
- `agent/` 执行 `npm run build` 通过。
- `http://localhost:8080/actuator/health` 返回 `UP`。
- `http://localhost:5173`、`http://localhost:5174` 均可访问。
- 真实接口烟测通过：张明登录、账户 2 个、转账预校验 `PASS`、小额转账订单 `TR202606021220100474069` 状态 `SUCCESS`、Agent 技能 13 个、Agent 转账草稿状态 `WAITING_CONFIRM`、后台管理与日终对账接口可用。
