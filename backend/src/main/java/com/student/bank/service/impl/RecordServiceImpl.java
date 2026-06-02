package com.student.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.student.bank.common.AuthContext;
import com.student.bank.common.BusinessException;
import com.student.bank.common.ErrorCode;
import com.student.bank.common.PageResponse;
import com.student.bank.entity.BankAccount;
import com.student.bank.entity.BankTransactionRecord;
import com.student.bank.mapper.BankAccountMapper;
import com.student.bank.mapper.BankTransactionRecordMapper;
import com.student.bank.service.AuditService;
import com.student.bank.service.RecordService;
import com.student.bank.util.SensitiveDataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecordServiceImpl implements RecordService {
    private final BankTransactionRecordMapper recordMapper;
    private final BankAccountMapper accountMapper;
    private final AuditService auditService;

    @Override
    public PageResponse<Map<String, Object>> query(String accountNumber, String direction, String transactionType,
                                                   LocalDate startDate, LocalDate endDate, BigDecimal minAmount,
                                                   BigDecimal maxAmount, String keyword, long pageNo, long pageSize) {
        LambdaQueryWrapper<BankTransactionRecord> query = buildQuery(accountNumber, direction, transactionType, startDate,
                endDate, minAmount, maxAmount, keyword);
        Page<BankTransactionRecord> page = recordMapper.selectPage(new Page<>(Math.max(1, pageNo), Math.min(Math.max(1, pageSize), 100)), query);
        return new PageResponse<>(page.getCurrent(), page.getSize(), page.getTotal(),
                page.getRecords().stream().map(this::view).toList());
    }

    @Override
    public Map<String, Object> detail(String recordNo) {
        BankTransactionRecord record = recordMapper.selectOne(new LambdaQueryWrapper<BankTransactionRecord>()
                .eq(BankTransactionRecord::getRecordNo, recordNo));
        if (record == null || !record.getUserId().equals(AuthContext.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "流水不存在或无权查看");
        }
        auditService.record("RECORD_DETAIL", "TRANSACTION_RECORD", recordNo, "SUCCESS", "查看流水详情");
        return view(record);
    }

    @Override
    public byte[] exportCsv(String accountNumber, String direction, String transactionType,
                            LocalDate startDate, LocalDate endDate, BigDecimal minAmount,
                            BigDecimal maxAmount, String keyword) {
        List<BankTransactionRecord> records = recordMapper.selectList(buildQuery(accountNumber, direction, transactionType,
                startDate, endDate, minAmount, maxAmount, keyword).last("LIMIT 5000"));
        StringBuilder csv = new StringBuilder("recordNo,createdAt,account,direction,type,amount,balanceAfter,counterparty,remark\n");
        for (BankTransactionRecord record : records) {
            csv.append(escape(record.getRecordNo())).append(',')
                    .append(escape(String.valueOf(record.getCreatedAt()))).append(',')
                    .append(escape(SensitiveDataUtil.maskAccount(record.getAccountNumber()))).append(',')
                    .append(escape(record.getDirection())).append(',')
                    .append(escape(record.getTransactionType())).append(',')
                    .append(record.getAmount()).append(',')
                    .append(record.getBalanceAfter()).append(',')
                    .append(escape(SensitiveDataUtil.maskAccount(record.getCounterpartyAccount()))).append(',')
                    .append(escape(record.getRemark())).append('\n');
        }
        auditService.record("RECORD_EXPORT", "TRANSACTION_RECORD", accountNumber, "SUCCESS", "导出流水 CSV");
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private LambdaQueryWrapper<BankTransactionRecord> buildQuery(String accountNumber, String direction, String transactionType,
                                                                 LocalDate startDate, LocalDate endDate, BigDecimal minAmount,
                                                                 BigDecimal maxAmount, String keyword) {
        Long userId = AuthContext.userId();
        List<String> ownedAccounts = accountMapper.selectList(new LambdaQueryWrapper<BankAccount>()
                        .eq(BankAccount::getUserId, userId))
                .stream().map(BankAccount::getAccountNumber).toList();
        if (ownedAccounts.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "没有可查询账户");
        }
        LambdaQueryWrapper<BankTransactionRecord> query = new LambdaQueryWrapper<>();
        query.eq(BankTransactionRecord::getUserId, userId);
        if (accountNumber != null && !accountNumber.isBlank()) {
            String cleaned = accountNumber.replaceAll("\\s+", "");
            if (!ownedAccounts.contains(cleaned)) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "只能导出或查询本人账户流水");
            }
            query.eq(BankTransactionRecord::getAccountNumber, cleaned);
        } else {
            query.in(BankTransactionRecord::getAccountNumber, ownedAccounts);
        }
        if (direction != null && !direction.isBlank()) {
            query.eq(BankTransactionRecord::getDirection, normalizeDirection(direction));
        }
        if (transactionType != null && !transactionType.isBlank()) {
            query.eq(BankTransactionRecord::getTransactionType, transactionType.toUpperCase());
        }
        if (startDate != null) {
            query.ge(BankTransactionRecord::getCreatedAt, startDate.atStartOfDay());
        }
        if (endDate != null) {
            query.lt(BankTransactionRecord::getCreatedAt, endDate.plusDays(1).atStartOfDay());
        }
        if (startDate != null && endDate != null && startDate.plusDays(370).isBefore(endDate)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "流水查询时间跨度不能超过 370 天");
        }
        if (minAmount != null) {
            query.ge(BankTransactionRecord::getAmount, minAmount);
        }
        if (maxAmount != null) {
            query.le(BankTransactionRecord::getAmount, maxAmount);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.and(w -> w.like(BankTransactionRecord::getRemark, keyword)
                    .or().like(BankTransactionRecord::getCounterpartyName, keyword)
                    .or().like(BankTransactionRecord::getCounterpartyAccount, keyword));
        }
        query.orderByDesc(BankTransactionRecord::getCreatedAt);
        return query;
    }

    private String normalizeDirection(String value) {
        return switch (value.toLowerCase()) {
            case "in", "转入" -> "IN";
            case "out", "转出" -> "OUT";
            default -> value.toUpperCase();
        };
    }

    private Map<String, Object> view(BankTransactionRecord record) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recordNo", record.getRecordNo());
        data.put("orderNo", record.getOrderNo());
        data.put("accountNumber", SensitiveDataUtil.maskAccount(record.getAccountNumber()));
        data.put("rawAccountNumber", record.getAccountNumber());
        data.put("direction", record.getDirection());
        data.put("transactionType", record.getTransactionType());
        data.put("category", record.getCategory());
        data.put("amount", record.getAmount());
        data.put("balanceAfter", record.getBalanceAfter());
        data.put("counterpartyAccount", SensitiveDataUtil.maskAccount(record.getCounterpartyAccount()));
        data.put("counterpartyName", SensitiveDataUtil.maskName(record.getCounterpartyName()));
        data.put("remark", record.getRemark());
        data.put("createdAt", record.getCreatedAt());
        return data;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
