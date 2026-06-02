package com.student.bank.service;

import java.time.LocalDate;
import java.util.Map;

public interface ReconciliationService {
    Map<String, Object> runDaily(LocalDate date);
    Map<String, Object> scanAndCompensate();
}
