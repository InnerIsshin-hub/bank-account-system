package com.student.bank.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.bank.common.AuthContext;
import com.student.bank.common.Result;
import com.student.bank.entity.BankAccount;
import com.student.bank.entity.BankAuditLog;
import com.student.bank.entity.BankTransferOrder;
import com.student.bank.entity.BankUser;
import com.student.bank.mapper.BankAccountMapper;
import com.student.bank.mapper.BankAuditLogMapper;
import com.student.bank.mapper.BankTransferOrderMapper;
import com.student.bank.mapper.BankUserMapper;
import com.student.bank.util.SensitiveDataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final BankUserMapper userMapper;
    private final BankAccountMapper accountMapper;
    private final BankTransferOrderMapper orderMapper;
    private final BankAuditLogMapper auditLogMapper;

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> users(@RequestParam(required = false) String keyword) {
        AuthContext.requireAdmin();
        LambdaQueryWrapper<BankUser> query = new LambdaQueryWrapper<BankUser>().orderByDesc(BankUser::getCreatedAt).last("LIMIT 200");
        if (keyword != null && !keyword.isBlank()) {
            query.like(BankUser::getUserName, keyword);
        }
        return Result.success(userMapper.selectList(query).stream().map(this::userView).toList());
    }

    @PostMapping("/users/{userId}/freeze")
    public Result<Void> freezeUser(@PathVariable Long userId) {
        AuthContext.requireAdmin();
        BankUser user = userMapper.selectById(userId);
        user.setStatus("FROZEN");
        user.setTokenVersion(user.getTokenVersion() + 1);
        userMapper.updateById(user);
        return Result.success("用户已冻结", null);
    }

    @PostMapping("/users/{userId}/unfreeze")
    public Result<Void> unfreezeUser(@PathVariable Long userId) {
        AuthContext.requireAdmin();
        BankUser user = userMapper.selectById(userId);
        user.setStatus("NORMAL");
        userMapper.updateById(user);
        return Result.success("用户已解冻", null);
    }

    @GetMapping("/accounts")
    public Result<List<Map<String, Object>>> accounts() {
        AuthContext.requireAdmin();
        return Result.success(accountMapper.selectList(new LambdaQueryWrapper<BankAccount>()
                        .orderByDesc(BankAccount::getCreatedAt)
                        .last("LIMIT 200"))
                .stream().map(this::accountView).toList());
    }

    @GetMapping("/transfers")
    public Result<List<BankTransferOrder>> transfers() {
        AuthContext.requireAdmin();
        return Result.success(orderMapper.selectList(new LambdaQueryWrapper<BankTransferOrder>()
                .orderByDesc(BankTransferOrder::getCreatedAt)
                .last("LIMIT 200")));
    }

    @GetMapping("/audits")
    public Result<List<BankAuditLog>> audits() {
        AuthContext.requireAdmin();
        return Result.success(auditLogMapper.selectList(new LambdaQueryWrapper<BankAuditLog>()
                .orderByDesc(BankAuditLog::getCreatedAt)
                .last("LIMIT 200")));
    }

    private Map<String, Object> userView(BankUser user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("userName", user.getUserName());
        data.put("idCardMasked", SensitiveDataUtil.maskIdCard(user.getIdCard()));
        data.put("phoneMasked", SensitiveDataUtil.maskPhone(user.getPhone()));
        data.put("status", user.getStatus());
        data.put("kycStatus", user.getKycStatus());
        data.put("role", user.getRole());
        data.put("createdAt", user.getCreatedAt());
        return data;
    }

    private Map<String, Object> accountView(BankAccount account) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accountNumber", SensitiveDataUtil.maskAccount(account.getAccountNumber()));
        data.put("userId", account.getUserId());
        data.put("accountType", account.getAccountType());
        data.put("status", account.getStatus());
        data.put("currency", account.getCurrency());
        data.put("availableBalance", account.getAvailableBalance());
        data.put("frozenBalance", account.getFrozenBalance());
        return data;
    }
}
