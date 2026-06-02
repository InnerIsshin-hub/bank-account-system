package com.student.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentChatDTO {
    @NotBlank(message = "消息不能为空")
    private String message;
}
