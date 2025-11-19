package org.drinkless.robots.handlers;


import lombok.extern.slf4j.Slf4j;
import org.drinkless.robots.beans.view.async.AsyncBean;
import org.drinkless.robots.database.service.IncludedService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 *
 * @author zyred
 * @since 2025/11/13 15:44
 */
@Slf4j
@Component
public class AsyncTaskHandler extends Thread {

    private static final LinkedBlockingQueue<AsyncBean> ASYNC_QUEUE = new LinkedBlockingQueue<>();

    @Resource
    private IncludedService includedService;


    public static void async(AsyncBean ab) {
        if (Objects.isNull(ab)) {
            return;
        }
        ASYNC_QUEUE.add(ab);
    }


    public AsyncTaskHandler() {
        this.start();
    }


    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                AsyncBean bean = ASYNC_QUEUE.take();
                if (Objects.nonNull(bean.getChat())) {
                    this.includedService.updateChat(bean.getChat());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
