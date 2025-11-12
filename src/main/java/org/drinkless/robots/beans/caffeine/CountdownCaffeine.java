package org.drinkless.robots.beans.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.SneakyThrows;
import org.checkerframework.checker.index.qual.NonNegative;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Component
public class CountdownCaffeine extends Thread {

    private static Cache<String, Integer> DESTORY_CACHE;

    public CountdownCaffeine(ExpireListener listener) {
        DESTORY_CACHE = Caffeine.newBuilder()
                .initialCapacity(100)
                .expireAfter(new DynamicExpire())
                .removalListener(listener)
                .scheduler(Scheduler.systemScheduler())
                .maximumSize(10_000_000)
                .build();
        super.start();
    }

    public static void set (Long chatId, Integer messageId, int duration) {
        String key = chatId + "_" + messageId;
        DESTORY_CACHE.put(key, duration);
    }

    @Override
    @SneakyThrows
    public void run() {
        while (!Thread.interrupted()) {
            TimeUnit.SECONDS.sleep(1);
            DESTORY_CACHE.cleanUp();
        }
    }

    private static class DynamicExpire implements Expiry<String, Integer> {

        @Override
        public long expireAfterCreate(String messageId, Integer expire, long currentTime) {
            return TimeUnit.SECONDS.toNanos(expire);
        }

        @Override
        public long expireAfterUpdate(String messageId, Integer expire, long currentTime, @NonNegative long currentDuration) {
            return currentDuration;
        }

        @Override
        public long expireAfterRead(String messageId, Integer expire, long currentTime, @NonNegative long currentDuration) {
            return currentDuration;
        }
    }
}
