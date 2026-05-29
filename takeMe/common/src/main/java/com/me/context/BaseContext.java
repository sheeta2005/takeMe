package com.me.context;
public class BaseContext {

    private static final ThreadLocal<Long> LOGIN_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> LOGIN_TYPE = new ThreadLocal<>();

    public static void setLoginId(Long id) {
        LOGIN_ID.set(id);
    }

    public static Long getLoginId() {
        return LOGIN_ID.get();
    }

    public static void setLoginType(Integer type) {
        LOGIN_TYPE.set(type);
    }

    public static Integer getLoginType() {
        return LOGIN_TYPE.get();
    }

    public static void clear() {
        LOGIN_ID.remove();
        LOGIN_TYPE.remove();
    }

    // 管理员
    public static boolean isAdmin() {
        return Integer.valueOf(0).equals(getLoginType());
    }

    // 志愿者
    public static boolean isVolunteer() {
        return Integer.valueOf(1).equals(getLoginType());
    }

    // 普通用户（老人）
    public static boolean isUser() {
        return Integer.valueOf(2).equals(getLoginType());
    }
}