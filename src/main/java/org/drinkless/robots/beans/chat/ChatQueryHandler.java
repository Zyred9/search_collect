package org.drinkless.robots.beans.chat;

import org.drinkless.robots.config.BotProperties;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.objects.chat.ChatFullInfo;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import javax.annotation.Resource;
import java.util.List;
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
@ConditionalOnBean(BotProperties.class)
public class ChatQueryHandler {

    @Resource private TelegramClient telegramClient;

    @SneakyThrows
    public boolean isChatAdmin (Long chatId, Long userId) {
        List<ChatMember> members = this.telegramClient.execute(
                GetChatAdministrators.builder()
                        .chatId(chatId)
                        .build()
        );
        for (ChatMember member : members) {
            if (Objects.equals(member.getUser().getId(), userId)) {
                return true;
            }
        }
        return false;
    }

    @SneakyThrows
    public String getGroupInviteLink(Long groupId) {
        ChatFullInfo fullInfo = this.getGroupInfo(groupId);
        if (Objects.isNull(fullInfo)) {
            return null;
        }
        return fullInfo.getInviteLink();
    }

    @SneakyThrows
    public ChatFullInfo getGroupInfo(Long groupId) {
        ChatFullInfo fullInfo = telegramClient.execute(
                GetChat.builder().chatId(groupId).build()
        );
        if (Objects.isNull(fullInfo)) {
            return null;
        }
        return fullInfo;
    }


    @SneakyThrows
    public boolean checkUserInGroup(Long groupId, Long id) {
        ChatMember chatMember = this.telegramClient.execute(GetChatMember.builder()
                .chatId(groupId)
                .userId(id)
                .build());
        String status = chatMember.getStatus();
        return !status.equals("left") && !status.equals("kicked");
    }

    @SneakyThrows
    public ChatFullInfo getChat (Long userId){
        return this.telegramClient.execute(GetChat.builder().chatId(userId).build());
    }
}
