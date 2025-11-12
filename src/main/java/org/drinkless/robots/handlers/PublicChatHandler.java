package org.drinkless.robots.handlers;

import org.drinkless.robots.config.BotProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import javax.annotation.Resource;

/**
 * <p>
 *     普通群
 * </p>
 *
 * @author admin
 * @since v 0.0.1
 */
@Component
public class PublicChatHandler extends AbstractHandler {

    @Resource private BotProperties properties;

    @Override
    public boolean support(Update update) {
        return update.hasMessage()
                && update.getMessage().hasText()
                && (update.getMessage().getChat().isGroupChat() || update.getMessage().getChat().isSuperGroupChat())
                && !properties.fromBackground(update.getMessage().getChatId());
    }

    @Override
    protected BotApiMethod<?> execute(Update update) {
        Message message = update.getMessage();
        String text = message.getText();

        return null;
    }

}
