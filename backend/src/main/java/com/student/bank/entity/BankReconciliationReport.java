package com.student.bank.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("bank_reconciliation_report")
public class BankReconciliationReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate reportDate;
    private Integer accountCount;
    private BigDecimal totalBalance;
    private Integer recordCount;
    private Integer orderCount;
    private Integer exceptionCount;
    private String status;
    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
