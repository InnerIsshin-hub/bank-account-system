package com.student.bank.service;

import com.student.bank.dto.LoanApplyDTO;

import java.util.List;
import java.util.Map;

public interface LoanService {
    Map<String, Object> apply(LoanApplyDTO dto);
    List<Map<String, Object>> myApplications();
    List<Map<String, Object>> repaymentPlans(Long loanApplicationId);
}
