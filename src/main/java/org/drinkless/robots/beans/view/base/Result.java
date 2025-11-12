package org.drinkless.robots.beans.view.base;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 通用返回结果封装类
 * @param <T> 返回数据类型
 */
@Setter
@Getter
public class Result<T> implements Serializable {
    /** 状态码，200 表示成功 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 返回数据 */
    private T data;

    public Result() {}

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功（无数据） */
    public static <T> Result<T> success() {
        return new Result<>(200, "Success", null);
    }

    /** 成功（有数据） */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "Success", data);
    }

    /** 成功（自定义信息和数据） */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /** 失败（默认 500） */
    public static <T> Result<T> error() {
        return new Result<>(500, "Internal Server Error", null);
    }

    /** 失败（自定义信息） */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /** 失败（自定义状态码和信息） */
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {
        return this.code == 200;
    }
}
