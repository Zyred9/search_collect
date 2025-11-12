package org.drinkless.robots.helper;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
public class DecimalHelper {

    public static String decimalParse (BigDecimal bigDecimal) {
        if (Objects.isNull(bigDecimal)) {
            return "";
        }
        return bigDecimal.stripTrailingZeros().toPlainString();
    }
}
