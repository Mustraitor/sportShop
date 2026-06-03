package org.server.utils;

/**
 * 基于 ThreadLocal 的工具类，用于在当前线程的生命周期内保存和获取登录用户的 ID
 */
public class BaseContext {

    // 1. 创建一个只能由当前请求线程访问的私有变量（ThreadLocal 隐形口袋）
    private static final ThreadLocal<Long> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 往当前线程的口袋里存入用户 ID
     * @param id 登录用户的唯一标识
     */
    public static void setCurrentId(Long id) {
        THREAD_LOCAL.set(id);
    }

    /**
     * 从当前线程的口袋里取出用户 ID
     * @return 当前登录用户的 ID
     */
    public static Long getCurrentId() {
        return THREAD_LOCAL.get();
    }

    /**
     * 移除当前线程口袋里的数据（防止 Tomcat 线程复用引发的内存泄漏）
     */
    public static void removeCurrentId() {
        THREAD_LOCAL.remove();
    }
}