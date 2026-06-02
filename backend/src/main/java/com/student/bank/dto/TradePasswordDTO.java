package com.student.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TradePasswordDTO {
    private String oldTradePassword;
    @NotBlank(message = "登录密码不能为空")
    private String loginPassword;
    @NotBlank(message = "新交易密码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "交易密码必须为 6 位数字")
    private String newTradePassword;
}
