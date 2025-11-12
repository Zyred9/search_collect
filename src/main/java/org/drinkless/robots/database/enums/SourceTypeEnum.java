package org.drinkless.robots.database.enums;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

/**
 *
 *
 * @author zyred
 * @since 2025/11/9 21:16
 */
@Getter
@AllArgsConstructor
public enum SourceTypeEnum {

    CHANNEL("0", "频道", "\uD83D\uDCE2"),
    GROUP("1", "群组", "\uD83D\uDC65"),
    VIDEO("2", "视频", "\uD83C\uDFAC"),
    PHOTO("3", "图片", "\uD83C\uDFDE"),
    AUDIO("4", "音频", "\uD83C\uDFA7"),
    TEXT("5", "文本", "\uD83D\uDCAC"),
    BOT("6", "机器人", "\uD83E\uDD16"),
    FILE("7", "文件(txt、gif、压缩包、pdf等)", "\uD83D\uDCC1"),

    FLUSH("100", "刷新", "\uD83D\uDD04"),
    ;


    @JsonValue
    @EnumValue
    private final String code;
    private final String desc;
    private final String icon;

    private static final Set<String> VIEWS = Set.of(VIDEO.code, PHOTO.code, AUDIO.code);
    private static final SourceTypeEnum[] KEYBOARDS = new SourceTypeEnum[]{CHANNEL, GROUP, VIDEO, PHOTO, AUDIO, TEXT, BOT, FILE};

    public static SourceTypeEnum[] keyboards (){
        return KEYBOARDS;
    }
    public static Set<String> views () {
        return VIEWS;
    }


    @JsonCreator
    public static SourceTypeEnum fromCode(String code) {
        for (SourceTypeEnum type : SourceTypeEnum.values()) {
            if (StrUtil.equals(type.code, code)) {
                return type;
            }
        }
        return null;
    }
 }
