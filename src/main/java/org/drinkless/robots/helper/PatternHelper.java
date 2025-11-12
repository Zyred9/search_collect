package org.drinkless.robots.helper;


import cn.hutool.core.util.StrUtil;

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
public class PatternHelper {

    private static final Pattern USDT_QUERY_PATTERN = Pattern.compile("^\\d+(\\.\\d{1,12})?[uU]$");

    private static final Pattern TRON_ADDRESS_PATTERN = Pattern.compile("^T[a-zA-Z0-9]{33}$");

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("""
            ^(?=.*[+\\-*/()])([\\d+\\-*/().\\s]+)$""");

    /**
     * + - * / () 表达式
     * @param text  表达式内容
     * @return  是否包含
     */
    public static boolean isExpression(String text) {
        text = StrUtil.removeAll(text, " ");
        Matcher matcher = EXPRESSION_PATTERN.matcher(text);
        boolean matches = matcher.matches();

        if (matches) {
            try {
                return Double.parseDouble(text) >= 0;
            } catch (NumberFormatException ex) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查询 usdt 试试价格
     *
     * @param text  1U，1u
     * @return      true
     */
    public static boolean usdtNumber(String text) {
        Matcher matcher = USDT_QUERY_PATTERN.matcher(text);
        return matcher.matches();
    }

    public static boolean isTronAddress(String text) {
        Matcher matcher = TRON_ADDRESS_PATTERN.matcher(text);
        return matcher.matches();
    }

}
