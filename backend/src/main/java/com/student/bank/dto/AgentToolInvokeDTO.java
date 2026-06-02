package com.student.bank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class AgentToolInvokeDTO {
    @NotBlank(message = "技能名称不能为空")
    private String skillName;

    private Map<String, Object> params = new LinkedHashMap<>();
}
