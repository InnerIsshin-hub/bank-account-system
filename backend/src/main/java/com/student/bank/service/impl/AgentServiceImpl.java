package com.student.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.bank.common.AuthContext;
import com.student.bank.common.BusinessException;
import com.student.bank.common.ErrorCode;
import com.student.bank.dto.TransferPrecheckDTO;
import com.student.bank.entity.BankAgentDraft;
import com.student.bank.entity.BankContact;
import com.student.bank.mapper.BankAgentDraftMapper;
import com.student.bank.mapper.BankContactMapper;
import com.student.bank.service.AccountService;
import com.student.bank.service.AgentService;
import com.student.bank.service.AuditService;
import com.student.bank.service.CreditCardService;
import com.student.bank.service.LoanService;
import com.student.bank.service.NotificationService;
import com.student.bank.service.ProductService;
import com.student.bank.service.RecordService;
import com.student.bank.service.RiskService;
import com.student.bank.util.SecurityUtil;
import com.student.bank.util.SensitiveDataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {
    private static final Pattern TRANSFER_PATTERN = Pattern.compile("给(.+?)(?:转|汇|打)(\\d+(?:\\.\\d{1,2})?)");

    private final AccountService accountService;
    private final RecordService recordService;
    private final ProductService productService;
    private final LoanService loanService;
    private final CreditCardService creditCardService;
    private final NotificationService notificationService;
    private final RiskService riskService;
    private final BankContactMapper contactMapper;
    private final BankAgentDraftMapper draftMapper;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Override
    public List<Map<String, Object>> listSkills() {
        List<Map<String, Object>> skills = new ArrayList<>();
        skills.add(skill("account-summary", "账户摘要", "READ", "LOW", "/api/agent/tools/account-summary", false,
                Map.of()));
        skills.add(skill("recent-records", "最近流水", "READ", "LOW", "/api/agent/tools/invoke", false,
                schema(field("accountNumber", "string", false, "付款或收款账户号"),
                        field("direction", "string", false, "IN 或 OUT"),
                        field("pageSize", "number", false, "返回条数，默认 10"))));
        skills.add(skill("contacts", "常用收款人", "READ", "LOW", "/api/agent/tools/invoke", false,
                schema(field("keyword", "string", false, "按联系人姓名模糊过滤"))));
        skills.add(skill("products", "产品列表", "READ", "LOW", "/api/agent/tools/invoke", false,
                schema(field("productType", "string", false, "DEPOSIT、WEALTH、LOAN、CREDIT_CARD"))));
        skills.add(skill("loan-progress", "贷款进度", "READ", "LOW", "/api/agent/tools/invoke", false,
                Map.of()));
        skills.add(skill("credit-card-bills", "信用卡账单", "READ", "LOW", "/api/agent/tools/invoke", false,
                schema(field("cardId", "number", false, "指定信用卡 ID，不传则返回卡片列表"))));
        skills.add(skill("notifications", "通知中心", "READ", "LOW", "/api/agent/tools/invoke", false,
                Map.of()));
        skills.add(skill("transfer-precheck", "转账预校验", "PRECHECK", "MEDIUM", "/api/agent/tools/transfer-precheck", false,
                schema(field("fromAccount", "string", true, "付款账户"),
                        field("toAccount", "string", true, "收款账户"),
                        field("toName", "string", false, "收款人姓名"),
                        field("amount", "number", false, "转账金额"))));
        skills.add(skill("risk-evaluate", "风控评估", "PRECHECK", "MEDIUM", "/api/agent/tools/invoke", false,
                schema(field("fromAccount", "string", true, "付款账户"),
                        field("toAccount", "string", true, "收款账户"),
                        field("amount", "number", true, "转账金额"))));
        skills.add(skill("create-transfer-draft", "创建转账草稿", "CONTROLLED_WRITE", "HIGH", "/api/agent/tools/create-transfer-draft", true,
                schema(field("fromAccount", "string", false, "付款账户"),
                        field("toAccount", "string", true, "收款账户"),
                        field("amount", "number", true, "转账金额"),
                        field("remark", "string", false, "转账备注"))));
        skills.add(skill("create-loan-draft", "创建贷款草稿", "CONTROLLED_WRITE", "HIGH", "/api/agent/tools/invoke", true,
                schema(field("productCode", "string", true, "贷款产品编码"),
                        field("amount", "number", true, "贷款金额"),
                        field("termMonths", "number", true, "贷款期数"))));
        skills.add(skill("create-product-draft", "创建理财购买草稿", "CONTROLLED_WRITE", "HIGH", "/api/agent/tools/invoke", true,
                schema(field("productCode", "string", true, "产品编码"),
                        field("accountNumber", "string", true, "扣款账户"),
                        field("amount", "number", true, "购买金额"))));
        skills.add(skill("bill-analysis", "智能账单分析", "READ", "LOW", "/api/agent/tools/bill-analysis", false,
                Map.of()));
        return skills;
    }

    @Override
    public Map<String, Object> invokeSkill(String skillName, Map<String, Object> params) {
        String normalized = skillName == null ? "" : skillName.trim().toLowerCase();
        Map<String, Object> safeParams = params == null ? Map.of() : params;
        Map<String, Object> result = switch (normalized) {
            case "account-summary" -> accountSummary();
            case "recent-records" -> recentRecords(safeParams);
            case "contacts" -> contacts(safeParams);
            case "products" -> Map.of("products", productService.listProducts(asString(safeParams.get("productType"))));
            case "loan-progress" -> Map.of("applications", loanService.myApplications());
            case "credit-card-bills" -> creditCardBills(safeParams);
            case "notifications" -> notificationService.center(AuthContext.userId());
            case "transfer-precheck" -> transferPrecheck(safeParams);
            case "risk-evaluate" -> riskEvaluate(safeParams);
            case "create-transfer-draft" -> createTransferDraft(safeParams);
            case "create-loan-draft" -> createDraft("LOAN_APPLY", safeParams, "AGENT_TOOL_CREATE_LOAN_DRAFT");
            case "create-product-draft" -> createDraft("PRODUCT_PURCHASE", safeParams, "AGENT_TOOL_CREATE_PRODUCT_DRAFT");
            case "bill-analysis" -> billAnalysis();
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的 Agent 技能：" + skillName);
        };
        auditService.record("AGENT_TOOL_INVOKE", "AGENT_TOOL", normalized, "SUCCESS", "Agent 技能统一调用");
        return result;
    }

    @Override
    public Map<String, Object> chat(String message) {
        auditService.record("AGENT_CHAT", "AGENT", null, "SUCCESS", "Agent 对话入口");
        Matcher matcher = TRANSFER_PATTERN.matcher(message);
        if (matcher.find()) {
            String receiver = matcher.group(1).trim();
            BigDecimal amount = new BigDecimal(matcher.group(2));
            List<BankContact> candidates = contactMapper.selectList(new LambdaQueryWrapper<BankContact>()
                    .eq(BankContact::getUserId, AuthContext.userId())
                    .like(BankContact::getContactName, receiver));
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("intent", "TRANSFER_DRAFT");
            data.put("receiver", receiver);
            data.put("amount", amount);
            if (candidates.size() == 1) {
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("toAccount", candidates.get(0).getAccountNumber());
                params.put("toName", candidates.get(0).getContactName());
                params.put("amount", amount);
                params.put("remark", extractRemark(message));
                data.put("draft", createTransferDraft(params));
            } else {
                data.put("message", candidates.isEmpty() ? "没有找到常用收款人，请先补充收款账户。" : "找到多个候选收款人，请选择后再确认。");
                data.put("candidates", candidates.stream().map(c -> Map.of(
                        "id", c.getId(),
                        "name", c.getContactName(),
                        "accountMasked", SensitiveDataUtil.maskAccount(c.getAccountNumber())
                )).toList());
            }
            return data;
        }
        if (message.contains("账单") || message.contains("消费")) {
            return billAnalysis();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("intent", "ACCOUNT_SUMMARY");
        data.put("message", "已为您查询账户摘要。涉及转账、密码、销户等高风险操作时，我只能创建草稿并引导您到页面确认。");
        data.put("summary", accountSummary());
        return data;
    }

    @Override
    public Map<String, Object> accountSummary() {
        Map<String, Object> data = new LinkedHashMap<>();
        List<Map<String, Object>> accounts = accountService.listAccounts();
        BigDecimal total = accounts.stream()
                .map(item -> (BigDecimal) item.get("availableBalance"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("accounts", accounts);
        data.put("totalAssets", total);
        auditService.record("AGENT_TOOL_ACCOUNT_SUMMARY", "AGENT_TOOL", null, "SUCCESS", "账户摘要工具");
        return data;
    }

    @Override
    public Map<String, Object> transferPrecheck(Map<String, Object> params) {
        TransferPrecheckDTO dto = new TransferPrecheckDTO();
        dto.setFromAccount(asString(params.get("fromAccount")));
        dto.setToAccount(asString(params.get("toAccount")));
        dto.setToName(asString(params.get("toName")));
        dto.setToBankName(asString(params.get("toBankName")));
        if (params.get("amount") != null) {
            dto.setAmount(new BigDecimal(String.valueOf(params.get("amount"))));
        }
        auditService.record("AGENT_TOOL_TRANSFER_PRECHECK", "AGENT_TOOL", dto.getToAccount(), "SUCCESS", "转账预校验工具");
        return accountService.precheck(dto);
    }

    @Override
    public Map<String, Object> createTransferDraft(Map<String, Object> params) {
        try {
            BankAgentDraft draft = new BankAgentDraft();
            draft.setDraftNo(SecurityUtil.generateOrderNo("DR"));
            draft.setUserId(AuthContext.userId());
            draft.setDraftType("TRANSFER");
            draft.setPayload(objectMapper.writeValueAsString(params));
            draft.setStatus("WAITING_CONFIRM");
            draftMapper.insert(draft);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("draftNo", draft.getDraftNo());
            data.put("status", draft.getStatus());
            data.put("payload", params);
            auditService.record("AGENT_TOOL_CREATE_TRANSFER_DRAFT", "AGENT_DRAFT", draft.getDraftNo(), "SUCCESS", "创建转账草稿");
            return data;
        } catch (Exception e) {
            throw new IllegalStateException("创建转账草稿失败", e);
        }
    }

    @Override
    public Map<String, Object> billAnalysis() {
        var page = recordService.query(null, "OUT", null,
                LocalDate.now().withDayOfMonth(1), LocalDate.now(), null, null, null, 1, 500);
        Map<String, BigDecimal> categoryAmount = new LinkedHashMap<>();
        for (Map<String, Object> record : page.getRecords()) {
            String category = String.valueOf(record.get("category"));
            BigDecimal amount = (BigDecimal) record.get("amount");
            categoryAmount.merge(category, amount, BigDecimal::add);
        }
        List<Map<String, Object>> chart = new ArrayList<>();
        categoryAmount.forEach((category, amount) -> chart.add(Map.of("category", category, "amount", amount)));
        BigDecimal total = categoryAmount.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("month", LocalDate.now().getMonthValue());
        data.put("totalExpense", total);
        data.put("categoryChart", chart);
        data.put("largestCategory", categoryAmount.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("NONE"));
        data.put("tips", total.compareTo(BigDecimal.valueOf(20000)) > 0 ? "本月支出较高，建议复核大额流水。" : "本月支出保持在较稳健区间。");
        auditService.record("AGENT_TOOL_BILL_ANALYSIS", "AGENT_TOOL", null, "SUCCESS", "智能账单分析");
        return data;
    }

    private Map<String, Object> recentRecords(Map<String, Object> params) {
        long pageNo = Math.max(1, asLong(params.get("pageNo"), 1));
        long pageSize = Math.min(50, Math.max(1, asLong(params.get("pageSize"), 10)));
        var page = recordService.query(asString(params.get("accountNumber")), asString(params.get("direction")),
                asString(params.get("transactionType")), LocalDate.now().minusMonths(3), LocalDate.now(),
                null, null, asString(params.get("keyword")), pageNo, pageSize);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pageNo", page.getPageNo());
        data.put("pageSize", page.getPageSize());
        data.put("total", page.getTotal());
        data.put("records", page.getRecords());
        auditService.record("AGENT_TOOL_RECENT_RECORDS", "AGENT_TOOL", null, "SUCCESS", "查询最近流水");
        return data;
    }

    private Map<String, Object> contacts(Map<String, Object> params) {
        String keyword = asString(params.get("keyword"));
        LambdaQueryWrapper<BankContact> query = new LambdaQueryWrapper<BankContact>()
                .eq(BankContact::getUserId, AuthContext.userId())
                .orderByDesc(BankContact::getCreatedAt);
        if (keyword != null && !keyword.isBlank()) {
            query.like(BankContact::getContactName, keyword.trim());
        }
        List<Map<String, Object>> contacts = contactMapper.selectList(query).stream()
                .map(contact -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", contact.getId());
                    item.put("contactName", contact.getContactName());
                    item.put("accountNumberMasked", SensitiveDataUtil.maskAccount(contact.getAccountNumber()));
                    item.put("accountNumber", contact.getAccountNumber());
                    item.put("bankName", contact.getBankName());
                    return item;
                }).toList();
        auditService.record("AGENT_TOOL_CONTACTS", "AGENT_TOOL", null, "SUCCESS", "查询常用收款人");
        return Map.of("contacts", contacts);
    }

    private Map<String, Object> creditCardBills(Map<String, Object> params) {
        Long cardId = asLongObject(params.get("cardId"));
        Map<String, Object> data = new LinkedHashMap<>();
        if (cardId == null) {
            data.put("cards", creditCardService.listCards());
        } else {
            data.put("bills", creditCardService.bills(cardId));
        }
        auditService.record("AGENT_TOOL_CREDIT_CARD_BILLS", "AGENT_TOOL", cardId == null ? null : String.valueOf(cardId),
                "SUCCESS", "查询信用卡账单");
        return data;
    }

    private Map<String, Object> riskEvaluate(Map<String, Object> params) {
        TransferPrecheckDTO dto = new TransferPrecheckDTO();
        dto.setFromAccount(asString(params.get("fromAccount")));
        dto.setToAccount(asString(params.get("toAccount")));
        dto.setToName(asString(params.get("toName")));
        dto.setToBankName(asString(params.get("toBankName")));
        if (params.get("amount") != null) {
            dto.setAmount(new BigDecimal(String.valueOf(params.get("amount"))));
        }
        Map<String, Object> data = riskService.evaluateTransfer(AuthContext.userId(), dto, SecurityUtil.generateOrderNo("AG"));
        auditService.record("AGENT_TOOL_RISK_EVALUATE", "AGENT_TOOL", dto.getToAccount(), "SUCCESS", "Agent 风控评估");
        return data;
    }

    private Map<String, Object> createDraft(String draftType, Map<String, Object> params, String auditType) {
        try {
            BankAgentDraft draft = new BankAgentDraft();
            draft.setDraftNo(SecurityUtil.generateOrderNo("DR"));
            draft.setUserId(AuthContext.userId());
            draft.setDraftType(draftType);
            draft.setPayload(objectMapper.writeValueAsString(params));
            draft.setStatus("WAITING_CONFIRM");
            draftMapper.insert(draft);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("draftNo", draft.getDraftNo());
            data.put("draftType", draft.getDraftType());
            data.put("status", draft.getStatus());
            data.put("payload", params);
            auditService.record(auditType, "AGENT_DRAFT", draft.getDraftNo(), "SUCCESS", "创建 Agent 草稿");
            return data;
        } catch (Exception e) {
            throw new IllegalStateException("创建 Agent 草稿失败", e);
        }
    }

    private Map<String, Object> skill(String name, String title, String category, String riskLevel,
                                      String endpoint, boolean confirmRequired, Map<String, Object> inputSchema) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name);
        data.put("title", title);
        data.put("category", category);
        data.put("riskLevel", riskLevel);
        data.put("endpoint", endpoint);
        data.put("confirmRequired", confirmRequired);
        data.put("inputSchema", inputSchema);
        return data;
    }

    @SafeVarargs
    private final Map<String, Object> schema(Map<String, Object>... fields) {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            String name = String.valueOf(field.get("name"));
            Map<String, Object> property = new LinkedHashMap<>();
            property.put("type", field.get("type"));
            property.put("description", field.get("description"));
            properties.put(name, property);
            if (Boolean.TRUE.equals(field.get("required"))) {
                required.add(name);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", "object");
        data.put("properties", properties);
        data.put("required", required);
        return data;
    }

    private Map<String, Object> field(String name, String type, boolean required, String description) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", name);
        data.put("type", type);
        data.put("required", required);
        data.put("description", description);
        return data;
    }

    private String extractRemark(String message) {
        int index = message.indexOf("备注");
        return index >= 0 ? message.substring(index + 2).trim() : "Agent 转账草稿";
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private long asLong(Object value, long defaultValue) {
        Long parsed = asLongObject(value);
        return parsed == null ? defaultValue : parsed;
    }

    private Long asLongObject(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Long.valueOf(String.valueOf(value));
    }
}
