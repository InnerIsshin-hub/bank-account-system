package com.student.bank.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.bank.entity.BankAccount;
import com.student.bank.entity.BankContact;
import com.student.bank.entity.BankCreditBill;
import com.student.bank.entity.BankCreditCard;
import com.student.bank.entity.BankHolding;
import com.student.bank.entity.BankKycRecord;
import com.student.bank.entity.BankLoanApplication;
import com.student.bank.entity.BankLoanRepaymentPlan;
import com.student.bank.entity.BankNotification;
import com.student.bank.entity.BankNotificationSendRecord;
import com.student.bank.entity.BankPasswordHistory;
import com.student.bank.entity.BankProduct;
import com.student.bank.entity.BankTransactionRecord;
import com.student.bank.entity.BankTransferOrder;
import com.student.bank.entity.BankUser;
import com.student.bank.entity.BankUserSecurity;
import com.student.bank.mapper.BankAccountMapper;
import com.student.bank.mapper.BankContactMapper;
import com.student.bank.mapper.BankCreditBillMapper;
import com.student.bank.mapper.BankCreditCardMapper;
import com.student.bank.mapper.BankHoldingMapper;
import com.student.bank.mapper.BankKycRecordMapper;
import com.student.bank.mapper.BankLoanApplicationMapper;
import com.student.bank.mapper.BankLoanRepaymentPlanMapper;
import com.student.bank.mapper.BankNotificationMapper;
import com.student.bank.mapper.BankNotificationSendRecordMapper;
import com.student.bank.mapper.BankPasswordHistoryMapper;
import com.student.bank.mapper.BankProductMapper;
import com.student.bank.mapper.BankTransactionRecordMapper;
import com.student.bank.mapper.BankTransferOrderMapper;
import com.student.bank.mapper.BankUserMapper;
import com.student.bank.mapper.BankUserSecurityMapper;
import com.student.bank.util.SecurityUtil;
import com.student.bank.util.SensitiveDataUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bank.demo-data", name = "enabled", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {
    private static final String DEMO_PASSWORD = "Demo@123";
    private static final String DEMO_TRADE_PASSWORD = "123456";
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String ADMIN_TRADE_PASSWORD = "654321";

    private final BankUserMapper userMapper;
    private final BankUserSecurityMapper securityMapper;
    private final BankPasswordHistoryMapper passwordHistoryMapper;
    private final BankKycRecordMapper kycRecordMapper;
    private final BankAccountMapper accountMapper;
    private final BankContactMapper contactMapper;
    private final BankNotificationMapper notificationMapper;
    private final BankNotificationSendRecordMapper sendRecordMapper;
    private final BankTransactionRecordMapper recordMapper;
    private final BankTransferOrderMapper orderMapper;
    private final BankHoldingMapper holdingMapper;
    private final BankLoanApplicationMapper loanMapper;
    private final BankLoanRepaymentPlanMapper planMapper;
    private final BankCreditCardMapper cardMapper;
    private final BankCreditBillMapper billMapper;
    private final BankProductMapper productMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        ensureProducts();

        DemoUser zhang = ensureUser("张明", "110101199001010011", "13800000011", "USER", DEMO_PASSWORD, DEMO_TRADE_PASSWORD);
        DemoUser li = ensureUser("李娜", "110101199002020022", "13800000022", "USER", DEMO_PASSWORD, DEMO_TRADE_PASSWORD);
        DemoUser admin = ensureUser("系统管理员", "110101198801010099", "13900000099", "ADMIN", ADMIN_PASSWORD, ADMIN_TRADE_PASSWORD);

        ensureAccount(zhang.userId(), "621700000000000001", "CURRENT", new BigDecimal("68200.50"));
        ensureAccount(zhang.userId(), "621700000000000002", "FIXED", new BigDecimal("20000.00"));
        ensureAccount(li.userId(), "621700000000000003", "CURRENT", new BigDecimal("35200.75"));
        ensureAccount(admin.userId(), "621700000000000099", "CURRENT", new BigDecimal("500000.00"));

        ensureContact(zhang.userId(), "李娜", "621700000000000003", "本行", "13800000022");
        ensureContact(zhang.userId(), "王房东", "622200000000000088", "示范他行", "13800000088");
        ensureContact(li.userId(), "张明", "621700000000000001", "本行", "13800000011");

        seedZhangScenario(zhang.userId());
        seedLiScenario(li.userId());
        seedAdminNotification(admin.userId());
        log.info("demo data ready: users={}, accounts={}", userMapper.selectCount(new LambdaQueryWrapper<>()),
                accountMapper.selectCount(new LambdaQueryWrapper<>()));
    }

    private void ensureProducts() {
        ensureProduct("DEP-3M", "三个月定期存款", "DEPOSIT", "LOW", 90, "1.8500", "1000.00", "到期自动转回活期账户");
        ensureProduct("DEP-1Y", "一年期稳健定存", "DEPOSIT", "LOW", 365, "2.2500", "1000.00", "提前支取按活期规则模拟计息");
        ensureProduct("WM-BALANCED", "稳健理财 A", "WEALTH", "MEDIUM", 180, "3.2000", "5000.00", "非保本浮动收益，购买前需确认风险提示");
        ensureProduct("LOAN-CONSUME", "消费贷", "LOAN", "MEDIUM", 365, "4.8000", "10000.00", "自动评分后生成还款计划");
        ensureProduct("CARD-GOLD", "金卡信用卡", "CREDIT_CARD", "MEDIUM", 0, "0.0000", "0.00", "自动审批额度，支持账单和分期 demo");
    }

    private void ensureProduct(String code, String name, String type, String risk, int termDays,
                               String yield, String minAmount, String description) {
        if (productMapper.selectCount(new LambdaQueryWrapper<BankProduct>().eq(BankProduct::getProductCode, code)) > 0) {
            return;
        }
        BankProduct product = new BankProduct();
        product.setProductCode(code);
        product.setProductName(name);
        product.setProductType(type);
        product.setRiskLevel(risk);
        product.setTermDays(termDays);
        product.setExpectedYield(new BigDecimal(yield));
        product.setMinAmount(new BigDecimal(minAmount));
        product.setStatus("ON_SHELF");
        product.setDescription(description);
        productMapper.insert(product);
    }

    private DemoUser ensureUser(String name, String idCard, String phone, String role, String loginPassword, String tradePassword) {
        String idHash = SensitiveDataUtil.hash(idCard);
        BankUser existing = userMapper.selectOne(new LambdaQueryWrapper<BankUser>().eq(BankUser::getIdCardHash, idHash));
        if (existing != null) {
            return new DemoUser(existing.getId(), idCard);
        }
        BankUser user = new BankUser();
        user.setUserName(name);
        user.setIdCard(SensitiveDataUtil.encrypt(idCard));
        user.setIdCardHash(idHash);
        user.setPhone(SensitiveDataUtil.encrypt(phone));
        user.setPhoneHash(SensitiveDataUtil.hash(phone));
        user.setStatus("NORMAL");
        user.setKycStatus("VERIFIED");
        user.setRole(role);
        user.setTokenVersion(0);
        user.setDeleted(0);
        userMapper.insert(user);

        BankUserSecurity security = new BankUserSecurity();
        security.setUserId(user.getId());
        security.setLoginPasswordHash(SecurityUtil.encode(loginPassword));
        security.setTradePasswordHash(SecurityUtil.encode(tradePassword));
        security.setLoginFailCount(0);
        security.setOtpEnabled(1);
        security.setOtpSecret("DEMO-OTP-" + user.getId());
        securityMapper.insert(security);

        BankPasswordHistory history = new BankPasswordHistory();
        history.setUserId(user.getId());
        history.setPasswordHash(security.getLoginPasswordHash());
        passwordHistoryMapper.insert(history);

        BankKycRecord kyc = new BankKycRecord();
        kyc.setUserId(user.getId());
        kyc.setIdCardHash(idHash);
        kyc.setFrontFileRef("demo/kyc/" + user.getId() + "-front.jpg");
        kyc.setBackFileRef("demo/kyc/" + user.getId() + "-back.jpg");
        kyc.setFaceResult("SIMULATED_PASS");
        kyc.setFaceScore(new BigDecimal("0.9820"));
        kyc.setChannel("DEMO_BOOTSTRAP");
        kyc.setStatus("VERIFIED");
        kyc.setReviewComment("演示数据自动通过实名核验");
        kyc.setReviewedAt(LocalDateTime.now().minusDays(30));
        kycRecordMapper.insert(kyc);
        return new DemoUser(user.getId(), idCard);
    }

    private void ensureAccount(Long userId, String accountNumber, String type, BigDecimal balance) {
        if (accountMapper.selectCount(new LambdaQueryWrapper<BankAccount>().eq(BankAccount::getAccountNumber, accountNumber)) > 0) {
            return;
        }
        BankAccount account = new BankAccount();
        account.setAccountNumber(accountNumber);
        account.setUserId(userId);
        account.setAccountType(type);
        account.setStatus("NORMAL");
        account.setCurrency("CNY");
        account.setAvailableBalance(balance.setScale(2, RoundingMode.HALF_UP));
        account.setFrozenBalance(BigDecimal.ZERO.setScale(2));
        account.setVersion(0);
        account.setOpenTime(LocalDateTime.now().minusDays(45));
        account.setDeleted(0);
        accountMapper.insert(account);
    }

    private void ensureContact(Long userId, String name, String accountNumber, String bankName, String phone) {
        if (contactMapper.selectCount(new LambdaQueryWrapper<BankContact>()
                .eq(BankContact::getUserId, userId)
                .eq(BankContact::getAccountNumber, accountNumber)) > 0) {
            return;
        }
        BankContact contact = new BankContact();
        contact.setUserId(userId);
        contact.setContactName(name);
        contact.setAccountNumber(accountNumber);
        contact.setBankName(bankName);
        contact.setPhone(SensitiveDataUtil.encrypt(phone));
        contact.setDeleted(0);
        contactMapper.insert(contact);
    }

    private void seedZhangScenario(Long userId) {
        if (recordMapper.selectCount(new LambdaQueryWrapper<BankTransactionRecord>().eq(BankTransactionRecord::getUserId, userId)) == 0) {
            insertRecord("RC-DEMO-ZHANG-001", null, userId, "621700000000000001", "IN", "32000.00", "76200.50",
                    null, "工资代发", "DEPOSIT", "INCOME", "五月工资", 18);
            insertRecord("RC-DEMO-ZHANG-002", "TR-DEMO-RENT-001", userId, "621700000000000001", "OUT", "4200.00", "72000.50",
                    "622200000000000088", "王房东", "TRANSFER", "TRANSFER", "房租", 12);
            insertRecord("RC-DEMO-ZHANG-003", "TR-DEMO-LINA-001", userId, "621700000000000001", "OUT", "580.00", "71420.50",
                    "621700000000000003", "李娜", "TRANSFER", "DINING", "聚餐 AA", 1);
            insertRecord("RC-DEMO-ZHANG-004", null, userId, "621700000000000001", "OUT", "128.00", "71292.50",
                    null, "城市交通", "WITHDRAW", "TRANSPORT", "通勤交通", 1);
            insertRecord("RC-DEMO-ZHANG-005", null, userId, "621700000000000001", "OUT", "899.00", "70393.50",
                    null, "线上商城", "WITHDRAW", "SHOPPING", "电子产品配件", 0);
        }
        ensureOrder(userId, "TR-DEMO-LINA-001", "621700000000000001", "621700000000000003", "580.00", "聚餐 AA", "SUCCESS");
        ensureHolding(userId, "621700000000000001", "WM-BALANCED", "WEALTH", "8000.00", 40, 180);
        ensureLoan(userId);
        ensureCreditCard(userId);
        ensureNotification(userId, "演示账户已就绪", "可使用张明账号体验转账、账单分析、理财、贷款和信用卡流程。", "DEMO_READY");
    }

    private void seedLiScenario(Long userId) {
        if (recordMapper.selectCount(new LambdaQueryWrapper<BankTransactionRecord>().eq(BankTransactionRecord::getUserId, userId)) == 0) {
            insertRecord("RC-DEMO-LI-001", "TR-DEMO-LINA-001", userId, "621700000000000003", "IN", "580.00", "35200.75",
                    "621700000000000001", "张明", "TRANSFER", "DINING", "聚餐 AA", 5);
            insertRecord("RC-DEMO-LI-002", null, userId, "621700000000000003", "OUT", "260.00", "34940.75",
                    null, "咖啡与轻食", "WITHDRAW", "DINING", "咖啡", 0);
        }
        ensureNotification(userId, "收到转账", "尾号 0003 的账户收到 580.00 元。", "TRANSFER_IN");
    }

    private void seedAdminNotification(Long userId) {
        ensureNotification(userId, "后台数据已初始化", "当前演示库包含客户、账户、交易、风控、通知、贷款和信用卡数据。", "ADMIN_DEMO_READY");
    }

    private void insertRecord(String recordNo, String orderNo, Long userId, String accountNumber, String direction,
                              String amount, String balanceAfter, String counterpartyAccount, String counterpartyName,
                              String transactionType, String category, String remark, int daysAgo) {
        BankTransactionRecord record = new BankTransactionRecord();
        record.setRecordNo(recordNo);
        record.setOrderNo(orderNo);
        record.setUserId(userId);
        record.setAccountNumber(accountNumber);
        record.setDirection(direction);
        record.setAmount(new BigDecimal(amount));
        record.setBalanceAfter(new BigDecimal(balanceAfter));
        record.setCounterpartyAccount(counterpartyAccount);
        record.setCounterpartyName(counterpartyName);
        record.setTransactionType(transactionType);
        record.setCategory(category);
        record.setRemark(remark);
        record.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        recordMapper.insert(record);
    }

    private void ensureOrder(Long userId, String orderNo, String fromAccount, String toAccount, String amount, String remark, String status) {
        if (orderMapper.selectCount(new LambdaQueryWrapper<BankTransferOrder>().eq(BankTransferOrder::getOrderNo, orderNo)) > 0) {
            return;
        }
        BankTransferOrder order = new BankTransferOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setFromAccount(fromAccount);
        order.setToAccount(toAccount);
        order.setToBankName("本行");
        order.setAmount(new BigDecimal(amount));
        order.setFee(BigDecimal.ZERO.setScale(2));
        order.setRemark(remark);
        order.setStatus(status);
        order.setIdempotencyKey("demo-" + orderNo);
        order.setRiskAction("PASS");
        order.setExecutedAt(LocalDateTime.now().minusDays(5));
        orderMapper.insert(order);
    }

    private void ensureHolding(Long userId, String accountNumber, String productCode, String productType,
                               String amount, int daysAgo, int termDays) {
        if (holdingMapper.selectCount(new LambdaQueryWrapper<BankHolding>()
                .eq(BankHolding::getUserId, userId)
                .eq(BankHolding::getProductCode, productCode)) > 0) {
            return;
        }
        BankHolding holding = new BankHolding();
        holding.setUserId(userId);
        holding.setAccountNumber(accountNumber);
        holding.setProductCode(productCode);
        holding.setProductType(productType);
        holding.setAmount(new BigDecimal(amount));
        holding.setStatus("HOLDING");
        holding.setStartAt(LocalDateTime.now().minusDays(daysAgo));
        holding.setMaturityAt(LocalDateTime.now().minusDays(daysAgo).plusDays(termDays));
        holdingMapper.insert(holding);
    }

    private void ensureLoan(Long userId) {
        if (loanMapper.selectCount(new LambdaQueryWrapper<BankLoanApplication>()
                .eq(BankLoanApplication::getUserId, userId)
                .eq(BankLoanApplication::getProductCode, "LOAN-CONSUME")) > 0) {
            return;
        }
        BankLoanApplication loan = new BankLoanApplication();
        loan.setUserId(userId);
        loan.setProductCode("LOAN-CONSUME");
        loan.setApplicantName("张明");
        loan.setAmount(new BigDecimal("30000.00"));
        loan.setTermMonths(12);
        loan.setPurpose("家电装修");
        loan.setAutoScore(new BigDecimal("85.50"));
        loan.setStatus("APPROVED");
        loan.setReviewComment("演示自动审批通过");
        loanMapper.insert(loan);
        for (int i = 1; i <= 12; i++) {
            BankLoanRepaymentPlan plan = new BankLoanRepaymentPlan();
            plan.setLoanApplicationId(loan.getId());
            plan.setUserId(userId);
            plan.setPeriodNo(i);
            plan.setDueDate(LocalDate.now().plusMonths(i));
            plan.setPrincipal(new BigDecimal("2500.00"));
            plan.setInterest(new BigDecimal("120.00"));
            plan.setTotalAmount(new BigDecimal("2620.00"));
            plan.setStatus("UNPAID");
            planMapper.insert(plan);
        }
    }

    private void ensureCreditCard(Long userId) {
        String cardNumber = "621700000000000801";
        if (cardMapper.selectCount(new LambdaQueryWrapper<BankCreditCard>().eq(BankCreditCard::getCardNumber, cardNumber)) > 0) {
            return;
        }
        BankCreditCard card = new BankCreditCard();
        card.setUserId(userId);
        card.setCardNumber(cardNumber);
        card.setProductCode("CARD-GOLD");
        card.setCreditLimit(new BigDecimal("50000.00"));
        card.setUsedAmount(new BigDecimal("3280.00"));
        card.setPoints(2680);
        card.setStatus("ACTIVE");
        card.setBillDay(10);
        card.setRepaymentDay(25);
        cardMapper.insert(card);

        BankCreditBill bill = new BankCreditBill();
        bill.setCreditCardId(card.getId());
        bill.setUserId(userId);
        bill.setBillMonth(YearMonth.now().toString());
        bill.setBillAmount(new BigDecimal("3280.00"));
        bill.setMinRepayment(new BigDecimal("328.00"));
        bill.setDueDate(LocalDate.now().withDayOfMonth(Math.min(25, LocalDate.now().lengthOfMonth())));
        bill.setStatus("UNPAID");
        billMapper.insert(bill);
    }

    private void ensureNotification(Long userId, String title, String content, String businessType) {
        if (notificationMapper.selectCount(new LambdaQueryWrapper<BankNotification>()
                .eq(BankNotification::getUserId, userId)
                .eq(BankNotification::getBusinessType, businessType)
                .eq(BankNotification::getTitle, title)) > 0) {
            return;
        }
        BankNotification notification = new BankNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setChannel("IN_APP");
        notification.setBusinessType(businessType);
        notification.setReadFlag(0);
        notificationMapper.insert(notification);

        BankNotificationSendRecord send = new BankNotificationSendRecord();
        send.setNotificationId(notification.getId());
        send.setUserId(userId);
        send.setChannel("PUSH");
        send.setTemplateCode(businessType);
        send.setPayload(title + " - " + content);
        send.setSendStatus("SUCCESS");
        send.setRetryCount(0);
        sendRecordMapper.insert(send);
    }

    private record DemoUser(Long userId, String idCard) {
    }
}
