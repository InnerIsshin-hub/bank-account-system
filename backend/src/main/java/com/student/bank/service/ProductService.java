package com.student.bank.service;

import com.student.bank.dto.HoldingPurchaseDTO;

import java.util.List;
import java.util.Map;

public interface ProductService {
    List<Map<String, Object>> listProducts(String productType);
    Map<String, Object> purchase(HoldingPurchaseDTO dto);
    List<Map<String, Object>> holdings();
    void redeem(Long holdingId);
}
