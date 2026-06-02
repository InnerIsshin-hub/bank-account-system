package com.student.bank.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class RegisterDTO {
    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 32, message = "姓名长度需在 2 到 32 个字符之间")
    private String userName;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)?\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$",
            message = "身份证号格式不正确")
    private String idCard;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "交易密码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "交易密码必须为 6 位数字")
    private String tradePassword;

    private String smsCode;
}
