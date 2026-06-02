package com.student.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.bank.common.AuthContext;
import com.student.bank.common.BusinessException;
import com.student.bank.common.ErrorCode;
import com.student.bank.dto.CreditCardApplyDTO;
import com.student.bank.entity.BankCreditBill;
import com.student.bank.entity.BankCreditCard;
import com.student.bank.mapper.BankCreditBillMapper;
import com.student.bank.mapper.BankCreditCardMapper;
import com.student.bank.service.AuditService;
import com.student.bank.service.CreditCardService;
import com.student.bank.service.NotificationService;
import com.student.bank.service.UserService;
import com.student.bank.util.SecurityUtil;
import com.student.bank.util.SensitiveDataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreditCardServiceImpl implements CreditCardService {
    private final BankCreditCardMapper cardMapper;
    private final BankCreditBillMapper billMapper;
    private final UserService userService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> apply(CreditCardApplyDTO dto) {
        BigDecimal income = dto.getMonthlyIncome() == null ? BigDecimal.valueOf(8000) : dto.getMonthlyIncome();
        BigDecimal limit = income.multiply(BigDecimal.valueOf(3)).min(BigDecimal.valueOf(100000)).setScale(2, RoundingMode.HALF_UP);
        BankCreditCard card = new BankCreditCard();
        card.setUserId(AuthContext.userId());
        card.setCardNumber(SecurityUtil.generateAccountNumber());
        card.setProductCode(dto.getProductCode());
        card.setCreditLimit(limit);
        card.setUsedAmount(BigDecimal.ZERO.setScale(2));
        card.setPoints(0);
        card.setStatus("PENDING_ACTIVATION");
        card.setBillDay(10);
        card.setRepaymentDay(25);
        cardMapper.insert(card);
        createCurrentBill(card);
        auditService.record("CREDIT_CARD_APPLY", "CREDIT_CARD", String.valueOf(card.getId()), "SUCCESS", "信用卡申请");
        notificationService.notify(AuthContext.userId(), "信用卡审批通过", "请在信用卡中心激活卡片。", "CREDIT_CARD");
        return cardView(card);
    }

    @Override
    public List<Map<String, Object>> listCards() {
        return cardMapper.selectList(new LambdaQueryWrapper<BankCreditCard>()
                        .eq(BankCreditCard::getUserId, AuthContext.userId())
                        .orderByDesc(BankCreditCard::getCreatedAt))
                .stream().map(this::cardView).toList();
    }

    @Override
    public void activate(Long cardId, String tradePassword) {
        BankCreditCard card = cardMapper.selectById(cardId);
        if (card == null || !card.getUserId().equals(AuthContext.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!userService.verifyTradePassword(AuthContext.userId(), tradePassword)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "交易密码错误");
        }
        card.setStatus("ACTIVE");
        cardMapper.updateById(card);
        auditService.record("CREDIT_CARD_ACTIVATE", "CREDIT_CARD", String.valueOf(cardId), "SUCCESS", "激活信用卡");
    }

    @Override
    public List<Map<String, Object>> bills(Long cardId) {
        BankCreditCard card = cardMapper.selectById(cardId);
        if (card == null || !card.getUserId().equals(AuthContext.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return billMapper.selectList(new LambdaQueryWrapper<BankCreditBill>()
                        .eq(BankCreditBill::getCreditCardId, cardId)
                        .orderByDesc(BankCreditBill::getBillMonth))
                .stream().map(this::billView).toList();
    }

    @Override
    public Map<String, Object> installment(Long billId, Integer periods) {
        BankCreditBill bill = billMapper.selectById(billId);
        if (bill == null || !bill.getUserId().equals(AuthContext.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        int p = periods == null ? 3 : periods;
        BigDecimal fee = bill.getBillAmount().multiply(BigDecimal.valueOf(0.006)).multiply(BigDecimal.valueOf(p)).setScale(2, RoundingMode.HALF_UP);
        Map<String, Object> data = billView(bill);
        data.put("periods", p);
        data.put("monthlyPrincipal", bill.getBillAmount().divide(BigDecimal.valueOf(p), 2, RoundingMode.HALF_UP));
        data.put("totalFee", fee);
        auditService.record("CREDIT_CARD_INSTALLMENT", "CREDIT_BILL", String.valueOf(billId), "SUCCESS", "信用卡分期");
        return data;
    }

    private void createCurrentBill(BankCreditCard card) {
        BankCreditBill bill = new BankCreditBill();
        bill.setCreditCardId(card.getId());
        bill.setUserId(card.getUserId());
        bill.setBillMonth(YearMonth.now().toString());
        bill.setBillAmount(BigDecimal.ZERO.setScale(2));
        bill.setMinRepayment(BigDecimal.ZERO.setScale(2));
        bill.setDueDate(LocalDate.now().withDayOfMonth(Math.min(25, LocalDate.now().lengthOfMonth())));
        bill.setStatus("UNPAID");
        billMapper.insert(bill);
    }

    private Map<String, Object> cardView(BankCreditCard card) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", card.getId());
        data.put("cardNumber", SensitiveDataUtil.maskAccount(card.getCardNumber()));
        data.put("productCode", card.getProductCode());
        data.put("creditLimit", card.getCreditLimit());
        data.put("usedAmount", card.getUsedAmount());
        data.put("points", card.getPoints());
        data.put("status", card.getStatus());
        data.put("billDay", card.getBillDay());
        data.put("repaymentDay", card.getRepaymentDay());
        return data;
    }

    private Map<String, Object> billView(BankCreditBill bill) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", bill.getId());
        data.put("billMonth", bill.getBillMonth());
        data.put("billAmount", bill.getBillAmount());
        data.put("minRepayment", bill.getMinRepayment());
        data.put("dueDate", bill.getDueDate());
        data.put("status", bill.getStatus());
        return data;
    }
}
