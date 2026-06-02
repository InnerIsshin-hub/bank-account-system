package com.student.bank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanApplyDTO {
    @NotBlank(message = "产品编码不能为空")
    private String productCode;
    @NotBlank(message = "申请人不能为空")
    private String applicantName;
    @DecimalMin(value = "1000.00", message = "贷款金额不能低于 1000 元")
    private BigDecimal amount;
    @NotNull(message = "期限不能为空")
    private Integer termMonths;
    private String purpose;
    private String receiveAccount;
}
