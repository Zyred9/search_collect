package org.drinkless.robots.helper;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.robots.beans.view.search.SearchBean;
import org.drinkless.robots.database.enums.SourceTypeEnum;
import org.drinkless.robots.database.enums.AuditStatusEnum;
import org.drinkless.tdlib.TdApi;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * TDLib 消息转换为 SearchBean 的工具类
 * <p>
 * 功能：
 * <ul>
 *   <li>将 TDLib 的 Message 对象转换为 Elasticsearch 的 SearchBean</li>
 *   <li>根据消息类型映射 SourceTypeEnum</li>
 *   <li>提取文本、媒体标题、频道信息等</li>
 *   <li>处理话题标签转链接</li>
 * </ul>
 *
 * @author zyred
 * @since 1.0
 */
@Slf4j
public class MessageConverter {

    private static final String TELEGRAM_BASE_URL = "https://t.me";
    
    /** source_name 字段最大长度限制 */
    private static final int MAX_SOURCE_NAME_LENGTH = 20;

    /**
     * 将 TDLib 消息转换为 SearchBean（带 Supergroup 信息）
     *
     * @param message    TDLib 消息对象
     * @param chat       TDLib 聊天对象
     * @param supergroup TDLib Supergroup 对象（可为 null，用于提取 username）
     * @param weight           权重
     * @return SearchBean 对象，如果消息类型不支持则返回 null
     */
    public static SearchBean convertToSearchBean(TdApi.Message message, TdApi.Chat chat, TdApi.Supergroup supergroup, int weight) {
        if (Objects.isNull(message) || Objects.isNull(chat)) {
            return null;
        }

        SourceTypeEnum sourceType = mapContentType(message.content);
        if (Objects.isNull(sourceType)) {
            return null;
        }

        String sourceName = extractSourceName(message.content);
        if (StrUtil.isBlank(sourceName)) {
            return null;
        }
        
        SearchBean bean = new SearchBean();
        bean.setType(sourceType);
        // 核心标识字段
        bean.setId(generateDocId(message.chatId, message.id));

        // 先提取 username,避免重复调用
        String username = extractUsername(supergroup);
        
        bean.setSourceName(sourceName);
        bean.setSourceUrl(buildMessageUrl(chat, message, username));
        
        // 频道/群组信息
        bean.setChatId(message.chatId);
        bean.setChannelName(StrUtil.nullToEmpty(chat.title));
        bean.setChannelUsername(StrUtil.nullToEmpty(username));
        bean.setChannelUrl(StrUtil.nullToEmpty(buildChannelUrl(username)));

        // 展示次数
        bean.setSubscribers(Objects.nonNull(supergroup) ? StrHelper.formatMemberCount(supergroup.memberCount) : null);
        bean.setViews(Objects.nonNull(message.interactionInfo) ? message.interactionInfo.viewCount : 0);
        
        // 消息元数据（TDLib 的 message.id 右移20位得到真实的消息ID）
        bean.setMessageId(message.id >> 20);
        bean.setCollectTime(message.date * 1000L);

        // 审核状态初始化（频道/群组相关消息统一初始化）
        bean.setAuditStatus(AuditStatusEnum.PENDING);
        bean.setAuditRemark("");
        bean.setAuditedBy("");
        bean.setAuditedAt(System.currentTimeMillis());
        bean.setMarked(isRestrictedContent(message, supergroup));
        bean.setWeight(weight);
        extractMediaInfo(message.content, bean);
        return bean;
    }

    /**
     * 生成 Elasticsearch 文档 ID
     * <p>
     * 使用 chatId 和 真实messageId 组合，确保唯一性
     * 注意：TDLib 的 message.id 需要右移20位才能得到真实的消息ID
     * </p>
     */
    private static String generateDocId(long chatId, long messageId) {
        // TDLib 的 message.id 右移20位得到真实的消息ID
        long realMessageId = messageId >> 20;
        return Math.abs(chatId) + String.valueOf(realMessageId);
    }

    /**
     * 映射 TDLib 消息类型到 SourceTypeEnum
     */
    private static SourceTypeEnum mapContentType(TdApi.MessageContent content) {
        if (content instanceof TdApi.MessageText) {
            return SourceTypeEnum.TEXT;
        }
        if (content instanceof TdApi.MessagePhoto) {
            return SourceTypeEnum.PHOTO;
        }
        if (content instanceof TdApi.MessageVideo) {
            return SourceTypeEnum.VIDEO;
        }
        if (content instanceof TdApi.MessageAudio || content instanceof TdApi.MessageVoiceNote) {
            return SourceTypeEnum.AUDIO;
        }
        if (content instanceof TdApi.MessageDocument || 
            content instanceof TdApi.MessageAnimation) {
            return SourceTypeEnum.FILE;
        }
        // 其他类型不保存
        return null;
    }

