package com.student.bank.common;

public enum ErrorCode {
    PARAM_ERROR(40001, "参数错误"),
    UNAUTHORIZED(40101, "未登录或登录已过期"),
    FORBIDDEN(40301, "无权限访问"),
    DUPLICATE_SUBMIT(40901, "重复提交"),
    ACCOUNT_LOCKED(42301, "账户已锁定"),
    RATE_LIMITED(42901, "操作过于频繁"),
    RISK_REJECTED(42902, "交易触发风控拦截"),
    SYSTEM_ERROR(50001, "系统异常，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
