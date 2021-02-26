package org.nantipov.kotikbot.service;

import lombok.extern.slf4j.Slf4j;
import org.nantipov.kotikbot.domain.MessageResource;
import org.nantipov.kotikbot.domain.SupplierMessage;
import org.nantipov.kotikbot.service.remote.TelegramBot;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;
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
            telegramBot.execute(new SendChatAction(String.valueOf(chatId), "typing"));
        } catch (TelegramApiException e) {
            log.warn("Could not send 'typing' event", e);
        }
    }

    public void sendSupplierMessage(SupplierMessage supplierMessage, long chatId) throws TelegramApiException {
        sayThatIAmTyping(chatId);
        var sendMessage = new SendMessage(String.valueOf(chatId), supplierMessage.getMarkdownText());
        sendMessage.setParseMode("MarkdownV2"); //TODO html also?
        telegramBot.execute(sendMessage);
        if (!supplierMessage.getResources().isEmpty()) {
            if (supplierMessage.getResources().size() < 2) {
                var res = supplierMessage.getResources().get(0);
                switch (res.getType()) {
                    case IMAGE:
                        try (var stream = new TelegramInputFileResource(res)) {
                            var sendPhoto = SendPhoto.builder()
                                                     .chatId(String.valueOf(chatId))
                                                     .photo(stream.getTelegramFile())
                                                     .caption(res.getCaption())
                                                     .build();
                            telegramBot.execute(sendPhoto);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported resource type " + res.getType());
                }
            } else {
                var mediaResources = supplierMessage.getResources()
                                                    .stream()
                                                    .map(this::getTelegramMedia)
                                                    .collect(Collectors.toList());
                var sendMediaGroup = new SendMediaGroup(String.valueOf(chatId), mediaResources);
                telegramBot.execute(sendMediaGroup);
            }
        }
    }

    private InputMedia getTelegramMedia(MessageResource messageResource) {
        if (messageResource.getType() == IMAGE) {
            return InputMediaPhoto.builder()
                                  .media(messageResource.getUrl())
                                  .caption(messageResource.getCaption())
                                  .build();
        } else {
            throw new IllegalArgumentException("Unsupported resource type " + messageResource.getType());
        }
    }

    private static class TelegramInputFileResource implements AutoCloseable {
        private final InputFile telegramFile;
        private final InputStream in;

        private TelegramInputFileResource(MessageResource messageResource) {
            try {
                in = new URL(messageResource.getUrl()).openStream();
                telegramFile = new InputFile(in, "file-" + UUID.randomUUID().toString() + ".dat");
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not prepare file " + messageResource);
            }
        }

        private InputFile getTelegramFile() {
            return telegramFile;
        }

        @Override
        public void close() {
            try {
                in.close();
            } catch (IOException e) {

            }
        }
    }
}