    /**
     * 提取消息源名称（文本或媒体标题）
     * <p>
     * 限制长度不超过 {@link #MAX_SOURCE_NAME_LENGTH} 个字符，超出部分截断并添加省略号
     * </p>
     */
    private static String extractSourceName(TdApi.MessageContent content) {
        String sourceName;
        
        if (content instanceof TdApi.MessageText mt) {
            sourceName = mt.text != null && mt.text.text != null ? mt.text.text : "";
        } else {
            // 媒体类型提取 caption
            String caption = "";
            if (content instanceof TdApi.MessagePhoto mp) {
                caption = mp.caption.text;
            } else if (content instanceof TdApi.MessageVideo mv) {
                if (StrUtil.isNotBlank(mv.caption.text)) {
                    caption = mv.caption.text;
                } else {
                    caption = mv.video.fileName;
                }
            } else if (content instanceof TdApi.MessageDocument md) {
                if (StrUtil.isNotBlank(md.caption.text)) {
                    caption = md.caption.text;
                } else {
                    caption = md.document.fileName;
                }
            } else if (content instanceof TdApi.MessageAudio ma) {
                if (StrUtil.isNotBlank(ma.caption.text)) {
                    caption = ma.caption.text;
                } else {
                    caption = ma.audio.fileName;
                }
            } else if (content instanceof TdApi.MessageVoiceNote mvn) {
                caption = mvn.caption.text;
            } else if (content instanceof TdApi.MessageAnimation man) {
                caption = man.caption.text;
            }
            sourceName = caption;
        }
        
        // 限制长度并截断
        return truncateSourceName(sourceName);
    }
    
    private static String truncateSourceName(String sourceName) {
        if (StrUtil.isBlank(sourceName)) {
            return "";
        }
        sourceName = sourceName.replaceAll("[\\r\\n\\t]", "");
        sourceName = sourceName.replaceAll("\\s+", " ");
        sourceName = sourceName.replace("#", "_");
        sourceName = sourceName.trim();
        if (sourceName.length() > MAX_SOURCE_NAME_LENGTH) {
            return StrUtil.sub(sourceName, 0, MAX_SOURCE_NAME_LENGTH);
        }
        return sourceName;
    }

    private static String buildMessageUrl(TdApi.Chat chat, TdApi.Message message, String username) {
        // 只处理超级群组/频道
        if (!(chat.type instanceof TdApi.ChatTypeSupergroup)) {
            return "";
        }

        // TDLib 的 message.id 右移20位得到真实的消息ID
        long realMessageId = message.id >> 20;

        if (StrUtil.isNotBlank(username)) {
            return StrUtil.format("{}/{}/{}", TELEGRAM_BASE_URL, username, realMessageId);
        }

        long chatIdForUrl = Math.abs(chat.id) - 1000000000000L;
        return StrUtil.format("{}/c/{}/{}", TELEGRAM_BASE_URL, chatIdForUrl, realMessageId);
    }
    
    /**
     * 构建频道/群组链接
     */
    private static String buildChannelUrl(String username) {
        if (StrUtil.isNotBlank(username)) {
            return TELEGRAM_BASE_URL + "/" + username;
        }
        // 私密群组没有公开链接
        return "";
    }

    private static boolean isRestrictedContent(TdApi.Message message, TdApi.Supergroup supergroup) {
        // 1. 检查群组/频道级别的敏感内容标记
        if (Objects.nonNull(supergroup)) {
            if (supergroup.hasSensitiveContent) {
                return true;
            }
            if (StrUtil.isNotBlank(supergroup.restrictionReason)) {
                return true;
            }
        }
        
        // 2. 检查消息级别的敏感内容标记
        if (message.hasSensitiveContent) {
            return true;
        }
        return StrUtil.isNotBlank(message.restrictionReason);
    }
    
    private static String extractUsername(TdApi.Supergroup supergroup) {
        if (Objects.isNull(supergroup)) {
            return "";

        }
        // 提取 active usernames 中的第一个
        if (Objects.nonNull(supergroup.usernames) && 
            Objects.nonNull(supergroup.usernames.activeUsernames) &&
            supergroup.usernames.activeUsernames.length > 0) {
            return supergroup.usernames.activeUsernames[0];
        }
        
        return "";
    }

    private static void extractMediaInfo(TdApi.MessageContent content, SearchBean bean) {
        if (content instanceof TdApi.MessageVideo mv) {
            bean.setTimes(mv.video.duration);
        } else if (content instanceof TdApi.MessageAudio ma) {
            bean.setTimes(ma.audio.duration);
        } else if (content instanceof TdApi.MessageVoiceNote mvn) {
            bean.setTimes(mvn.voiceNote.duration);
        }
    }

    /**
     * 转换时间戳为 LocalDateTime
     */
    private static LocalDateTime convertTimestamp(int timestamp) {
        return LocalDateTime.ofInstant(
            Instant.ofEpochSecond(timestamp),
            ZoneId.systemDefault()
        );
    }
}
