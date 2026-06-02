package com.student.bank.common;

public final class AuthContext {
    private static final ThreadLocal<AuthUser> CURRENT = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthUser user) {
        CURRENT.set(user);
    }

    public static AuthUser get() {
        return CURRENT.get();
    }

    public static AuthUser requireUser() {
        AuthUser user = CURRENT.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public static Long userId() {
        return requireUser().getUserId();
    }

    public static boolean isAdmin() {
        AuthUser user = CURRENT.get();
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
    }

    public static void clear() {
        CURRENT.remove();
    }
}
