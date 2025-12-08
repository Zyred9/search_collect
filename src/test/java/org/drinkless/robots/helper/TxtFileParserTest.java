package org.drinkless.robots.helper;

import org.drinkless.robots.beans.view.search.SearchBean;
import org.drinkless.robots.database.enums.SourceTypeEnum;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TxtFileParser 单元测试
 *
 * @author admin
 * @since 1.0
 */
class TxtFileParserTest {

    /**
     * 测试解析视频消息
     */
    @Test
    void testParseVideoMessage() {
        String content = "🎬[03:01] 精神小妹_与的那些事_044_双S训狗_17509353712 (https://t.me/smtj0038206/13883)";
        
        List<SearchBean> beans = TxtFileParser.parse(content);
        
        assertEquals(1, beans.size());
        SearchBean bean = beans.get(0);
        
        assertEquals(SourceTypeEnum.VIDEO, bean.getType());
        assertEquals("精神小妹_与的那些事_044_双S训狗_17509353712", bean.getSourceName());
        assertEquals("https://t.me/smtj0038206/13883", bean.getSourceUrl());
        assertEquals("smtj0038206", bean.getChannelUsername());
        assertEquals(13883L, bean.getMessageId());
        assertEquals(181, bean.getTimes()); // 3分1秒 = 181秒
    }

    /**
     * 测试解析文本消息
     */
    @Test
    void testParseTextMessage() {
        String content = "💬 11-02 00:28 听说广西精神小妹很多是不是真的 (https://t.me/mugoutiantang888/71530)";
        
        List<SearchBean> beans = TxtFileParser.parse(content);
        
        assertEquals(1, beans.size());
        SearchBean bean = beans.get(0);
        
        assertEquals(SourceTypeEnum.TEXT, bean.getType());
        assertEquals("听说广西精神小妹很多是不是真的", bean.getSourceName());
        assertEquals("https://t.me/mugoutiantang888/71530", bean.getSourceUrl());
    }

    /**
     * 测试解析图片消息
     */
    @Test
    void testParsePhotoMessage() {
        String content = "🏞 精神小妹可yy (https://t.me/mugoutiantang888/72546)";
        
        List<SearchBean> beans = TxtFileParser.parse(content);
        
        assertEquals(1, beans.size());
        SearchBean bean = beans.get(0);
        
        assertEquals(SourceTypeEnum.PHOTO, bean.getType());
        assertEquals("精神小妹可yy", bean.getSourceName());
    }

    /**
     * 测试解析频道消息（带订阅数）
     */
    @Test
    void testParseChannelMessage() {
        String content = "📢 收藏纹身精神小妹极品资源 (https://t.me/crbkry) 1k";
        
        List<SearchBean> beans = TxtFileParser.parse(content);
        
        assertEquals(1, beans.size());
        SearchBean bean = beans.get(0);
        
        assertEquals(SourceTypeEnum.CHANNEL, bean.getType());
        assertEquals("收藏纹身精神小妹极品资源", bean.getSourceName());
        assertEquals("https://t.me/crbkry", bean.getSourceUrl());
        assertEquals("crbkry", bean.getChannelUsername());
        assertEquals("1k", bean.getSubscribers());
    }

    /**
     * 测试解析多行内容
     */
    @Test
    void testParseMultipleLines() {
        String content = """
            🏞 精神小妹可yy (https://t.me/mugoutiantang888/72546)
            💬 10-17 08:33 我想跪舔精神小妹纹身太妹 (https://t.me/mugoutiantang888/55640)
            🎬[03:01] 精神小妹_与的那些事_044_双S训狗 (https://t.me/smtj0038206/13883)
            📢 收藏纹身精神小妹极品资源 (https://t.me/crbkry) 1k
            
            📢 精神小妹玉足抖音网红 (https://t.me/sexjsxmyz) 302
            """;
        
        List<SearchBean> beans = TxtFileParser.parse(content);
        
        assertEquals(5, beans.size());
        assertEquals(SourceTypeEnum.PHOTO, beans.get(0).getType());
        assertEquals(SourceTypeEnum.TEXT, beans.get(1).getType());
        assertEquals(SourceTypeEnum.VIDEO, beans.get(2).getType());
        assertEquals(SourceTypeEnum.CHANNEL, beans.get(3).getType());
        assertEquals(SourceTypeEnum.CHANNEL, beans.get(4).getType());
    }

    /**
     * 测试解析空内容
     */
    @Test
    void testParseEmptyContent() {
        String content = "";
        List<SearchBean> beans = TxtFileParser.parse(content);
        assertTrue(beans.isEmpty());
    }

    /**
     * 测试解析无效内容（没有链接）
     */
    @Test
    void testParseInvalidContent() {
        String content = "这是一段没有链接的文本";
        List<SearchBean> beans = TxtFileParser.parse(content);
        assertTrue(beans.isEmpty());
    }
}
