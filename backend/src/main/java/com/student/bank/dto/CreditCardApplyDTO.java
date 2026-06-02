package com.student.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreditCardApplyDTO {
    @NotBlank(message = "产品编码不能为空")
    private String productCode;
    private String occupation;
    private BigDecimal monthlyIncome;
    private String address;
}
