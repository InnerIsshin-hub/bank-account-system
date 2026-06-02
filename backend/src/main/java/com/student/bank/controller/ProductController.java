package com.student.bank.controller;

import com.student.bank.common.Result;
import com.student.bank.dto.HoldingPurchaseDTO;
import com.student.bank.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public Result<List<Map<String, Object>>> products(@RequestParam(required = false) String productType) {
        return Result.success(productService.listProducts(productType));
    }

    @PostMapping("/purchase")
    public Result<Map<String, Object>> purchase(@Valid @RequestBody HoldingPurchaseDTO dto) {
        return Result.success(productService.purchase(dto));
    }

    @GetMapping("/holdings")
    public Result<List<Map<String, Object>>> holdings() {
        return Result.success(productService.holdings());
    }

    @PostMapping("/holdings/{holdingId}/redeem")
    public Result<Void> redeem(@PathVariable Long holdingId) {
        productService.redeem(holdingId);
        return Result.success("赎回成功", null);
    }
}
