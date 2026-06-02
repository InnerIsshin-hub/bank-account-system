package com.student.bank.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bank_account")
public class BankAccount {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String accountNumber;
    private Long userId;
    private String accountType;
    private String status;
    private String currency;
    private BigDecimal availableBalance;
    private BigDecimal frozenBalance;

    @Version
    private Integer version;

    private LocalDateTime openTime;
    private LocalDateTime closedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
