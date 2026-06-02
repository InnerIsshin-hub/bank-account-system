package com.student.bank.service;

import com.student.bank.dto.TransferPrecheckDTO;

import java.util.Map;

public interface RiskService {
    Map<String, Object> evaluateTransfer(Long userId, TransferPrecheckDTO dto, String orderNo);
}
