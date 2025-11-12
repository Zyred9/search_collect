package org.drinkless.robots.sender;

import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.groupadministration.PromoteChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.RestrictChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatAdministratorCustomTitle;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatTitle;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/**
 * <p>
 *
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Component
public class SyncSender {

    private static TelegramClient telegramClient;

    public SyncSender(TelegramClient telegramClient) {
        SyncSender.telegramClient = telegramClient;
    }

    @SneakyThrows
    public static Message send (SendMessage msg) {
        return SyncSender.telegramClient.execute(msg);
    }


    @SneakyThrows
    public static Boolean send (SetChatTitle msg) {
        return SyncSender.telegramClient.execute(msg);
    }


    @SneakyThrows
    public static User send (GetMe msg) {
        return SyncSender.telegramClient.execute(msg);
    }

    @SneakyThrows
    public static void send(PromoteChatMember promote) {
        SyncSender.telegramClient.execute(promote);
    }

    @SneakyThrows
    public static void send(SetChatAdministratorCustomTitle title) {
        SyncSender.telegramClient.execute(title);
    }

    // 设置用户的发言权限
    @SneakyThrows
    public static void send(RestrictChatMember title) {
        SyncSender.telegramClient.execute(title);
    }


}
