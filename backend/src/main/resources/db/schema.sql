CREATE TABLE IF NOT EXISTS bank_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_name VARCHAR(64) NOT NULL,
  id_card VARCHAR(512) NOT NULL,
  id_card_hash VARCHAR(128) NOT NULL,
  phone VARCHAR(512) NOT NULL,
  phone_hash VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  kyc_status VARCHAR(32) NOT NULL DEFAULT 'VERIFIED',
  role VARCHAR(32) NOT NULL DEFAULT 'USER',
  token_version INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_bank_user_id_card_hash (id_card_hash),
  UNIQUE KEY uk_bank_user_phone_hash (phone_hash),
  KEY idx_bank_user_status (status),
  KEY idx_bank_user_kyc_status (kyc_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_user_security (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  login_password_hash VARCHAR(128) NOT NULL,
  trade_password_hash VARCHAR(128) NOT NULL,
  login_fail_count INT NOT NULL DEFAULT 0,
  locked_until DATETIME NULL,
  otp_enabled TINYINT NOT NULL DEFAULT 0,
  otp_secret VARCHAR(128) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bank_user_security_user (user_id),
  CONSTRAINT fk_bank_user_security_user FOREIGN KEY (user_id) REFERENCES bank_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_password_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  password_hash VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_bank_password_history_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  account_number VARCHAR(32) NOT NULL,
  user_id BIGINT NOT NULL,
  account_type VARCHAR(32) NOT NULL DEFAULT 'CURRENT',
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
  available_balance DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  frozen_balance DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  version INT NOT NULL DEFAULT 0,
  open_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  closed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_bank_account_number (account_number),
  KEY idx_bank_account_user (user_id),
  KEY idx_bank_account_status (status),
  CONSTRAINT fk_bank_account_user FOREIGN KEY (user_id) REFERENCES bank_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_transfer_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  from_account VARCHAR(32) NOT NULL,
  to_account VARCHAR(32) NOT NULL,
  to_bank_name VARCHAR(64) NULL,
  amount DECIMAL(18,2) NOT NULL,
  fee DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  remark VARCHAR(200) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'INIT',
  failure_reason VARCHAR(255) NULL,
  idempotency_key VARCHAR(128) NULL,
  risk_action VARCHAR(32) NOT NULL DEFAULT 'PASS',
  scheduled_at DATETIME NULL,
  executed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bank_transfer_order_no (order_no),
  UNIQUE KEY uk_bank_transfer_order_idempotent (user_id, idempotency_key),
  KEY idx_bank_transfer_order_user_time (user_id, created_at),
  KEY idx_bank_transfer_order_status (status),
  KEY idx_bank_transfer_order_accounts (from_account, to_account)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_transaction_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_no VARCHAR(64) NOT NULL,
  order_no VARCHAR(64) NULL,
  user_id BIGINT NOT NULL,
  account_number VARCHAR(32) NOT NULL,
  direction VARCHAR(8) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  balance_after DECIMAL(18,2) NOT NULL,
  counterparty_account VARCHAR(32) NULL,
  counterparty_name VARCHAR(64) NULL,
  transaction_type VARCHAR(32) NOT NULL,
  category VARCHAR(32) NOT NULL DEFAULT 'TRANSFER',
  remark VARCHAR(200) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bank_transaction_record_no (record_no),
  KEY idx_bank_transaction_record_user_time (user_id, created_at),
  KEY idx_bank_transaction_record_account_time (account_number, created_at),
  KEY idx_bank_transaction_record_order (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_contact (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  contact_name VARCHAR(64) NOT NULL,
  account_number VARCHAR(32) NOT NULL,
  bank_name VARCHAR(64) NOT NULL DEFAULT '本行',
  phone VARCHAR(512) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_bank_contact_user_account (user_id, account_number),
  KEY idx_bank_contact_user_name (user_id, contact_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_kyc_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  id_card_hash VARCHAR(128) NOT NULL,
  front_file_ref VARCHAR(255) NULL,
  back_file_ref VARCHAR(255) NULL,
  face_result VARCHAR(64) NULL,
  face_score DECIMAL(8,4) NULL,
  channel VARCHAR(64) NOT NULL DEFAULT 'MANUAL_DEMO',
  status VARCHAR(32) NOT NULL DEFAULT 'VERIFIED',
  review_comment VARCHAR(255) NULL,
  reviewed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_bank_kyc_record_user (user_id),
  KEY idx_bank_kyc_record_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(500) NOT NULL,
  channel VARCHAR(32) NOT NULL DEFAULT 'IN_APP',
  business_type VARCHAR(64) NOT NULL,
  read_flag TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_bank_notification_user_read (user_id, read_flag, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_notification_send_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  notification_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  channel VARCHAR(32) NOT NULL,
  template_code VARCHAR(64) NOT NULL,
  payload VARCHAR(1000) NOT NULL,
  send_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  failure_reason VARCHAR(255) NULL,
  retry_count INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_bank_notification_send_user (user_id, created_at),
  KEY idx_bank_notification_send_status (send_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  operation_type VARCHAR(64) NOT NULL,
  resource_type VARCHAR(64) NOT NULL,
  resource_id VARCHAR(128) NULL,
  ip VARCHAR(64) NULL,
  user_agent VARCHAR(255) NULL,
  trace_id VARCHAR(64) NULL,
  result VARCHAR(32) NOT NULL,
  detail VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_bank_audit_log_user_time (user_id, created_at),
  KEY idx_bank_audit_log_operation (operation_type, created_at),
  KEY idx_bank_audit_log_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_risk_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  account_number VARCHAR(32) NULL,
  order_no VARCHAR(64) NULL,
  risk_type VARCHAR(64) NOT NULL,
  risk_score DECIMAL(8,2) NOT NULL,
  risk_level VARCHAR(32) NOT NULL,
  action VARCHAR(32) NOT NULL,
  reason VARCHAR(500) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bank_risk_event_id (event_id),
  KEY idx_bank_risk_event_user_time (user_id, created_at),
  KEY idx_bank_risk_event_action (action, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_code VARCHAR(64) NOT NULL,
  product_name VARCHAR(120) NOT NULL,
  product_type VARCHAR(32) NOT NULL,
  risk_level VARCHAR(32) NOT NULL,
  term_days INT NOT NULL DEFAULT 0,
  expected_yield DECIMAL(10,4) NOT NULL DEFAULT 0.0000,
  min_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(32) NOT NULL DEFAULT 'ON_SHELF',
  description VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bank_product_code (product_code),
  KEY idx_bank_product_type_status (product_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_holding (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  account_number VARCHAR(32) NOT NULL,
  product_code VARCHAR(64) NOT NULL,
  product_type VARCHAR(32) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'HOLDING',
  start_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  maturity_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_bank_holding_user (user_id, status),
  KEY idx_bank_holding_product (product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_loan_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_code VARCHAR(64) NOT NULL,
  applicant_name VARCHAR(64) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  term_months INT NOT NULL,
  purpose VARCHAR(200) NULL,
  auto_score DECIMAL(8,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(32) NOT NULL DEFAULT 'APPLYING',
  review_comment VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_bank_loan_application_user (user_id, created_at),
  KEY idx_bank_loan_application_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_loan_repayment_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  loan_application_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  period_no INT NOT NULL,
  due_date DATE NOT NULL,
  principal DECIMAL(18,2) NOT NULL,
  interest DECIMAL(18,2) NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'UNPAID',
  paid_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_bank_loan_repayment_user_due (user_id, due_date),
  KEY idx_bank_loan_repayment_loan (loan_application_id, period_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_credit_card (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  card_number VARCHAR(32) NOT NULL,
  product_code VARCHAR(64) NOT NULL,
  credit_limit DECIMAL(18,2) NOT NULL,
  used_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  points INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_ACTIVATION',
  bill_day INT NOT NULL DEFAULT 10,
  repayment_day INT NOT NULL DEFAULT 25,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bank_credit_card_number (card_number),
  KEY idx_bank_credit_card_user (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_credit_bill (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  credit_card_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  bill_month VARCHAR(16) NOT NULL,
  bill_amount DECIMAL(18,2) NOT NULL,
  min_repayment DECIMAL(18,2) NOT NULL,
  due_date DATE NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'UNPAID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_bank_credit_bill_card_month (credit_card_id, bill_month),
  KEY idx_bank_credit_bill_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_message_outbox (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  topic VARCHAR(128) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  business_key VARCHAR(128) NOT NULL,
  payload VARCHAR(2000) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bank_message_outbox_event (business_key, event_type),
  KEY idx_bank_message_outbox_status (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_reconciliation_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  report_date DATE NOT NULL,
  account_count INT NOT NULL DEFAULT 0,
  total_balance DECIMAL(18,2) NOT NULL DEFAULT 0.00,
  record_count INT NOT NULL DEFAULT 0,
  order_count INT NOT NULL DEFAULT 0,
  exception_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  detail VARCHAR(1000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bank_reconciliation_report_date (report_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_agent_draft (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  draft_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  draft_type VARCHAR(64) NOT NULL,
  payload VARCHAR(2000) NOT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'WAITING_CONFIRM',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bank_agent_draft_no (draft_no),
  KEY idx_bank_agent_draft_user (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_scheduled_transfer (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  order_no VARCHAR(64) NOT NULL,
  from_account VARCHAR(32) NOT NULL,
  to_account VARCHAR(32) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  remark VARCHAR(200) NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
  scheduled_at DATETIME NOT NULL,
  executed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bank_scheduled_transfer_order (order_no),
  KEY idx_bank_scheduled_transfer_due (status, scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bank_batch_transfer_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_no VARCHAR(64) NOT NULL,
  user_id BIGINT NOT NULL,
  total_count INT NOT NULL DEFAULT 0,
  success_count INT NOT NULL DEFAULT 0,
  failed_count INT NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL DEFAULT 'INIT',
  detail VARCHAR(2000) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bank_batch_transfer_task_no (task_no),
  KEY idx_bank_batch_transfer_task_user (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
