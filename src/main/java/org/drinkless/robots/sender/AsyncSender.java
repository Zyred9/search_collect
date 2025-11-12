package org.drinkless.robots.sender;

import cn.hutool.json.JSONUtil;
import org.drinkless.robots.helper.ThreadHelper;
import lombok.extern.slf4j.Slf4j;
import org.drinkless.robots.config.BotProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.*;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.DeleteMyCommands;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.*;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatStickerSet;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.PinChatMessage;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinAllChatMessages;
import org.telegram.telegrambots.meta.api.methods.pinnedmessages.UnpinChatMessage;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Telegram Bot 异步消息发送器（带QPS限流）
 * <p>
 * 核心功能：
 * <ul>
 *   <li>异步发送Telegram消息，避免阻塞主线程</li>
 *   <li>使用滑动窗口限流，保证QPS不超过20</li>
 *   <li>消息队列无限制，不丢失任何消息</li>
 * </ul>
 * 
 * <pre>
 * QPS限流策略：
 * 1. 维护最近1秒内的发送时间戳队列（最多20个）
 * 2. 低负载时（QPS < 20）：消息立即发送
 * 3. 高负载时（QPS = 20）：计算等待时间，排队发送
 * 
 * 示例：
 * AsyncSender.async(SendMessage.builder()
 *     .chatId(123456L)
 *     .text("Hello World")
 *     .build());
 * </pre>
 *
 * @author zyred
 * @since 1.0
 */
@Slf4j
@Component
public class AsyncSender {

    @Resource private BotProperties properties;
    @Resource private TelegramClient telegramClient;

    // ==================== QPS限流配置 ====================
    private static final int TARGET_QPS = 20;
    private static final long TIME_WINDOW_MS = 1000L;

    // ==================== 消息队列 ====================
    private static final LinkedBlockingQueue<PartialBotApiMethod<?>> MESSAGE_QUEUE = new LinkedBlockingQueue<>();
    
    // ==================== 滑动窗口时间戳队列 ====================
    private final ConcurrentLinkedQueue<Long> sendTimestamps = new ConcurrentLinkedQueue<>();

    /**
     * 异步发送消息（静态方法，供全局调用）
     *
     * @param message Telegram API方法对象
     */
    public static void async(PartialBotApiMethod<?> message) {
        if (Objects.isNull(message)) {
            return;
        }
        MESSAGE_QUEUE.add(message);
    }

    /**
     * 构造函数，启动消费者线程
     */
    public AsyncSender() {
        this.startConsumer();
    }

    /**
     * 启动消息消费者线程
     */
    private void startConsumer() {
        ThreadHelper.execute(() -> {
            while (!Thread.interrupted()) {
                PartialBotApiMethod<?> message = null;
                try {
                    // 阻塞获取消息
                    message = MESSAGE_QUEUE.take();
                    
                    // QPS限流：计算需要等待的时间
                    long waitTime = calculateWaitTime();
                    if (waitTime > 0) {
                        if (properties.isLogs()) {
                            log.debug("[限流] 当前QPS已达 {}, 等待 {}ms 后发送，队列剩余: {}", 
                                TARGET_QPS, waitTime, MESSAGE_QUEUE.size());
                        }
                        ThreadHelper.sleepMs(waitTime);
                    }
                    
                    // 发送消息
                    if (properties.isLogs()) {
                        log.info("[异步发送] {}", JSONUtil.toJsonStr(message));
                    }
                    this.processorSend(message);
                    
                    // 记录发送时间戳
                    recordSendTime();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("[消费者] 线程被中断，停止发送");
                    break;
                } catch (TelegramApiException e) {
                    log.error("[发送失败] 消息内容: {}, 错误: {}", JSONUtil.toJsonStr(message), e.getMessage(), e);
                }
            }
        });
    }

    /**
     * 计算需要等待的时间（毫秒）
     * <p>
     * 滑动窗口算法：
     * 1. 如果窗口未满（< 20条），返回0（立即发送）
     * 2. 如果窗口已满（= 20条）：
     *    - 计算最早时间戳距离现在的时长
     *    - 如果不足1秒，返回需要等待的时间
     *    - 如果超过1秒，返回0（最早时间戳会被移除）
     * </p>
     *
     * @return 需要等待的毫秒数
     */
    private long calculateWaitTime() {
        long now = System.currentTimeMillis();
        
        // 清理超过时间窗口的旧时间戳
        while (!sendTimestamps.isEmpty()) {
            Long earliest = sendTimestamps.peek();
            if (earliest == null || now - earliest >= TIME_WINDOW_MS) {
                sendTimestamps.poll();
            } else {
                break;
            }
        }
        
        // 如果窗口未满，立即发送
        if (sendTimestamps.size() < TARGET_QPS) {
            return 0;
        }
        
        // 窗口已满，计算等待时间
        Long earliest = sendTimestamps.peek();
        if (earliest == null) {
            return 0;
        }
        
        long timeSinceEarliest = now - earliest;
        if (timeSinceEarliest < TIME_WINDOW_MS) {
            // 需要等待到最早时间戳过期（加10ms缓冲）
            return TIME_WINDOW_MS - timeSinceEarliest + 10;
        }
        
        return 0;
    }

    /**
     * 记录本次发送的时间戳
     */
    private void recordSendTime() {
        sendTimestamps.add(System.currentTimeMillis());
    }

