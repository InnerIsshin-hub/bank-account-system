package com.student.bank.controller;

import com.student.bank.common.Result;
import com.student.bank.dto.AgentChatDTO;
import com.student.bank.dto.AgentToolInvokeDTO;
import com.student.bank.service.AgentService;
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
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {
    private final AgentService agentService;

    @GetMapping("/skills")
    public Result<List<Map<String, Object>>> skills() {
        return Result.success(agentService.listSkills());
    }

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@Valid @RequestBody AgentChatDTO dto) {
        return Result.success(agentService.chat(dto.getMessage()));
    }

    @PostMapping("/tools/invoke")
    public Result<Map<String, Object>> invoke(@Valid @RequestBody AgentToolInvokeDTO dto) {
        return Result.success(agentService.invokeSkill(dto.getSkillName(), dto.getParams()));
    }

    @PostMapping("/tools/account-summary")
    public Result<Map<String, Object>> accountSummary() {
        return Result.success(agentService.accountSummary());
    }

    @PostMapping("/tools/transfer-precheck")
    public Result<Map<String, Object>> transferPrecheck(@RequestBody Map<String, Object> params) {
        return Result.success(agentService.transferPrecheck(params));
    }

    @PostMapping("/tools/create-transfer-draft")
    public Result<Map<String, Object>> createTransferDraft(@RequestBody Map<String, Object> params) {
        return Result.success(agentService.createTransferDraft(params));
    }

    @PostMapping("/tools/bill-analysis")
    public Result<Map<String, Object>> billAnalysis() {
        return Result.success(agentService.billAnalysis());
    }
}
