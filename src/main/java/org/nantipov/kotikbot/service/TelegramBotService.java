package org.nantipov.kotikbot.service;

import lombok.extern.slf4j.Slf4j;
import org.nantipov.kotikbot.config.BotProperties;
import org.nantipov.kotikbot.domain.MessageResource;
import org.nantipov.kotikbot.domain.SupplierMessage;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

import java.util.stream.Collectors;
import javax.annotation.PostConstruct;

import static org.nantipov.kotikbot.domain.MessageResourceType.IMAGE;

@Slf4j
@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final BotProperties botProperties;
    private final CommandService commandService;

    public TelegramBotService(BotProperties botProperties, CommandService commandService) {
        this.botProperties = botProperties;
        this.commandService = commandService;
    }

    @PostConstruct
    public void init() {
        ApiContextInitializer.init();
        BotSession session = ApiContext.getInstance(BotSession.class);
        session.setToken(this.getBotToken());
        session.setOptions(this.getOptions());
        session.setCallback(this);
        session.start();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {
            var message = update.getMessage();
            if (message.hasText() && message.getText().startsWith("/")) {
                commandService.command(message.getChatId(), message.getText());
            }
        }
    }

    @Override
    public String getBotUsername() {
        return botProperties.getName();
    }

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }

    public void sayThatIAmTyping(long chatId) {
        try {
            sendApiMethod(new SendChatAction(chatId, "typing"));
        } catch (TelegramApiException e) {
            log.warn("Could not send 'typing' event", e);
        }
    }

    public void sendSupplierMessage(SupplierMessage supplierMessage, long chatId) throws TelegramApiException {
        sayThatIAmTyping(chatId);
        var sendMessage = new SendMessage(chatId, supplierMessage.getMarkdownText());
        sendMessage.setParseMode("MarkdownV2"); //TODO html also?
        sendApiMethod(sendMessage);
        if (!supplierMessage.getResources().isEmpty()) {
            var mediaResources = supplierMessage.getResources()
                                                .stream()
                                                .map(this::getTelegramMedia)
                                                .collect(Collectors.toList());
            var sendMediaGroup = new SendMediaGroup(chatId, mediaResources);
            execute(sendMediaGroup);
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
