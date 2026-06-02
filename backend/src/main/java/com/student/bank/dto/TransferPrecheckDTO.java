package com.student.bank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferPrecheckDTO {
    private String fromAccount;
    @NotBlank(message = "收款账户不能为空")
    @Pattern(regexp = "^\\d{16,19}$", message = "收款账户格式不正确")
    private String toAccount;
    private String toName;
    private String toBankName;
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;
}
