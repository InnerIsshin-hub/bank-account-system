package com.student.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.bank.common.AuthContext;
import com.student.bank.common.BusinessException;
import com.student.bank.common.ErrorCode;
import com.student.bank.dto.ContactDTO;
import com.student.bank.entity.BankContact;
import com.student.bank.mapper.BankContactMapper;
import com.student.bank.service.AuditService;
import com.student.bank.service.ContactService;
import com.student.bank.util.SensitiveDataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {
    private final BankContactMapper contactMapper;
    private final AuditService auditService;

    @Override
    public List<Map<String, Object>> list() {
        return contactMapper.selectList(new LambdaQueryWrapper<BankContact>()
                        .eq(BankContact::getUserId, AuthContext.userId())
                        .orderByDesc(BankContact::getCreatedAt))
                .stream().map(this::view).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> save(ContactDTO dto) {
        BankContact contact = dto.getId() == null ? new BankContact() : contactMapper.selectById(dto.getId());
        if (contact == null || (contact.getId() != null && !contact.getUserId().equals(AuthContext.userId()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        contact.setUserId(AuthContext.userId());
        contact.setContactName(dto.getContactName());
        contact.setAccountNumber(dto.getAccountNumber().replaceAll("\\s+", ""));
        contact.setBankName(dto.getBankName() == null || dto.getBankName().isBlank() ? "本行" : dto.getBankName());
        contact.setPhone(dto.getPhone() == null || dto.getPhone().isBlank() ? null : SensitiveDataUtil.encrypt(dto.getPhone()));
        contact.setDeleted(0);
        if (contact.getId() == null) {
            contactMapper.insert(contact);
        } else {
            contactMapper.updateById(contact);
        }
        auditService.record("SAVE_CONTACT", "CONTACT", String.valueOf(contact.getId()), "SUCCESS", "保存常用收款人");
        return view(contact);
    }

    @Override
    public void delete(Long id) {
        BankContact contact = contactMapper.selectById(id);
        if (contact == null || !contact.getUserId().equals(AuthContext.userId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        contactMapper.deleteById(id);
        auditService.record("DELETE_CONTACT", "CONTACT", String.valueOf(id), "SUCCESS", "删除常用收款人");
    }

    private Map<String, Object> view(BankContact contact) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", contact.getId());
        data.put("contactName", contact.getContactName());
        data.put("accountNumber", contact.getAccountNumber());
        data.put("accountNumberMasked", SensitiveDataUtil.maskAccount(contact.getAccountNumber()));
        data.put("bankName", contact.getBankName());
        data.put("phoneMasked", SensitiveDataUtil.maskPhone(contact.getPhone()));
        data.put("createdAt", contact.getCreatedAt());
        return data;
    }
}
