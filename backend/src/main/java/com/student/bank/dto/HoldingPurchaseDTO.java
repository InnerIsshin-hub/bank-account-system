package com.student.bank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HoldingPurchaseDTO {
    @NotBlank(message = "付款账户不能为空")
    private String accountNumber;
    @NotBlank(message = "产品编码不能为空")
    private String productCode;
    @DecimalMin(value = "0.01", message = "购买金额必须大于 0")
    private BigDecimal amount;
    private Boolean riskAccepted;
    private String tradePassword;
}
