package com.student.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CloseAccountDTO {
    @NotBlank(message = "登录密码不能为空")
    private String password;
    private String reason;
}
