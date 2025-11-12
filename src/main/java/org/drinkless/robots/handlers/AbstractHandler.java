package org.drinkless.robots.handlers;

import cn.hutool.json.JSONUtil;
import org.drinkless.robots.config.Constants;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendAnimation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.util.ArrayList;
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
@SuppressWarnings("all")
@Slf4j
public abstract class AbstractHandler {

    private static final List<AbstractHandler> HANDLERS = new ArrayList<>(128);

    protected AbstractHandler() {
        HANDLERS.add(this);
    }

    private static List<AbstractHandler> getHandlers () {
        return HANDLERS;
    }


    public static BotApiMethod<?> doExecute(Update update, boolean logs) {
        for (AbstractHandler handler : getHandlers()) {
            if (handler.support(update)) {
                BotApiMethod<?> result = handler.execute(update);
                if (Objects.nonNull(result) && logs) {
                    log.info("处理器：{}，\n处理消息内容：{}\n响应结果：{}",
                            handler.getClass().getSimpleName(),
                            JSONUtil.toJsonStr(update),
                            JSONUtil.toJsonStr(result)
                    );
                }
                return result;
            }
        }
        return null;
    }

    public abstract boolean support(Update update);

    protected abstract BotApiMethod<?> execute(Update update);


    protected SendMessage ok(Message message) {
        return ok(message, Constants.SUCCESS);
    }

    protected SendMessage ok(Long chatId) {
        return ok(chatId, Constants.SUCCESS);
    }

    protected SendMessage ok(Message message, String text) {
        return this.ok(message, text, null);
    }

    protected SendMessage ok(Long chatId, String text) {
        return this.ok(chatId, text, null);
    }

    protected SendMessage ok(Message message, String text, ReplyKeyboard markup) {
        return ok(message.getChatId(), text, markup);
    }

    protected SendMessage ok(Long chatId, String text, ReplyKeyboard markup) {
        return SendMessage.builder()
                .chatId(chatId)
                .disableWebPagePreview(true)
                .replyMarkup(markup)
                .text(text)
                .build();
    }

    protected SendMessage reply(Message message) {
        return this.reply(message, Constants.SUCCESS);
    }

    protected SendMessage reply(Message message, String text) {
        return this.reply(message, text, null);
    }

    protected SendMessage reply(Message message, String text, InlineKeyboardMarkup markup) {
        return SendMessage.builder()
                .chatId(message.getChatId())
                .replyMarkup(markup)
                .disableWebPagePreview(true)
                .replyToMessageId(message.getMessageId())
                .text(text)
                .build();
    }

    protected SendMessage reply(Message message, String text, ReplyKeyboard markup) {
        return SendMessage.builder()
                .chatId(message.getChatId())
                .replyMarkup(markup)
                .disableWebPagePreview(true)
                .replyToMessageId(message.getMessageId())
                .text(text)
                .build();
    }


    protected SendMessage markdown(Message message, String text) {
        return markdown(message, text, null);
    }

    protected SendMessage markdown(Long chatId, String text) {
        return markdown(chatId, text, null);
    }

    protected SendMessage markdown(Message message, String text, InlineKeyboardMarkup markup) {
        return markdown(message.getChatId(), text, markup);
    }

    protected SendMessage markdown(Long chatId, String text, InlineKeyboardMarkup markup) {
        return SendMessage.builder()
                .chatId(chatId)
                .replyMarkup(markup)
                .parseMode(ParseMode.MARKDOWN)
                .disableWebPagePreview(true)
                .text(text)
                .build();
    }

    protected SendMessage markdownReply(Message message, String text) {
        return this.markdownReply(message, text, null);
    }

    protected SendMessage markdownReply(Message message, String text, InlineKeyboardMarkup markup) {
        return SendMessage.builder()
                .replyToMessageId(message.getMessageId())
                .chatId(message.getChatId())
                .parseMode(ParseMode.MARKDOWN)
                .disableWebPagePreview(true)
                .replyMarkup(markup)
                .text(text)
                .build();
    }



    protected DeleteMessage delete(Message message) {
        return DeleteMessage.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .build();
    }

    protected SendPhoto photo(Message message, String fileId, String caption, InlineKeyboardMarkup markup) {
        return SendPhoto.builder()
                .chatId(message.getChatId())
                .photo(new InputFile(fileId))
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(markup)
                .caption(caption)
                .build();
    }

