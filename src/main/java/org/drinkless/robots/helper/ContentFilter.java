package org.drinkless.robots.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.robots.beans.view.search.SearchBean;

import java.util.Arrays;
import java.util.List;

/**
 * 内容安全过滤器
 * <p>
 * 功能：
 * <ul>
 *   <li>检测诈骗机器人（jisou 等）</li>
 *   <li>检测敏感关键词（儿童色情、诈骗等）</li>
 *   <li>语言过滤（仅保留中文内容）</li>
 * </ul>
 *
 * @author zyred
 * @since 1.0
 */
@Slf4j
public class ContentFilter {

    // ==================== 诈骗机器人黑名单 ====================
    private static final List<String> JISOU_KEYWORDS = Arrays.asList(
        "@jisou", "/jisou", "jisou1bot", "jisou2bot", "jisou123",
        "jisoubot", "ji_sou", "jі_sou" // 变体防止规避
    );

    // ==================== 敏感关键词黑名单 ====================
    private static final List<String> SENSITIVE_KEYWORDS = Arrays.asList(
        // 儿童色情相关
        "cp", "儿童", "幼女", "萝莉", "正太", "童",
        // 诈骗相关
        "刷单", "兼职赚钱", "日赚", "免费领取", "加微信",
        // 博彩相关
        "菠菜", "开户", "彩票", "赌博"
    );

    // ==================== 语言检测器（仅检测中文） ====================
    private static final LanguageDetector LANGUAGE_DETECTOR = LanguageDetectorBuilder
        .fromLanguages(Language.CHINESE, Language.ENGLISH, Language.RUSSIAN, Language.ARABIC)
        .build();

    /**
     * 检查内容是否应该被过滤
     * <p>
     * 过滤规则：
     * <ol>
     *   <li>包含诈骗机器人关键词 → 过滤</li>
     *   <li>包含敏感关键词 → 过滤</li>
     *   <li>非中文内容 → 过滤</li>
     * </ol>
     * </p>
     *
     * @param searchBean 待检测的搜索文档
     * @return true=需要过滤（丢弃），false=保留
     */
    public static boolean shouldFilter(SearchBean searchBean) {
        if (searchBean == null || StrUtil.isBlank(searchBean.getSourceName())) {
            return true; // 空内容直接过滤
        }

        String content = searchBean.getSourceName().toLowerCase();

        // 1. 检测诈骗机器人
        if (containsJisou(content)) {
            log.warn("[内容过滤] 检测到诈骗机器人，chatId={}, msgId={}, content={}", 
                searchBean.getChatId(), searchBean.getMessageId(), 
                StrUtil.brief(content, 50));
            return true;
        }

        // 2. 检测敏感关键词
        if (containsSensitiveKeywords(content)) {
            log.warn("[内容过滤] 检测到敏感关键词，chatId={}, msgId={}, content={}", 
                searchBean.getChatId(), searchBean.getMessageId(), 
                StrUtil.brief(content, 50));
            return true;
        }

        // 3. 语言过滤（仅保留中文）
        if (!isChineseContent(content)) {
            log.debug("[内容过滤] 非中文内容，chatId={}, msgId={}", 
                searchBean.getChatId(), searchBean.getMessageId());
            return true;
        }

        return false;
    }

    /**
     * 检测是否包含诈骗机器人关键词
     */
    private static boolean containsJisou(String content) {
        return JISOU_KEYWORDS.stream()
            .anyMatch(keyword -> content.contains(keyword.toLowerCase()));
    }

    /**
     * 检测是否包含敏感关键词
     */
    private static boolean containsSensitiveKeywords(String content) {
        return SENSITIVE_KEYWORDS.stream()
            .anyMatch(keyword -> content.contains(keyword.toLowerCase()));
    }

    /**
     * 检测是否为中文内容
     * <p>
     * 检测逻辑：
     * <ul>
     *   <li>使用 Lingua 语言检测库</li>
     *   <li>要求中文置信度 > 50%</li>
     *   <li>或包含中文字符的比例 > 30%</li>
     * </ul>
     * </p>
     */
    private static boolean isChineseContent(String content) {
        if (StrUtil.isBlank(content)) {
            return false;
        }

        // 去除空格和标点，只保留有效字符
        String cleanContent = content.replaceAll("[\\s\\p{Punct}]+", "");
        if (cleanContent.length() < 3) {
            return false; // 内容太短，无法判断
        }

        // 方法1：使用语言检测库
        try {
            Language detectedLanguage = LANGUAGE_DETECTOR.detectLanguageOf(cleanContent);
            if (detectedLanguage == Language.CHINESE) {
                return true;
            }
        } catch (Exception e) {
            log.debug("[语言检测] 检测失败，使用备用方案: {}", e.getMessage());
        }

        // 方法2：统计中文字符比例（备用方案）
        return calculateChineseRatio(cleanContent) > 0.3;
    }

    /**
     * 计算中文字符占比
     */
    private static double calculateChineseRatio(String text) {
        if (StrUtil.isBlank(text)) {
            return 0.0;
        }

        long chineseCount = text.chars()
            .filter(ContentFilter::isChineseChar)
            .count();

        return (double) chineseCount / text.length();
    }

    /**
     * 判断是否为中文字符（包括中文标点）
     */
    private static boolean isChineseChar(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
            || block == Character.UnicodeBlock.GENERAL_PUNCTUATION;
    }

    /**
     * 批量过滤消息列表
     *
     * @param searchBeans 待过滤的消息列表
     * @return 过滤后的消息列表
     */
    public static List<SearchBean> filterList(List<SearchBean> searchBeans) {
        if (CollUtil.isEmpty(searchBeans)) {
            return searchBeans;
        }

        List<SearchBean> filtered = searchBeans.stream()
            .filter(bean -> !shouldFilter(bean))
            .toList();

        int filteredCount = searchBeans.size() - filtered.size();
        if (filteredCount > 0) {
            log.info("[内容过滤] 过滤了 {} 条消息，保留 {} 条", filteredCount, filtered.size());
        }

        return filtered;
    }
}
