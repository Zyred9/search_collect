package org.drinkless.robots.helper;

import org.drinkless.robots.beans.view.search.SearchBean;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 快速解析测试 - 读取 data.txt 并打印
 *
 * @author admin
 * @since 1.0
 */
class QuickParseTest {

    @Test
    void testQuickParse() throws Exception {
        // 读取文件
        String content = Files.readString(
            Paths.get("src/main/resources/script/data.txt"), 
            StandardCharsets.UTF_8
        );

        // 解析
        List<SearchBean> beans = TxtFileParser.parse(content);

        // 打印
        System.out.println("\n解析结果：共 " + beans.size() + " 条记录\n");
        System.out.println("=".repeat(80));

        beans.forEach(bean -> System.out.printf("%-6s | %-50s | %s\n",
            bean.getType().getIcon(),
            truncate(bean.getSourceName(), 50),
            bean.getSourceUrl()
        ));

        System.out.println("=".repeat(80));
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }
}
