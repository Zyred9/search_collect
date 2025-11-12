package org.drinkless.robots.helper;

import org.apache.commons.jexl3.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@SuppressWarnings("all")
public class JexlCalculator {
    private static final JexlEngine jexl = new JexlBuilder().create();

    public static void main(String[] args) {
        int scale = 2; // 保留2位小数

        testCalculation("10 / 3", scale);                    // 3.33
        testCalculation("(10 + 5) / 3", scale);             // 5.00
        testCalculation("2 * 3 + 1", scale);                // 7.00
        testCalculation("(2.5 + 1.5) * 3", scale);          // 12.00
        testCalculation("(10.5 + 5.5) * 2.0 / 3", scale);   // 10.67
        testCalculation("10.5 * 2", scale);                 // 21.00
        testCalculation("((10 + 5) * 2 - 5) / 2.5", scale); // 10.00
    }

    public static BigDecimal calculate(String expression) {
        return calculate(expression, 2);
    }

    public static String calculateStr(String expression) {
        return DecimalHelper.decimalParse(calculate(expression, 2));
    }

    public static BigDecimal calculate(String expression, int scale) {
        try {
            // 输入验证
            if (expression == null || expression.trim().isEmpty()) {
                throw new IllegalArgumentException("表达式不能为空");
            }
            String modifiedExpression = convertToDecimalExpression(expression.trim());
            JexlExpression expr = jexl.createExpression(modifiedExpression);
            JexlContext context = new MapContext();
            Object result = expr.evaluate(context);

            if (result instanceof Number) {
                return new BigDecimal(result.toString())
                        .setScale(scale, RoundingMode.HALF_UP);
            }
            throw new RuntimeException("计算结果不是数字");

        } catch (JexlException je) {
            throw new RuntimeException("JEXL表达式错误: " + je.getMessage(), je);
        } catch (Exception e) {
            throw new RuntimeException("计算错误: " + e.getMessage(), e);
        }
    }

    /**
     * 将表达式中的整数转换为小数形式
     * 改进后的正则表达式，只匹配独立的整数
     */
    private static String convertToDecimalExpression(String expression) {
        return expression.replaceAll("(?<!\\d*\\.)(\\b\\d+\\b)(?!\\.\\d*)", "$1.0");
    }

    private static void testCalculation(String expression, int scale) {
        try {
            BigDecimal result = calculate(expression, scale);
            System.out.printf("%s = %." + scale + "f%n", expression, result);
        } catch (Exception e) {
            System.out.printf("%s = 错误: %s%n", expression, e.getMessage());
        }
        System.out.println(); // 添加空行，使输出更清晰
    }
}