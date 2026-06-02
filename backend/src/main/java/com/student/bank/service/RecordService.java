package com.student.bank.service;

import com.student.bank.common.PageResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface RecordService {
    PageResponse<Map<String, Object>> query(String accountNumber, String direction, String transactionType,
                                            LocalDate startDate, LocalDate endDate, BigDecimal minAmount,
                                            BigDecimal maxAmount, String keyword, long pageNo, long pageSize);
    Map<String, Object> detail(String recordNo);
    byte[] exportCsv(String accountNumber, String direction, String transactionType,
                     LocalDate startDate, LocalDate endDate, BigDecimal minAmount,
                     BigDecimal maxAmount, String keyword);
}
