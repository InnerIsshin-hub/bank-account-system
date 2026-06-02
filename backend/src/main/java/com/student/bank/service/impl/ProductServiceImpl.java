package com.student.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.bank.common.AuthContext;
import com.student.bank.common.BusinessException;
import com.student.bank.common.ErrorCode;
import com.student.bank.dto.HoldingPurchaseDTO;
import com.student.bank.entity.BankHolding;
import com.student.bank.entity.BankProduct;
import com.student.bank.mapper.BankHoldingMapper;
import com.student.bank.mapper.BankProductMapper;
import com.student.bank.service.AccountService;
import com.student.bank.service.AuditService;
import com.student.bank.service.NotificationService;
import com.student.bank.service.ProductService;
import com.student.bank.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final BankProductMapper productMapper;
    private final BankHoldingMapper holdingMapper;
    private final AccountService accountService;
    private final UserService userService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Override
    public List<Map<String, Object>> listProducts(String productType) {
        LambdaQueryWrapper<BankProduct> query = new LambdaQueryWrapper<BankProduct>()
                .eq(BankProduct::getStatus, "ON_SHELF")
                .orderByAsc(BankProduct::getProductType)
                .orderByAsc(BankProduct::getMinAmount);
        if (productType != null && !productType.isBlank()) {
            query.eq(BankProduct::getProductType, productType.toUpperCase());
        }
        return productMapper.selectList(query).stream().map(this::productView).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> purchase(HoldingPurchaseDTO dto) {
        BankProduct product = productMapper.selectOne(new LambdaQueryWrapper<BankProduct>()
                .eq(BankProduct::getProductCode, dto.getProductCode())
                .eq(BankProduct::getStatus, "ON_SHELF"));
        if (product == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "产品不存在或已下架");
        }
        if (dto.getAmount().compareTo(product.getMinAmount()) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "购买金额低于起购金额");
        }
        if (!"DEPOSIT".equals(product.getProductType()) && !Boolean.TRUE.equals(dto.getRiskAccepted())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "购买理财/基金/国债前必须确认风险提示");
        }
        if (!userService.verifyTradePassword(AuthContext.userId(), dto.getTradePassword())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "交易密码错误");
        }
        accountService.withdraw(dto.getAccountNumber(), dto.getAmount());
        BankHolding holding = new BankHolding();
        holding.setUserId(AuthContext.userId());
        holding.setAccountNumber(dto.getAccountNumber());
        holding.setProductCode(product.getProductCode());
        holding.setProductType(product.getProductType());
        holding.setAmount(dto.getAmount());
        holding.setStatus("HOLDING");
        holding.setStartAt(LocalDateTime.now());
        holding.setMaturityAt(product.getTermDays() == null || product.getTermDays() == 0 ? null : LocalDateTime.now().plusDays(product.getTermDays()));
        holdingMapper.insert(holding);
        auditService.record("PURCHASE_PRODUCT", "PRODUCT", product.getProductCode(), "SUCCESS", "购买产品");
        notificationService.notify(AuthContext.userId(), "产品购买成功", product.getProductName() + " 已生成持仓。", "PRODUCT_PURCHASE");
        return holdingView(holding);
    }

    @Override
    public List<Map<String, Object>> holdings() {
        return holdingMapper.selectList(new LambdaQueryWrapper<BankHolding>()
                        .eq(BankHolding::getUserId, AuthContext.userId())
                        .orderByDesc(BankHolding::getCreatedAt))
                .stream().map(this::holdingView).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void redeem(Long holdingId) {
        BankHolding holding = holdingMapper.selectById(holdingId);
        if (holding == null || !holding.getUserId().equals(AuthContext.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (!"HOLDING".equals(holding.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前持仓状态不可赎回");
        }
        accountService.deposit(holding.getAccountNumber(), holding.getAmount());
        holding.setStatus("REDEEMED");
        holdingMapper.updateById(holding);
        auditService.record("REDEEM_PRODUCT", "HOLDING", String.valueOf(holdingId), "SUCCESS", "赎回持仓");
    }

    private Map<String, Object> productView(BankProduct product) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("productCode", product.getProductCode());
        data.put("productName", product.getProductName());
        data.put("productType", product.getProductType());
        data.put("riskLevel", product.getRiskLevel());
        data.put("termDays", product.getTermDays());
        data.put("expectedYield", product.getExpectedYield());
        data.put("minAmount", product.getMinAmount());
        data.put("description", product.getDescription());
        return data;
    }

    private Map<String, Object> holdingView(BankHolding holding) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", holding.getId());
        data.put("accountNumber", holding.getAccountNumber());
        data.put("productCode", holding.getProductCode());
        data.put("productType", holding.getProductType());
        data.put("amount", holding.getAmount());
        data.put("status", holding.getStatus());
        data.put("startAt", holding.getStartAt());
        data.put("maturityAt", holding.getMaturityAt());
        return data;
    }
}
