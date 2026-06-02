package com.student.bank.common;

import lombok.Data;
import org.slf4j.MDC;

@Data
public class Result<T> {
    private Integer code;
    private String msg;
    private T data;
    private String traceId;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg("success");
        r.setData(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    public static <T> Result<T> success(String msg, T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMsg(msg);
        r.setData(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    public static <T> Result<T> error(String msg) {
        return error(ErrorCode.SYSTEM_ERROR.getCode(), msg);
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> Result<T> error(ErrorCode errorCode, String msg) {
        return error(errorCode.getCode(), msg);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
