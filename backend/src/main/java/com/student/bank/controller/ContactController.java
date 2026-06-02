package com.student.bank.controller;

import com.student.bank.common.Result;
import com.student.bank.dto.ContactDTO;
import com.student.bank.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {
    private final ContactService contactService;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.success(contactService.list());
    }

    @PostMapping
    public Result<Map<String, Object>> save(@Valid @RequestBody ContactDTO dto) {
        return Result.success(contactService.save(dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        contactService.delete(id);
        return Result.success("删除成功", null);
    }
}
