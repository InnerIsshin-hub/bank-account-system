package com.student.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.bank.entity.BankAccount;
import com.student.bank.entity.BankReconciliationReport;
import com.student.bank.entity.BankTransactionRecord;
import com.student.bank.entity.BankTransferOrder;
import com.student.bank.mapper.BankAccountMapper;
import com.student.bank.mapper.BankReconciliationReportMapper;
import com.student.bank.mapper.BankTransactionRecordMapper;
import com.student.bank.mapper.BankTransferOrderMapper;
import com.student.bank.service.AuditService;
import com.student.bank.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {
    private final BankAccountMapper accountMapper;
    private final BankTransferOrderMapper orderMapper;
    private final BankTransactionRecordMapper recordMapper;
    private final BankReconciliationReportMapper reportMapper;
    private final AuditService auditService;

    @Override
    public Map<String, Object> runDaily(LocalDate date) {
        List<BankAccount> accounts = accountMapper.selectList(new LambdaQueryWrapper<>());
        BigDecimal totalBalance = accounts.stream()
                .map(BankAccount::getAvailableBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        int recordCount = Math.toIntExact(recordMapper.selectCount(new LambdaQueryWrapper<BankTransactionRecord>()
                .ge(BankTransactionRecord::getCreatedAt, start)
                .lt(BankTransactionRecord::getCreatedAt, end)));
        int orderCount = Math.toIntExact(orderMapper.selectCount(new LambdaQueryWrapper<BankTransferOrder>()
                .ge(BankTransferOrder::getCreatedAt, start)
                .lt(BankTransferOrder::getCreatedAt, end)));
        int exceptionCount = Math.toIntExact(orderMapper.selectCount(new LambdaQueryWrapper<BankTransferOrder>()
                .in(BankTransferOrder::getStatus, List.of("PROCESSING", "FAILED"))
                .ge(BankTransferOrder::getCreatedAt, start)
                .lt(BankTransferOrder::getCreatedAt, end)));

        BankReconciliationReport report = new BankReconciliationReport();
        report.setReportDate(date);
        report.setAccountCount(accounts.size());
        report.setTotalBalance(totalBalance);
        report.setRecordCount(recordCount);
        report.setOrderCount(orderCount);
        report.setExceptionCount(exceptionCount);
        report.setStatus(exceptionCount == 0 ? "NORMAL" : "EXCEPTION");
        report.setDetail("日终账户余额、订单状态、流水数量已汇总；异常订单需人工确认或补偿。");
        reportMapper.insert(report);
        auditService.record("RECONCILIATION", "REPORT", String.valueOf(date), "SUCCESS", report.getStatus());
        return reportView(report);
    }

    @Override
    public Map<String, Object> scanAndCompensate() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<BankTransferOrder> processing = orderMapper.selectList(new LambdaQueryWrapper<BankTransferOrder>()
                .eq(BankTransferOrder::getStatus, "PROCESSING")
                .lt(BankTransferOrder::getUpdatedAt, threshold));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("processingOlderThan30Min", processing.size());
        data.put("orders", processing.stream().map(BankTransferOrder::getOrderNo).toList());
        data.put("strategy", "本 demo 保留人工处理标记；跨系统接入后可自动重试或退款补偿");
        auditService.record("COMPENSATION_SCAN", "TRANSFER_ORDER", null, "SUCCESS", "异常订单扫描");
        return data;
    }

    private Map<String, Object> reportView(BankReconciliationReport report) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reportDate", report.getReportDate());
        data.put("accountCount", report.getAccountCount());
        data.put("totalBalance", report.getTotalBalance());
        data.put("recordCount", report.getRecordCount());
        data.put("orderCount", report.getOrderCount());
        data.put("exceptionCount", report.getExceptionCount());
        data.put("status", report.getStatus());
        data.put("detail", report.getDetail());
        return data;
    }
}
