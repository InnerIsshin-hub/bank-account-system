package com.student.bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ContactDTO {
    private Long id;
    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;
    @NotBlank(message = "收款账户不能为空")
    @Pattern(regexp = "^\\d{16,19}$", message = "账户格式不正确")
    private String accountNumber;
    private String bankName;
    private String phone;
}
