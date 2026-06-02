package com.student.bank.controller;

import com.student.bank.common.PageResponse;
import com.student.bank.common.Result;
import com.student.bank.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {
    private final RecordService recordService;

    @GetMapping
    public Result<PageResponse<Map<String, Object>>> query(@RequestParam(required = false) String accountNumber,
                                                           @RequestParam(required = false) String direction,
                                                           @RequestParam(required = false) String transactionType,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                           @RequestParam(required = false) BigDecimal minAmount,
                                                           @RequestParam(required = false) BigDecimal maxAmount,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(defaultValue = "1") long pageNo,
                                                           @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(recordService.query(accountNumber, direction, transactionType, startDate, endDate,
                minAmount, maxAmount, keyword, pageNo, pageSize));
    }

    @GetMapping("/{recordNo}")
    public Result<Map<String, Object>> detail(@PathVariable String recordNo) {
        return Result.success(recordService.detail(recordNo));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String accountNumber,
                                         @RequestParam(required = false) String direction,
                                         @RequestParam(required = false) String transactionType,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                         @RequestParam(required = false) BigDecimal minAmount,
                                         @RequestParam(required = false) BigDecimal maxAmount,
                                         @RequestParam(required = false) String keyword) {
        byte[] csv = recordService.exportCsv(accountNumber, direction, transactionType, startDate, endDate,
                minAmount, maxAmount, keyword);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=records.csv")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }
}
