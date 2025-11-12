package org.drinkless.robots.helper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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
public class TimeHelper {

    public static String formatV2_ (LocalDateTime time) {
        if (Objects.isNull(time)) {
            return null;
        }
        return StrHelper.specialResult(TimeHelper.format(time, "MM-dd HH:mm:ss"));
    }

    public static String format(LocalDateTime time) {
        if (Objects.isNull(time)) {
            return null;
        }
        return format(time, "yyyy-MM-dd HH:mm:ss");
    }


    public static String format(LocalDateTime time, String pattener) {
        if (Objects.isNull(time)) {
            return null;
        }
        return time.format(DateTimeFormatter.ofPattern(pattener));
    }

    public static long getTimestamp(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();
        ZoneOffset hongKongOffset = ZoneOffset.of("+08:00");
        return now.toEpochSecond(hongKongOffset);
    }

    public static String getTimestamp(String status, int wasOnline) {
        LocalDateTime time = LocalDateTime.ofEpochSecond(wasOnline, 0, ZoneOffset.of("+08:00"));
        return status + ",上次上线:" + format(time);
    }

    // 将 1分钟，1小时，10小时，1天，2天 转换为 分钟

    public static Integer convertMinutes(String timeStr) {
        Pattern pattern = Pattern.compile("(\\d+)([天小时分钟秒月]+)");
        Matcher matcher = pattern.matcher(timeStr);

        if (matcher.find()) {
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "秒", "分钟":
                    return amount;
                case "小时":
                    return amount * 60;
                case "天":
                    return amount * 24 * 60;
                case "月":
                    return amount * 30 * 24 * 60;
            }
        }
        return null;
    }


    public static void main(String[] args) {
        Integer minutes = convertMinutes("1分钟");
        System.out.println(minutes);
    }


    /**
     * 获取解封时间
     *
     * @param minutes       分钟
     * @return              解封时间
     */
    public static long getTelegramTime (Integer minutes) {
        return System.currentTimeMillis() / 1000 + (minutes * 60);
    }
    // 将分钟转换为 1分钟，1小时，10小时，1天，2天

    public static String convertTime(Integer minutes) {
        if (Objects.isNull(minutes)) {
            return null;
        }
        if (minutes < 60) {
            return minutes + "分钟";
        } else if (minutes < 60 * 24) {
            return minutes / 60 + "小时";
        } else if (minutes < 60 * 24 * 30) {
            return minutes / 60 / 24 + "天";
        } else {
            return minutes / 60 / 24 / 30 + "月";
        }
    }
}
