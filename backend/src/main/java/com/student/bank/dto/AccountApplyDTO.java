package com.student.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccountApplyDTO {
    @NotBlank(message = "账户类型不能为空")
    private String accountType;
    private String currency;
    private String tradePassword;
}
