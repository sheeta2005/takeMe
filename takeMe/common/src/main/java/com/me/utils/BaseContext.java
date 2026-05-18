package com.me.utils;

public class BaseContext {

    // 存储当前登录用户ID
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    // 存储当前登录用户身份：admin/elder/volunteer
    private static final ThreadLocal<String> USER_ROLE = new ThreadLocal<>();

    /**
     * 设置用户ID
     * @param id 用户ID
     */
    public static void setUserId(Long id) {
        USER_ID.set(id);
    }

    /**
     * 获取用户ID
     * @return 用户ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 设置用户身份
     * @param role 身份（admin/elder/volunteer）
     */
    public static void setUserRole(String role) {
        USER_ROLE.set(role);
    }

    /**
     * 获取用户身份
     * @return 身份
     */
    public static String getUserRole() {
        return USER_ROLE.get();
    }

    /**
     * 清除ThreadLocal
     */
    public static void remove() {
        USER_ID.remove();
        USER_ROLE.remove();
    }
}