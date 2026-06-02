package com.student.bank.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("bank_user")
public class BankUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String userName;
    private String idCard;
    private String idCardHash;
    private String phone;
    private String phoneHash;
    private String status;
    private String kycStatus;
    private String role;
    private Integer tokenVersion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
