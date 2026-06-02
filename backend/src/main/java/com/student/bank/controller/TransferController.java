package com.student.bank.controller;

import com.student.bank.common.Result;
import com.student.bank.dto.BatchTransferDTO;
import com.student.bank.dto.TransferConfirmDTO;
import com.student.bank.dto.TransferExecuteDTO;
import com.student.bank.dto.TransferPrecheckDTO;
import com.student.bank.entity.BankTransferOrder;
import com.student.bank.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/transfer")
@RequiredArgsConstructor
public class TransferController {
    private final AccountService accountService;

    @PostMapping("/precheck")
    public Result<Map<String, Object>> precheck(@Valid @RequestBody TransferPrecheckDTO dto) {
        return Result.success(accountService.precheck(dto));
    }

    @PostMapping("/confirm")
    public Result<Map<String, Object>> confirm(@Valid @RequestBody TransferConfirmDTO dto) {
        return Result.success(accountService.confirmTransfer(dto));
    }

    @PostMapping("/execute")
    public Result<Map<String, Object>> execute(@Valid @RequestBody TransferExecuteDTO dto) {
        return Result.success(accountService.executeTransfer(dto));
    }

    @GetMapping("/{orderNo}")
    public Result<BankTransferOrder> order(@PathVariable String orderNo) {
        return Result.success(accountService.getOrder(orderNo));
    }

    @PostMapping("/batch/precheck")
    public Result<Map<String, Object>> batchPrecheck(@Valid @RequestBody BatchTransferDTO dto) {
        return Result.success(accountService.batchPrecheck(dto));
    }

    @PostMapping("/batch/execute")
    public Result<Map<String, Object>> batchExecute(@Valid @RequestBody BatchTransferDTO dto) {
        return Result.success(accountService.batchExecute(dto));
    }
}
