package com.student.bank.controller;

import com.student.bank.common.Result;
import com.student.bank.dto.LoanApplyDTO;
import com.student.bank.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loanService;

    @PostMapping("/apply")
    public Result<Map<String, Object>> apply(@Valid @RequestBody LoanApplyDTO dto) {
        return Result.success(loanService.apply(dto));
    }

    @GetMapping
    public Result<List<Map<String, Object>>> myApplications() {
        return Result.success(loanService.myApplications());
    }

    @GetMapping("/{loanApplicationId}/repayment-plans")
    public Result<List<Map<String, Object>>> plans(@PathVariable Long loanApplicationId) {
        return Result.success(loanService.repaymentPlans(loanApplicationId));
    }
}
