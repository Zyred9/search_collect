package org.drinkless.robots.helper;

import cn.hutool.core.util.StrUtil;
import org.drinkless.tdlib.TdApi;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
public class StrHelper {

    public static String phoneNumber (String phoneNumber) {
        if (StrUtil.isBlank(phoneNumber)) {
            return "";
        }
        return StrUtil.replace(phoneNumber, "+", "");
    }

    public static String nickname (String first, String last) {
        if (StrUtil.isAllBlank(first, last)) {
            return "";
        }
        String name = first;
        if (StrUtil.isAllNotBlank(first, last)) {
            name = first + " " + last;
        }
        return specialChar(name);
    }


    public static String specialChar(String input) {
        String specialChar = "[_*~`>#+\\-=|{}.!]";
        return input.replaceAll(specialChar, "\\\\$0");
    }

    public static String specialResult(String input) {
        if (StrUtil.isBlank(input)) {
            return "";
        }
        String specialChar = "[_~>#+\\-=|{}.!]";
        return input.replaceAll(specialChar, "\\\\$0");
    }

    public static String specialLong(Long value) {
        return specialChar(java.lang.String.valueOf(value));
    }


    public static String getKey(Long userId) {
        return userId + "";
    }

    public static int extractPort(String jdbcUrl) {
        String regex = ":(\\d+)/";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(jdbcUrl);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }

    public static String getProcessor () {
        return "config:aa5ea63d928347cfa8ee3517e2eef31c";
    }

    public static String formatSecondsToTime(Integer seconds) {
        if (Objects.isNull(seconds) || seconds < 0) {
            return "";
        }

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        if (hours > 0) {
            return java.lang.String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return java.lang.String.format("%02d:%02d", minutes, secs);
        }
    }

    public static String formatMemberCount(int member) {
        // 格式化逻辑：大于1000显示为k
        if (member >= 1000) {
            double k = member / 1000.0;
            return String.format("%.1fk", k);
        }
        return String.valueOf(member);
    }
}
