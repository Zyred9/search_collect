package org.drinkless.robots.helper;

import org.drinkless.robots.beans.view.search.SearchBean;
import org.drinkless.robots.database.enums.SourceTypeEnum;

/**
 * ContentFilter 测试工具类
 * <p>
 * 用于快速测试内容过滤功能
 * </p>
 *
 * @author admin
 * @since 1.0
 */
public class ContentFilterTest {

    public static void main(String[] args) {
        System.out.println("========== 内容过滤测试 ==========\n");

        // 测试案例1：正常中文内容
        testFilter("正常中文消息，这是一个测试", false);

        // 测试案例2：诈骗机器人
        testFilter("请使用 @jisou 搜索资源", true);
        testFilter("输入 /jisou123 查找", true);

        // 测试案例3：敏感关键词
        testFilter("这里有儿童视频，快来看", true);
        testFilter("日赚1000元，加微信领取", true);
        testFilter("萝莉资源分享", true);

        // 测试案例4：英文内容
        testFilter("Hello world, this is a test message", true);

        // 测试案例5：俄文内容
        testFilter("Привет мир", true);

        // 测试案例6：混合内容（中文占比高）
        testFilter("这是一条混合消息 with some English words", false);

        // 测试案例7：混合内容（中文占比低）
        testFilter("Hello 你好 world 世界", true);

        // 测试案例8：空内容
        testFilter("", true);
        testFilter("   ", true);

        System.out.println("\n========== 测试完成 ==========");
    }

    private static void testFilter(String content, boolean expectedFiltered) {
        SearchBean bean = new SearchBean()
            .setId("")
            .setSourceName(content)
            .setType(SourceTypeEnum.TEXT)
            .setChatId(123456L)
            .setMessageId(789L);

        boolean actualFiltered = ContentFilter.shouldFilter(bean);
        String result = actualFiltered == expectedFiltered ? "✅ 通过" : "❌ 失败";

        System.out.printf("%s | 内容: %-40s | 预期: %-4s | 实际: %-4s\n",
            result,
            content.length() > 40 ? content.substring(0, 37) + "..." : content,
            expectedFiltered ? "过滤" : "保留",
            actualFiltered ? "过滤" : "保留"
        );
    }
}
