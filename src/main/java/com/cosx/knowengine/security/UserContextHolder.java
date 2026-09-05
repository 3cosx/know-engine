package com.cosx.knowengine.security;

import com.cosx.knowengine.exception.BusinessException;

import java.util.Optional;

public final class UserContextHolder {

    private static final ThreadLocal<CurrentUser> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(CurrentUser user) {
        CONTEXT.set(user);
    }

    public static Optional<CurrentUser> get() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static CurrentUser requireCurrentUser() {
        return get().orElseThrow(() -> BusinessException.unauthorized("当前请求未登录"));
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
