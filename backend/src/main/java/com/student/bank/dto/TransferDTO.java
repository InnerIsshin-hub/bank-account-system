package com.student.bank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransferDTO {
    @NotBlank(message = "付款账户不能为空")
    @Pattern(regexp = "^\\d{16,19}$", message = "付款账户格式不正确")
    private String fromAccount;

    @NotBlank(message = "收款账户不能为空")
    @Pattern(regexp = "^\\d{16,19}$", message = "收款账户格式不正确")
    private String toAccount;

    private String toName;
    private String toBankName;

    @NotNull(message = "转账金额不能为空")
    @DecimalMin(value = "0.01", message = "转账金额必须大于 0")
    private BigDecimal amount;

    @Size(max = 200, message = "备注最多 200 个字符")
    private String remark;

    @NotBlank(message = "交易密码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "交易密码必须为 6 位数字")
    private String tradePassword;

    @NotBlank(message = "幂等键不能为空")
    @Size(max = 128, message = "幂等键过长")
    private String idempotencyKey;

    private String otpCode;
    private LocalDateTime scheduledAt;
}
