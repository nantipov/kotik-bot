package org.nantipov.kotikbot.service;

import lombok.extern.slf4j.Slf4j;
import org.nantipov.kotikbot.domain.MessageResource;
import org.nantipov.kotikbot.domain.SupplierMessage;
import org.nantipov.kotikbot.service.remote.TelegramBot;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.stream.Collectors;

import static org.nantipov.kotikbot.domain.MessageResourceType.IMAGE;

@Slf4j
@Service
public class TelegramBotService {

    private final TelegramBot telegramBot;

    public TelegramBotService(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void sayThatIAmTyping(long chatId) {
        try {
            telegramBot.execute(new SendChatAction(chatId, "typing"));
        } catch (TelegramApiException e) {
            log.warn("Could not send 'typing' event", e);
        }
    }

    public void sendSupplierMessage(SupplierMessage supplierMessage, long chatId) throws TelegramApiException {
        sayThatIAmTyping(chatId);
        var sendMessage = new SendMessage(chatId, supplierMessage.getMarkdownText());
        sendMessage.setParseMode("MarkdownV2"); //TODO html also?
        telegramBot.execute(sendMessage);
        if (!supplierMessage.getResources().isEmpty()) {
            var mediaResources = supplierMessage.getResources()
                                                .stream()
                                                .map(this::getTelegramMedia)
                                                .collect(Collectors.toList());
            var sendMediaGroup = new SendMediaGroup(chatId, mediaResources);
            telegramBot.execute(sendMediaGroup);
        }
    }

    private InputMedia getTelegramMedia(MessageResource messageResource) {
        if (messageResource.getType() == IMAGE) {
            return new InputMediaPhoto(messageResource.getUrl(), messageResource.getCaption());
        } else {
            throw new IllegalArgumentException("Unsupported resource type " + messageResource.getType());
        }
    }
}
