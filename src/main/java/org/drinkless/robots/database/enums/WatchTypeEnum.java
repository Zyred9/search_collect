package org.drinkless.robots.database.enums;


import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WatchTypeEnum {

    GROUP(1, "群组"),
    SUPER_GROUP(2, "超级群组"),
    CHANNEL(3, "频道");

    @EnumValue
    private final int code;;
    private final String desc;
}