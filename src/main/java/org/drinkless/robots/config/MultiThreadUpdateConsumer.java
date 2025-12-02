package org.drinkless.robots.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import cn.hutool.core.util.StrUtil;
import org.drinkless.robots.helper.ThreadHelper;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
public interface MultiThreadUpdateConsumer extends LongPollingUpdateConsumer {

    Map<String, List<Update>> TEMP_UPDATES = new java.util.concurrent.ConcurrentHashMap<>();

    ThreadPoolExecutor EX = ExecutorBuilder.create()
            .setCorePoolSize(2)
            .setMaxPoolSize(20)      // 最大线程20个,降低并发压力
            .setThreadFactory(ThreadFactoryBuilder.create()
                    .setNamePrefix("long_poll_")
                    .build())
            .build();

    default void consume(List<Update> updates) {
        boolean add = true;
        for (Update update : updates) {
            if (update.hasMessage() && (update.getMessage().hasPhoto()
                    || update.getMessage().hasVideo())
                    && StrUtil.isNotBlank(update.getMessage().getMediaGroupId())) {
                String mediaGroupId = update.getMessage().getMediaGroupId();
                if (!TEMP_UPDATES.containsKey(mediaGroupId)) {
                    List<Update> newUpdates = new ArrayList<>();
                    newUpdates.add(update);
                    TEMP_UPDATES.put(mediaGroupId, newUpdates);
                } else {
                    List<Update> list = TEMP_UPDATES.get(mediaGroupId);
                    list.add(update);
                }
                add = false;
            }
        }

        if (add) {
            EX.execute(() -> {
                CollUtil.sort(updates, (o1, o2) -> o2.getUpdateId() - o1.getUpdateId());
                for (Update update : updates) {
                    this.consume(update);
                }
            });
        } else {
            ThreadHelper.execute(() -> {
                ThreadHelper.sleep(2);
                Set<Map.Entry<String, List<Update>>> entries = TEMP_UPDATES.entrySet();
                for (Map.Entry<String, List<Update>> entry : entries) {
                    List<Update> mergeUpdates = TEMP_UPDATES.remove(entry.getKey());
                    CollUtil.sort(mergeUpdates, (o1, o2) -> o2.getUpdateId() - o1.getUpdateId());
                    for (Update update : mergeUpdates) {
                        this.consume(update);
                    }
                }
            });
        }
    }

    void consume(Update updates);
}
