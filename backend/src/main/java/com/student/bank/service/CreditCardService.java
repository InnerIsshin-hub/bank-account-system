package com.student.bank.service;

import com.student.bank.dto.CreditCardApplyDTO;

import java.util.List;
import java.util.Map;

public interface CreditCardService {
    Map<String, Object> apply(CreditCardApplyDTO dto);
    List<Map<String, Object>> listCards();
    void activate(Long cardId, String tradePassword);
    List<Map<String, Object>> bills(Long cardId);
    Map<String, Object> installment(Long billId, Integer periods);
}
