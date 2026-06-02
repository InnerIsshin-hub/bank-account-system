package com.student.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.student.bank.common.AuthContext;
import com.student.bank.common.AuthUser;
import com.student.bank.common.BusinessException;
import com.student.bank.common.ErrorCode;
import com.student.bank.dto.AccountApplyDTO;
import com.student.bank.dto.BatchTransferDTO;
import com.student.bank.dto.CloseAccountDTO;
import com.student.bank.dto.FreezeAccountDTO;
import com.student.bank.dto.TransferConfirmDTO;
import com.student.bank.dto.TransferDTO;
import com.student.bank.dto.TransferExecuteDTO;
import com.student.bank.dto.TransferPrecheckDTO;
import com.student.bank.entity.BankAccount;
import com.student.bank.entity.BankBatchTransferTask;
import com.student.bank.entity.BankScheduledTransfer;
import com.student.bank.entity.BankTransactionRecord;
import com.student.bank.entity.BankTransferOrder;
import com.student.bank.entity.BankUser;
import com.student.bank.mapper.BankAccountMapper;
import com.student.bank.mapper.BankBatchTransferTaskMapper;
import com.student.bank.mapper.BankScheduledTransferMapper;
import com.student.bank.mapper.BankTransactionRecordMapper;
import com.student.bank.mapper.BankTransferOrderMapper;
import com.student.bank.mapper.BankUserMapper;
import com.student.bank.service.AccountService;
import com.student.bank.service.AuditService;
import com.student.bank.service.EventPublisherService;
import com.student.bank.service.NotificationService;
import com.student.bank.service.RiskService;
import com.student.bank.service.UserService;
import com.student.bank.util.SecurityUtil;
import com.student.bank.util.SensitiveDataUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final BankAccountMapper accountMapper;
    private final BankUserMapper userMapper;
    private final BankTransferOrderMapper transferOrderMapper;
    private final BankTransactionRecordMapper transactionRecordMapper;
    private final BankScheduledTransferMapper scheduledTransferMapper;
    private final BankBatchTransferTaskMapper batchTransferTaskMapper;
    private final UserService userService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final RiskService riskService;
    private final EventPublisherService eventPublisherService;
    private final LocalLockService localLockService;
    private final RateLimitService rateLimitService;

    @Value("${bank.security.single-transfer-limit:50000}")
    private BigDecimal singleTransferLimit;

    @Value("${bank.security.daily-transfer-limit:100000}")
    private BigDecimal dailyTransferLimit;

    @Value("${bank.security.large-transfer-threshold:20000}")
    private BigDecimal largeTransferThreshold;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deposit(String accountNumber, BigDecimal amount) {
        BigDecimal normalized = requireMoney(amount);
        BankAccount account = requireAccount(accountNumber);
        account.setAvailableBalance(account.getAvailableBalance().add(normalized));
        accountMapper.updateById(account);
        writeRecord(null, account, "IN", normalized, account.getAvailableBalance(), null, "系统", "DEPOSIT", "DEPOSIT", "存款");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(String accountNumber, BigDecimal amount) {
        BigDecimal normalized = requireMoney(amount);
        BankAccount account = requireAccount(accountNumber);
        assertOwnOrAdmin(account);
        if (!"NORMAL".equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账户状态不允许出账");
        }
        if (account.getAvailableBalance().compareTo(normalized) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "余额不足");
        }
        account.setAvailableBalance(account.getAvailableBalance().subtract(normalized));
        accountMapper.updateById(account);
        writeRecord(null, account, "OUT", normalized, account.getAvailableBalance(), null, "系统", "WITHDRAW", "WITHDRAW", "取款");
    }

    @Override
    public Map<String, Object> transfer(TransferDTO dto) {
        TransferConfirmDTO confirmDTO = new TransferConfirmDTO();
        confirmDTO.setFromAccount(cleanAccount(dto.getFromAccount()));
        confirmDTO.setToAccount(cleanAccount(dto.getToAccount()));
        confirmDTO.setToName(dto.getToName());
        confirmDTO.setToBankName(dto.getToBankName());
        confirmDTO.setAmount(dto.getAmount());
        confirmDTO.setRemark(dto.getRemark());
        confirmDTO.setIdempotencyKey(dto.getIdempotencyKey());
        confirmDTO.setScheduledAt(dto.getScheduledAt());
        Map<String, Object> confirmed = confirmTransfer(confirmDTO);
        TransferExecuteDTO executeDTO = new TransferExecuteDTO();
        executeDTO.setOrderNo(String.valueOf(confirmed.get("orderNo")));
        executeDTO.setTradePassword(dto.getTradePassword());
        executeDTO.setOtpCode(dto.getOtpCode());
        return executeTransfer(executeDTO);
    }

    @Override
    public BigDecimal getBalance(String accountNumber) {
        BankAccount account = requireAccount(accountNumber);
        assertOwnOrAdmin(account);
        auditService.record("QUERY_BALANCE", "ACCOUNT", accountNumber, "SUCCESS", "查询账户余额");
        return account.getAvailableBalance();
    }

    @Override
    public List<Map<String, Object>> listAccounts() {
        Long userId = AuthContext.userId();
        return accountMapper.selectList(new LambdaQueryWrapper<BankAccount>()
                        .eq(BankAccount::getUserId, userId)
                        .orderByAsc(BankAccount::getId))
                .stream().map(this::accountView).toList();
    }

    @Override
    public Map<String, Object> getAccountDetail(String accountNumber) {
        BankAccount account = requireAccount(accountNumber);
        assertOwnOrAdmin(account);
        Map<String, Object> data = accountView(account);
        data.put("accountNumberFull", account.getAccountNumber());
        data.put("recentRecords", transactionRecordMapper.selectList(new LambdaQueryWrapper<BankTransactionRecord>()
                .eq(BankTransactionRecord::getAccountNumber, account.getAccountNumber())
                .orderByDesc(BankTransactionRecord::getCreatedAt)
                .last("LIMIT 5")).stream().map(this::recordView).toList());
        auditService.record("ACCOUNT_DETAIL", "ACCOUNT", accountNumber, "SUCCESS", "查询账户详情");
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> applyAccount(AccountApplyDTO dto) {
        Long userId = AuthContext.userId();
        long count = accountMapper.selectCount(new LambdaQueryWrapper<BankAccount>().eq(BankAccount::getUserId, userId));
        if (count >= 8) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "同一用户最多申请 8 个账户");
        }
        if (dto.getTradePassword() != null && !userService.verifyTradePassword(userId, dto.getTradePassword())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "交易密码错误");
        }

        BankAccount account = new BankAccount();
        account.setAccountNumber(generateUniqueAccountNumber());
        account.setUserId(userId);
        account.setAccountType(normalizeAccountType(dto.getAccountType()));
        account.setStatus("NORMAL");
        account.setCurrency(dto.getCurrency() == null || dto.getCurrency().isBlank() ? "CNY" : dto.getCurrency().toUpperCase());
        account.setAvailableBalance(BigDecimal.ZERO.setScale(2));
        account.setFrozenBalance(BigDecimal.ZERO.setScale(2));
        account.setVersion(0);
        account.setOpenTime(LocalDateTime.now());
        account.setDeleted(0);
        accountMapper.insert(account);
        auditService.record("APPLY_ACCOUNT", "ACCOUNT", account.getAccountNumber(), "SUCCESS", "申请新账户");
        notificationService.notify(userId, "新账户已开通", "卡号尾号 " + account.getAccountNumber().substring(account.getAccountNumber().length() - 4), "ACCOUNT_OPENED");
        return accountView(account);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeAccount(String accountNumber, CloseAccountDTO dto) {
        BankAccount account = requireAccount(accountNumber);
        assertOwnOrAdmin(account);
        if (!userService.verifyPassword(toVerify(dto.getPassword()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "登录密码验证失败");
        }
        if (account.getAvailableBalance().compareTo(BigDecimal.ZERO) != 0
                || account.getFrozenBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账户余额和冻结余额必须为 0 才能销户");
        }
        long pending = transferOrderMapper.selectCount(new LambdaQueryWrapper<BankTransferOrder>()
                .eq(BankTransferOrder::getUserId, account.getUserId())
                .eq(BankTransferOrder::getFromAccount, account.getAccountNumber())
                .in(BankTransferOrder::getStatus, List.of("INIT", "PROCESSING", "SCHEDULED")));
        if (pending > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在未完成订单，禁止销户");
        }
        account.setStatus("CLOSED");
        account.setClosedAt(LocalDateTime.now());
        accountMapper.updateById(account);
        auditService.record("CLOSE_ACCOUNT", "ACCOUNT", accountNumber, "SUCCESS", dto.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeAccount(String accountNumber, FreezeAccountDTO dto) {
        BankAccount account = requireAccount(accountNumber);
        assertOwnOrAdmin(account);
        account.setStatus("FROZEN");
        accountMapper.updateById(account);
        auditService.record("FREEZE_ACCOUNT", "ACCOUNT", accountNumber, "SUCCESS", dto.getReason());
        notificationService.notify(account.getUserId(), "账户已冻结", "卡号尾号 " + accountNumber.substring(accountNumber.length() - 4) + " 已暂停出账。", "ACCOUNT_FROZEN");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeAccount(String accountNumber, FreezeAccountDTO dto) {
        BankAccount account = requireAccount(accountNumber);
        assertOwnOrAdmin(account);
        account.setStatus("NORMAL");
        accountMapper.updateById(account);
        auditService.record("UNFREEZE_ACCOUNT", "ACCOUNT", accountNumber, "SUCCESS", dto.getReason());
    }

    @Override
    public Map<String, Object> precheck(TransferPrecheckDTO dto) {
        rateLimitService.check("transfer-precheck:" + AuthContext.userId(), 30, Duration.ofMinutes(1));
        dto.setFromAccount(cleanAccount(dto.getFromAccount()));
        dto.setToAccount(cleanAccount(dto.getToAccount()));
        BankAccount to = accountMapper.selectOne(new LambdaQueryWrapper<BankAccount>()
                .eq(BankAccount::getAccountNumber, dto.getToAccount()));
        if (to == null && (dto.getToBankName() == null || dto.getToBankName().isBlank())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "收款账户不存在");
        }
        if (dto.getFromAccount() != null && dto.getFromAccount().equals(dto.getToAccount())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不允许给同一账户转账");
        }
        if (dto.getAmount() != null) {
            requireMoney(dto.getAmount());
        }
        String orderNo = SecurityUtil.generateOrderNo("PRE");
        Map<String, Object> risk = riskService.evaluateTransfer(AuthContext.userId(), dto, orderNo);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exists", to != null);
        data.put("counterpartyNameMasked", to == null ? "跨行账户" : SensitiveDataUtil.maskName(userMapper.selectById(to.getUserId()).getUserName()));
        data.put("counterpartyAccountMasked", SensitiveDataUtil.maskAccount(dto.getToAccount()));
        data.put("bankName", to == null ? dto.getToBankName() : "本行");
        data.put("fee", BigDecimal.ZERO.setScale(2));
        data.put("risk", risk);
        data.put("enhancedAuthRequired", !"PASS".equals(risk.get("action")));
        auditService.record("TRANSFER_PRECHECK", "ACCOUNT", dto.getToAccount(), "SUCCESS", "转账预校验");
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmTransfer(TransferConfirmDTO dto) {
        Long userId = AuthContext.userId();
        String fromAccount = cleanAccount(dto.getFromAccount());
        String toAccount = cleanAccount(dto.getToAccount());
        BigDecimal amount = requireMoney(dto.getAmount());
        if (fromAccount.equals(toAccount)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不允许给同一账户转账");
        }
        BankTransferOrder existing = transferOrderMapper.selectOne(new LambdaQueryWrapper<BankTransferOrder>()
                .eq(BankTransferOrder::getUserId, userId)
                .eq(BankTransferOrder::getIdempotencyKey, dto.getIdempotencyKey()));
        if (existing != null) {
            return orderView(existing);
        }

        BankAccount from = requireAccount(fromAccount);
        assertOwnOrAdmin(from);
        TransferPrecheckDTO precheck = new TransferPrecheckDTO();
        precheck.setFromAccount(fromAccount);
        precheck.setToAccount(toAccount);
        precheck.setToName(dto.getToName());
        precheck.setToBankName(dto.getToBankName());
        precheck.setAmount(amount);
        Map<String, Object> risk = riskService.evaluateTransfer(userId, precheck, null);
        String action = String.valueOf(risk.get("action"));
        if ("REJECT".equals(action) || "MANUAL_REVIEW".equals(action)) {
            throw new BusinessException(ErrorCode.RISK_REJECTED, "交易触发风控：" + action);
        }

        BankTransferOrder order = new BankTransferOrder();
        order.setOrderNo(SecurityUtil.generateOrderNo("TR"));
        order.setUserId(userId);
        order.setFromAccount(fromAccount);
        order.setToAccount(toAccount);
        order.setToBankName(dto.getToBankName() == null ? "本行" : dto.getToBankName());
        order.setAmount(amount);
        order.setFee(BigDecimal.ZERO.setScale(2));
        order.setRemark(dto.getRemark());
        order.setStatus(dto.getScheduledAt() != null && dto.getScheduledAt().isAfter(LocalDateTime.now()) ? "SCHEDULED" : "INIT");
        order.setIdempotencyKey(dto.getIdempotencyKey());
        order.setRiskAction(action);
        order.setScheduledAt(dto.getScheduledAt());
        transferOrderMapper.insert(order);
        if ("SCHEDULED".equals(order.getStatus())) {
            BankScheduledTransfer scheduled = new BankScheduledTransfer();
            scheduled.setUserId(userId);
            scheduled.setOrderNo(order.getOrderNo());
            scheduled.setFromAccount(fromAccount);
            scheduled.setToAccount(toAccount);
            scheduled.setAmount(amount);
            scheduled.setRemark(dto.getRemark());
            scheduled.setStatus("SCHEDULED");
            scheduled.setScheduledAt(dto.getScheduledAt());
            scheduledTransferMapper.insert(scheduled);
        }
        auditService.record("TRANSFER_CONFIRM", "TRANSFER_ORDER", order.getOrderNo(), "SUCCESS", "创建转账确认订单");
        return orderView(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> executeTransfer(TransferExecuteDTO dto) {
        BankTransferOrder order = requireOrder(dto.getOrderNo());
        if (!order.getUserId().equals(AuthContext.userId()) && !AuthContext.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能执行他人的转账订单");
        }
        return localLockService.withLock("transfer:" + order.getFromAccount(), Duration.ofSeconds(5),
                () -> executeTransferLocked(order, dto));
    }

    @Override
    public BankTransferOrder getOrder(String orderNo) {
        BankTransferOrder order = requireOrder(orderNo);
        if (!order.getUserId().equals(AuthContext.userId()) && !AuthContext.isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return order;
    }

    @Override
    public Map<String, Object> batchPrecheck(BatchTransferDTO dto) {
        List<Map<String, Object>> details = new ArrayList<>();
        for (TransferConfirmDTO item : dto.getItems()) {
            TransferPrecheckDTO precheckDTO = new TransferPrecheckDTO();
            precheckDTO.setFromAccount(item.getFromAccount());
            precheckDTO.setToAccount(item.getToAccount());
            precheckDTO.setAmount(item.getAmount());
            precheckDTO.setToBankName(item.getToBankName());
            try {
                Map<String, Object> result = precheck(precheckDTO);
                result.put("success", true);
                details.add(result);
            } catch (Exception e) {
                Map<String, Object> fail = new LinkedHashMap<>();
                fail.put("success", false);
                fail.put("toAccount", item.getToAccount());
                fail.put("message", e.getMessage());
                details.add(fail);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", dto.getItems().size());
        data.put("details", details);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchExecute(BatchTransferDTO dto) {
        BankBatchTransferTask task = new BankBatchTransferTask();
        task.setTaskNo(SecurityUtil.generateOrderNo("BT"));
        task.setUserId(AuthContext.userId());
        task.setTotalCount(dto.getItems().size());
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setStatus("PROCESSING");
        batchTransferTaskMapper.insert(task);

        List<Map<String, Object>> details = new ArrayList<>();
        int success = 0;
        int failed = 0;
        for (TransferConfirmDTO item : dto.getItems()) {
            try {
                Map<String, Object> confirmed = confirmTransfer(item);
                TransferExecuteDTO executeDTO = new TransferExecuteDTO();
                executeDTO.setOrderNo(String.valueOf(confirmed.get("orderNo")));
                executeDTO.setTradePassword(dto.getTradePassword());
                executeDTO.setOtpCode(dto.getOtpCode());
                Map<String, Object> result = executeTransfer(executeDTO);
                result.put("success", true);
                details.add(result);
                success++;
            } catch (Exception e) {
                Map<String, Object> fail = new LinkedHashMap<>();
                fail.put("success", false);
                fail.put("toAccount", item.getToAccount());
                fail.put("message", e.getMessage());
                details.add(fail);
                failed++;
            }
        }
        task.setSuccessCount(success);
        task.setFailedCount(failed);
        task.setStatus(failed == 0 ? "SUCCESS" : (success == 0 ? "FAILED" : "PARTIAL_SUCCESS"));
        task.setDetail(details.toString());
        batchTransferTaskMapper.updateById(task);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("taskNo", task.getTaskNo());
        data.put("status", task.getStatus());
        data.put("successCount", success);
        data.put("failedCount", failed);
        data.put("details", details);
        return data;
    }

    private Map<String, Object> executeTransferLocked(BankTransferOrder order, TransferExecuteDTO dto) {
        BankTransferOrder currentOrder = requireOrder(order.getOrderNo());
        if ("SUCCESS".equals(currentOrder.getStatus())) {
            return orderView(currentOrder);
        }
        if ("SCHEDULED".equals(currentOrder.getStatus()) && currentOrder.getScheduledAt() != null && currentOrder.getScheduledAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "定时转账未到执行时间");
        }
        if (!List.of("INIT", "SCHEDULED", "PROCESSING").contains(currentOrder.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单状态不允许执行");
        }
        if (!userService.verifyTradePassword(currentOrder.getUserId(), dto.getTradePassword())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "交易密码错误");
        }
        if ("CHALLENGE".equals(currentOrder.getRiskAction()) && !"000000".equals(dto.getOtpCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "大额或中风险交易需要短信/OTP 验证码，demo 验证码为 000000");
        }

        List<BankAccount> locked = lockAccounts(currentOrder.getFromAccount(), currentOrder.getToAccount());
        BankAccount from = locked.stream().filter(a -> a.getAccountNumber().equals(currentOrder.getFromAccount())).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAM_ERROR, "付款账户不存在"));
        BankAccount to = locked.stream().filter(a -> a.getAccountNumber().equals(currentOrder.getToAccount())).findFirst().orElse(null);
        assertOwnOrAdmin(from);
        if (!"NORMAL".equals(from.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "付款账户状态不允许出账");
        }
        if (to == null && (currentOrder.getToBankName() == null || "本行".equals(currentOrder.getToBankName()))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "收款账户不存在");
        }
        if (to != null && !"NORMAL".equals(to.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "收款账户状态不正常");
        }
        if (from.getAvailableBalance().compareTo(currentOrder.getAmount()) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "余额不足");
        }
        checkLimit(currentOrder);

        TransferPrecheckDTO precheck = new TransferPrecheckDTO();
        precheck.setFromAccount(currentOrder.getFromAccount());
        precheck.setToAccount(currentOrder.getToAccount());
        precheck.setToBankName(currentOrder.getToBankName());
        precheck.setAmount(currentOrder.getAmount());
        Map<String, Object> risk = riskService.evaluateTransfer(currentOrder.getUserId(), precheck, currentOrder.getOrderNo());
        String action = String.valueOf(risk.get("action"));
        if ("REJECT".equals(action) || "MANUAL_REVIEW".equals(action)) {
            throw new BusinessException(ErrorCode.RISK_REJECTED, "交易触发风控：" + action);
        }
        if ("CHALLENGE".equals(action) && !"000000".equals(dto.getOtpCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该交易需要增强认证，demo 验证码为 000000");
        }

        currentOrder.setStatus("PROCESSING");
        transferOrderMapper.updateById(currentOrder);
        BigDecimal fromBalance = from.getAvailableBalance().subtract(currentOrder.getAmount());
        from.setAvailableBalance(fromBalance);
        accountMapper.updateById(from);

        BankUser fromUser = userMapper.selectById(from.getUserId());
        BankUser toUser = to == null ? null : userMapper.selectById(to.getUserId());
        writeRecord(currentOrder.getOrderNo(), from, "OUT", currentOrder.getAmount(), fromBalance, currentOrder.getToAccount(),
                toUser == null ? "跨行账户" : toUser.getUserName(), "TRANSFER", classify(currentOrder.getRemark()), currentOrder.getRemark());

        if (to == null) {
            currentOrder.setStatus("PROCESSING");
            currentOrder.setFailureReason("跨行转账已提交，等待清算系统回执");
        } else {
            BigDecimal toBalance = to.getAvailableBalance().add(currentOrder.getAmount());
            to.setAvailableBalance(toBalance);
            accountMapper.updateById(to);
            writeRecord(currentOrder.getOrderNo(), to, "IN", currentOrder.getAmount(), toBalance, currentOrder.getFromAccount(),
                    fromUser.getUserName(), "TRANSFER", classify(currentOrder.getRemark()), currentOrder.getRemark());
            currentOrder.setStatus("SUCCESS");
            currentOrder.setExecutedAt(LocalDateTime.now());
            notificationService.notify(to.getUserId(), "收到转账", "您尾号 " + to.getAccountNumber().substring(to.getAccountNumber().length() - 4)
                    + " 的账户收到转账 " + currentOrder.getAmount() + " 元。", "TRANSFER_IN");
        }

        transferOrderMapper.updateById(currentOrder);
        notificationService.notify(from.getUserId(), "转账提交成功", "您尾号 " + from.getAccountNumber().substring(from.getAccountNumber().length() - 4)
                + " 的账户转出 " + currentOrder.getAmount() + " 元。", "TRANSFER_OUT");
        auditService.record("TRANSFER_EXECUTE", "TRANSFER_ORDER", currentOrder.getOrderNo(), "SUCCESS", "执行转账");
        eventPublisherService.publish("bank.transfer.transaction", "TRANSFER_" + currentOrder.getStatus(), currentOrder.getOrderNo(), orderView(currentOrder));
        log.info("transfer order {} status {}", currentOrder.getOrderNo(), currentOrder.getStatus());
        return orderView(currentOrder);
    }

    private void checkLimit(BankTransferOrder order) {
        if (order.getAmount().compareTo(singleTransferLimit) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "超过单笔限额");
        }
        LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        BigDecimal dailyUsed = transferOrderMapper.selectList(new LambdaQueryWrapper<BankTransferOrder>()
                        .eq(BankTransferOrder::getUserId, order.getUserId())
                        .eq(BankTransferOrder::getStatus, "SUCCESS")
                        .ge(BankTransferOrder::getCreatedAt, start)
                        .lt(BankTransferOrder::getCreatedAt, end))
                .stream()
                .map(BankTransferOrder::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (dailyUsed.add(order.getAmount()).compareTo(dailyTransferLimit) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "超过单日转账限额");
        }
        if (order.getAmount().compareTo(largeTransferThreshold) > 0
                && (LocalTime.now().isBefore(LocalTime.of(6, 0)) || LocalTime.now().isAfter(LocalTime.of(23, 0)))) {
            log.info("large transfer in sensitive time: {}", order.getOrderNo());
        }
    }

    private List<BankAccount> lockAccounts(String fromAccount, String toAccount) {
        List<String> accountNumbers = new ArrayList<>(List.of(fromAccount, toAccount));
        accountNumbers.sort(Comparator.naturalOrder());
        List<BankAccount> accounts = new ArrayList<>();
        for (String accountNumber : accountNumbers) {
            BankAccount account = accountMapper.selectForUpdate(accountNumber);
            if (account != null) {
                accounts.add(account);
            }
        }
        return accounts;
    }

    private void writeRecord(String orderNo, BankAccount account, String direction, BigDecimal amount, BigDecimal balanceAfter,
                             String counterpartyAccount, String counterpartyName, String transactionType, String category, String remark) {
        BankTransactionRecord record = new BankTransactionRecord();
        record.setRecordNo(SecurityUtil.generateOrderNo("RC"));
        record.setOrderNo(orderNo);
        record.setUserId(account.getUserId());
        record.setAccountNumber(account.getAccountNumber());
        record.setDirection(direction);
        record.setAmount(amount);
        record.setBalanceAfter(balanceAfter);
        record.setCounterpartyAccount(counterpartyAccount);
        record.setCounterpartyName(counterpartyName);
        record.setTransactionType(transactionType);
        record.setCategory(category == null ? "TRANSFER" : category);
        record.setRemark(remark);
        transactionRecordMapper.insert(record);
        eventPublisherService.publish("bank.transaction.log", "TRANSACTION_RECORD", record.getRecordNo(), recordView(record));
    }

    private BigDecimal requireMoney(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "金额必须大于 0");
        }
        if (!SecurityUtil.hasAtMostTwoDecimals(amount)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "金额最多支持两位小数");
        }
        return SecurityUtil.normalizeMoney(amount);
    }

    private BankAccount requireAccount(String accountNumber) {
        String cleaned = cleanAccount(accountNumber);
        BankAccount account = accountMapper.selectOne(new LambdaQueryWrapper<BankAccount>()
                .eq(BankAccount::getAccountNumber, cleaned));
        if (account == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账户不存在");
        }
        return account;
    }

    private BankTransferOrder requireOrder(String orderNo) {
        BankTransferOrder order = transferOrderMapper.selectOne(new LambdaQueryWrapper<BankTransferOrder>()
                .eq(BankTransferOrder::getOrderNo, orderNo));
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "转账订单不存在");
        }
        return order;
    }

    private void assertOwnOrAdmin(BankAccount account) {
        AuthUser user = AuthContext.get();
        if (user == null) {
            return;
        }
        if (!account.getUserId().equals(user.getUserId()) && !"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能操作本人账户");
        }
    }

    private String generateUniqueAccountNumber() {
        for (int i = 0; i < 8; i++) {
            String accountNumber = SecurityUtil.generateAccountNumber();
            if (accountMapper.selectCount(new LambdaQueryWrapper<BankAccount>()
                    .eq(BankAccount::getAccountNumber, accountNumber)) == 0) {
                return accountNumber;
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成唯一卡号失败");
    }

    private String normalizeAccountType(String type) {
        if (type == null || type.isBlank()) {
            return "CURRENT";
        }
        return switch (type.toUpperCase()) {
            case "FIXED", "定期" -> "FIXED";
            case "CREDIT_CARD", "信用卡" -> "CREDIT_CARD";
            case "LOAN", "LOAN_REPAYMENT", "贷款" -> "LOAN_REPAYMENT";
            default -> "CURRENT";
        };
    }

    private String cleanAccount(String accountNumber) {
        return accountNumber == null ? null : accountNumber.replaceAll("\\s+", "");
    }

    private com.student.bank.dto.VerifyPasswordDTO toVerify(String password) {
        com.student.bank.dto.VerifyPasswordDTO dto = new com.student.bank.dto.VerifyPasswordDTO();
        dto.setPassword(password);
        dto.setType("login");
        return dto;
    }

    private String classify(String remark) {
        if (remark == null) {
            return "TRANSFER";
        }
        String text = remark.toLowerCase();
        if (text.contains("饭") || text.contains("餐") || text.contains("咖啡")) {
            return "DINING";
        }
        if (text.contains("车") || text.contains("交通") || text.contains("打车")) {
            return "TRANSPORT";
        }
        if (text.contains("购") || text.contains("买")) {
            return "SHOPPING";
        }
        if (text.contains("理财") || text.contains("基金")) {
            return "WEALTH";
        }
        return "TRANSFER";
    }

    private Map<String, Object> accountView(BankAccount account) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accountNumber", account.getAccountNumber());
        data.put("accountNumberMasked", SensitiveDataUtil.maskAccount(account.getAccountNumber()));
        data.put("accountType", account.getAccountType());
        data.put("status", account.getStatus());
        data.put("currency", account.getCurrency());
        data.put("availableBalance", account.getAvailableBalance());
        data.put("frozenBalance", account.getFrozenBalance());
        data.put("balance", account.getAvailableBalance());
        data.put("openTime", account.getOpenTime());
        return data;
    }

    private Map<String, Object> orderView(BankTransferOrder order) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", order.getOrderNo());
        data.put("fromAccount", SensitiveDataUtil.maskAccount(order.getFromAccount()));
        data.put("toAccount", SensitiveDataUtil.maskAccount(order.getToAccount()));
        data.put("amount", order.getAmount());
        data.put("fee", order.getFee());
        data.put("remark", order.getRemark());
        data.put("status", order.getStatus());
        data.put("failureReason", order.getFailureReason());
        data.put("riskAction", order.getRiskAction());
        data.put("createdAt", order.getCreatedAt());
        data.put("executedAt", order.getExecutedAt());
        BankAccount from = accountMapper.selectOne(new LambdaQueryWrapper<BankAccount>().eq(BankAccount::getAccountNumber, order.getFromAccount()));
        if (from != null) {
            data.put("latestBalance", from.getAvailableBalance());
        }
        return data;
    }

    private Map<String, Object> recordView(BankTransactionRecord record) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recordNo", record.getRecordNo());
        data.put("orderNo", record.getOrderNo());
        data.put("accountNumber", SensitiveDataUtil.maskAccount(record.getAccountNumber()));
        data.put("direction", record.getDirection());
        data.put("amount", record.getAmount());
        data.put("balanceAfter", record.getBalanceAfter());
        data.put("counterpartyAccount", SensitiveDataUtil.maskAccount(record.getCounterpartyAccount()));
        data.put("counterpartyName", SensitiveDataUtil.maskName(record.getCounterpartyName()));
        data.put("transactionType", record.getTransactionType());
        data.put("category", record.getCategory());
        data.put("remark", record.getRemark());
        data.put("createdAt", record.getCreatedAt());
        return data;
    }
}
