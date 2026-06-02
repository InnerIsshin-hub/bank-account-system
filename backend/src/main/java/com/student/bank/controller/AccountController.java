package com.student.bank.controller;

import com.student.bank.common.Result;
import com.student.bank.dto.AccountApplyDTO;
import com.student.bank.dto.CloseAccountDTO;
import com.student.bank.dto.FreezeAccountDTO;
import com.student.bank.dto.TransferDTO;
import com.student.bank.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.success(accountService.listAccounts());
    }

    @GetMapping("/{accountNumber}")
    public Result<Map<String, Object>> detail(@PathVariable String accountNumber) {
        return Result.success(accountService.getAccountDetail(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public Result<BigDecimal> balance(@PathVariable String accountNumber) {
        return Result.success(accountService.getBalance(accountNumber));
    }

    @GetMapping("/balance/{accountNumber}")
    public Result<BigDecimal> legacyBalance(@PathVariable String accountNumber) {
        return Result.success(accountService.getBalance(accountNumber));
    }

    @PostMapping("/apply")
    public Result<Map<String, Object>> apply(@Valid @RequestBody AccountApplyDTO dto) {
        return Result.success(accountService.applyAccount(dto));
    }

    @PostMapping("/{accountNumber}/freeze")
    public Result<Void> freeze(@PathVariable String accountNumber, @RequestBody FreezeAccountDTO dto) {
        accountService.freezeAccount(accountNumber, dto);
        return Result.success("账户已冻结", null);
    }

    @PostMapping("/{accountNumber}/unfreeze")
    public Result<Void> unfreeze(@PathVariable String accountNumber, @RequestBody FreezeAccountDTO dto) {
        accountService.unfreezeAccount(accountNumber, dto);
        return Result.success("账户已解冻", null);
    }

    @DeleteMapping("/{accountNumber}")
    public Result<Void> close(@PathVariable String accountNumber, @Valid @RequestBody CloseAccountDTO dto) {
        accountService.closeAccount(accountNumber, dto);
        return Result.success("账户已销户", null);
    }

    @PostMapping("/transfer")
    public Result<Map<String, Object>> legacyTransfer(@Valid @RequestBody TransferDTO dto) {
        return Result.success("转账成功", accountService.transfer(dto));
    }
}
