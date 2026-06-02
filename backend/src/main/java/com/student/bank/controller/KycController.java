package com.student.bank.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.bank.common.AuthContext;
import com.student.bank.common.Result;
import com.student.bank.entity.BankKycRecord;
import com.student.bank.mapper.BankKycRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {
    private final BankKycRecordMapper kycRecordMapper;

    @GetMapping("/status")
    public Result<BankKycRecord> status() {
        return Result.success(kycRecordMapper.selectOne(new LambdaQueryWrapper<BankKycRecord>()
                .eq(BankKycRecord::getUserId, AuthContext.userId())
                .orderByDesc(BankKycRecord::getCreatedAt)
                .last("LIMIT 1")));
    }

    @PostMapping("/submit")
    public Result<BankKycRecord> submit(@RequestBody Map<String, String> body) {
        BankKycRecord record = new BankKycRecord();
        record.setUserId(AuthContext.userId());
        record.setIdCardHash(body.getOrDefault("idCardHash", "provided-by-register"));
        record.setFrontFileRef(body.get("frontFileRef"));
        record.setBackFileRef(body.get("backFileRef"));
        record.setFaceResult("SIMULATED_PASS");
        record.setFaceScore(BigDecimal.valueOf(0.96));
        record.setChannel("THIRD_PARTY_SIMULATED");
        record.setStatus("VERIFIED");
        record.setReviewedAt(LocalDateTime.now());
        kycRecordMapper.insert(record);
        return Result.success(record);
    }
}
