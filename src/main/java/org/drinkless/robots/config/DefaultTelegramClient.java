package org.drinkless.robots.config;


import org.telegram.telegrambots.client.AbstractTelegramClient;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatPhoto;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.stickers.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 *
 *
 * @author admin
 * @since 2025/11/11 15:46
 */
public class DefaultTelegramClient extends AbstractTelegramClient{
    @Override
    public <T extends Serializable, Method extends BotApiMethod<T>> CompletableFuture<T> executeAsync(Method method) throws TelegramApiException {
        return null;
    }

    @Override
    public CompletableFuture<Message> executeAsync(SendDocument sendDocument) {
        return null;
    }

    @Override
    public CompletableFuture<Message> executeAsync(SendPhoto sendPhoto) {
        return null;
    }

    @Override
    public CompletableFuture<Message> executeAsync(SendVideo sendVideo) {
        return null;
    }

    @Override
    public CompletableFuture<Message> executeAsync(SendVideoNote sendVideoNote) {
        return null;
    }

    @Override
    public CompletableFuture<Message> executeAsync(SendSticker sendSticker) {
        return null;
    }

    @Override
    public CompletableFuture<Message> executeAsync(SendAudio sendAudio) {
        return null;
    }

    @Override
    public CompletableFuture<Message> executeAsync(SendVoice sendVoice) {
        return null;
    }

    @Override
    public CompletableFuture<List<Message>> executeAsync(SendMediaGroup sendMediaGroup) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> executeAsync(SetChatPhoto setChatPhoto) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> executeAsync(AddStickerToSet addStickerToSet) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> executeAsync(ReplaceStickerInSet replaceStickerInSet) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> executeAsync(SetStickerSetThumbnail setStickerSetThumbnail) {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> executeAsync(CreateNewStickerSet createNewStickerSet) {
        return null;
    }

    @Override
    public CompletableFuture<File> executeAsync(UploadStickerFile uploadStickerFile) {
        return null;
    }

    @Override
    public CompletableFuture<Serializable> executeAsync(EditMessageMedia editMessageMedia) {
        return null;
    }

    @Override
    public CompletableFuture<Message> executeAsync(SendAnimation sendAnimation) {
        return null;
    }

    @Override
    public CompletableFuture<java.io.File> downloadFileAsync(File file) {
        return null;
    }

    @Override
    public CompletableFuture<InputStream> downloadFileAsStreamAsync(File file) {
        return null;
    }
}