    /**
     * 处理并发送消息
     * <p>
     * 根据消息类型调用对应的Telegram API方法
     * </p>
     *
     * @param message Telegram API方法对象
     * @throws TelegramApiException 发送失败时抛出
     */
    private void processorSend(PartialBotApiMethod<?> message) throws TelegramApiException {
        // ==================== 发送消息类型 ====================
        if (message instanceof SendMessage sendMessage) {
            telegramClient.execute(sendMessage);
        } else if (message instanceof SendPhoto photo) {
            telegramClient.execute(photo);
        } else if (message instanceof SendVideo video) {
            telegramClient.execute(video);
        } else if (message instanceof SendAudio audio) {
            telegramClient.execute(audio);
        } else if (message instanceof SendDocument doc) {
            telegramClient.execute(doc);
        } else if (message instanceof SendAnimation animation) {
            telegramClient.execute(animation);
        } else if (message instanceof SendSticker sticker) {
            telegramClient.execute(sticker);
        } else if (message instanceof SendVoice voice) {
            telegramClient.execute(voice);
        } else if (message instanceof SendVideoNote videoNote) {
            telegramClient.execute(videoNote);
        } else if (message instanceof SendLocation location) {
            telegramClient.execute(location);
        } else if (message instanceof SendVenue venue) {
            telegramClient.execute(venue);
        } else if (message instanceof SendContact contact) {
            telegramClient.execute(contact);
        } else if (message instanceof SendPoll poll) {
            telegramClient.execute(poll);
        } else if (message instanceof SendDice dice) {
            telegramClient.execute(dice);
        } else if (message instanceof SendMediaGroup mediaGroup) {
            telegramClient.execute(mediaGroup);
        } else if (message instanceof SendInvoice invoice) {
            telegramClient.execute(invoice);
        } else if (message instanceof SendGame game) {
            telegramClient.execute(game);
        }

        // ==================== 编辑消息类型 ====================
        else if (message instanceof EditMessageText edit) {
            telegramClient.execute(edit);
        } else if (message instanceof EditMessageCaption caption) {
            telegramClient.execute(caption);
        } else if (message instanceof EditMessageMedia media) {
            telegramClient.execute(media);
        } else if (message instanceof EditMessageReplyMarkup markup) {
            telegramClient.execute(markup);
        }
        
        // ==================== 删除/转发消息 ====================
        else if (message instanceof DeleteMessage delete) {
            telegramClient.execute(delete);
        } else if (message instanceof ForwardMessage forward) {
            telegramClient.execute(forward);
        } else if (message instanceof CopyMessage copy) {
            telegramClient.execute(copy);
        }
        
        // ==================== 回调查询 ====================
        else if (message instanceof AnswerCallbackQuery answer) {
            telegramClient.execute(answer);
        } else if (message instanceof AnswerInlineQuery inlineAnswer) {
            telegramClient.execute(inlineAnswer);
        }
        
        // ==================== 群组管理 ====================
        else if (message instanceof BanChatMember ban) {
            telegramClient.execute(ban);
        } else if (message instanceof UnbanChatMember unban) {
            telegramClient.execute(unban);
        } else if (message instanceof RestrictChatMember restrict) {
            telegramClient.execute(restrict);
        } else if (message instanceof PromoteChatMember promote) {
            telegramClient.execute(promote);
        } else if (message instanceof SetChatAdministratorCustomTitle customTitle) {
            telegramClient.execute(customTitle);
        } else if (message instanceof SetChatPermissions permissions) {
            telegramClient.execute(permissions);
        } else if (message instanceof ExportChatInviteLink exportLink) {
            telegramClient.execute(exportLink);
        } else if (message instanceof CreateChatInviteLink createLink) {
            telegramClient.execute(createLink);
        } else if (message instanceof RevokeChatInviteLink revokeLink) {
            telegramClient.execute(revokeLink);
        } else if (message instanceof ApproveChatJoinRequest approve) {
            telegramClient.execute(approve);
        } else if (message instanceof DeclineChatJoinRequest decline) {
            telegramClient.execute(decline);
        }
        
        // ==================== 聊天设置 ====================
        else if (message instanceof SetChatTitle chatTitle) {
            telegramClient.execute(chatTitle);
        } else if (message instanceof SetChatDescription description) {
            telegramClient.execute(description);
        } else if (message instanceof SetChatPhoto chatPhoto) {
            telegramClient.execute(chatPhoto);
        } else if (message instanceof DeleteChatPhoto deleteChatPhoto) {
            telegramClient.execute(deleteChatPhoto);
        } else if (message instanceof SetChatStickerSet stickerSet) {
            telegramClient.execute(stickerSet);
        } else if (message instanceof DeleteChatStickerSet deleteStickerSet) {
            telegramClient.execute(deleteStickerSet);
        }
        
        // ==================== 消息置顶 ====================
        else if (message instanceof PinChatMessage pin) {
            telegramClient.execute(pin);
        } else if (message instanceof UnpinChatMessage unpin) {
            telegramClient.execute(unpin);
        } else if (message instanceof UnpinAllChatMessages unpinAll) {
            telegramClient.execute(unpinAll);
        }
        
        // ==================== 机器人命令 ====================
        else if (message instanceof SetMyCommands cmd) {
            telegramClient.execute(cmd);
        } else if (message instanceof DeleteMyCommands deleteCmd) {
            telegramClient.execute(deleteCmd);
        }

        // ==================== 未知类型 ====================
        else {
            log.warn("[未支持的消息类型] {}", message.getClass().getSimpleName());
        }
    }
}
