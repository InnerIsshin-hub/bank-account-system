package com.student.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.bank.common.AuthContext;
import com.student.bank.dto.LoanApplyDTO;
import com.student.bank.entity.BankLoanApplication;
import com.student.bank.entity.BankLoanRepaymentPlan;
import com.student.bank.mapper.BankLoanApplicationMapper;
import com.student.bank.mapper.BankLoanRepaymentPlanMapper;
import com.student.bank.service.AccountService;
import com.student.bank.service.AuditService;
import com.student.bank.service.LoanService;
import com.student.bank.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {
    private final BankLoanApplicationMapper loanMapper;
    private final BankLoanRepaymentPlanMapper planMapper;
    private final AccountService accountService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> apply(LoanApplyDTO dto) {
        BigDecimal score = BigDecimal.valueOf(100).subtract(dto.getAmount().divide(BigDecimal.valueOf(2000), 2, RoundingMode.HALF_UP));
        String status = score.compareTo(BigDecimal.valueOf(70)) >= 0 ? "APPROVED" : "MANUAL_REVIEW";
        BankLoanApplication app = new BankLoanApplication();
        app.setUserId(AuthContext.userId());
        app.setProductCode(dto.getProductCode());
        app.setApplicantName(dto.getApplicantName());
        app.setAmount(dto.getAmount());
        app.setTermMonths(dto.getTermMonths());
        app.setPurpose(dto.getPurpose());
        app.setAutoScore(score.max(BigDecimal.ZERO));
        app.setStatus(status);
        app.setReviewComment("APPROVED".equals(status) ? "自动审批通过" : "进入人工复核");
        loanMapper.insert(app);
        createPlans(app);
        if ("APPROVED".equals(status) && dto.getReceiveAccount() != null && !dto.getReceiveAccount().isBlank()) {
            accountService.deposit(dto.getReceiveAccount(), dto.getAmount());
        }
        auditService.record("LOAN_APPLY", "LOAN", String.valueOf(app.getId()), "SUCCESS", status);
        notificationService.notify(AuthContext.userId(), "贷款申请已提交", "当前状态：" + status, "LOAN_APPLY");
        return loanView(app);
    }

    @Override
    public List<Map<String, Object>> myApplications() {
        return loanMapper.selectList(new LambdaQueryWrapper<BankLoanApplication>()
                        .eq(BankLoanApplication::getUserId, AuthContext.userId())
                        .orderByDesc(BankLoanApplication::getCreatedAt))
                .stream().map(this::loanView).toList();
    }

    @Override
    public List<Map<String, Object>> repaymentPlans(Long loanApplicationId) {
        return planMapper.selectList(new LambdaQueryWrapper<BankLoanRepaymentPlan>()
                        .eq(BankLoanRepaymentPlan::getUserId, AuthContext.userId())
                        .eq(BankLoanRepaymentPlan::getLoanApplicationId, loanApplicationId)
                        .orderByAsc(BankLoanRepaymentPlan::getPeriodNo))
                .stream().map(this::planView).toList();
    }

    private void createPlans(BankLoanApplication app) {
        BigDecimal principal = app.getAmount().divide(BigDecimal.valueOf(app.getTermMonths()), 2, RoundingMode.HALF_UP);
        BigDecimal interest = app.getAmount().multiply(BigDecimal.valueOf(0.004)).setScale(2, RoundingMode.HALF_UP);
        for (int i = 1; i <= app.getTermMonths(); i++) {
            BankLoanRepaymentPlan plan = new BankLoanRepaymentPlan();
            plan.setLoanApplicationId(app.getId());
            plan.setUserId(app.getUserId());
            plan.setPeriodNo(i);
            plan.setDueDate(LocalDate.now().plusMonths(i));
            plan.setPrincipal(principal);
            plan.setInterest(interest);
            plan.setTotalAmount(principal.add(interest));
            plan.setStatus("UNPAID");
            planMapper.insert(plan);
        }
    }

    private Map<String, Object> loanView(BankLoanApplication app) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", app.getId());
        data.put("productCode", app.getProductCode());
        data.put("amount", app.getAmount());
        data.put("termMonths", app.getTermMonths());
        data.put("autoScore", app.getAutoScore());
        data.put("status", app.getStatus());
        data.put("reviewComment", app.getReviewComment());
        data.put("createdAt", app.getCreatedAt());
        return data;
    }

    private Map<String, Object> planView(BankLoanRepaymentPlan plan) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", plan.getId());
        data.put("periodNo", plan.getPeriodNo());
        data.put("dueDate", plan.getDueDate());
        data.put("principal", plan.getPrincipal());
        data.put("interest", plan.getInterest());
        data.put("totalAmount", plan.getTotalAmount());
        data.put("status", plan.getStatus());
        return data;
    }
}
