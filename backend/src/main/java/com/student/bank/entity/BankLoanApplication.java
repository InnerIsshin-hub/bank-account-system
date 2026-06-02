package com.student.bank.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bank_loan_application")
public class BankLoanApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String productCode;
    private String applicantName;
    private BigDecimal amount;
    private Integer termMonths;
    private String purpose;
    private BigDecimal autoScore;
    private String status;
    private String reviewComment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
