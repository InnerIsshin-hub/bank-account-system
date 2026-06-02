package com.student.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.bank.dto.TransferPrecheckDTO;
import com.student.bank.entity.BankContact;
import com.student.bank.entity.BankRiskEvent;
import com.student.bank.mapper.BankContactMapper;
import com.student.bank.mapper.BankRiskEventMapper;
import com.student.bank.service.RiskService;
import com.student.bank.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiskServiceImpl implements RiskService {
    private final BankRiskEventMapper riskEventMapper;
    private final BankContactMapper contactMapper;

    @Value("${bank.security.single-transfer-limit:50000}")
    private BigDecimal singleTransferLimit;

    @Value("${bank.security.large-transfer-threshold:20000}")
    private BigDecimal largeTransferThreshold;

    @Override
    public Map<String, Object> evaluateTransfer(Long userId, TransferPrecheckDTO dto, String orderNo) {
        BigDecimal amount = dto.getAmount() == null ? BigDecimal.ZERO : SecurityUtil.normalizeMoney(dto.getAmount());
        List<String> reasons = new ArrayList<>();
        BigDecimal score = BigDecimal.TEN;

        if (amount.compareTo(singleTransferLimit) > 0) {
            score = score.add(BigDecimal.valueOf(80));
            reasons.add("超过单笔限额");
        } else if (amount.compareTo(largeTransferThreshold) > 0) {
            score = score.add(BigDecimal.valueOf(35));
            reasons.add("大额转账需增强认证");
        }

        LocalTime now = LocalTime.now();
        if (now.isAfter(LocalTime.of(0, 0)) && now.isBefore(LocalTime.of(5, 0))) {
            score = score.add(BigDecimal.valueOf(20));
            reasons.add("凌晨敏感时段交易");
        }

        Long contactCount = contactMapper.selectCount(new LambdaQueryWrapper<BankContact>()
                .eq(BankContact::getUserId, userId)
                .eq(BankContact::getAccountNumber, dto.getToAccount()));
        if (contactCount == 0 && amount.compareTo(BigDecimal.valueOf(5000)) > 0) {
            score = score.add(BigDecimal.valueOf(15));
            reasons.add("陌生收款人大额交易");
        }

        String action;
        String level;
        if (score.compareTo(BigDecimal.valueOf(90)) >= 0) {
            action = "REJECT";
            level = "HIGH";
        } else if (score.compareTo(BigDecimal.valueOf(70)) >= 0) {
            action = "MANUAL_REVIEW";
            level = "HIGH";
        } else if (score.compareTo(BigDecimal.valueOf(40)) >= 0) {
            action = "CHALLENGE";
            level = "MEDIUM";
        } else {
            action = "PASS";
            level = "LOW";
        }

        if (!"PASS".equals(action)) {
            BankRiskEvent event = new BankRiskEvent();
            event.setEventId(SecurityUtil.generateOrderNo("RISK"));
            event.setUserId(userId);
            event.setAccountNumber(dto.getFromAccount());
            event.setOrderNo(orderNo);
            event.setRiskType("TRANSFER_RULE");
            event.setRiskScore(score);
            event.setRiskLevel(level);
            event.setAction(action);
            event.setReason(String.join("；", reasons));
            event.setStatus("OPEN");
            riskEventMapper.insert(event);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("action", action);
        data.put("riskLevel", level);
        data.put("riskScore", score);
        data.put("reasons", reasons);
        return data;
    }
}
