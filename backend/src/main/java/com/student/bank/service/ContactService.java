package com.student.bank.service;

import com.student.bank.dto.ContactDTO;

import java.util.List;
import java.util.Map;

public interface ContactService {
    List<Map<String, Object>> list();
    Map<String, Object> save(ContactDTO dto);
    void delete(Long id);
}
