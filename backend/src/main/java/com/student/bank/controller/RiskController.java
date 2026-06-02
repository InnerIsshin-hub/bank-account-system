package com.student.bank.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.bank.common.AuthContext;
import com.student.bank.common.Result;
import com.student.bank.dto.TransferPrecheckDTO;
import com.student.bank.entity.BankRiskEvent;
import com.student.bank.mapper.BankRiskEventMapper;
import com.student.bank.service.RiskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {
    private final RiskService riskService;
    private final BankRiskEventMapper riskEventMapper;

    @PostMapping("/evaluate")
    public Result<Map<String, Object>> evaluate(@Valid @RequestBody TransferPrecheckDTO dto) {
        return Result.success(riskService.evaluateTransfer(AuthContext.userId(), dto, null));
    }

    @GetMapping("/events")
    public Result<List<BankRiskEvent>> events() {
        AuthContext.requireAdmin();
        return Result.success(riskEventMapper.selectList(new LambdaQueryWrapper<BankRiskEvent>()
                .orderByDesc(BankRiskEvent::getCreatedAt)
                .last("LIMIT 200")));
    }
}
