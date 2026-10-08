package com.me.context;
public class BaseContext {

    private static final ThreadLocal<Long> LOGIN_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> LOGIN_TYPE = new ThreadLocal<>();

    //保存当前登录账号
    public static void setLoginId(Long id) {
        LOGIN_ID.set(id);
    }

    //获取当前登录账号
    public static Long getLoginId() {
        return LOGIN_ID.get();
    }

    //保存当前登录角色
    public static void setLoginType(Integer type) {
        LOGIN_TYPE.set(type);
    }

    //获取当前登录角色
    public static Integer getLoginType() {
        return LOGIN_TYPE.get();
    }

    //清理当前线程登录信息
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