    protected SendVideo video(Message message, String fileId, String caption, InlineKeyboardMarkup markup) {
        return SendVideo.builder()
                .chatId(message.getChatId())
                .video(new InputFile(fileId))
                .parseMode(ParseMode.MARKDOWN)
                .replyMarkup(markup)
                .caption(caption)
                .build();
    }

    protected EditMessageText editText(Message message, String text) {
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .disableWebPagePreview(true)
                .messageId(message.getMessageId())
                .text(text)
                .build();
    }

    protected EditMessageText editText(Message message, String text, InlineKeyboardMarkup markup) {
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .disableWebPagePreview(true)
                .messageId(message.getMessageId())
                .replyMarkup(markup)
                .text(text)
                .build();
    }

    protected EditMessageText editMarkdown(Message message, String text) {
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .disableWebPagePreview(true)
                .parseMode(ParseMode.MARKDOWN)
                .messageId(message.getMessageId())
                .text(text)
                .build();
    }

    protected EditMessageText editMarkdown(Message message, String text, InlineKeyboardMarkup markup) {
        return EditMessageText.builder()
                .chatId(message.getChatId())
                .disableWebPagePreview(true)
                .parseMode(ParseMode.MARKDOWN)
                .messageId(message.getMessageId())
                .replyMarkup(markup)
                .text(text)
                .build();
    }

    protected EditMessageCaption editCaption(Message message, String caption) {
        return EditMessageCaption.builder()
                .messageId(message.getMessageId())
                .messageId(message.getMessageId())
                .chatId(message.getChatId())
                .caption(caption)
                .build();
    }

    protected EditMessageCaption editCaption(Message message, String caption, InlineKeyboardMarkup markup) {
        return EditMessageCaption.builder()
                .messageId(message.getMessageId())
                .messageId(message.getMessageId())
                .chatId(message.getChatId())
                .replyMarkup(markup)
                .caption(caption)
                .build();
    }

    protected EditMessageCaption editCaptionMarkdown(Message message, String caption) {
        return EditMessageCaption.builder()
                .messageId(message.getMessageId())
                .messageId(message.getMessageId())
                .parseMode(ParseMode.MARKDOWN)
                .chatId(message.getChatId())
                .caption(caption)
                .build();
    }

    protected EditMessageCaption editCaptionMarkdown(Message message, String caption, InlineKeyboardMarkup markup) {
        return EditMessageCaption.builder()
                .messageId(message.getMessageId())
                .messageId(message.getMessageId())
                .parseMode(ParseMode.MARKDOWN)
                .chatId(message.getChatId())
                .replyMarkup(markup)
                .caption(caption)
                .build();
    }

    protected EditMessageReplyMarkup editKeyboard(Message message, InlineKeyboardMarkup markup) {
        return EditMessageReplyMarkup.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .replyMarkup(markup)
                .build();
    }

    protected AnswerCallbackQuery answerAlert (CallbackQuery callbackQuery, String text) {
        return AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .showAlert(true)
                .text(text)
                .build();
    }

    protected AnswerCallbackQuery answer (CallbackQuery callbackQuery, String text) {
        return AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .text(text)
                .build();
    }

    protected SendAnimation animation (Message message, String caption, String fileId) {
        return SendAnimation.builder()
                .caption(caption)
                .chatId(message.getChatId())
                .animation(new InputFile(fileId))
                .build();
    }

    protected SendAnimation animation (Message message, String caption, String fileId, InlineKeyboardMarkup markup) {
        return SendAnimation.builder()
                .caption(caption)
                .replyMarkup(markup)
                .chatId(message.getChatId())
                .animation(new InputFile(fileId))
                .build();
    }

    protected SendAnimation animationMarkdown (Message message, String caption, String fileId) {
        return SendAnimation.builder()
                .caption(caption)
                .chatId(message.getChatId())
                .parseMode(ParseMode.MARKDOWN)
                .animation(new InputFile(fileId))
                .build();
    }

    protected SendAnimation animationMarkdown (Message message, String caption, String fileId, InlineKeyboardMarkup markup) {
        return SendAnimation.builder()
                .caption(caption)
                .replyMarkup(markup)
                .chatId(message.getChatId())
                .parseMode(ParseMode.MARKDOWN)
                .animation(new InputFile(fileId))
                .build();
    }

    protected SendMessage fail (Message message) {
        return ok(message, Constants.FAILED);
    }
}
