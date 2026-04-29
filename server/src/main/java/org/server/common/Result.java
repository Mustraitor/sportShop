package org.server.common;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;  // 200成功 400参数错误 401未登录 500异常
    private String msg;
    private T data;

    // ================= 成功 =================

    public static <T> Result<T> success(T data) {
        return build(200, "success", data);
    }

    public static <T> Result<T> success(String msg, T data) {
        return build(200, msg, data);
    }

    // ================= 失败 =================

    public static <T> Result<T> error(String msg) {
        return build(500, msg, null);
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return build(code, msg, null);
    }

    // ================= 核心方法 =================

    private static <T> Result<T> build(Integer code, String msg, T data) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        r.data = data;
        return r;
    }
}
