package com.student.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TransferExecuteDTO {
    @NotBlank(message = "订单号不能为空")
    private String orderNo;
    @NotBlank(message = "交易密码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "交易密码必须为 6 位数字")
    private String tradePassword;
    private String otpCode;
}
