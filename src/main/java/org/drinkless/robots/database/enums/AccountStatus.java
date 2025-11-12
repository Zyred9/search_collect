package org.drinkless.robots.database.enums;


import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountStatus {

    INITIALIZING(0, "初始化"),
    NEED_CODE(1, "需要验证码"),
    LOGGED_IN(2, "已登录"),
    LOGIN_FAILED(3, "登录失败"),
    OFFLINE(4, "下线");

    @EnumValue
    private final int code;;
    private final String desc;
}