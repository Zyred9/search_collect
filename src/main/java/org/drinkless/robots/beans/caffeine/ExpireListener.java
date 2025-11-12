package org.drinkless.robots.beans.caffeine;

import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Component
public class ExpireListener implements RemovalListener<String, Integer> {

    @Override
    public void onRemoval(@Nullable String key, @Nullable Integer expire, RemovalCause cause) {
        if (StrUtil.isBlank(key) || Objects.isNull(expire) || !RemovalCause.EXPIRED.equals(cause)) {
            return;
        }
    }
}
