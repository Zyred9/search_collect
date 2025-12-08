package org.drinkless.robots.config;

import lombok.Getter;

/**
 * 自定义业务异常
 * <pre>
 * 用于业务逻辑中抛出可预期的异常
 * 异常信息将直接返回给前端用户
 * 
 * 使用示例：
 * throw new SelfException("用户不存在");
 * throw new SelfException(404, "资源未找到");
 * </pre>
 *
 * @author admin
 * @since 1.0
 */
@Getter
public class SelfException extends RuntimeException {

    /** 错误码，默认500 **/
    private final int code;
    /** 错误信息 **/
    private final String message;

    /**
     * 构造业务异常（默认错误码500）
     *
     * @param message 错误信息
     */
    public SelfException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    /**
     * 构造业务异常（自定义错误码）
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public SelfException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造业务异常（带原始异常）
     *
     * @param message 错误信息
     * @param cause   原始异常
     */
    public SelfException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
        this.message = message;
    }

    /**
     * 构造业务异常（完整参数）
     *
     * @param code    错误码
     * @param message 错误信息
     * @param cause   原始异常
     */
    public SelfException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
}
