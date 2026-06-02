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
@TableName("bank_kyc_record")
public class BankKycRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String idCardHash;
    private String frontFileRef;
    private String backFileRef;
    private String faceResult;
    private BigDecimal faceScore;
    private String channel;
    private String status;
    private String reviewComment;
    private LocalDateTime reviewedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
