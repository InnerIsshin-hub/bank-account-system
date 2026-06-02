INSERT INTO bank_product (product_code, product_name, product_type, risk_level, term_days, expected_yield, min_amount, status, description)
VALUES
('DEP-3M', '三个月定期存款', 'DEPOSIT', 'LOW', 90, 1.8500, 1000.00, 'ON_SHELF', '到期自动转回活期账户'),
('DEP-1Y', '一年期稳健定存', 'DEPOSIT', 'LOW', 365, 2.2500, 1000.00, 'ON_SHELF', '提前支取按活期规则模拟计息'),
('WM-BALANCED', '稳健理财 A', 'WEALTH', 'MEDIUM', 180, 3.2000, 5000.00, 'ON_SHELF', '非保本浮动收益，购买前需确认风险提示'),
('LOAN-CONSUME', '消费贷', 'LOAN', 'MEDIUM', 365, 4.8000, 10000.00, 'ON_SHELF', '自动评分后生成还款计划'),
('CARD-GOLD', '金卡信用卡', 'CREDIT_CARD', 'MEDIUM', 0, 0.0000, 0.00, 'ON_SHELF', '自动审批额度，支持账单和分期 demo')
ON DUPLICATE KEY UPDATE
  product_name = VALUES(product_name),
  product_type = VALUES(product_type),
  risk_level = VALUES(risk_level),
  term_days = VALUES(term_days),
  expected_yield = VALUES(expected_yield),
  min_amount = VALUES(min_amount),
  status = VALUES(status),
  description = VALUES(description);
