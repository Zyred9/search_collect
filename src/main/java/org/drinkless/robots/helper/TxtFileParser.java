package org.drinkless.robots.helper;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.robots.beans.view.search.SearchBean;
import org.drinkless.robots.database.enums.SourceTypeEnum;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class TxtFileParser {

    /** source_name 字段最大长度限制 */
    private static final int MAX_SOURCE_NAME_LENGTH = 26;
    
    private static final Pattern LINK_PATTERN = Pattern.compile("https://t\\.me/([^/\\s)]+)(?:/(\\d+))?");
    private static final Pattern DURATION_PATTERN = Pattern.compile("\\[(\\d{1,2}:\\d{2}(?::\\d{2})?)\\]");
    private static final Pattern SUBSCRIBERS_PATTERN = Pattern.compile("\\)\\s*([\\d.]+[kKwW万千]?)\\s*$");
    private static final Pattern DATETIME_PATTERN = Pattern.compile("(\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{2})");
    private static final Map<String, SourceTypeEnum> EMOJI_TYPE_MAP = Map.of(
        "🎬", SourceTypeEnum.VIDEO,
        "🏞", SourceTypeEnum.PHOTO,
        "🎧", SourceTypeEnum.AUDIO,
        "💬", SourceTypeEnum.TEXT,
        "📢", SourceTypeEnum.CHANNEL,
        "👥", SourceTypeEnum.GROUP,
        "🤖", SourceTypeEnum.BOT,
        "📁", SourceTypeEnum.FILE
    );

    /**
     * 解析 TXT 文件内容为 SearchBean 列表
     *
     * @param content TXT 文件的文本内容
     * @return SearchBean 列表
     */
    public static List<SearchBean> parse(String content) {
        if (StrUtil.isBlank(content)) {
            log.warn("TXT 文件内容为空");
            return Collections.emptyList();
        }

        List<SearchBean> result = new ArrayList<>();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // 跳过空行
            if (StrUtil.isBlank(line)) {
                continue;
            }

            try {
                SearchBean bean = parseLine(line);
                if (Objects.nonNull(bean)) {
                    result.add(bean);
                }
            } catch (Exception e) {
                log.error("解析第 {} 行失败: {}, 错误: {}", i + 1, line, e.getMessage());
            }
        }

        log.info("TXT 文件解析完成，共解析 {} 条记录", result.size());
        return result;
    }

    private static SearchBean parseLine(String line) {
        // 提取 Telegram 链接
        Matcher linkMatcher = LINK_PATTERN.matcher(line);
        if (!linkMatcher.find()) {
            log.debug("未找到 Telegram 链接，跳过: {}", line);
            return null;
        }

        String channelUsername = linkMatcher.group(1);
        String messageIdStr = linkMatcher.group(2);
        String fullLink = linkMatcher.group(0);

        // 识别消息类型（根据表情符号）
        SourceTypeEnum type = detectType(line);

        // 创建 SearchBean
        SearchBean bean = new SearchBean()
            .setType(type)
            .setSourceUrl(fullLink)
            .setChannelUsername(channelUsername)
            .setChannelUrl("https://t.me/" + channelUsername)
            .setCollectTime(LocalDateTime.now());

        // 设置消息 ID 和聊天 ID（如果存在）
        if (StrUtil.isNotBlank(messageIdStr)) {
            bean.setMessageId(Long.parseLong(messageIdStr));
        }

        // 提取订阅数（行尾的数字）
        Matcher subsMatcher = SUBSCRIBERS_PATTERN.matcher(line);
        if (subsMatcher.find()) {
            bean.setSubscribers(subsMatcher.group(1));
        }

        // 提取时长（视频/音频）
        if (type == SourceTypeEnum.VIDEO || type == SourceTypeEnum.AUDIO) {
            Matcher durationMatcher = DURATION_PATTERN.matcher(line);
            if (durationMatcher.find()) {
                String duration = durationMatcher.group(1);
                bean.setTimes(parseTimeToSeconds(duration));
            }
        }

        // 提取标题/内容（从表情符号后到链接前的部分）
        String title = extractTitle(line, type);
        // 限制长度并截断
        String truncatedTitle = truncateSourceName(title);
        bean.setSourceName(truncatedTitle);
        bean.setChannelName(truncatedTitle);

        // 生成唯一 ID（基于链接）
        bean.setId(generateId());

        return bean;
    }

    /**
     * 检测消息类型（根据表情符号）
     *
     * @param line 文本行
     * @return 消息类型
     */
    private static SourceTypeEnum detectType(String line) {
        for (Map.Entry<String, SourceTypeEnum> entry : EMOJI_TYPE_MAP.entrySet()) {
            if (line.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        // 默认返回文本类型
        return SourceTypeEnum.TEXT;
    }

    private static String extractTitle(String line, SourceTypeEnum type) {
        String title = line;

        // 1. 去除开头的表情符号
        for (String emoji : EMOJI_TYPE_MAP.keySet()) {
            if (title.startsWith(emoji)) {
                title = title.substring(emoji.length()).trim();
                break;
            }
        }

        // 2. 去除时长标记（视频/音频）
        if (type == SourceTypeEnum.VIDEO || type == SourceTypeEnum.AUDIO) {
            title = DURATION_PATTERN.matcher(title).replaceFirst("").trim();
        }

        // 3. 去除日期时间（文本消息）
        if (type == SourceTypeEnum.TEXT) {
            title = DATETIME_PATTERN.matcher(title).replaceFirst("").trim();
        }

        // 4. 去除链接部分 (https://...)
        int linkStart = title.indexOf("(https://");
        if (linkStart != -1) {
            title = title.substring(0, linkStart).trim();
        }

        return title;
    }

    private static Integer parseTimeToSeconds(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            if (parts.length == 2) {
                // MM:SS 格式
                return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
            } else if (parts.length == 3) {
                // HH:MM:SS 格式
                return Integer.parseInt(parts[0]) * 3600
                    + Integer.parseInt(parts[1]) * 60
                    + Integer.parseInt(parts[2]);
            }
        } catch (NumberFormatException e) {
            log.warn("时长解析失败: {}", timeStr);
        }
        return null;
    }

    private static String generateId() {
        // 使用 hashCode 并转换为正数的 Long
        return UUID.fastUUID().toString(true);
    }
    
    /**
     * 截断 source_name 字段到指定长度
     * <p>
     * 如果超过最大长度，截断并添加省略号 "..."
     * </p>
     *
     * @param sourceName 原始名称
     * @return 截断后的名称
     */
    private static String truncateSourceName(String sourceName) {
        if (StrUtil.isBlank(sourceName)) {
            return "";
        }
        
        // 去除首尾空白
        sourceName = sourceName.trim();
        
        // 如果长度未超限，直接返回
        if (sourceName.length() <= MAX_SOURCE_NAME_LENGTH) {
            return sourceName;
        }
        
        // 超长则截断并添加省略号
        String truncated = StrUtil.sub(sourceName, 0, MAX_SOURCE_NAME_LENGTH - 3) + "...";
        log.debug("[TXT解析] source_name 超长已截断: 原长度={}, 截断后={}", sourceName.length(), truncated);
        
        return truncated;
    }
}
