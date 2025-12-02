package org.drinkless.robots.helper;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadFactoryBuilder;
import lombok.SneakyThrows;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
public class ThreadHelper {

    static final ThreadPoolExecutor EX = ExecutorBuilder.create()
            .setCorePoolSize(5)      // 核心线程5个,降低资源占用
            .setMaxPoolSize(20)      // 最大线程20个,匹配数据库连接池大小
            .setKeepAliveTime(60L, TimeUnit.SECONDS)  // 空闲线程60秒后回收
            .setThreadFactory(ThreadFactoryBuilder.create()
                    .setNamePrefix("temp-thread-")
                    .build())
            .build();

    public static void execute(Runnable runnable) {
        EX.execute(runnable);
    }


    @SneakyThrows
    public static void sleep (int time) {
        TimeUnit.SECONDS.sleep(time);
    }

    @SneakyThrows
    public static void sleepMs(long time) {
        TimeUnit.MILLISECONDS.sleep(time);
    }
}
