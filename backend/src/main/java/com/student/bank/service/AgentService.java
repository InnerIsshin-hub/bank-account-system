package com.student.bank.service;

import java.util.List;
import java.util.Map;

public interface AgentService {
    List<Map<String, Object>> listSkills();
    Map<String, Object> invokeSkill(String skillName, Map<String, Object> params);
    Map<String, Object> chat(String message);
    Map<String, Object> accountSummary();
    Map<String, Object> transferPrecheck(Map<String, Object> params);
    Map<String, Object> createTransferDraft(Map<String, Object> params);
    Map<String, Object> billAnalysis();
}
