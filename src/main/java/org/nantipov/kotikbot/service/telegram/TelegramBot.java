package org.nantipov.kotikbot.service.telegram;

import lombok.extern.slf4j.Slf4j;
import org.nantipov.kotikbot.domain.MessageResource;
import org.nantipov.kotikbot.domain.RoomAction;
import org.nantipov.kotikbot.domain.SupplierMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import static org.nantipov.kotikbot.domain.MessageResourceType.IMAGE;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final String token;
    private final String name;
    private final BotUpdatesProcessorService botUpdatesProcessorService;

    public TelegramBot(@Value("${telegram.token}") String token,
                       @Value("${bot.name}") String name,
                       BotUpdatesProcessorService botUpdatesProcessorService) {
        this.token = token;
        this.name = name;
        this.botUpdatesProcessorService = botUpdatesProcessorService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        botUpdatesProcessorService.processUpdate(update)
                                  .ifPresent(response -> {
                                      try {
                                          sendSupplierMessage(response.getMessage(), response.getProviderRoomKey());
                                      } catch (TelegramApiException e) {
                                          log.warn(
                                                  "Could not delivery message to chat {}",
                                                  response.getProviderRoomKey(), e
                                          );
                                      }
                                  });
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    public void sendSupplierMessage(SupplierMessage supplierMessage,
                                    String providerRoomKey) throws TelegramApiException {
        sayThatIAmTyping(providerRoomKey);
        if (supplierMessage.getResources().size() != 1) {
            var sendMessage = new SendMessage(String.valueOf(providerRoomKey), supplierMessage.getMarkdownText());
            sendMessage.setParseMode("MarkdownV2"); //TODO html also?
        }
        if (!supplierMessage.getResources().isEmpty()) {
            if (supplierMessage.getResources().size() < 2) {
                var res = supplierMessage.getResources().get(0);
                switch (res.getType()) {
                    case IMAGE:
                        try (var stream = new TelegramInputFileResource(res)) {
                            var sendPhoto = SendPhoto.builder()
                                                     .chatId(String.valueOf(providerRoomKey))
                                                     .photo(stream.getTelegramFile())
                                                     .parseMode("MarkdownV2")
                                                     .caption(
                                                             supplierMessage.getMarkdownText() + " \\- " +
                                                             res.getCaption()
                                                     )
                                                     .build();
                            execute(sendPhoto);
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
                var sendMediaGroup = new SendMediaGroup(String.valueOf(providerRoomKey), mediaResources);
                execute(sendMediaGroup);
            }
        }
    }

    @PostConstruct
    public void communicateAvailableCommands() {
        try {
            execute(
                    SetMyCommands.builder()
                                 .commands(
                                         Arrays.stream(RoomAction.values())
                                               .filter(action -> action != RoomAction.UNKNOWN &&
                                                                 action != RoomAction.START)
                                               .map(action -> BotCommand.builder()
                                                                        .command(action.name().toLowerCase())
                                                                        .description(action.getDescription())
                                                                        .build()
                                               )
                                               .collect(Collectors.toList())
                                 )
                                 .build()
            );
        } catch (TelegramApiException e) {
            log.warn("Could not setup commands");
        }
    }

    private void sayThatIAmTyping(String chatId) {
        try {
            execute(new SendChatAction(chatId, "typing"));
        } catch (TelegramApiException e) {
            log.warn("Could not send 'typing' event", e);
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
