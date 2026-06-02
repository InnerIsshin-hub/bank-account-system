# 验证汇总

- 后端编译：`mvn -q -DskipTests compile` 通过。
- 后端测试：`mvn -q test` 通过。
- 前端依赖安全：`npm audit fix` 后 `found 0 vulnerabilities`。
- 前端构建：`npm run build` 通过。
- Agent 构建：`agent/` 执行 `npm run build` 通过。
- 运行健康检查：`http://localhost:8080/actuator/health` 返回 `{"status":"UP"}`。
- 前端页面：Vue `http://localhost:5173` 可访问，TypeScript Agent `http://localhost:5174` 可访问。
- 算法服务：`http://localhost:8090/health` 返回 `{"status":"UP"}`。
- 组件状态：MySQL `bank_db` 有 23 张表；Redis `PING` 返回 `PONG`；RabbitMQ `notification.sms.queue`、`notification.email.queue`、`notification.push.queue` 存在。
- 真实接口烟测：登录、账户列表、转账预校验、真实小额转账、流水、联系人、理财、贷款、信用卡、通知、Agent 技能、Agent 账单分析、Agent 草稿、后台管理、日终对账均通过。
- 新增烟测：账户详情、OTP 绑定/解绑、批量转账预校验和执行、定时转账订单、风控事件查询、补偿扫描均通过。
- 算法烟测：AML 资金风险检测识别临界拆分和资金环路；KYC OCR/人脸接口识别身份证号并检测到人脸区域。
- 算法单测：`algorithm/` 执行 `pytest -q`，3 个用例通过。
- Docker Compose：配置文件已补齐；当前验证以容器内本地安装组件和开发服务器为准。

已跳过：深度学习模型离线训练、GraphSAGE/DNN 等训练任务。
