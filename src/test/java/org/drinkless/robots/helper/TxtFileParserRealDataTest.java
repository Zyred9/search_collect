package org.drinkless.robots.helper;

import org.drinkless.robots.beans.view.search.SearchBean;
import org.drinkless.robots.database.enums.SourceTypeEnum;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 真实数据解析测试 - 读取 data.txt 文件并解析
 *
 * @author zyred
 * @since 1.0
 */
class TxtFileParserRealDataTest {

    /**
     * 读取 data.txt 文件并解析为 SearchBean 对象，然后打印到控制台
     */
    @Test
    void testParseRealDataFile() throws IOException {
        // 1. 读取文件路径（相对于项目根目录）
        String filePath = "src/main/resources/script/data.txt";
        
        System.out.println("========================================");
        System.out.println("开始读取文件: " + filePath);
        System.out.println("========================================\n");

        // 2. 读取文件内容
        String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
        
        System.out.println("文件内容预览（前200字符）:");
        System.out.println(content.substring(0, Math.min(200, content.length())));
        System.out.println("...\n");

        // 3. 解析文件
        List<SearchBean> searchBeans = TxtFileParser.parse(content);

        // 4. 打印统计信息
        System.out.println("========================================");
        System.out.println("解析统计");
        System.out.println("========================================");
        System.out.println("总行数: " + content.split("\n").length);
        System.out.println("解析成功: " + searchBeans.size() + " 条记录");
        System.out.println();

        // 5. 按类型分组统计
        Map<SourceTypeEnum, Long> typeCount = searchBeans.stream()
            .collect(Collectors.groupingBy(SearchBean::getType, Collectors.counting()));

        System.out.println("========================================");
        System.out.println("按类型统计");
        System.out.println("========================================");
        typeCount.forEach((type, count) -> 
            System.out.printf("%s %s: %d 条\n", type.getIcon(), type.getDesc(), count)
        );
        System.out.println();

        // 6. 打印详细信息
        System.out.println("========================================");
        System.out.println("详细记录列表");
        System.out.println("========================================\n");

        for (int i = 0; i < searchBeans.size(); i++) {
            SearchBean bean = searchBeans.get(i);
            System.out.println("【记录 " + (i + 1) + "】");
            System.out.println("  类型: " + bean.getType().getIcon() + " " + bean.getType().getDesc());
            System.out.println("  ID: " + bean.getId());
            System.out.println("  标题: " + bean.getSourceName());
            System.out.println("  链接: " + bean.getSourceUrl());
            System.out.println("  频道: " + bean.getChannelUsername());
            System.out.println("  消息ID: " + bean.getMessageId());
            
            if (bean.getTimes() != null) {
                System.out.println("  时长: " + formatSeconds(bean.getTimes()) + " (" + bean.getTimes() + "秒)");
            }
            
            if (bean.getSubscribers() != null) {
                System.out.println("  订阅数: " + bean.getSubscribers());
            }
            
            System.out.println("  采集时间: " + bean.getCollectTime());
            System.out.println();
        }

        // 7. 打印一些特殊案例
        System.out.println("========================================");
        System.out.println("特殊案例展示");
        System.out.println("========================================\n");

        // 视频类型
        searchBeans.stream()
            .filter(b -> b.getType() == SourceTypeEnum.VIDEO)
            .findFirst()
            .ifPresent(bean -> {
                System.out.println("【视频案例】");
                System.out.println("  原始可能格式: 🎬[" + formatSeconds(bean.getTimes()) + "] " + bean.getSourceName() + " (" + bean.getSourceUrl() + ")");
                System.out.println("  解析后标题: " + bean.getSourceName());
                System.out.println("  时长: " + bean.getTimes() + "秒");
                System.out.println();
            });

        // 频道类型（带订阅数）
        searchBeans.stream()
            .filter(b -> b.getType() == SourceTypeEnum.CHANNEL && b.getSubscribers() != null)
            .findFirst()
            .ifPresent(bean -> {
                System.out.println("【频道案例（带订阅数）】");
                System.out.println("  原始可能格式: 📢 " + bean.getSourceName() + " (" + bean.getSourceUrl() + ") " + bean.getSubscribers());
                System.out.println("  解析后标题: " + bean.getSourceName());
                System.out.println("  订阅数: " + bean.getSubscribers());
                System.out.println();
            });

        // 文本类型
        searchBeans.stream()
            .filter(b -> b.getType() == SourceTypeEnum.TEXT)
            .findFirst()
            .ifPresent(bean -> {
                System.out.println("【文本案例】");
                System.out.println("  原始可能格式: 💬 [日期时间] " + bean.getSourceName() + " (" + bean.getSourceUrl() + ")");
                System.out.println("  解析后标题: " + bean.getSourceName());
                System.out.println();
            });

        System.out.println("========================================");
        System.out.println("测试完成！");
        System.out.println("========================================");
    }

    /**
     * 格式化秒数为 HH:MM:SS 或 MM:SS
     *
     * @param seconds 秒数
     * @return 格式化后的时间字符串
     */
    private String formatSeconds(Integer seconds) {
        if (seconds == null) {
            return "00:00";
        }
        
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }

    /**
     * 简化版测试 - 只打印关键信息
     */
    @Test
    void testParseRealDataSimple() throws IOException {
        String filePath = "src/main/resources/script/data.txt";
        String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
        List<SearchBean> searchBeans = TxtFileParser.parse(content);

        System.out.println("\n========== 简化输出 ==========");
        System.out.println("解析成功: " + searchBeans.size() + " 条记录\n");

        searchBeans.forEach(bean -> {
            String timeInfo = bean.getTimes() != null ? " [" + formatSeconds(bean.getTimes()) + "]" : "";
            String subsInfo = bean.getSubscribers() != null ? " 👥" + bean.getSubscribers() : "";
            System.out.printf("%s%s %s%s\n", 
                bean.getType().getIcon(), 
                timeInfo,
                bean.getSourceName(), 
                subsInfo
            );
        });

        System.out.println("\n========== 完成 ==========");
    }

    /**
     * JSON格式输出测试
     */
    @Test
    void testParseRealDataAsJson() throws IOException {
        String filePath = "src/main/resources/script/data.txt";
        String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
        List<SearchBean> searchBeans = TxtFileParser.parse(content);

        System.out.println("\n========== JSON 格式输出（前3条） ==========\n");

        searchBeans.stream()
            .limit(3)
            .forEach(bean -> {
                System.out.println("{");
                System.out.println("  \"id\": " + bean.getId() + ",");
                System.out.println("  \"type\": \"" + bean.getType().getDesc() + "\",");
                System.out.println("  \"sourceName\": \"" + bean.getSourceName() + "\",");
                System.out.println("  \"sourceUrl\": \"" + bean.getSourceUrl() + "\",");
                System.out.println("  \"channelUsername\": \"" + bean.getChannelUsername() + "\",");
                System.out.println("  \"messageId\": " + bean.getMessageId() + ",");
                if (bean.getTimes() != null) {
                    System.out.println("  \"times\": " + bean.getTimes() + ",");
                }
                if (bean.getSubscribers() != null) {
                    System.out.println("  \"subscribers\": \"" + bean.getSubscribers() + "\",");
                }
                System.out.println("  \"collectTime\": \"" + bean.getCollectTime() + "\"");
                System.out.println("},\n");
            });

        System.out.println("========== 完成 ==========");
    }
}
