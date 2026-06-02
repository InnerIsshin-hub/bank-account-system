package com.student.bank.controller;

import com.student.bank.common.AuthContext;
import com.student.bank.common.Result;
import com.student.bank.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/ops")
@RequiredArgsConstructor
public class OpsController {
    private final ReconciliationService reconciliationService;

    @PostMapping("/reconciliation/run")
    public Result<Map<String, Object>> reconciliation(@RequestParam(required = false)
                                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AuthContext.requireAdmin();
        return Result.success(reconciliationService.runDaily(date == null ? LocalDate.now() : date));
    }

    @PostMapping("/compensation/scan")
    public Result<Map<String, Object>> compensate() {
        AuthContext.requireAdmin();
        return Result.success(reconciliationService.scanAndCompensate());
    }
}
