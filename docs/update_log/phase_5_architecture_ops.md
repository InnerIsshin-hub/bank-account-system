# 第 5 阶段：架构增强

- Redis/Redisson 相关能力以本地限流、锁、token 黑名单实现 demo fallback，并在配置和 Compose 中预留 Redis。
- RocketMQ/Kafka/RabbitMQ 主题、交换机和队列通过本地 `bank_message_outbox` 落库事件预留：转账事件、交易日志、审计事件、通知事件。
- 增加对账报表表和日终对账接口，支持账户余额、流水、订单状态汇总。
- 增加异常订单扫描与补偿入口，跨行/处理中订单可进入人工处理策略。
- 接入 Actuator、健康检查和 Prometheus 指标端点。
- 前端新增后台管理页，管理员可查看用户、账户、转账订单、审计日志并冻结/解冻用户。

验证：后端编译和测试通过；外部 MQ/Redis 未作为主交易依赖，避免 demo 环境缺组件导致资金链路失败。
